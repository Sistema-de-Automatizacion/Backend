package com.automatization.comunications.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.automatization.comunications.model.entity.Contract;

public interface IRepositoryContract extends JpaRepository<Contract, String> {

    @Query(value = "SELECT c.contrato, c.arrendador, c.TELULT, c.dia_canon, c.cuota, c.estado_semana, c.deuda_cli, c.placa, c.fecha_semanal, c.saldo"
    + " FROM db_packgps.vw_sv_all_motos_semanal AS c"
    + " INNER JOIN ("
    + "   SELECT contrato, MAX(fecha_semanal) AS last_semana"
    + "   FROM db_packgps.vw_sv_all_motos_semanal"
    + "   WHERE fecha_semanal <= CURRENT_DATE"
    + "   GROUP BY contrato"
    + " ) latest ON c.contrato = latest.contrato AND c.fecha_semanal = latest.last_semana", nativeQuery = true)
    public List<Object[]> findAllPayoutAndContract();

    @Query(value = "SELECT c.contrato, c.arrendador, c.TELULT, c.cuota, p.Recaudo, c.placa, p.FECHA"
    + " FROM db_packgps.vw_gd_recaudo_bruto AS p"
    + " INNER JOIN db_packgps.vw_sv_all_motos_semanal AS c ON c.contrato = p.CONTRATO"
    + " INNER JOIN ("
    + "   SELECT contrato, MAX(fecha_semanal) AS last_semana"
    + "   FROM db_packgps.vw_sv_all_motos_semanal"
    + "   WHERE fecha_semanal <= CURRENT_DATE"
    + "   GROUP BY contrato"
    + " ) latest ON c.contrato = latest.contrato AND c.fecha_semanal = latest.last_semana"
    + " WHERE DATE(p.FECHA) BETWEEN :startDate AND :endDate"
    + "   AND p.Recaudo > 0"
    + "   AND (p.empresa IS NULL OR p.empresa <> 'RENTAYA_D')"
    + " ORDER BY p.FECHA DESC", nativeQuery = true)
    public List<Object[]> findClientsPaidBetween(@Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
}
