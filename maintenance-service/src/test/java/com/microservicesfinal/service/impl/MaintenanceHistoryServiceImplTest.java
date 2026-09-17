package com.microservicesfinal.service.impl;

import com.microservicesfinal.dto.requests.CreateMaintenanceHistoryRequest;
import com.microservicesfinal.dto.responses.MaintenanceHistoryResponse;
import com.microservicesfinal.exceptions.ResourceNotFoundException;
import com.microservicesfinal.model.MaintenanceHistory;
import com.microservicesfinal.model.enums.MaintenanceType;
import com.microservicesfinal.repository.MaintenanceHistoryRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the only class in maintenance-service that makes decisions.
 *
 * No Spring context, no database, no Kafka: the repository is a Mockito mock,
 * so a red test here means this service's logic is wrong and nothing else.
 */
@ExtendWith(MockitoExtension.class)
class MaintenanceHistoryServiceImplTest {

    @Mock
    private MaintenanceHistoryRepo maintenanceHistoryRepo;

    @InjectMocks
    private MaintenanceHistoryServiceImpl maintenanceHistoryService;

    private MaintenanceHistory storedHistory(Integer id,
                                             Integer vehicleId,
                                             MaintenanceType type,
                                             Integer mileage,
                                             LocalDateTime completedDate) {
        return MaintenanceHistory.builder()
                .id(id)
                .vehicleId(vehicleId)
                .maintenanceType(type)
                .completedMileage(mileage)
                .completedDate(completedDate)
                .build();
    }

    @Nested
    @DisplayName("getMaintenanceHistory")
    class GetMaintenanceHistory {

        @Test
        @DisplayName("returns the stored record with every field mapped onto the response")
        void getMaintenanceHistory_whenTheRecordExists_returnsItWithEveryFieldMapped() {
            LocalDateTime completedDate = LocalDateTime.of(2026, 3, 14, 9, 30);
            MaintenanceHistory stored =
                    storedHistory(7, 42, MaintenanceType.OIL_CHANGE, 55_000, completedDate);
            when(maintenanceHistoryRepo.findById(7)).thenReturn(Optional.of(stored));

            MaintenanceHistoryResponse response = maintenanceHistoryService.getMaintenanceHistory(7);

            assertEquals(7, response.getMaintenanceHistoryId());
            assertEquals(42, response.getVehicleId());
            assertEquals(MaintenanceType.OIL_CHANGE, response.getMaintenanceType());
            assertEquals(55_000, response.getCompletedMileage());
            assertEquals(completedDate, response.getCompletedDate());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException naming the id when the record is missing")
        void getMaintenanceHistory_whenTheRecordIsMissing_throwsResourceNotFoundNamingTheId() {
            when(maintenanceHistoryRepo.findById(99)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(
                    ResourceNotFoundException.class,
                    () -> maintenanceHistoryService.getMaintenanceHistory(99));

            assertTrue(thrown.getMessage().contains("99"),
                    "the message must name the id the caller asked for, but was: " + thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("getAllMaintenanceHistory")
    class GetAllMaintenanceHistory {

        @Test
        @DisplayName("returns an empty list, not null, when the repository holds nothing")
        void getAllMaintenanceHistory_whenNoRecordsExist_returnsEmptyListNotNull() {
            when(maintenanceHistoryRepo.findAll()).thenReturn(Collections.emptyList());

            List<MaintenanceHistoryResponse> responses = maintenanceHistoryService.getAllMaintenanceHistory();

            assertNotNull(responses);
            assertEquals(0, responses.size());
        }

        @Test
        @DisplayName("returns all three stored records in the order the repository gave them")
        void getAllMaintenanceHistory_whenThreeRecordsExist_returnsAllThreeInRepositoryOrder() {
            when(maintenanceHistoryRepo.findAll()).thenReturn(List.of(
                    storedHistory(1, 42, MaintenanceType.OIL_CHANGE, 30_000, LocalDateTime.of(2026, 1, 5, 8, 0)),
                    storedHistory(2, 42, MaintenanceType.TIRE_ROTATION, 40_000, LocalDateTime.of(2026, 2, 5, 8, 0)),
                    storedHistory(3, 43, MaintenanceType.SPARK_PLUGS, 50_000, LocalDateTime.of(2026, 3, 5, 8, 0))));

            List<MaintenanceHistoryResponse> responses = maintenanceHistoryService.getAllMaintenanceHistory();

            assertEquals(3, responses.size());
            assertEquals(1, responses.get(0).getMaintenanceHistoryId());
            assertEquals(2, responses.get(1).getMaintenanceHistoryId());
            assertEquals(3, responses.get(2).getMaintenanceHistoryId());
        }
    }

    @Nested
    @DisplayName("getAllMaintenanceHistoryByVehicleId")
    class GetAllMaintenanceHistoryByVehicleId {

        @Test
        @DisplayName("returns an empty list, not null, when the vehicle has no records")
        void getAllMaintenanceHistoryByVehicleId_whenVehicleHasNoRecords_returnsEmptyListNotNull() {
            when(maintenanceHistoryRepo.findAllByVehicleId(42)).thenReturn(Collections.emptyList());

            List<MaintenanceHistoryResponse> responses =
                    maintenanceHistoryService.getAllMaintenanceHistoryByVehicleId(42);

            assertNotNull(responses);
            assertEquals(0, responses.size());
        }

        @Test
        @DisplayName("returns the vehicle's single record")
        void getAllMaintenanceHistoryByVehicleId_whenVehicleHasOneRecord_returnsThatOneRecord() {
            when(maintenanceHistoryRepo.findAllByVehicleId(42)).thenReturn(List.of(
                    storedHistory(11, 42, MaintenanceType.CABIN_FILTER, 22_500,
                            LocalDateTime.of(2026, 4, 1, 12, 0))));

            List<MaintenanceHistoryResponse> responses =
                    maintenanceHistoryService.getAllMaintenanceHistoryByVehicleId(42);

            assertEquals(1, responses.size());
            assertEquals(11, responses.get(0).getMaintenanceHistoryId());
            assertEquals(MaintenanceType.CABIN_FILTER, responses.get(0).getMaintenanceType());
        }
    }

    @Nested
    @DisplayName("getAllRecentMaintenanceHistoryForVehicleWithId")
    class GetAllRecentMaintenanceHistoryForVehicleWithId {

        @Test
        @DisplayName("returns a placeholder row for every maintenance type when the vehicle has never been serviced")
        void getAllRecentMaintenanceHistoryForVehicleWithId_whenVehicleHasNoRecords_returnsAPlaceholderForEveryType() {
            stubLatest(MaintenanceType.OIL_CHANGE, Optional.empty());
            stubLatest(MaintenanceType.TIRE_ROTATION, Optional.empty());
            stubLatest(MaintenanceType.CABIN_FILTER, Optional.empty());
            stubLatest(MaintenanceType.SPARK_PLUGS, Optional.empty());

            List<MaintenanceHistoryResponse> responses =
                    maintenanceHistoryService.getAllRecentMaintenanceHistoryForVehicleWithId(42);

            assertEquals(4, responses.size());
            assertEquals(42, responses.get(0).getVehicleId());
            assertEquals(MaintenanceType.OIL_CHANGE, responses.get(0).getMaintenanceType());
            assertNull(responses.get(0).getCompletedDate());
            assertNull(responses.get(0).getCompletedMileage());
            assertNull(responses.get(0).getMaintenanceHistoryId());
        }

        @Test
        @DisplayName("returns one entry per maintenance type, in enum declaration order, when every type has a record")
        void getAllRecentMaintenanceHistoryForVehicleWithId_whenEveryTypeHasARecord_returnsExactlyOneEntryPerType() {
            stubLatest(MaintenanceType.OIL_CHANGE, Optional.of(
                    storedHistory(1, 42, MaintenanceType.OIL_CHANGE, 30_000, LocalDateTime.of(2026, 1, 5, 8, 0))));
            stubLatest(MaintenanceType.TIRE_ROTATION, Optional.of(
                    storedHistory(2, 42, MaintenanceType.TIRE_ROTATION, 31_000, LocalDateTime.of(2026, 2, 5, 8, 0))));
            stubLatest(MaintenanceType.CABIN_FILTER, Optional.of(
                    storedHistory(3, 42, MaintenanceType.CABIN_FILTER, 32_000, LocalDateTime.of(2026, 3, 5, 8, 0))));
            stubLatest(MaintenanceType.SPARK_PLUGS, Optional.of(
                    storedHistory(4, 42, MaintenanceType.SPARK_PLUGS, 33_000, LocalDateTime.of(2026, 4, 5, 8, 0))));

            List<MaintenanceHistoryResponse> responses =
                    maintenanceHistoryService.getAllRecentMaintenanceHistoryForVehicleWithId(42);

            assertEquals(4, responses.size());
            assertEquals(MaintenanceType.OIL_CHANGE, responses.get(0).getMaintenanceType());
            assertEquals(MaintenanceType.TIRE_ROTATION, responses.get(1).getMaintenanceType());
            assertEquals(MaintenanceType.CABIN_FILTER, responses.get(2).getMaintenanceType());
            assertEquals(MaintenanceType.SPARK_PLUGS, responses.get(3).getMaintenanceType());
        }

        @Test
        @DisplayName("keeps stored records and placeholders side by side, each in its own type's slot")
        void getAllRecentMaintenanceHistoryForVehicleWithId_whenSomeTypesHaveRecords_mixesStoredRecordsWithPlaceholders() {
            stubLatest(MaintenanceType.OIL_CHANGE, Optional.of(
                    storedHistory(1, 42, MaintenanceType.OIL_CHANGE, 30_000, LocalDateTime.of(2026, 1, 5, 8, 0))));
            stubLatest(MaintenanceType.TIRE_ROTATION, Optional.empty());
            stubLatest(MaintenanceType.CABIN_FILTER, Optional.empty());
            stubLatest(MaintenanceType.SPARK_PLUGS, Optional.of(
                    storedHistory(4, 42, MaintenanceType.SPARK_PLUGS, 33_000, LocalDateTime.of(2026, 4, 5, 8, 0))));

            List<MaintenanceHistoryResponse> responses =
                    maintenanceHistoryService.getAllRecentMaintenanceHistoryForVehicleWithId(42);

            assertEquals(30_000, responses.get(0).getCompletedMileage());
            assertNull(responses.get(1).getCompletedMileage());
            assertNull(responses.get(2).getCompletedMileage());
            assertEquals(33_000, responses.get(3).getCompletedMileage());
        }

        private void stubLatest(MaintenanceType type, Optional<MaintenanceHistory> result) {
            when(maintenanceHistoryRepo
                    .findTopByVehicleIdAndMaintenanceTypeOrderByCompletedDateDesc(42, type))
                    .thenReturn(result);
        }
    }

    @Nested
    @DisplayName("saveMaintenanceHistory")
    class SaveMaintenanceHistory {

        @Test
        @DisplayName("persists the vehicle id, type and mileage taken from the request")
        void saveMaintenanceHistory_whenCalled_persistsTheRequestFieldsOnTheEntity() {
            CreateMaintenanceHistoryRequest request = CreateMaintenanceHistoryRequest.builder()
                    .vehicleId(42)
                    .maintenanceType(MaintenanceType.OIL_CHANGE)
                    .completedMileage(60_000)
                    .build();
            when(maintenanceHistoryRepo.save(any(MaintenanceHistory.class)))
                    .thenAnswer(call -> call.getArgument(0));

            maintenanceHistoryService.saveMaintenanceHistory(request);

            ArgumentCaptor<MaintenanceHistory> saved = ArgumentCaptor.forClass(MaintenanceHistory.class);
            verify(maintenanceHistoryRepo).save(saved.capture());
            assertEquals(42, saved.getValue().getVehicleId());
            assertEquals(MaintenanceType.OIL_CHANGE, saved.getValue().getMaintenanceType());
            assertEquals(60_000, saved.getValue().getCompletedMileage());
        }

        @Test
        @DisplayName("stamps completedDate itself, because the request does not carry one")
        void saveMaintenanceHistory_whenCalled_stampsTheCompletedDateOnTheSavedEntity() {
            CreateMaintenanceHistoryRequest request = CreateMaintenanceHistoryRequest.builder()
                    .vehicleId(42)
                    .maintenanceType(MaintenanceType.TIRE_ROTATION)
                    .completedMileage(60_000)
                    .build();
            when(maintenanceHistoryRepo.save(any(MaintenanceHistory.class)))
                    .thenAnswer(call -> call.getArgument(0));

            MaintenanceHistoryResponse response = maintenanceHistoryService.saveMaintenanceHistory(request);

            assertNotNull(response.getCompletedDate(),
                    "the service must set completedDate; the create request has no such field");
        }
    }
}
