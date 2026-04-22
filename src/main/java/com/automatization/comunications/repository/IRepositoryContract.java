package com.automatization.comunications.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.automatization.comunications.model.entity.Contract;

public interface IRepositoryContract extends JpaRepository<Contract, String> {

    @Query(value = "SELECT c.contrato, c.arrendador, c.TELULT, c.dia_canon, c.cuota, c.estado_semana, c.deuda_cli, c.placa, c.fecha_semanal"
    + " FROM db_packgps.vw_sv_all_motos_semanal AS c"
    + " INNER JOIN ("
    + "   SELECT contrato, MAX(fecha_semanal) AS last_semana"
    + "   FROM db_packgps.vw_sv_all_motos_semanal"
    + "   WHERE fecha_semanal <= CURRENT_DATE"
    + "   GROUP BY contrato"
    + " ) latest ON c.contrato = latest.contrato AND c.fecha_semanal = latest.last_semana", nativeQuery = true)
    public List<Object[]> findAllPayoutAndContract();

    @Query(value = "SELECT c.contrato, c.arrendador, c.TELULT, c.cuota, p.Recaudo, c.placa, p.FECHA"
    + " FROM db_packgps.vw_sv_all_motos_semanal AS c "
    + " INNER JOIN db_packgps.vw_gd_recaudo_bruto AS p ON c.contrato = p.CONTRATO"
    + " WHERE DATE(p.FECHA) = :date"
    + "   AND p.Recaudo > 0"
    + "   AND (p.empresa IS NULL OR p.empresa <> 'RENTAYA_D')", nativeQuery = true)
    public List<Object[]> findClientsPaidByDate(@Param("date") LocalDate date);
}
