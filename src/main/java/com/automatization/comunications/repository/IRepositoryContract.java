package com.automatization.comunications.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.automatization.comunications.model.entity.Contract;

public interface IRepositoryContract extends JpaRepository<Contract, String> {

    @Query(value = "SELECT c.contrato, c.arrendador, c.TELULT, c.dia_canon, c.cuota, c.estado_semana, p.Recaudo, c.deuda_cli, c.placa, c.fecha_semanal"
    + " FROM vw_sv_all_motos_semanal AS c "
    + " LEFT JOIN vw_gd_recaudo_bruto AS p ON c.contrato = p.CONTRATO", nativeQuery = true)
    public List<Object[]> findAllPayoutAndContract();

    @Query(value = "SELECT c.contrato, c.arrendador, c.TELULT, c.cuota, p.Recaudo, c.placa, p.FECHA"
    + " FROM vw_sv_all_motos_semanal AS c "
    + " INNER JOIN vw_gd_recaudo_bruto AS p ON c.contrato = p.CONTRATO"
    + " WHERE DATE(p.FECHA) = :date", nativeQuery = true)
    public List<Object[]> findClientsPaidByDate(@Param("date") LocalDate date);
}
