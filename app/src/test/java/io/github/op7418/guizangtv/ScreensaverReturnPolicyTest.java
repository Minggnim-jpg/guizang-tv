package io.github.op7418.guizangtv;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScreensaverReturnPolicyTest {
    private static final String HOME="example.home", SAVER="com.mitv.screensaver", STOCK="com.mitv.tvhome";
    private ScreensaverReturnPolicy fresh(){return new ScreensaverReturnPolicy(HOME);}
    private void saver(ScreensaverReturnPolicy p){p.window(SAVER,SAVER+".RenderActivity");}
    private ScreensaverReturnPolicy.Action stock(ScreensaverReturnPolicy p){return p.window(STOCK,STOCK+".MainActivityUserMode");}
    @Test public void homeRoundTripReturnsOnlyOnce(){
        ScreensaverReturnPolicy p=fresh();p.homeVisible();saver(p);saver(p);
        assertEquals(ScreensaverReturnPolicy.Action.SCHEDULE,stock(p));
        assertEquals(ScreensaverReturnPolicy.Action.NONE,stock(p));
        assertTrue(p.claim());assertFalse(p.claim());
    }
    @Test public void videoRoundTripNeverClaimsHome(){
        ScreensaverReturnPolicy p=fresh();p.homeVisible();p.window("video.app","Player");saver(p);stock(p);assertFalse(p.claim());
    }
    @Test public void explicitlyUsingStockHomeIsRespected(){
        ScreensaverReturnPolicy p=fresh();p.homeVisible();stock(p);saver(p);stock(p);assertFalse(p.claim());
    }
    @Test public void launchingVideoAtExitCancelsDelayedReturn(){
        ScreensaverReturnPolicy p=fresh();p.homeVisible();saver(p);stock(p);p.window("video.app","Player");assertFalse(p.claim());
    }
    @Test public void normalHomeNavigationIsNeverTakenOver(){
        ScreensaverReturnPolicy p=fresh();p.homeVisible();stock(p);assertFalse(p.claim());
    }
    @Test public void stockBootIntermediateWaitsForRealHome(){
        ScreensaverReturnPolicy p=fresh();p.homeVisible();saver(p);
        assertEquals(ScreensaverReturnPolicy.Action.NONE,p.window(STOCK,STOCK+".BootActivity"));
        assertFalse(p.claim());assertEquals(ScreensaverReturnPolicy.Action.SCHEDULE,stock(p));assertTrue(p.claim());
    }
    @Test public void returningDirectlyToOwnHomeDoesNotRelaunch(){
        ScreensaverReturnPolicy p=fresh();p.homeVisible();saver(p);p.window(HOME,"Home");stock(p);assertFalse(p.claim());
    }
    @Test public void unknownOriginAndSleepAreConservative(){
        ScreensaverReturnPolicy p=fresh();saver(p);stock(p);assertFalse(p.claim());
        p.homeVisible();saver(p);p.clear();stock(p);assertFalse(p.claim());
    }
    @Test public void transientSystemOverlayDoesNotBecomeOrigin(){
        ScreensaverReturnPolicy p=fresh();p.homeVisible();p.window("com.xiaomi.mitv.systemui","Overlay");saver(p);stock(p);assertTrue(p.claim());
    }
}
