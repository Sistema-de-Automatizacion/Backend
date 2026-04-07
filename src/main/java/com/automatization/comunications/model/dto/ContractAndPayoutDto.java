package com.automatization.comunications.model.dto;

public record ContractAndPayoutDto(
        String id,
        String nameClient,
        String phoneNumber,
        double paymentContract,
        String paymentDay,
        Double paymentPayout,
        String StateWeek,
        String message,
        String date) {

}
