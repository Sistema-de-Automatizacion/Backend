package com.automatization.comunications.controller;

import java.util.List;

import com.automatization.comunications.model.entity.Notification;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.automatization.comunications.model.dto.ContractAndPayoutDto;

import com.automatization.comunications.model.dto.ErrorNotificationDto;
import com.automatization.comunications.model.dto.NotificationDto;
import com.automatization.comunications.service.INotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestParam;




@RestController
@Validated
@Tag(name = "NotificationController", description = "Controlador para gestionar las notificaciones de pago de los contratos")
public class NotificationController {

    private final INotificationService notificationService;

    public NotificationController(INotificationService notificationService) {
        this.notificationService = notificationService;
    }


    @PostMapping("save/notification")
    @Operation(summary = "Save a notification", description = "Guarda una notificación en la base de datos")
    public ResponseEntity<NotificationDto> save(@Valid @RequestBody NotificationDto notificationDto) {
        notificationService.saveNotification(notificationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationDto);
    }

    @PostMapping("save/error-notification")
    @Operation(summary = "Save an error notification", description = "Guarda una notificacion que no pudo ser enviada en la base de datos")
    public ResponseEntity<ErrorNotificationDto> saveErrorNotification(@Valid @RequestBody ErrorNotificationDto errorNotificationDto) {
        notificationService.saveErrorNotification(errorNotificationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(errorNotificationDto);
    }

    @GetMapping("contracts/next-to-pay")
    @Operation(summary = "Find contracts next to pay", description =
            "Obtiene una lista de contratos que están próximos a pagar con su respectivo mensaje de cobro")
    public ResponseEntity<List<ContractAndPayoutDto>> findContractsNextToPay() {
        return ResponseEntity.ok(notificationService.findContractNextTopay());
    }


    @GetMapping("get/notifications")
    @Operation(summary = "Find notifications", description = "Obtiene una lista de notificaciones por ID de contrato")
    public ResponseEntity<List<Notification>> findNotifications(
            @RequestParam
            @NotBlank(message = "El ID del contrato no puede estar vacío")
            @Pattern(regexp = "\\d+", message = "El ID del contrato debe contener solo dígitos")
            String id) {
        return ResponseEntity.ok(notificationService.findNotifications(id));
    }

}
