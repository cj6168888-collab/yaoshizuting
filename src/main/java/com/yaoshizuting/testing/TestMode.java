package com.yaoshizuting.testing;

import java.util.concurrent.atomic.AtomicReference;

public class TestMode {
    public enum Mode { STABLE, CONCURRENT }
    private static final AtomicReference<Mode> mode = new AtomicReference<>(Mode.STABLE);

    public static void setMode(Mode m) { mode.set(m); }
    public static Mode getMode() { return mode.get(); }
    public static boolean isConcurrent() { return mode.get() == Mode.CONCURRENT; }
}
