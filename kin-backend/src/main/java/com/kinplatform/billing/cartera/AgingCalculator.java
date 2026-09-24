package com.kinplatform.billing.cartera;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calculo de maduracion (aging) de cartera. Job nocturno recalcula
 * days_overdue, aging_bucket y status de todas las cuentas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgingCalculator {

    public static final String CURRENT = "CURRENT";
    public static final String BUCKET_1_30 = "1-30";
    public static final String BUCKET_31_60 = "31-60";
    public static final String BUCKET_61_90 = "61-90";
    public static final String BUCKET_91_180 = "91-180";
    public static final String BUCKET_180_PLUS = "180+";

    private final AccountsReceivableRepository repository;

    public int daysOverdue(LocalDate dueDate, LocalDate today) {
        if (dueDate == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(dueDate, today);
        return days > 0 ? (int) days : 0;
    }

    public String bucket(int daysOverdue) {
        if (daysOverdue <= 0) return CURRENT;
        if (daysOverdue <= 30) return BUCKET_1_30;
        if (daysOverdue <= 60) return BUCKET_31_60;
        if (daysOverdue <= 90) return BUCKET_61_90;
        if (daysOverdue <= 180) return BUCKET_91_180;
        return BUCKET_180_PLUS;
    }

    public BigDecimal pending(AccountsReceivable ar) {
        if (ar.getPendingValueCop() != null) {
            return ar.getPendingValueCop();
        }
        BigDecimal total = ar.getTotalValueCop() == null ? BigDecimal.ZERO : ar.getTotalValueCop();
        BigDecimal paid = ar.getPaidValueCop() == null ? BigDecimal.ZERO : ar.getPaidValueCop();
        return total.subtract(paid);
    }

    public AccountsReceivable recalculate(AccountsReceivable ar, LocalDate today) {
        int days = daysOverdue(ar.getDueDate(), today);
        ar.setDaysOverdue(days);
        ar.setAgingBucket(bucket(days));

        BigDecimal pending = pending(ar);
        BigDecimal paid = ar.getPaidValueCop() == null ? BigDecimal.ZERO : ar.getPaidValueCop();
        if (pending.signum() <= 0) {
            ar.setStatus(AccountsReceivable.ArStatus.PAID);
        } else if (days > 0) {
            ar.setStatus(AccountsReceivable.ArStatus.OVERDUE);
        } else if (paid.signum() > 0) {
            ar.setStatus(AccountsReceivable.ArStatus.PARTIAL);
        } else {
            ar.setStatus(AccountsReceivable.ArStatus.PENDING);
        }
        return ar;
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void recalculateAll() {
        LocalDate today = LocalDate.now();
        repository.findAll().forEach(ar -> repository.save(recalculate(ar, today)));
        log.info("Aging de cartera recalculado ({})", today);
    }
}
