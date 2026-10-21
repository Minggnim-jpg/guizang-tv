package io.github.op7418.guizangtv;

/** Decisions are deliberately independent of the Android lifecycle. */
final class StartupPolicy {
    private StartupPolicy() { }
    static boolean supported(String model, int sdk) {
        return "MiTV-ASTP0".equals(model) && sdk == 28;
    }
    static boolean boot(boolean enabled, int boot, int attempted, long elapsed) {
        return enabled && boot >= 0 && boot != attempted && elapsed >= 0 && elapsed <= 600_000;
    }
    static boolean wake(boolean enabled, boolean wasOff, boolean interactive) {
        return enabled && wasOff && interactive;
    }
}
