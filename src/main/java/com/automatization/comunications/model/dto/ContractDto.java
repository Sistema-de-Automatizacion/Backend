package com.automatization.comunications.model.dto;


public record ContractDto(
    String id, 
    String nameClient, 
    String phoneNumber,
    String message,
    String dateNotification
) {
} 
