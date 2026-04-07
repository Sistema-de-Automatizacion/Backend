package com.automatization.comunications.service;

import java.util.List;

import com.automatization.comunications.model.dto.ContractAndPayoutDto;
import com.automatization.comunications.model.dto.ErrorNotificationDto;
import com.automatization.comunications.model.dto.NotificationDto;
import com.automatization.comunications.model.entity.Notification;

public interface INotificationService {
    public List<ContractAndPayoutDto> findContractNextTopay();
    public List<Notification> findNotifications(String id);
    public void saveNotification(NotificationDto notificationDto);
    public boolean deleteNotification(Long id);
    public void saveErrorNotification(ErrorNotificationDto errorNotificationDto);
}
