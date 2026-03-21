package com.automatization.comunications.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.automatization.comunications.model.Contract;

public interface IRepositoryContract extends JpaRepository<Contract, String> {

}
