package com.automatization.comunications.service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.automatization.comunications.controller.NotificationController;
import com.automatization.comunications.model.dto.ContractAndPayoutDto;
import com.automatization.comunications.model.dto.ErrorNotificationDto;
import com.automatization.comunications.model.dto.NotificationDto;
import com.automatization.comunications.model.entity.ErrorNotification;
import com.automatization.comunications.model.entity.Notification;
import com.automatization.comunications.repository.IRepositoryContract;
import com.automatization.comunications.repository.IRepositoryErrorNotification;
import com.automatization.comunications.repository.IRepositoryNotification;

import ch.qos.logback.classic.Logger;

@Service
public class NotificationService implements INotificationService {

    private static final Logger log = (Logger) LoggerFactory.getLogger(NotificationController.class);

    private IRepositoryNotification repositoryNotification;
    private IRepositoryContract repositoryContract;
    private IRepositoryErrorNotification repositoryErrorNotification;

    public NotificationService(IRepositoryNotification repositoryNotification, IRepositoryContract repositoryContract, IRepositoryErrorNotification repositoryErrorNotification) {
        this.repositoryNotification = repositoryNotification;
        this.repositoryContract = repositoryContract;
        this.repositoryErrorNotification = repositoryErrorNotification;
    }

    @Override
    public List<ContractAndPayoutDto> findContractNextTopay() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateNow = LocalDateTime.now(ZoneId.of("America/Bogota"));
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        List<ContractAndPayoutDto> contracts = repositoryContract.findAllPayoutAndContract().stream()
            .map(contract -> {
                String id = toStringValue(contract[0]);
                String nameClient = toStringValue(contract[1]);
                String phoneNumber = toStringValue(contract[2]);
                String paymentDay = toStringValue(contract[3]);
                double paymentContract = toDoubleValue(contract[4]);
                String stateWeek = toStringValue(contract[5]);
                Double paymentPayout = toNullableDoubleValue(contract[6]);
                paymentPayout = paymentPayout != null ? paymentPayout * 1000 : null;
                String date = dateNow.format(formatter);
                double accumulatedDebt = toDoubleValue(contract[7])*1000;
                String licensePlate = toStringValue(contract[8]);

                String payDay = nameDay(paymentDay);
                double debt = accumulatedDebt - (paymentPayout != null ? paymentPayout : 0d);
                if(debt < 0) {
                    debt = 0;
                }

                String message = null;
                if(debt > 0){
                message = "Cuota vencida\n" + //
                                        "Hola " + nameClient + ", te informamos el estado actual de tu contrato de arrendamiento de la motocicleta con placa " + licensePlate + ":\n" + //
                                        "\n" + //
                                        "📌 Deuda acumulada a la fecha: $" + decimalFormat.format(debt) + " COP\n" + //
                                        "📌 Cuota de esta semana: $" + decimalFormat.format(paymentContract * 1000) + " COP\n" + //
                                        "📌 Total a pagar: $" + decimalFormat.format(accumulatedDebt) + " COP\n" + //
                                        "\n" + //
                                        "Te invitamos a ponerte al día con tus obligaciones para mantener vigente tu contrato y evitar intereses por mora adicionales.\n" + //
                                        "\n" + //
                                        "Si ya realizaste el pago, por favor haz caso omiso a este mensaje.\n" + //
                                        "\n" + //
                                        "Motos del Caribe Renting SAS\n" + //
                                        "Para soporte: +57 304 4558351";

                }
                if (paymentPayout == null) {
                    message = "Recordatorio de pago\n" + //
                                                "Hola " + nameClient + ", te recordamos que tu cuota de arrendamiento de la motocicleta con placa " + licensePlate + " por valor de $" + decimalFormat.format(paymentContract * 1000) + " COP vence el " + paymentDay + ".\n" + //
                                                "\n" + //
                                                "Realiza tu pago a tiempo para evitar intereses por mora.\n" + //
                                                "\n" + //
                                                "Gracias por confiar en Motos del Caribe Renting SAS.";
                } 
                return new ContractAndPayoutDto(
                    id,
                    nameClient,
                    phoneNumber,
                    paymentContract,
                    payDay,
                    paymentPayout,
                    stateWeek,
                    date,
                    accumulatedDebt,
                    debt,
                    message
                );
            })
            .filter(contract -> !isJustified(contract.StateWeek()))
            .filter(contract -> contract.paymentPayout() == null
                || Double.compare(contract.paymentContract() - contract.paymentPayout(), 0d) != 0)
            .collect(Collectors.toList());
        return contracts;
    }

    @Override
    public List<Notification> findNotifications(String id) {
        return repositoryNotification.findByNumContract(id);
    }

    @Override
    public Page<Notification> findAllNotifications(Pageable pageable) {
        return repositoryNotification.findAll(pageable);
    }

    @Override
    public Page<ErrorNotification> findAllErrorNotifications(Pageable pageable) {
        return repositoryErrorNotification.findAll(pageable);
    }


    @Override
    public boolean deleteNotification(Long id) {
        if (repositoryNotification.existsById(id)) {
            repositoryNotification.deleteById(id);
            log.info("Notificacion eliminada correctamente, id: " + id);
            return true;
        } else {
            log.warn("No se encontró la notificación con id: " + id);
            return false;
        }
    }

    @Override
    public void saveNotification(NotificationDto notificationDto) {
        Notification notification = new Notification(
            null,
            notificationDto.numContract(),
            notificationDto.nameClient(),
            notificationDto.phoneNumber(),
            notificationDto.dayRemember()
        );
        repositoryNotification.save(notification);
        log.info("Notificacion guardada correctamente: " + notification);
    }

    private String toStringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private double toDoubleValue(Object value) {
        if (value == null) {
            return 0d;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private Double toNullableDoubleValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private boolean isJustified(String stateWeek) {
        return stateWeek != null && "JUSTIFICADO".equalsIgnoreCase(stateWeek.trim());
    }

    @Override
    public void saveErrorNotification(ErrorNotificationDto errorNotificationDto) {
        ErrorNotification errorNotification = new ErrorNotification(
            null,
            errorNotificationDto.numContract(),
            errorNotificationDto.nameClient(),
            errorNotificationDto.phoneNumber(),
            errorNotificationDto.dayRemember(),
            errorNotificationDto.errorMessage()
        );
        repositoryErrorNotification.save(errorNotification);
        log.info("Notificacion de error guardada correctamente: " + errorNotification);
    }

    private String nameDay(String dayRemember) {
            if ("Mar".equals(dayRemember)) {
                return "Martes";
            }
            if ("Mier".equals(dayRemember)) {
                return "Miércoles";
            }
            if ("Jue".equals(dayRemember)) {
                return "Jueves";
            }
            if ("QUI".equals(dayRemember)) {
                return "Quincenal";
            }
        return dayRemember;
    }

}
