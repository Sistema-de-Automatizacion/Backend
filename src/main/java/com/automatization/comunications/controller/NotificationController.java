package com.automatization.comunications.controller;

import java.util.List;

import com.automatization.comunications.model.entity.Notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.RequestParam;




@RestController
@Tag(name = "NotificationController", description = "Controlador para gestionar las notificaciones de pago de los contratos")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final INotificationService notificationService;

    public NotificationController(INotificationService notificationService) {
        this.notificationService = notificationService;
    }


    @PostMapping("save/notification")
    @Operation(summary = "Save a notification", description = "Guarda una notificación en la base de datos")
    public ResponseEntity<NotificationDto> save(@Valid @RequestBody NotificationDto notificationDto) {
        try{
            notificationService.saveNotification(notificationDto);
            return ResponseEntity.ok(notificationDto);
        }
        catch(Exception e){
            return ResponseEntity.status(HttpStatus.CREATED).body(notificationDto);
        }
    
    }

    @PostMapping("save/error-notification")
    @Operation(summary = "Save an error notification", description = "Guarda una notificacion que no pudo ser enviada en la base de datos")
    public ResponseEntity<ErrorNotificationDto> saveErrorNotification(@Valid @RequestBody ErrorNotificationDto errorNotificationDto) {
        try{
            notificationService.saveErrorNotification(errorNotificationDto);
            return ResponseEntity.ok(errorNotificationDto);
        }
        catch(Exception e){
            return ResponseEntity.status(HttpStatus.CREATED).body(errorNotificationDto);
        }
    
    }

    @GetMapping("contracts/next-to-pay")
    @Operation(summary = "Find contracts next to pay", description = 
            "Obtiene una lista de contratos que están próximos a pagar con su respectivo mensaje de cobro")
    public ResponseEntity<List<ContractAndPayoutDto>> findContractsNextToPay() {
        try {
            List<ContractAndPayoutDto> contracts = notificationService.findContractNextTopay();
            return ResponseEntity.ok(contracts);
        } catch (Exception e) {
            log.error("Error al obtener los contratos próximos a pagar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    

    @GetMapping("get/notifications")
    @Operation(summary = "Find notifications", description = "Obtiene una lista de notificaciones por ID de contrato")
    public ResponseEntity<List<Notification>> findNotifications(@RequestParam String id) {
        try {
            List<Notification> notifications = notificationService.findNotifications(id);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Error al obtener las notificaciones", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
