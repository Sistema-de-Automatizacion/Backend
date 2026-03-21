package com.automatization.comunications.model;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NotificationDto(

    @NotBlank(message = "El número de contrato no puede estar vacío")
    @Pattern(regexp = "\\d+", message = "El número de contrato debe contener solo dígitos")
    String numContract,

    @NotBlank(message = "El nombre del cliente no puede estar vacío")
    String nameClient,

    @NotBlank(message = "El número de teléfono no puede estar vacío")
    @Pattern(regexp = "\\d{10}", message = "El número de teléfono debe contener exactamente 10 dígitos")
    String phoneNumber,
    
    @NotBlank(message = "El día de recordatorio no puede estar vacío")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", message = "El día de recordatorio debe tener el formato YYYY-MM-DD HH:mm:ss")
    String dayRemember) {}
