package com.automatization.comunications.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.automatization.comunications.model.Notification;

public interface IRepositoryNotification extends  JpaRepository<Notification, String>   {

}
