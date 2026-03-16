package com.automatization.comunications.model;

import com.fasterxml.jackson.annotation.JsonProperty;


public record ContractDto(
    String id, 
    String nameClient, 
    String phoneNumber,
    String message,
    @JsonProperty("dateNotification")
    String dateNotification
) {
} 
