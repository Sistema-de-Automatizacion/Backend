package com.automatization.comunications.service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.automatization.comunications.controller.NotificationController;
import com.automatization.comunications.model.ContractDto;
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
    public List<ContractDto> findContractNextTopay() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateNow = LocalDateTime.now();
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        List<ContractDto> contracts = repositoryContract.findAll().stream()
            .map(contract -> new ContractDto(
                contract.getId(),
                contract.getNameClient(),
                contract.getPhoneNumber(),
                "Recuerda que tu pago es el d\u00EDa " + contract.getPayDay() + " por un valor de $" + (decimalFormat.format(contract.getPayment() * 1000)),
                dateNow.format(formatter)
            ))
            .toList();
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

}
