package com.automatization.comunications.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.automatization.comunications.model.dto.ContractAndPayoutDto;
import com.automatization.comunications.model.dto.ErrorNotificationDto;
import com.automatization.comunications.model.dto.NotificationDto;
import com.automatization.comunications.model.entity.ErrorNotification;
import com.automatization.comunications.model.entity.Notification;

public interface INotificationService {
    public List<ContractAndPayoutDto> findContractNextTopay();
    public List<Notification> findNotifications(String id);
    public Page<Notification> findAllNotifications(Pageable pageable);
    public Page<ErrorNotification> findAllErrorNotifications(Pageable pageable);
    public void saveNotification(NotificationDto notificationDto);
    public boolean deleteNotification(Long id);
    public void saveErrorNotification(ErrorNotificationDto errorNotificationDto);
}
