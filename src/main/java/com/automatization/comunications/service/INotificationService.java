package com.automatization.comunications.service;

import java.util.List;

import com.automatization.comunications.model.ContractAndPayoutDto;
import com.automatization.comunications.model.Notification;
import com.automatization.comunications.model.NotificationDto;

public interface INotificationService {
    public List<ContractAndPayoutDto> findContractNextTopay();
    public List<Notification> findNotifications(String id);
    public void saveNotification(NotificationDto notificationDto);
    public boolean deleteNotification(String id);
}
