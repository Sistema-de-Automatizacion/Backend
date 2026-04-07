package com.automatization.comunications.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.automatization.comunications.model.entity.Notification;

public interface IRepositoryNotification extends  JpaRepository<Notification, Long>   {

    List<Notification> findByNumContract(String numContract);

}
