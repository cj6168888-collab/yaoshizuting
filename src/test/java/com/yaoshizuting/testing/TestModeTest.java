package com.yaoshizuting.testing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestModeTest {

    @AfterEach
    void resetMode() {
        TestMode.setMode(TestMode.Mode.STABLE);
    }

    @Test
    void defaultsToStableMode() {
        TestMode.setMode(TestMode.Mode.STABLE);

        assertEquals(TestMode.Mode.STABLE, TestMode.getMode());
        assertFalse(TestMode.isConcurrent());
    }

    @Test
    void setModeSwitchesConcurrentFlag() {
        TestMode.setMode(TestMode.Mode.CONCURRENT);

        assertEquals(TestMode.Mode.CONCURRENT, TestMode.getMode());
        assertTrue(TestMode.isConcurrent());
    }
}
