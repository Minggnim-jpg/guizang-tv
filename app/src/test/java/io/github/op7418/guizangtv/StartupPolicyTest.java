package io.github.op7418.guizangtv;
import org.junit.Test;
import static org.junit.Assert.*;

public class StartupPolicyTest {
    @Test public void onlyTargetFirmwareFamilyIsEnabled() {
        assertTrue(StartupPolicy.supported("MiTV-ASTP0", 28));
        assertFalse(StartupPolicy.supported("MiTV-ASTP0", 34));
        assertFalse(StartupPolicy.supported("MiTV-MTSU0", 28));
    }
    @Test public void bootDoesNotReplayOrInterruptLaterUse() {
        assertTrue(StartupPolicy.boot(true, 2, 1, 50_000));
        assertFalse(StartupPolicy.boot(true, 2, 2, 50_000));
        assertFalse(StartupPolicy.boot(true, 2, 1, 600_001));
        assertFalse(StartupPolicy.boot(true, -1, -1, 50_000));
        assertFalse(StartupPolicy.boot(false, 2, 1, 50_000));
    }
    @Test public void serviceCreationAndDuplicateScreenOnAreNotWakeEvents() {
        assertFalse(StartupPolicy.wake(true, false, true));
        assertFalse(StartupPolicy.wake(true, true, false));
        assertFalse(StartupPolicy.wake(false, true, true));
        assertTrue(StartupPolicy.wake(true, true, true));
    }
}
