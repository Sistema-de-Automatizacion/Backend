package com.automatization.comunications.model.entity;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Immutable
@Table(name = "vw_gd_recaudo_bruto", schema = "db_packgps")
public class Payout {

    @Column(name = "CONTRATO")
    @Id
    private String idPayout;

    @Column(name = "FECHA")
    private String paymentDay;

    @Column(name = "Recaudo")
    private double payment;

    @Column(name = "empresa")
    private String brachOffice;

}
