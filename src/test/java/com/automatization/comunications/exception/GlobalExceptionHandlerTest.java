package com.automatization.comunications.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesBeanValidationErrorsAsBadRequestWithFieldMap() {
        BindingResult binding = mock(BindingResult.class);
        when(binding.getFieldErrors()).thenReturn(List.of(
                new FieldError("notificationDto", "numContract", "no puede estar vacío"),
                new FieldError("notificationDto", "phoneNumber",  "formato inválido")));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(binding);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsEntry("status", 400);
        assertThat(body).containsEntry("message", "Validation failed");
        assertThat(body).containsKey("details");
        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) body.get("details");
        assertThat(details).containsEntry("numContract", "no puede estar vacío");
        assertThat(details).containsEntry("phoneNumber", "formato inválido");
    }

    @Test
    void handlesConstraintViolationAsBadRequest() {
        ConstraintViolationException ex = new ConstraintViolationException("id must match \\d+", null);

        ResponseEntity<Map<String, Object>> response = handler.handleConstraint(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Constraint violation");
        assertThat(response.getBody().get("details").toString()).contains("id must match");
    }

    @Test
    void handlesMissingRequestParameterAsBadRequest() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("id", "String");

        ResponseEntity<Map<String, Object>> response = handler.handleMissingParam(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Missing request parameter");
        assertThat(response.getBody().get("details").toString())
                .contains("id")
                .contains("String");
    }

    @Test
    void handlesTypeMismatchAsBadRequest() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("not-a-number", Integer.class, "page", null, null);

        ResponseEntity<Map<String, Object>> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Invalid parameter type");
        assertThat(response.getBody().get("details").toString())
                .contains("page")
                .contains("not-a-number");
    }

    @Test
    void handlesDatabaseFailuresAsServiceUnavailable() {
        DataAccessResourceFailureException ex =
                new DataAccessResourceFailureException("connection refused");

        ResponseEntity<Map<String, Object>> response = handler.handleDatabase(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("status", 503);
        assertThat(response.getBody()).containsEntry("message", "Database unavailable");
        // No exponer el mensaje interno al cliente
        assertThat(response.getBody()).doesNotContainKey("details");
    }

    @Test
    void handlesGenericExceptionAsInternalServerError() {
        RuntimeException ex = new RuntimeException("boom");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
        assertThat(response.getBody()).containsEntry("message", "Internal server error");
    }

    @Test
    void responseBodyAlwaysIncludesTimestampAndErrorCode() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneric(new RuntimeException("x"));

        assertThat(response.getBody()).containsKeys("timestamp", "status", "error", "message");
        assertThat(response.getBody().get("error")).isEqualTo("Internal Server Error");
    }
}
