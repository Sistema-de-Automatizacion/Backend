package com.automatization.comunications.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.automatization.comunications.model.entity.ErrorNotification;

public interface IRepositoryErrorNotification extends JpaRepository<ErrorNotification, Long> {

}
