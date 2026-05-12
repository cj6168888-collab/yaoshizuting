package com.yaoshizuting.enums;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusTest {

    @Test
    void fromCodeReturnsEveryPersistedStatus() {
        Map<Integer, OrderStatus> expectedStatuses = Map.of(
                0, OrderStatus.PENDING,
                1, OrderStatus.PAID,
                2, OrderStatus.PROCESSING,
                3, OrderStatus.COMPLETED,
                4, OrderStatus.CANCELLED,
                5, OrderStatus.REFUNDED);

        expectedStatuses.forEach((code, status) -> assertEquals(status, OrderStatus.fromCode(code)));
    }

    @Test
    void statusMetadataIsExposedForPersistenceAndApiSerialization() {
        assertEquals(0, OrderStatus.PENDING.getCode());
        assertEquals("待支付", OrderStatus.PENDING.getDesc());
        assertEquals(1, OrderStatus.PAID.getCode());
        assertEquals("已支付", OrderStatus.PAID.getDesc());
        assertEquals(2, OrderStatus.PROCESSING.getCode());
        assertEquals("处理中", OrderStatus.PROCESSING.getDesc());
        assertEquals(3, OrderStatus.COMPLETED.getCode());
        assertEquals("已完成", OrderStatus.COMPLETED.getDesc());
        assertEquals(4, OrderStatus.CANCELLED.getCode());
        assertEquals("已取消", OrderStatus.CANCELLED.getDesc());
        assertEquals(5, OrderStatus.REFUNDED.getCode());
        assertEquals("已退款", OrderStatus.REFUNDED.getDesc());
    }

    @Test
    void fromCodeRejectsUnknownStatus() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> OrderStatus.fromCode(99));

        assertEquals("Unknown order status: 99", exception.getMessage());
    }
}
