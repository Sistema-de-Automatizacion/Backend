package com.automatization.comunications.model;


public record ContractDto(
    String id, 
    String nameClient, 
    String phoneNumber,
    String message,
    String dateNotification
) {
} 
