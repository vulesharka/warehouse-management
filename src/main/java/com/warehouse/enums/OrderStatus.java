package com.warehouse.enums;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    CREATED,
    AWAITING_APPROVAL,
    APPROVED,
    DECLINED,
    UNDER_DELIVERY,
    FULFILLED,
    CANCELED;

    private Set<OrderStatus> allowed;

    static {
        CREATED.allowed           = EnumSet.of(AWAITING_APPROVAL, CANCELED);
        AWAITING_APPROVAL.allowed = EnumSet.of(APPROVED, DECLINED, CANCELED);
        APPROVED.allowed          = EnumSet.of(UNDER_DELIVERY, CANCELED);
        DECLINED.allowed          = EnumSet.of(AWAITING_APPROVAL, CANCELED);
        UNDER_DELIVERY.allowed    = EnumSet.of(FULFILLED);
        FULFILLED.allowed         = EnumSet.noneOf(OrderStatus.class);
        CANCELED.allowed          = EnumSet.noneOf(OrderStatus.class);
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowed.contains(next);
    }
}
