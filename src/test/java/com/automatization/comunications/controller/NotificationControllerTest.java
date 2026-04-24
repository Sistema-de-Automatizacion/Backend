package com.automatization.comunications.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.automatization.comunications.exception.GlobalExceptionHandler;
import com.automatization.comunications.model.dto.ContractAndPayoutDto;
import com.automatization.comunications.model.entity.ErrorNotification;
import com.automatization.comunications.model.entity.Notification;
import com.automatization.comunications.service.INotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(NotificationController.class)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper json = new ObjectMapper();

    @MockitoBean private INotificationService service;

    // ---------------- /contracts/next-to-pay ----------------

    @Test
    void nextToPayReturnsListFromService() throws Exception {
        ContractAndPayoutDto dto = new ContractAndPayoutDto(
                "1001", "Juan", "3000000000",
                220_000d, "Martes", null, "", "2026-04-22 10:00:00",
                220_000d, 0d, "Recordatorio de pago...");
        given(service.findContractNextTopay()).willReturn(List.of(dto));

        mockMvc.perform(get("/contracts/next-to-pay"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].id").value("1001"))
               .andExpect(jsonPath("$[0].accumulatedDebt").value(220_000d));
    }

    // ---------------- /contracts/paid-this-week ----------------

    @Test
    void paidThisWeekReturnsListFromService() throws Exception {
        ContractAndPayoutDto dto = new ContractAndPayoutDto(
                "2001", "Ana", "3000000001",
                200_000d, "21/04/2026", 200_000d, null, "2026-04-22 10:00:00",
                0d, 0d, "Hola Ana, hemos recibido tu pago...");
        given(service.findClientsPaidThisWeek()).willReturn(List.of(dto));

        mockMvc.perform(get("/contracts/paid-this-week"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$[0].id").value("2001"))
               .andExpect(jsonPath("$[0].paymentPayout").value(200_000d));
    }

    // ---------------- /get/notifications?id=... ----------------

    @Test
    void getNotificationsReturnsOkWhenIdIsNumeric() throws Exception {
        given(service.findNotifications("123")).willReturn(List.of());

        mockMvc.perform(get("/get/notifications").param("id", "123"))
               .andExpect(status().isOk());
    }

    @Test
    void getNotificationsReturns400WhenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/get/notifications").param("id", "abc"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void getNotificationsReturns400WhenIdIsMissing() throws Exception {
        mockMvc.perform(get("/get/notifications"))
               .andExpect(status().isBadRequest());
    }

    // ---------------- /notifications/all ----------------

    @Test
    void allNotificationsReturnsOkWithDefaultPaging() throws Exception {
        given(service.findAllNotifications(any(Pageable.class)))
                .willReturn(new PageImpl<Notification>(List.of()));

        mockMvc.perform(get("/notifications/all"))
               .andExpect(status().isOk())
               .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void allNotificationsRejectsNegativePage() throws Exception {
        mockMvc.perform(get("/notifications/all").param("page", "-1"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void allNotificationsRejectsSizeAboveMax() throws Exception {
        mockMvc.perform(get("/notifications/all").param("size", "500"))
               .andExpect(status().isBadRequest());
    }

    @Test
    void allErrorNotificationsReturnsOk() throws Exception {
        given(service.findAllErrorNotifications(any(Pageable.class)))
                .willReturn(new PageImpl<ErrorNotification>(List.of()));

        mockMvc.perform(get("/notifications/errors/all"))
               .andExpect(status().isOk());
    }

    // ---------------- /save/notification ----------------

    @Test
    void saveNotificationReturns201WhenBodyIsValid() throws Exception {
        var body = new com.automatization.comunications.model.dto.NotificationDto(
                "1001", "Juan", "3000000000", "2026-04-22 10:00:00");

        mockMvc.perform(post("/save/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
               .andExpect(status().isCreated());

        verify(service).saveNotification(body);
    }

    @Test
    void saveNotificationReturns400WhenContractIsBlank() throws Exception {
        var body = new com.automatization.comunications.model.dto.NotificationDto(
                "", "Juan", "3000000000", "2026-04-22 10:00:00");

        mockMvc.perform(post("/save/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void saveNotificationReturns400WhenPhoneIsNot10Digits() throws Exception {
        var body = new com.automatization.comunications.model.dto.NotificationDto(
                "1001", "Juan", "12345", "2026-04-22 10:00:00");

        mockMvc.perform(post("/save/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
               .andExpect(status().isBadRequest());
    }

    // ---------------- /save/error-notification ----------------

    @Test
    void saveErrorNotificationReturns201WhenBodyIsValid() throws Exception {
        var body = new com.automatization.comunications.model.dto.ErrorNotificationDto(
                "1001", "Juan", "3000000000", "2026-04-22 10:00:00", "SMTP 500");

        mockMvc.perform(post("/save/error-notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
               .andExpect(status().isCreated());

        verify(service).saveErrorNotification(body);
    }
}
