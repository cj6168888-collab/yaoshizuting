package com.yaoshizuting.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountStatusTest {

    @Test
    void fromCodeReturnsMatchingStatus() {
        assertEquals(AccountStatus.NORMAL, AccountStatus.fromCode(1));
        assertEquals(AccountStatus.FROZEN, AccountStatus.fromCode(0));
    }

    @Test
    void statusMetadataIsExposedForPersistenceAndApiSerialization() {
        assertEquals(1, AccountStatus.NORMAL.getCode());
        assertEquals("正常", AccountStatus.NORMAL.getDesc());
        assertEquals(0, AccountStatus.FROZEN.getCode());
        assertEquals("冻结", AccountStatus.FROZEN.getDesc());
    }

    @Test
    void fromCodeRejectsUnknownStatus() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AccountStatus.fromCode(99));

        assertEquals("Unknown account status: 99", exception.getMessage());
    }
}
