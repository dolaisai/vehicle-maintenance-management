package com.microservicesfinal.service.impl;

import com.microservicesfinal.dto.requests.CreateAlertRequest;
import com.microservicesfinal.dto.requests.CreateMaintenanceHistoryRequest;
import com.microservicesfinal.dto.requests.PatchAlertRequest;
import com.microservicesfinal.dto.responses.AlertResponse;
import com.microservicesfinal.exceptions.ResourceNotFoundException;
import com.microservicesfinal.model.Alert;
import com.microservicesfinal.model.enums.AlertStatus;
import com.microservicesfinal.model.enums.MaintenanceType;
import com.microservicesfinal.repository.AlertRepo;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AlertServiceImpl.
 *
 * Plain Mockito, no Spring context: every collaborator of this class is an
 * interface or a bean it only sends messages to, so a context would add
 * seconds of startup and unrelated failure modes without proving anything.
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceImplTest {

    @Mock
    private AlertRepo alertRepo;

    @Mock
    private KafkaTemplate<String, CreateMaintenanceHistoryRequest> kafkaTemplate;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AlertServiceImpl alertService;

    private static Alert alert(Integer id, AlertStatus status) {
        return Alert.builder()
                .id(id)
                .vehicleId(7)
                .vehicleMileage(42_000)
                .maintenanceType(MaintenanceType.OIL_CHANGE)
                .status(status)
                .createdOn(LocalDateTime.of(2026, 1, 1, 9, 0))
                .build();
    }

    @Nested
    class GetAlert {

        @Test
        @DisplayName("getAlert returns the stored alert mapped to a response")
        void getAlert_whenTheAlertExists_returnsItMappedToAResponse() {
            when(alertRepo.findById(7)).thenReturn(Optional.of(alert(7, AlertStatus.ACTIVE)));

            AlertResponse response = alertService.getAlert(7);

            assertEquals(7, response.getAlertId());
            assertEquals(MaintenanceType.OIL_CHANGE, response.getMaintenanceType());
            assertEquals(AlertStatus.ACTIVE, response.getStatus());
        }

        @Test
        @DisplayName("getAlert rejects an unknown id instead of returning null")
        void getAlert_whenTheAlertIsMissing_throwsResourceNotFoundNamingTheId() {
            when(alertRepo.findById(99)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown =
                    assertThrows(ResourceNotFoundException.class, () -> alertService.getAlert(99));

            assertTrue(thrown.getMessage().contains("99"), "message was: " + thrown.getMessage());
        }
    }

    @Nested
    class GetAllActiveAlerts {

        @Test
        @DisplayName("getAllActiveAlerts returns an empty list, not null, when nothing is active")
        void getAllActiveAlerts_whenNoAlertIsActive_returnsAnEmptyListNotNull() {
            when(alertRepo.findAllByStatus(AlertStatus.ACTIVE)).thenReturn(List.of());

            List<AlertResponse> responses = alertService.getAllActiveAlerts();

            assertNotNull(responses);
            assertTrue(responses.isEmpty());
        }

        @Test
        @DisplayName("getAllActiveAlerts maps every active alert and keeps the repository order")
        void getAllActiveAlerts_whenThreeAlertsAreActive_returnsAllThreeInRepositoryOrder() {
            when(alertRepo.findAllByStatus(AlertStatus.ACTIVE)).thenReturn(List.of(
                    alert(1, AlertStatus.ACTIVE),
                    alert(2, AlertStatus.ACTIVE),
                    alert(3, AlertStatus.ACTIVE)));

            List<AlertResponse> responses = alertService.getAllActiveAlerts();

            assertEquals(3, responses.size());
            assertEquals(1, responses.get(0).getAlertId());
            assertEquals(2, responses.get(1).getAlertId());
            assertEquals(3, responses.get(2).getAlertId());
        }
    }

    @Nested
    class GetAllAlerts {

        @Test
        @DisplayName("getAllAlerts returns an empty list when no alert exists")
        void getAllAlerts_whenNoAlertExists_returnsAnEmptyList() {
            when(alertRepo.findAll()).thenReturn(List.of());

            List<AlertResponse> responses = alertService.getAllAlerts();

            assertNotNull(responses);
            assertTrue(responses.isEmpty());
        }

        @Test
        @DisplayName("getAllAlerts does not filter by status, so completed alerts come back too")
        void getAllAlerts_whenAlertsOfBothStatusesExist_returnsTheCompletedOnesToo() {
            when(alertRepo.findAll()).thenReturn(List.of(
                    alert(1, AlertStatus.ACTIVE),
                    alert(2, AlertStatus.COMPLETED),
                    alert(3, AlertStatus.COMPLETED)));

            List<AlertResponse> responses = alertService.getAllAlerts();

            assertEquals(3, responses.size());
            assertEquals(AlertStatus.COMPLETED, responses.get(1).getStatus());
            assertEquals(AlertStatus.COMPLETED, responses.get(2).getStatus());
        }
    }

    @Nested
    class SaveAlert {

        private CreateAlertRequest request(AlertStatus status) {
            return CreateAlertRequest.builder()
                    .vehicleId(7)
                    .vehicleMileage(42_000)
                    .maintenanceType(MaintenanceType.OIL_CHANGE)
                    .status(status)
                    .build();
        }

        @Test
        @DisplayName("saveAlert returns the alert the repository persisted")
        void saveAlert_whenNoActiveAlertExists_returnsTheSavedAlert() {
            when(alertRepo.findByVehicleIdAndMaintenanceTypeAndStatus(
                    7, MaintenanceType.OIL_CHANGE, AlertStatus.ACTIVE)).thenReturn(Optional.empty());
            when(alertRepo.save(any(Alert.class))).thenReturn(alert(42, AlertStatus.ACTIVE));

            AlertResponse response = alertService.saveAlert(request(null));

            assertEquals(42, response.getAlertId());
            assertEquals(7, response.getVehicleId());
            assertEquals(AlertStatus.ACTIVE, response.getStatus());
        }

        @Test
        @DisplayName("saveAlert overrides the requested status, so a new alert is always ACTIVE")
        void saveAlert_whenTheRequestStatusIsCompleted_persistsTheAlertAsActive() {
            when(alertRepo.findByVehicleIdAndMaintenanceTypeAndStatus(
                    7, MaintenanceType.OIL_CHANGE, AlertStatus.ACTIVE)).thenReturn(Optional.empty());
            when(alertRepo.save(any(Alert.class))).thenReturn(alert(42, AlertStatus.ACTIVE));
            ArgumentCaptor<Alert> persisted = ArgumentCaptor.forClass(Alert.class);

            alertService.saveAlert(request(AlertStatus.COMPLETED));

            verify(alertRepo).save(persisted.capture());
            assertEquals(AlertStatus.ACTIVE, persisted.getValue().getStatus());
        }

        @Test
        @DisplayName("saveAlert stamps createdOn before persisting, rather than leaving it null")
        void saveAlert_whenANewAlertIsCreated_persistsItWithACreatedOnTimestamp() {
            when(alertRepo.findByVehicleIdAndMaintenanceTypeAndStatus(
                    7, MaintenanceType.OIL_CHANGE, AlertStatus.ACTIVE)).thenReturn(Optional.empty());
            when(alertRepo.save(any(Alert.class))).thenReturn(alert(42, AlertStatus.ACTIVE));
            ArgumentCaptor<Alert> persisted = ArgumentCaptor.forClass(Alert.class);

            alertService.saveAlert(request(null));

            verify(alertRepo).save(persisted.capture());
            assertNotNull(persisted.getValue().getCreatedOn());
        }

        @Test
        @DisplayName("saveAlert notifies by email with the maintenance type and vehicle in the message")
        void saveAlert_whenANewAlertIsCreated_sendsAnEmailNamingTheVehicleAndMaintenanceType() {
            when(alertRepo.findByVehicleIdAndMaintenanceTypeAndStatus(
                    7, MaintenanceType.OIL_CHANGE, AlertStatus.ACTIVE)).thenReturn(Optional.empty());
            when(alertRepo.save(any(Alert.class))).thenReturn(alert(42, AlertStatus.ACTIVE));
            ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

            alertService.saveAlert(request(null));

            verify(emailService).sendEmail(anyString(), subject.capture(), body.capture());
            assertEquals("Maintenance Due: OIL_CHANGE", subject.getValue());
            assertTrue(body.getValue().contains("vehicle ID: 7"), "body was: " + body.getValue());
        }

        @Test
        @DisplayName("saveAlert does not create a second alert while one is already active")
        void saveAlert_whenAnActiveAlertAlreadyExists_returnsTheExistingAlertWithoutSaving() {
            when(alertRepo.findByVehicleIdAndMaintenanceTypeAndStatus(
                    7, MaintenanceType.OIL_CHANGE, AlertStatus.ACTIVE))
                    .thenReturn(Optional.of(alert(13, AlertStatus.ACTIVE)));

            AlertResponse response = alertService.saveAlert(request(null));

            assertEquals(13, response.getAlertId());
            verify(alertRepo, never()).save(any(Alert.class));
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("saveAlert survives a mail outage and still returns the created alert")
        void saveAlert_whenTheEmailServiceThrows_stillReturnsTheSavedAlert() {
            when(alertRepo.findByVehicleIdAndMaintenanceTypeAndStatus(
                    7, MaintenanceType.OIL_CHANGE, AlertStatus.ACTIVE)).thenReturn(Optional.empty());
            when(alertRepo.save(any(Alert.class))).thenReturn(alert(42, AlertStatus.ACTIVE));
            doThrow(new RuntimeException("mail server down"))
                    .when(emailService).sendEmail(anyString(), anyString(), anyString());

            AlertResponse response = alertService.saveAlert(request(null));

            assertEquals(42, response.getAlertId());
        }
    }

    @Nested
    class PatchAlert {

        @Test
        @DisplayName("patchAlert rejects an unknown id")
        void patchAlert_whenTheAlertIsMissing_throwsResourceNotFoundNamingTheId() {
            when(alertRepo.findById(99)).thenReturn(Optional.empty());
            PatchAlertRequest request = PatchAlertRequest.builder().status(AlertStatus.COMPLETED).build();

            ResourceNotFoundException thrown = assertThrows(
                    ResourceNotFoundException.class, () -> alertService.patchAlert(request, 99));

            assertTrue(thrown.getMessage().contains("99"), "message was: " + thrown.getMessage());
        }

        @Test
        @DisplayName("patchAlert closes an active alert and records when it was closed")
        void patchAlert_whenTheStatusMovesToCompleted_returnsTheAlertCompletedWithAClosedOnTimestamp() {
            when(alertRepo.findById(7)).thenReturn(Optional.of(alert(7, AlertStatus.ACTIVE)));
            when(alertRepo.save(any(Alert.class))).thenAnswer(call -> call.getArgument(0));
            PatchAlertRequest request = PatchAlertRequest.builder().status(AlertStatus.COMPLETED).build();

            AlertResponse response = alertService.patchAlert(request, 7);

            assertEquals(AlertStatus.COMPLETED, response.getStatus());
            assertNotNull(response.getClosedOn());
        }

        @Test
        @DisplayName("patchAlert publishes a maintenance history event when an alert is completed")
        void patchAlert_whenTheStatusMovesToCompleted_publishesAMaintenanceHistoryEventOnCreateHistoryTopic() {
            when(alertRepo.findById(7)).thenReturn(Optional.of(alert(7, AlertStatus.ACTIVE)));
            when(alertRepo.save(any(Alert.class))).thenAnswer(call -> call.getArgument(0));
            PatchAlertRequest request = PatchAlertRequest.builder().status(AlertStatus.COMPLETED).build();
            ArgumentCaptor<CreateMaintenanceHistoryRequest> event =
                    ArgumentCaptor.forClass(CreateMaintenanceHistoryRequest.class);

            alertService.patchAlert(request, 7);

            verify(kafkaTemplate).send(eq("createHistoryTopic"), event.capture());
            assertEquals(7, event.getValue().getVehicleId());
            assertEquals(MaintenanceType.OIL_CHANGE, event.getValue().getMaintenanceType());
            assertEquals(42_000, event.getValue().getCompletedMileage());
        }

        @Test
        @DisplayName("patchAlert is idempotent: re-completing a completed alert publishes nothing")
        void patchAlert_whenTheAlertIsAlreadyCompleted_returnsItWithoutSavingOrPublishing() {
            when(alertRepo.findById(7)).thenReturn(Optional.of(alert(7, AlertStatus.COMPLETED)));
            PatchAlertRequest request = PatchAlertRequest.builder().status(AlertStatus.COMPLETED).build();

            AlertResponse response = alertService.patchAlert(request, 7);

            assertEquals(AlertStatus.COMPLETED, response.getStatus());
            verify(alertRepo, never()).save(any(Alert.class));
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        @DisplayName("patchAlert with status ACTIVE is a read, not a write")
        void patchAlert_whenTheRequestedStatusIsActive_leavesTheAlertActiveWithoutSavingOrPublishing() {
            when(alertRepo.findById(7)).thenReturn(Optional.of(alert(7, AlertStatus.ACTIVE)));
            PatchAlertRequest request = PatchAlertRequest.builder().status(AlertStatus.ACTIVE).build();

            AlertResponse response = alertService.patchAlert(request, 7);

            assertEquals(AlertStatus.ACTIVE, response.getStatus());
            verify(alertRepo, never()).save(any(Alert.class));
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        @DisplayName("patchAlert tolerates a request with no status and changes nothing")
        void patchAlert_whenTheRequestedStatusIsNull_leavesTheAlertUnchanged() {
            when(alertRepo.findById(7)).thenReturn(Optional.of(alert(7, AlertStatus.ACTIVE)));
            PatchAlertRequest request = PatchAlertRequest.builder().build();

            AlertResponse response = alertService.patchAlert(request, 7);

            assertEquals(AlertStatus.ACTIVE, response.getStatus());
            assertNull(response.getClosedOn());
        }

        @Test
        @DisplayName("patchAlert lets a broker failure reach the caller, so the transaction rolls back")
        void patchAlert_whenTheKafkaPublishFails_propagatesTheException() {
            when(alertRepo.findById(7)).thenReturn(Optional.of(alert(7, AlertStatus.ACTIVE)));
            when(alertRepo.save(any(Alert.class))).thenAnswer(call -> call.getArgument(0));
            when(kafkaTemplate.send(eq("createHistoryTopic"), any(CreateMaintenanceHistoryRequest.class)))
                    .thenThrow(new KafkaException("broker unavailable"));
            PatchAlertRequest request = PatchAlertRequest.builder().status(AlertStatus.COMPLETED).build();

            assertThrows(KafkaException.class, () -> alertService.patchAlert(request, 7));
        }
    }

    @Nested
    class DeleteAlert {

        @Test
        @DisplayName("deleteAlert deletes the alert with the given id")
        void deleteAlert_whenTheAlertExists_deletesItById() {
            alertService.deleteAlert(7);

            verify(alertRepo).deleteById(7);
        }

        @Test
        @DisplayName("deleteAlert translates the Spring empty-result error into ResourceNotFound")
        void deleteAlert_whenTheAlertIsMissing_throwsResourceNotFoundNamingTheId() {
            doThrow(new EmptyResultDataAccessException(1)).when(alertRepo).deleteById(99);

            ResourceNotFoundException thrown =
                    assertThrows(ResourceNotFoundException.class, () -> alertService.deleteAlert(99));

            assertTrue(thrown.getMessage().contains("99"), "message was: " + thrown.getMessage());
        }
    }
}
