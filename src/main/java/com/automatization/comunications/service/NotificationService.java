package com.automatization.comunications.service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.automatization.comunications.controller.NotificationController;
import com.automatization.comunications.model.ContractAndPayoutDto;
import com.automatization.comunications.model.Notification;
import com.automatization.comunications.model.NotificationDto;
import com.automatization.comunications.repository.IRepositoryContract;
import com.automatization.comunications.repository.IRepositoryNotification;

import ch.qos.logback.classic.Logger;

@Service
public class NotificationService implements INotificationService {

    private static final Logger log = (Logger) LoggerFactory.getLogger(NotificationController.class);

    private IRepositoryNotification repositoryNotification;
    private IRepositoryContract repositoryContract;

    public NotificationService(IRepositoryNotification repositoryNotification, IRepositoryContract repositoryContract) {
        this.repositoryNotification = repositoryNotification;
        this.repositoryContract = repositoryContract;
    }

    @Override
    public List<ContractAndPayoutDto> findContractNextTopay() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateNow = LocalDateTime.now(ZoneId.of("America/Bogota"));
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        List<ContractAndPayoutDto> contracts = repositoryContract.findAllPayoutAndContract().stream()
            .map(contract -> {
                String id = toStringValue(contract[0]);
                String nameClient = toStringValue(contract[1]);
                String phoneNumber = toStringValue(contract[2]);
                String paymentDay = toStringValue(contract[3]);
                double paymentContract = toDoubleValue(contract[4]);
                String stateWeek = toStringValue(contract[5]);
                Double paymentPayout = toNullableDoubleValue(contract[6]);
                double pendingBalance = paymentPayout == null ? paymentContract : paymentContract - paymentPayout;
                String date = dateNow.format(formatter);

                String message;
                if (paymentPayout == null) {
                    message = "Hola " + nameClient + ", recuerda que tienes un pago programado para el dia "
                         + paymentDay + " por un valor de $" + decimalFormat.format(paymentContract*1000)
                         +". Atentamente: Motos del Caribe Renting S.A.S.";
                } else {
                    message = "Hola " + nameClient + ", hemos recibido tu abono por valor de: $"
                        + decimalFormat.format(paymentPayout*1000)
                        + ". Tu saldo pendiente es de $" + decimalFormat.format(pendingBalance*1000)
                        + ". Gracias por tu pago, atentamente: Motos del Caribe Renting S.A.S.";
                }

                return new ContractAndPayoutDto(
                    id,
                    nameClient,
                    phoneNumber,
                    paymentContract,
                    paymentDay,
                    paymentPayout,
                    stateWeek,
                    message,
                    date
                );
            })
            .filter(contract -> !isJustified(contract.StateWeek()))
            .filter(contract -> contract.paymentPayout() == null
                || Double.compare(contract.paymentContract() - contract.paymentPayout(), 0d) != 0)
            .collect(Collectors.toList());
        return contracts;
    }

    @Override
    public List<Notification> findNotifications(String id) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean deleteNotification(String id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void saveNotification(NotificationDto notificationDto) {
        Notification notification = new Notification(
            null,
            notificationDto.numContract(),
            notificationDto.nameClient(),
            notificationDto.phoneNumber(),
            notificationDto.dayRemember()
        );
        repositoryNotification.save(notification);
        log.info("Notificacion guardada correctamente: " + notification);
    }

    private String toStringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private double toDoubleValue(Object value) {
        if (value == null) {
            return 0d;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private Double toNullableDoubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private boolean isJustified(String stateWeek) {
        return stateWeek != null && "JUSTIFICADO".equalsIgnoreCase(stateWeek.trim());
    }

}
