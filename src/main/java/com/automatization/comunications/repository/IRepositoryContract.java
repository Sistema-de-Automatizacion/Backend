package com.automatization.comunications.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.automatization.comunications.model.entity.Contract;

public interface IRepositoryContract extends JpaRepository<Contract, String> {

    @Query(value = "SELECT c.contrato, c.arrendador, c.TELULT, c.dia_canon, c.cuota, c.estado_semana, p.Recaudo, c.deuda_cli, c.placa"
    + " FROM vw_sv_all_motos_semanal AS c "
    + " LEFT JOIN vw_gd_recaudo_bruto AS p ON c.contrato = p.CONTRATO", nativeQuery = true)
    public List<Object[]> findAllPayoutAndContract();
}
