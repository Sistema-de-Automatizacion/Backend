package com.automatization.comunications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

import com.automatization.comunications.model.dto.ContractAndPayoutDto;
import com.automatization.comunications.model.dto.ErrorNotificationDto;
import com.automatization.comunications.model.dto.NotificationDto;
import com.automatization.comunications.model.entity.ErrorNotification;
import com.automatization.comunications.model.entity.Notification;
import com.automatization.comunications.repository.IRepositoryContract;
import com.automatization.comunications.repository.IRepositoryErrorNotification;
import com.automatization.comunications.repository.IRepositoryNotification;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Mock private IRepositoryNotification repoNotif;
    @Mock private IRepositoryContract repoContract;
    @Mock private IRepositoryErrorNotification repoError;

    @InjectMocks private NotificationService service;

    // --------------------------------------------------------------------
    // findContractNextTopay
    // --------------------------------------------------------------------

    @Test
    void findContractNextTopay_returnsEmptyWhenNoRows() {
        when(repoContract.findAllPayoutAndContract()).thenReturn(List.of());

        assertThat(service.findContractNextTopay()).isEmpty();
    }

    @Test
    void findContractNextTopay_skipsContractWithZeroDebt() {
        // deuda_cli = 0 -> el contrato no debe aparecer
        Object[] row = contractRow("1001", "JUAN", "3000000000", "Mar",
                220d, "", 0d, "ABC01D", Date.valueOf("2026-04-21"));
        when(repoContract.findAllPayoutAndContract()).thenReturn(List.<Object[]>of(row));

        assertThat(service.findContractNextTopay()).isEmpty();
    }

    @Test
    void findContractNextTopay_skipsCancelledContracts() {
        Object[] row = contractRow("1002", "PEDRO", "3000000001", "Mier",
                220d, "CANCELADO", 220d, "ABC02D", Date.valueOf("2026-04-21"));
        when(repoContract.findAllPayoutAndContract()).thenReturn(List.<Object[]>of(row));

        assertThat(service.findContractNextTopay()).isEmpty();
    }

    @Test
    void findContractNextTopay_generatesReminderWhenDebtEqualsCuota() {
        // deuda_cli == cuota -> no hay mora arrastrada -> RECORDATORIO
        Object[] row = contractRow("1003", "MARÍA", "3000000002", "Mar",
                220d, "", 220d, "XYZ03D", Date.valueOf("2026-04-21"));
        when(repoContract.findAllPayoutAndContract()).thenReturn(List.<Object[]>of(row));

        List<ContractAndPayoutDto> result = service.findContractNextTopay();

        assertThat(result).hasSize(1);
        ContractAndPayoutDto dto = result.get(0);
        assertThat(dto.id()).isEqualTo("1003");
        assertThat(dto.paymentContract()).isEqualTo(220_000d);  // *1000
        assertThat(dto.accumulatedDebt()).isEqualTo(220_000d);
        assertThat(dto.debt()).isEqualTo(0d);                   // sin mora arrastrada
        assertThat(dto.message())
                .startsWith("Recordatorio de pago")
                .contains("MARÍA")
                .contains("XYZ03D")
                .contains("$220000 COP");
    }

    @Test
    void findContractNextTopay_generatesMoraWhenDebtGreaterThanCuota() {
        // deuda_cli > cuota -> hay mora arrastrada -> MORA
        Object[] row = contractRow("1004", "EDWIN", "3042824012", "Mar",
                208d, "", 280.1d, "REJ22G", Date.valueOf("2026-04-21"));
        when(repoContract.findAllPayoutAndContract()).thenReturn(List.<Object[]>of(row));

        List<ContractAndPayoutDto> result = service.findContractNextTopay();

        assertThat(result).hasSize(1);
        ContractAndPayoutDto dto = result.get(0);
        assertThat(dto.paymentContract()).isEqualTo(208_000d);
        assertThat(dto.accumulatedDebt()).isEqualTo(280_100d);
        assertThat(dto.debt()).isEqualTo(72_100d);  // 280_100 - 208_000
        assertThat(dto.message())
                .startsWith("Cuota vencida")
                .contains("EDWIN")
                .contains("REJ22G")
                .contains("$72100 COP")     // mora arrastrada
                .contains("$208000 COP")    // cuota de la semana
                .contains("$280100 COP")    // total
                .contains("Para soporte: +57 304 4558351");
    }

    @Test
    void findContractNextTopay_translatesDiaCanonToFullName() {
        Object[] row = contractRow("1005", "ANA", "3000000003", "Jue",
                200d, "", 200d, "DEF04D", Date.valueOf("2026-04-21"));
        when(repoContract.findAllPayoutAndContract()).thenReturn(List.<Object[]>of(row));

        ContractAndPayoutDto dto = service.findContractNextTopay().get(0);

        assertThat(dto.paymentDay()).isEqualTo("Jueves");
    }

    @Test
    void findContractNextTopay_computesDueDateFromDiaCanon() {
        // Cliente que paga los Martes -> la fecha en el mensaje debe ser el martes de ESTA semana.
        Object[] row = contractRow("1006", "LUIS", "3000000004", "Mar",
                200d, "", 200d, "GHI05D", Date.valueOf("2026-04-21"));
        when(repoContract.findAllPayoutAndContract()).thenReturn(List.<Object[]>of(row));

        String message = service.findContractNextTopay().get(0).message();

        LocalDate expectedDueDate = LocalDate.now(BOGOTA)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plusDays(1); // Mar = +1
        assertThat(message).contains("vence el " + expectedDueDate.format(DDMMYYYY));
    }

    @Test
    void findContractNextTopay_keepsEstadoSemanaBlankAndNull() {
        Object[] rowBlank = contractRow("2001", "A", "3000000010", "Mar",
                200d, "",   200d, "AAA01A", Date.valueOf("2026-04-21"));
        Object[] rowNull  = contractRow("2002", "B", "3000000011", "Mar",
                200d, null, 200d, "AAA02A", Date.valueOf("2026-04-21"));
        when(repoContract.findAllPayoutAndContract()).thenReturn(List.<Object[]>of(rowBlank, rowNull));

        assertThat(service.findContractNextTopay()).hasSize(2);
    }

    @Test
    void findContractNextTopay_setsPaymentPayoutNull() {
        // El endpoint ya no muestra pagos: paymentPayout debe ser null en el DTO
        Object[] row = contractRow("1007", "CLARA", "3000000005", "Vie",
                100d, "", 100d, "JKL06D", Date.valueOf("2026-04-21"));
        when(repoContract.findAllPayoutAndContract()).thenReturn(List.<Object[]>of(row));

        assertThat(service.findContractNextTopay().get(0).paymentPayout()).isNull();
    }

    // --------------------------------------------------------------------
    // findClientsPaidThisWeek
    // --------------------------------------------------------------------

    @Test
    void findClientsPaidThisWeek_returnsEmptyWhenNoPayments() {
        when(repoContract.findClientsPaidBetween(any(), any())).thenReturn(List.of());

        assertThat(service.findClientsPaidThisWeek()).isEmpty();
    }

    @Test
    void findClientsPaidThisWeek_mapsRowToPaidDto() {
        Object[] row = paidRow("3001", "RAFAEL", "3169575568",
                195d, 195d, "KDW12H", Date.valueOf("2026-04-21"));
        when(repoContract.findClientsPaidBetween(any(), any())).thenReturn(List.<Object[]>of(row));

        List<ContractAndPayoutDto> result = service.findClientsPaidThisWeek();

        assertThat(result).hasSize(1);
        ContractAndPayoutDto dto = result.get(0);
        assertThat(dto.id()).isEqualTo("3001");
        assertThat(dto.paymentContract()).isEqualTo(195_000d);
        assertThat(dto.paymentPayout()).isEqualTo(195_000d);
        assertThat(dto.paymentDay()).isEqualTo("21/04/2026"); // fecha del pago formateada
        assertThat(dto.message())
                .startsWith("Hola RAFAEL")
                .contains("$195000 COP")
                .contains("KDW12H")
                .contains("🏍️");
    }

    @Test
    void findClientsPaidThisWeek_queriesFromMondayToSunday() {
        when(repoContract.findClientsPaidBetween(any(), any())).thenReturn(List.of());

        service.findClientsPaidThisWeek();

        ArgumentCaptor<LocalDate> start = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> end = ArgumentCaptor.forClass(LocalDate.class);
        verify(repoContract).findClientsPaidBetween(start.capture(), end.capture());

        assertThat(start.getValue().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(end.getValue().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(start.getValue().plusDays(6)).isEqualTo(end.getValue());
    }

    // --------------------------------------------------------------------
    // Notifications CRUD
    // --------------------------------------------------------------------

    @Test
    void findNotifications_delegatesToRepo() {
        Notification n = new Notification(1L, "1001", "Juan", "3000000000", "Mar");
        when(repoNotif.findByNumContract("1001")).thenReturn(List.of(n));

        assertThat(service.findNotifications("1001")).containsExactly(n);
    }

    @Test
    void findAllNotifications_delegatesToRepo() {
        Pageable page = PageRequest.of(0, 10);
        Page<Notification> expected = new PageImpl<>(List.of());
        when(repoNotif.findAll(page)).thenReturn(expected);

        assertThat(service.findAllNotifications(page)).isSameAs(expected);
    }

    @Test
    void findAllErrorNotifications_delegatesToRepo() {
        Pageable page = PageRequest.of(1, 20);
        Page<ErrorNotification> expected = new PageImpl<>(List.of());
        when(repoError.findAll(page)).thenReturn(expected);

        assertThat(service.findAllErrorNotifications(page)).isSameAs(expected);
    }

    @Test
    void saveNotification_persistsEntityBuiltFromDto() {
        NotificationDto dto = new NotificationDto("1001", "Juan", "3000000000", "Mar");

        service.saveNotification(dto);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repoNotif).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getNumContract()).isEqualTo("1001");
        assertThat(saved.getNameClient()).isEqualTo("Juan");
        assertThat(saved.getPhoneNumber()).isEqualTo("3000000000");
        assertThat(saved.getDayRemember()).isEqualTo("Mar");
    }

    @Test
    void saveErrorNotification_persistsEntityBuiltFromDto() {
        ErrorNotificationDto dto = new ErrorNotificationDto(
                "1001", "Juan", "3000000000", "Mar", "SMTP 500");

        service.saveErrorNotification(dto);

        ArgumentCaptor<ErrorNotification> captor = ArgumentCaptor.forClass(ErrorNotification.class);
        verify(repoError).save(captor.capture());
        ErrorNotification saved = captor.getValue();
        assertThat(saved.getErrorMessage()).isEqualTo("SMTP 500");
    }

    @Test
    void deleteNotification_deletesAndReturnsTrueWhenExists() {
        when(repoNotif.existsById(42L)).thenReturn(true);

        assertThat(service.deleteNotification(42L)).isTrue();
        verify(repoNotif).deleteById(42L);
    }

    @Test
    void deleteNotification_returnsFalseAndSkipsDeleteWhenMissing() {
        when(repoNotif.existsById(99L)).thenReturn(false);

        assertThat(service.deleteNotification(99L)).isFalse();
        verify(repoNotif, org.mockito.Mockito.never()).deleteById(any());
    }

    // --------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------

    /** Fila tal como la devuelve el repo para findAllPayoutAndContract (9 columnas). */
    private Object[] contractRow(String id, String name, String phone, String diaCanon,
                                  double cuota, String estadoSemana, double deudaCli,
                                  String placa, Date fechaSemanal) {
        return new Object[] {
                id, name, phone, diaCanon, cuota, estadoSemana, deudaCli, placa, fechaSemanal
        };
    }

    /** Fila tal como la devuelve el repo para findClientsPaidBetween (7 columnas). */
    private Object[] paidRow(String id, String name, String phone,
                              double cuota, double recaudo, String placa, Date fecha) {
        return new Object[] {
                id, name, phone, cuota, recaudo, placa, fecha
        };
    }
}
