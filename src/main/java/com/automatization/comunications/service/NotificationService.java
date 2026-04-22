package com.automatization.comunications.service;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
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

    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DUE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String SUPPORT_LINE = "Para soporte: +57 304 4558351";

    private static final String MORA_TEMPLATE = """
            Cuota vencida
            Hola %s, te informamos el estado actual de tu contrato de arrendamiento de la motocicleta con placa %s:

            📌 Deuda acumulada a la fecha: $%d COP
            📌 Cuota de esta semana: $%d COP
            📌 Total a pagar: $%d COP

            Te invitamos a ponerte al día con tus obligaciones para mantener vigente tu contrato y evitar intereses por mora adicionales.

            Si ya realizaste el pago, por favor haz caso omiso a este mensaje.

            Motos del Caribe Renting SAS
            %s""";

    private static final String REMINDER_TEMPLATE = """
            Recordatorio de pago
            Hola %s , te recordamos que tu cuota de arrendamiento de la motocicleta con placa %s por valor de $%d COP vence el %s.

            Realiza tu pago a tiempo para evitar intereses por mora.

            Gracias por confiar en Motos del Caribe Renting SAS.
            %s""";

    private static final String PAYMENT_RECEIVED_TEMPLATE = """
            Hola %s, hemos recibido tu pago por valor de $%d COP correspondiente a la cuota de arrendamiento de la motocicleta con placa %s.

            Gracias por tu pago puntual y por confiar en Motos del Caribe Renting SAS. 🏍️
            %s""";

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
        String date = LocalDateTime.now(BOGOTA_ZONE).format(TIMESTAMP_FORMATTER);
        return repositoryContract.findAllPayoutAndContract().stream()
            .map(contract -> {
                String id = toStringValue(contract[0]);
                String nameClient = toStringValue(contract[1]);
                String phoneNumber = toStringValue(contract[2]);
                String paymentDay = toStringValue(contract[3]);
                double paymentContract = toDoubleValue(contract[4]) * 1000;
                String stateWeek = toStringValue(contract[5]);
                double totalToPay = toDoubleValue(contract[6]) * 1000;
                String licensePlate = toStringValue(contract[7]);
                LocalDate dueDate = computeClientDueDate(paymentDay);

                String payDay = nameDay(paymentDay);
                double carriedOverDebt = totalToPay - paymentContract;
                if (carriedOverDebt < 0) {
                    carriedOverDebt = 0;
                }

                String message = null;
                if (carriedOverDebt > 0) {
                    message = buildMoraMessage(nameClient, licensePlate, carriedOverDebt, paymentContract, totalToPay);
                } else if (totalToPay > 0) {
                    message = buildReminderMessage(nameClient, licensePlate, paymentContract, dueDate);
                }
                return new ContractAndPayoutDto(
                    id,
                    nameClient,
                    phoneNumber,
                    paymentContract,
                    payDay,
                    null,
                    stateWeek,
                    date,
                    totalToPay,
                    carriedOverDebt,
                    message
                );
            })
            .filter(contract -> contract.StateWeek() == null || contract.StateWeek().isBlank())
            .filter(contract -> contract.accumulatedDebt() > 0)
            .collect(Collectors.toList());
    }

    @Override
    public List<ContractAndPayoutDto> findClientsPaidThisWeek() {
        LocalDate today = LocalDate.now(BOGOTA_ZONE);
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        String date = LocalDateTime.now(BOGOTA_ZONE).format(TIMESTAMP_FORMATTER);
        return repositoryContract.findClientsPaidBetween(monday, sunday).stream()
            .map(row -> {
                String id = toStringValue(row[0]);
                String nameClient = toStringValue(row[1]);
                String phoneNumber = toStringValue(row[2]);
                double paymentContract = toDoubleValue(row[3]) * 1000;
                double paymentPayout = toDoubleValue(row[4]) * 1000;
                String licensePlate = toStringValue(row[5]);
                LocalDate paymentDate = toLocalDate(row[6]);
                String paymentDateLabel = paymentDate != null ? paymentDate.format(DUE_DATE_FORMATTER) : null;

                String message = buildPaymentReceivedMessage(nameClient, paymentPayout, licensePlate);

                return new ContractAndPayoutDto(
                    id,
                    nameClient,
                    phoneNumber,
                    paymentContract,
                    paymentDateLabel,
                    paymentPayout,
                    null,
                    date,
                    0d,
                    0d,
                    message
                );
            })
            .collect(Collectors.toList());
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

    private String buildMoraMessage(String nameClient, String licensePlate, double debt, double weeklyFee, double totalDue) {
        return MORA_TEMPLATE.formatted(
            nameClient,
            licensePlate,
            (long) debt,
            (long) weeklyFee,
            (long) totalDue,
            SUPPORT_LINE
        );
    }

    private String buildReminderMessage(String nameClient, String licensePlate, double weeklyFee, LocalDate dueDate) {
        String formattedDueDate = dueDate != null ? dueDate.format(DUE_DATE_FORMATTER) : "";
        return REMINDER_TEMPLATE.formatted(
            nameClient,
            licensePlate,
            (long) weeklyFee,
            formattedDueDate,
            SUPPORT_LINE
        );
    }

    private String buildPaymentReceivedMessage(String nameClient, double paymentAmount, String licensePlate) {
        return PAYMENT_RECEIVED_TEMPLATE.formatted(
            nameClient,
            (long) paymentAmount,
            licensePlate,
            SUPPORT_LINE
        );
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

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.util.Date utilDate) {
            return utilDate.toInstant().atZone(BOGOTA_ZONE).toLocalDate();
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        return LocalDate.parse(value.toString());
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

    private LocalDate computeClientDueDate(String dayOfPay) {
        LocalDate monday = LocalDate.now(BOGOTA_ZONE).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (dayOfPay == null) {
            return monday;
        }
        return switch (dayOfPay.trim()) {
            case "Lun" -> monday;
            case "Mar" -> monday.plusDays(1);
            case "Mier" -> monday.plusDays(2);
            case "Jue" -> monday.plusDays(3);
            case "Vie" -> monday.plusDays(4);
            case "Sab" -> monday.plusDays(5);
            case "Dom" -> monday.plusDays(6);
            default -> monday;
        };
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
