package com.microservicesfinal.service.impl;

import com.microservicesfinal.clientRequest.ClientMaintenance;
import com.microservicesfinal.clientRequest.ClientVehicle;
import com.microservicesfinal.dto.requests.CreateAlertRequest;
import com.microservicesfinal.dto.responses.MaintenanceHistoryResponse;
import com.microservicesfinal.dto.responses.MaintenanceIntervalResponse;
import com.microservicesfinal.dto.responses.VehicleResponse;
import com.microservicesfinal.enums.AlertStatus;
import com.microservicesfinal.enums.MaintenanceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the rule evaluation logic.
 *
 * The rules live in the private evaluation(...) method, so they are exercised
 * through the two public entry points, syncOne and syncAll. The only observable
 * output of the class is the message it publishes to Kafka, so every assertion
 * is about that message: whether it was sent, how many were sent, and what they
 * carried.
 *
 * Two thresholds are under test, and they do not agree with each other:
 *   - mileage uses >=, so a vehicle is due AT the interval (inclusive)
 *   - the tire rotation date uses isBefore, so a vehicle is NOT due at the
 *     interval instant, only after it has passed (exclusive)
 * The boundary tests below pin both, one mile and one day on each side.
 */
@ExtendWith(MockitoExtension.class)
class RuleEvaluationServiceImplTest {

    private static final String ALERT_TOPIC = "createAlertTopic";
    private static final Integer VEHICLE_ID = 1;
    private static final String MAKE = "toyota";
    private static final String MODEL = "camry";

    @Mock
    private ClientVehicle clientVehicle;

    @Mock
    private ClientMaintenance clientMaintenance;

    @Mock
    private KafkaTemplate<String, CreateAlertRequest> kafkaTemplate;

    @InjectMocks
    private RuleEvaluationServiceImpl ruleEvaluationService;

    // region Z — Zero

    @Test
    @DisplayName("syncOne sends nothing when the vehicle has no recent history records")
    void syncOne_whenTheHistoryListIsEmpty_sendsNoAlert() {
        givenSyncOneReturns(vehicleWithMileage(500_000), oilChangeInterval(5_000), Collections.emptyList());

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate, never()).send(anyString(), any(CreateAlertRequest.class));
    }

    @Test
    @DisplayName("A record with no id means never serviced, so the whole odometer counts against the interval")
    void syncOne_whenTheVehicleIsNeverServicedForAnOilChange_treatsPastMileageAsZero() {
        MaintenanceHistoryResponse neverServiced = mileageRecord(MaintenanceType.OIL_CHANGE, null, null);
        givenSyncOneReturns(vehicleWithMileage(10_000), oilChangeInterval(10_000), List.of(neverServiced));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate).send(eq(ALERT_TOPIC), any(CreateAlertRequest.class));
    }

    @Test
    @DisplayName("syncOne stops before the maintenance calls when the vehicle service returns nothing")
    void syncOne_whenTheVehicleServiceReturnsNull_doesNotRequestTheMaintenanceInterval() {
        when(clientVehicle.requestSingleVehicle(VEHICLE_ID))
                .thenReturn(CompletableFuture.completedFuture(null));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(clientMaintenance, never()).requestSingleMaintenanceInterval(anyString(), anyString());
    }

    @Test
    @DisplayName("syncAll sends nothing when there are no vehicles at all")
    void syncAll_whenNoVehiclesExist_sendsNoAlert() {
        when(clientVehicle.requestAllVehicles())
                .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));

        ruleEvaluationService.syncAll();

        verify(kafkaTemplate, never()).send(anyString(), any(CreateAlertRequest.class));
    }

    // endregion

    // region O — One

    @Test
    @DisplayName("One overdue oil change record produces exactly one alert")
    void syncOne_whenOneOilChangeRecordIsOverdue_sendsExactlyOneAlert() {
        MaintenanceHistoryResponse lastOilChange = mileageRecord(MaintenanceType.OIL_CHANGE, 7, 30_000);
        givenSyncOneReturns(vehicleWithMileage(45_000), oilChangeInterval(10_000), List.of(lastOilChange));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate, times(1)).send(eq(ALERT_TOPIC), any(CreateAlertRequest.class));
    }

    @Test
    @DisplayName("One oil change record still inside its interval produces no alert")
    void syncOne_whenOneOilChangeRecordIsWithinTheInterval_sendsNoAlert() {
        MaintenanceHistoryResponse lastOilChange = mileageRecord(MaintenanceType.OIL_CHANGE, 7, 30_000);
        givenSyncOneReturns(vehicleWithMileage(33_000), oilChangeInterval(10_000), List.of(lastOilChange));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate, never()).send(anyString(), any(CreateAlertRequest.class));
    }

    // endregion

    // region M — Many

    @Test
    @DisplayName("Every overdue maintenance type raises its own alert, in record order")
    void syncOne_whenAllFourMaintenanceTypesAreOverdue_sendsOneAlertPerType() {
        VehicleResponse vehicle = VehicleResponse.builder()
                .vehicleId(VEHICLE_ID)
                .make(MAKE)
                .model(MODEL)
                .mileage(100_000)
                .createdAt(LocalDateTime.now().minusYears(5))
                .build();
        MaintenanceIntervalResponse interval = MaintenanceIntervalResponse.builder()
                .oilChangeIntervalMiles(5_000)
                .tireRotationIntervalMonths(6)
                .sparkPlugChangeIntervalMiles(30_000)
                .cabinFilterChangeIntervalMiles(20_000)
                .build();
        List<MaintenanceHistoryResponse> history = List.of(
                mileageRecord(MaintenanceType.OIL_CHANGE, null, null),
                tireRotationRecord(null, null),
                mileageRecord(MaintenanceType.SPARK_PLUGS, null, null),
                mileageRecord(MaintenanceType.CABIN_FILTER, null, null));
        givenSyncOneReturns(vehicle, interval, history);

        ruleEvaluationService.syncOne(VEHICLE_ID);

        ArgumentCaptor<CreateAlertRequest> alerts = ArgumentCaptor.forClass(CreateAlertRequest.class);
        verify(kafkaTemplate, times(4)).send(eq(ALERT_TOPIC), alerts.capture());
        assertEquals(MaintenanceType.OIL_CHANGE, alerts.getAllValues().get(0).getMaintenanceType());
        assertEquals(MaintenanceType.TIRE_ROTATION, alerts.getAllValues().get(1).getMaintenanceType());
        assertEquals(MaintenanceType.SPARK_PLUGS, alerts.getAllValues().get(2).getMaintenanceType());
        assertEquals(MaintenanceType.CABIN_FILTER, alerts.getAllValues().get(3).getMaintenanceType());
    }

    @Test
    @DisplayName("Only the overdue record among three raises an alert")
    void syncOne_whenThreeRecordsAndOnlyOneIsOverdue_sendsOnlyThatTypesAlert() {
        MaintenanceIntervalResponse interval = MaintenanceIntervalResponse.builder()
                .oilChangeIntervalMiles(10_000)
                .sparkPlugChangeIntervalMiles(30_000)
                .cabinFilterChangeIntervalMiles(20_000)
                .build();
        List<MaintenanceHistoryResponse> history = List.of(
                mileageRecord(MaintenanceType.OIL_CHANGE, 1, 44_000),
                mileageRecord(MaintenanceType.SPARK_PLUGS, 2, 10_000),
                mileageRecord(MaintenanceType.CABIN_FILTER, 3, 40_000));
        givenSyncOneReturns(vehicleWithMileage(45_000), interval, history);

        ruleEvaluationService.syncOne(VEHICLE_ID);

        ArgumentCaptor<CreateAlertRequest> alert = ArgumentCaptor.forClass(CreateAlertRequest.class);
        verify(kafkaTemplate, times(1)).send(eq(ALERT_TOPIC), alert.capture());
        assertEquals(MaintenanceType.SPARK_PLUGS, alert.getValue().getMaintenanceType());
    }

    @Test
    @DisplayName("syncAll skips the vehicle whose interval is missing and keeps going")
    void syncAll_whenOneOfThreeVehiclesHasNoInterval_stillEvaluatesTheOtherTwo() {
        VehicleResponse first = vehicleWithMileage(1, "toyota", "camry", 50_000);
        VehicleResponse second = vehicleWithMileage(2, "honda", "civic", 50_000);
        VehicleResponse third = vehicleWithMileage(3, "ford", "focus", 50_000);
        when(clientVehicle.requestAllVehicles())
                .thenReturn(CompletableFuture.completedFuture(List.of(first, second, third)));
        when(clientMaintenance.requestSingleMaintenanceInterval("toyota", "camry"))
                .thenReturn(CompletableFuture.completedFuture(oilChangeInterval(10_000)));
        when(clientMaintenance.requestSingleMaintenanceInterval("honda", "civic"))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(clientMaintenance.requestSingleMaintenanceInterval("ford", "focus"))
                .thenReturn(CompletableFuture.completedFuture(oilChangeInterval(10_000)));
        when(clientMaintenance.requestRecentVehicleHistoryRecords(anyInt()))
                .thenReturn(CompletableFuture.completedFuture(
                        List.of(mileageRecord(MaintenanceType.OIL_CHANGE, null, null))));

        ruleEvaluationService.syncAll();

        ArgumentCaptor<CreateAlertRequest> alerts = ArgumentCaptor.forClass(CreateAlertRequest.class);
        verify(kafkaTemplate, times(2)).send(eq(ALERT_TOPIC), alerts.capture());
        assertEquals(1, alerts.getAllValues().get(0).getVehicleId());
        assertEquals(3, alerts.getAllValues().get(1).getVehicleId());
    }

    // endregion

    // region B — Boundary

    @ParameterizedTest(name = "{0} miles against a {1} mile interval sends {2} alert(s)")
    @CsvSource({
            "9999,  10000, 0",
            "10000, 10000, 1",
            "10001, 10000, 1"
    })
    @DisplayName("The oil change threshold is inclusive: due at the interval, not one mile later")
    void syncOne_atTheOilChangeMileageBoundary_alertsOnceTheIntervalIsReached(
            int mileage, int intervalMiles, int expectedAlerts) {
        MaintenanceHistoryResponse neverServiced = mileageRecord(MaintenanceType.OIL_CHANGE, null, null);
        givenSyncOneReturns(vehicleWithMileage(mileage), oilChangeInterval(intervalMiles), List.of(neverServiced));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate, times(expectedAlerts)).send(eq(ALERT_TOPIC), any(CreateAlertRequest.class));
    }

    @ParameterizedTest(name = "odometer {0} after a service at 10000 miles sends {1} alert(s)")
    @CsvSource({
            "19999, 0",
            "20000, 1",
            "20001, 1"
    })
    @DisplayName("The oil change threshold counts miles since the last service, and is inclusive there too")
    void syncOne_atTheOilChangeMileageBoundaryAfterAPastService_subtractsTheCompletedMileage(
            int mileage, int expectedAlerts) {
        MaintenanceHistoryResponse lastOilChange = mileageRecord(MaintenanceType.OIL_CHANGE, 7, 10_000);
        givenSyncOneReturns(vehicleWithMileage(mileage), oilChangeInterval(10_000), List.of(lastOilChange));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate, times(expectedAlerts)).send(eq(ALERT_TOPIC), any(CreateAlertRequest.class));
    }

    @ParameterizedTest(name = "odometer {0} after a spark plug change at 20000 miles sends {1} alert(s)")
    @CsvSource({
            "49999, 0",
            "50000, 1",
            "50001, 1"
    })
    @DisplayName("The spark plug threshold is inclusive, and stays silent one mile short of it")
    void syncOne_atTheSparkPlugMileageBoundary_alertsOnceTheIntervalIsReached(
            int mileage, int expectedAlerts) {
        MaintenanceIntervalResponse interval = MaintenanceIntervalResponse.builder()
                .sparkPlugChangeIntervalMiles(30_000)
                .build();
        MaintenanceHistoryResponse lastChange = mileageRecord(MaintenanceType.SPARK_PLUGS, 9, 20_000);
        givenSyncOneReturns(vehicleWithMileage(mileage), interval, List.of(lastChange));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate, times(expectedAlerts)).send(eq(ALERT_TOPIC), any(CreateAlertRequest.class));
    }

    @ParameterizedTest(name = "odometer {0} after a cabin filter change at 25000 miles sends {1} alert(s)")
    @CsvSource({
            "44999, 0",
            "45000, 1",
            "45001, 1"
    })
    @DisplayName("The cabin filter threshold is inclusive, and stays silent one mile short of it")
    void syncOne_atTheCabinFilterMileageBoundary_alertsOnceTheIntervalIsReached(
            int mileage, int expectedAlerts) {
        MaintenanceIntervalResponse interval = MaintenanceIntervalResponse.builder()
                .cabinFilterChangeIntervalMiles(20_000)
                .build();
        MaintenanceHistoryResponse lastChange = mileageRecord(MaintenanceType.CABIN_FILTER, 9, 25_000);
        givenSyncOneReturns(vehicleWithMileage(mileage), interval, List.of(lastChange));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate, times(expectedAlerts)).send(eq(ALERT_TOPIC), any(CreateAlertRequest.class));
    }

    @Test
    @DisplayName("A tire rotation one day past its six month interval is overdue")
    void syncOne_whenTheLastTireRotationIsOneDayPastTheInterval_sendsTireRotationAlert() {
        LocalDateTime oneDayPastDue = LocalDateTime.now().minusMonths(6).minusDays(1);
        MaintenanceHistoryResponse lastRotation = tireRotationRecord(4, oneDayPastDue);
        givenSyncOneReturns(vehicleWithMileage(45_000), tireRotationInterval(6), List.of(lastRotation));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        ArgumentCaptor<CreateAlertRequest> alert = ArgumentCaptor.forClass(CreateAlertRequest.class);
        verify(kafkaTemplate).send(eq(ALERT_TOPIC), alert.capture());
        assertEquals(MaintenanceType.TIRE_ROTATION, alert.getValue().getMaintenanceType());
    }

    @Test
    @DisplayName("A tire rotation one day short of its six month interval is not yet overdue")
    void syncOne_whenTheLastTireRotationIsOneDayShortOfTheInterval_sendsNoAlert() {
        LocalDateTime oneDayBeforeDue = LocalDateTime.now().minusMonths(6).plusDays(1);
        MaintenanceHistoryResponse lastRotation = tireRotationRecord(4, oneDayBeforeDue);
        givenSyncOneReturns(vehicleWithMileage(45_000), tireRotationInterval(6), List.of(lastRotation));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate, never()).send(anyString(), any(CreateAlertRequest.class));
    }

    @Test
    @DisplayName("The tire rotation date check is strict: it fires as soon as the interval instant has passed")
    void syncOne_whenTheLastTireRotationIsOneSecondPastTheInterval_sendsTireRotationAlert() {
        LocalDateTime oneSecondPastDue = LocalDateTime.now().minusMonths(6).minusSeconds(1);
        MaintenanceHistoryResponse lastRotation = tireRotationRecord(4, oneSecondPastDue);
        givenSyncOneReturns(vehicleWithMileage(45_000), tireRotationInterval(6), List.of(lastRotation));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate).send(eq(ALERT_TOPIC), any(CreateAlertRequest.class));
    }

    @Test
    @DisplayName("A never rotated vehicle is measured from its creation date")
    void syncOne_whenTheVehicleWasNeverRotatedAndWasCreatedBeforeTheInterval_sendsTireRotationAlert() {
        VehicleResponse vehicle = vehicleCreatedAt(LocalDateTime.now().minusMonths(6).minusDays(1));
        MaintenanceHistoryResponse neverRotated = tireRotationRecord(null, null);
        givenSyncOneReturns(vehicle, tireRotationInterval(6), List.of(neverRotated));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate).send(eq(ALERT_TOPIC), any(CreateAlertRequest.class));
    }

    @Test
    @DisplayName("A never rotated vehicle younger than the interval raises nothing")
    void syncOne_whenTheVehicleWasNeverRotatedAndWasCreatedInsideTheInterval_sendsNoAlert() {
        VehicleResponse vehicle = vehicleCreatedAt(LocalDateTime.now().minusMonths(6).plusDays(1));
        MaintenanceHistoryResponse neverRotated = tireRotationRecord(null, null);
        givenSyncOneReturns(vehicle, tireRotationInterval(6), List.of(neverRotated));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate, never()).send(anyString(), any(CreateAlertRequest.class));
    }

    // endregion

    // region I — Interface

    @Test
    @DisplayName("An overdue rule publishes a CreateAlertRequest describing the vehicle on createAlertTopic")
    void syncOne_whenAnOilChangeIsOverdue_publishesCreateAlertRequestOnCreateAlertTopic() {
        MaintenanceHistoryResponse lastOilChange = mileageRecord(MaintenanceType.OIL_CHANGE, 7, 30_000);
        givenSyncOneReturns(vehicleWithMileage(45_000), oilChangeInterval(10_000), List.of(lastOilChange));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        ArgumentCaptor<CreateAlertRequest> alert = ArgumentCaptor.forClass(CreateAlertRequest.class);
        verify(kafkaTemplate).send(eq(ALERT_TOPIC), alert.capture());
        assertEquals(VEHICLE_ID, alert.getValue().getVehicleId());
        assertEquals(45_000, alert.getValue().getVehicleMileage());
        assertEquals(MaintenanceType.OIL_CHANGE, alert.getValue().getMaintenanceType());
        assertEquals(AlertStatus.ACTIVE, alert.getValue().getStatus());
    }

    // endregion

    // region E — Exceptional

    @Test
    @DisplayName("syncOne stops before the history call when the interval lookup fails")
    void syncOne_whenTheMaintenanceIntervalIsNull_doesNotRequestTheHistory() {
        when(clientVehicle.requestSingleVehicle(VEHICLE_ID))
                .thenReturn(CompletableFuture.completedFuture(vehicleWithMileage(45_000)));
        when(clientMaintenance.requestSingleMaintenanceInterval(MAKE, MODEL))
                .thenReturn(CompletableFuture.completedFuture(null));

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(clientMaintenance, never()).requestRecentVehicleHistoryRecords(anyInt());
    }

    @Test
    @DisplayName("syncOne swallows a failed history lookup instead of alerting")
    void syncOne_whenTheHistoryLookupReturnsNull_sendsNoAlert() {
        givenSyncOneReturns(vehicleWithMileage(45_000), oilChangeInterval(10_000), null);

        ruleEvaluationService.syncOne(VEHICLE_ID);

        verify(kafkaTemplate, never()).send(anyString(), any(CreateAlertRequest.class));
    }

    @Test
    @DisplayName("syncAll swallows the vehicle client fallback's null result")
    void syncAll_whenTheVehicleLookupReturnsNull_sendsNoAlert() {
        when(clientVehicle.requestAllVehicles())
                .thenReturn(CompletableFuture.completedFuture(null));

        ruleEvaluationService.syncAll();

        verify(kafkaTemplate, never()).send(anyString(), any(CreateAlertRequest.class));
    }

    @Test
    @DisplayName("A history record with no maintenance type reaches the switch unguarded and throws")
    void syncOne_whenAHistoryRecordHasNoMaintenanceType_throwsNullPointerException() {
        MaintenanceHistoryResponse untyped = MaintenanceHistoryResponse.builder()
                .maintenanceHistoryId(7)
                .vehicleId(VEHICLE_ID)
                .completedMileage(30_000)
                .build();
        givenSyncOneReturns(vehicleWithMileage(45_000), oilChangeInterval(10_000), List.of(untyped));

        assertThrows(NullPointerException.class, () -> ruleEvaluationService.syncOne(VEHICLE_ID));

        verify(kafkaTemplate, never()).send(anyString(), any(CreateAlertRequest.class));
    }

    // endregion

    // region fixtures

    private void givenSyncOneReturns(VehicleResponse vehicle,
                                     MaintenanceIntervalResponse interval,
                                     List<MaintenanceHistoryResponse> history) {
        when(clientVehicle.requestSingleVehicle(VEHICLE_ID))
                .thenReturn(CompletableFuture.completedFuture(vehicle));
        when(clientMaintenance.requestSingleMaintenanceInterval(vehicle.getMake(), vehicle.getModel()))
                .thenReturn(CompletableFuture.completedFuture(interval));
        when(clientMaintenance.requestRecentVehicleHistoryRecords(VEHICLE_ID))
                .thenReturn(CompletableFuture.completedFuture(history));
    }

    private static VehicleResponse vehicleWithMileage(int mileage) {
        return vehicleWithMileage(VEHICLE_ID, MAKE, MODEL, mileage);
    }

    private static VehicleResponse vehicleWithMileage(Integer vehicleId, String make, String model, int mileage) {
        return VehicleResponse.builder()
                .vehicleId(vehicleId)
                .make(make)
                .model(model)
                .mileage(mileage)
                .build();
    }

    private static VehicleResponse vehicleCreatedAt(LocalDateTime createdAt) {
        return VehicleResponse.builder()
                .vehicleId(VEHICLE_ID)
                .make(MAKE)
                .model(MODEL)
                .mileage(45_000)
                .createdAt(createdAt)
                .build();
    }

    private static MaintenanceIntervalResponse oilChangeInterval(Integer miles) {
        return MaintenanceIntervalResponse.builder()
                .oilChangeIntervalMiles(miles)
                .build();
    }

    private static MaintenanceIntervalResponse tireRotationInterval(Integer months) {
        return MaintenanceIntervalResponse.builder()
                .tireRotationIntervalMonths(months)
                .build();
    }

    private static MaintenanceHistoryResponse mileageRecord(MaintenanceType type,
                                                            Integer historyId,
                                                            Integer completedMileage) {
        return MaintenanceHistoryResponse.builder()
                .maintenanceHistoryId(historyId)
                .vehicleId(VEHICLE_ID)
                .maintenanceType(type)
                .completedMileage(completedMileage)
                .build();
    }

    private static MaintenanceHistoryResponse tireRotationRecord(Integer historyId, LocalDateTime completedDate) {
        return MaintenanceHistoryResponse.builder()
                .maintenanceHistoryId(historyId)
                .vehicleId(VEHICLE_ID)
                .maintenanceType(MaintenanceType.TIRE_ROTATION)
                .completedDate(completedDate)
                .build();
    }

    // endregion
}
