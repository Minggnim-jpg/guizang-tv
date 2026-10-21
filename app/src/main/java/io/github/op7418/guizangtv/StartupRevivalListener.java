package io.github.op7418.guizangtv;

import android.content.Intent;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** System binding can revive the process after Xiaomi clears third-party apps.
 * Notification content is neither inspected, saved nor acted upon. */
public final class StartupRevivalListener extends NotificationListenerService {
    static volatile boolean connected;
    @Override public void onListenerConnected() {
        connected=true;
        if(!BootStart.supported())return;
        BootStart.event(this,"系统已绑定启动恢复服务");
        if(!getSystemService(PowerManager.class).isInteractive())
            BootStart.prefs(this).edit().putBoolean("screen_off",true).apply();
        BootStart.ensureAdb(this);
        ScreensaverReturnService.sync(this);
        if(BootStart.enabled(this)||BootStart.wakeEnabled(this)||BootStart.keepAdb(this)) {
            try { startForegroundService(new Intent(this,StartupService.class).setAction(StartupService.RECOVER)); }
            catch(RuntimeException error){BootStart.status(this,"恢复服务失败："+error.getClass().getSimpleName());}
        }
    }
    @Override public void onListenerDisconnected(){connected=false;}
    @Override public void onDestroy(){connected=false;super.onDestroy();}
    @Override public void onNotificationPosted(StatusBarNotification ignored) { }
    @Override public void onNotificationRemoved(StatusBarNotification ignored) { }
}
