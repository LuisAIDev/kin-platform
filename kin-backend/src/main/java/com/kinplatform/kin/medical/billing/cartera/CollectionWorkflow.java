package com.kinplatform.kin.medical.billing.cartera;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Decide la proxima accion de cobro segun la antiguedad de la mora.
 */
@Component
public class CollectionWorkflow {

    public enum CollectionAction {
        NONE, REMINDER, SECOND_NOTICE, ESCALATION, LEGAL
    }

    public CollectionAction nextAction(AccountsReceivable ar) {
        BigDecimal pending = pending(ar);
        if (pending.signum() <= 0) {
            return CollectionAction.NONE;
        }
        int days = ar.getDaysOverdue() == null ? 0 : ar.getDaysOverdue();
        if (days <= 0) return CollectionAction.NONE;
        if (days <= 30) return CollectionAction.REMINDER;
        if (days <= 60) return CollectionAction.SECOND_NOTICE;
        if (days <= 90) return CollectionAction.ESCALATION;
        return CollectionAction.LEGAL;
    }

    private BigDecimal pending(AccountsReceivable ar) {
        BigDecimal total = ar.getTotalValueCop() == null ? BigDecimal.ZERO : ar.getTotalValueCop();
        BigDecimal paid = ar.getPaidValueCop() == null ? BigDecimal.ZERO : ar.getPaidValueCop();
        return total.subtract(paid);
    }
}

