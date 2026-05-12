package com.yaoshizuting.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderNoUtilsTest {

    @Test
    void generateOrderSnUsesOrderPrefixTimestampAndRandomSuffix() {
        String orderSn = OrderNoUtils.generateOrderSn();

        assertTrue(orderSn.matches("ORD\\d{20}"));
    }

    @Test
    void generateWithdrawSnUsesWithdrawPrefixTimestampAndRandomSuffix() {
        String withdrawSn = OrderNoUtils.generateWithdrawSn();

        assertTrue(withdrawSn.matches("WTH\\d{20}"));
    }

    @Test
    void generateSimpleUuidReturnsCompactUuid() {
        String uuid = OrderNoUtils.generateSimpleUUID();

        assertTrue(uuid.matches("[0-9a-fA-F]{32}"));
        assertFalse(uuid.contains("-"));
    }
}
