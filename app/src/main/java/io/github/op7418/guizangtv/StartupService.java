package io.github.op7418.guizangtv;

import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;

/** Experimental Android 9 startup service. No Home interception, polling or wake locks. */
public final class StartupService extends Service {
    static final String BOOT = "startup.boot";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean registered;
    private final Runnable afterWake = () -> {
        if (BootStart.wakeEnabled(this) && interactive()) launch("待机唤醒");
        finishIfTemporary();
    };
    private final Runnable afterBoot = () -> {
        int boot = Settings.Global.getInt(getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        if (StartupPolicy.boot(BootStart.enabled(this), boot,
                BootStart.prefs(this).getInt("attempted", -1), SystemClock.elapsedRealtime()) && interactive()) {
            // Record before attempting: a duplicate delivery must not steal focus again.
            if (BootStart.prefs(this).edit().putInt("attempted", boot).commit()) launch("完整开机");
        }
        finishIfTemporary();
    };
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                handler.removeCallbacksAndMessages(null);
                BootStart.prefs(context).edit().putBoolean("screen_off", true).apply();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                boolean off = BootStart.prefs(context).getBoolean("screen_off", false);
                BootStart.prefs(context).edit().putBoolean("screen_off", false).apply();
                if (StartupPolicy.wake(BootStart.wakeEnabled(context), off, interactive())) {
                    handler.removeCallbacks(afterWake);
                    handler.postDelayed(afterWake, 2000);
                }
            }
        }
    };
    private boolean interactive() { return getSystemService(PowerManager.class).isInteractive(); }
    // This service is deliberately unreachable outside Android 9. Android 14's foreground
    // service types have no applicable role here; do not grant unused newer-platform powers.
    @android.annotation.SuppressLint("ForegroundServiceType")
    @Override public void onCreate() {
        super.onCreate();
        if (!BootStart.supported()) { stopSelf(); return; }
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("startup", "归藏启动服务", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, FrameHomeActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new Notification.Builder(this, "startup")
                .setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("归藏待机唤醒服务")
                .setContentText("可在归藏的电视与启动设置中关闭").setContentIntent(open).setOngoing(true).build());
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screen, filter);
        registered = true;
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!BootStart.supported() || (!BootStart.enabled(this) && !BootStart.wakeEnabled(this))) {
            stopSelf(); return START_NOT_STICKY;
        }
        if (intent != null && BOOT.equals(intent.getAction())) {
            BootStart.prefs(this).edit().putBoolean("screen_off", false).apply();
            handler.removeCallbacks(afterBoot);
            handler.postDelayed(afterBoot, 3000);
        } else if (intent == null) {
            // A process restart is not a wake event unless a prior screen-off was observed.
            boolean off = BootStart.prefs(this).getBoolean("screen_off", false);
            if (StartupPolicy.wake(BootStart.wakeEnabled(this), off, interactive())) {
                BootStart.prefs(this).edit().putBoolean("screen_off", false).apply();
                handler.postDelayed(afterWake, 2000);
            }
        }
        finishIfTemporaryUnlessBoot(intent);
        return BootStart.wakeEnabled(this) ? START_STICKY : START_NOT_STICKY;
    }
    private void finishIfTemporaryUnlessBoot(Intent intent) {
        if (intent == null || !BOOT.equals(intent.getAction())) finishIfTemporary();
    }
    private void finishIfTemporary() { if (!BootStart.wakeEnabled(this)) stopSelf(); }
    private void launch(String reason) {
        try {
            startActivity(new Intent(this, FrameHomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra("startup.reason", reason));
            BootStart.status(this, reason + "：已请求启动，等待前台确认");
        } catch (RuntimeException error) {
            BootStart.status(this, reason + "失败：" + error.getClass().getSimpleName());
        }
    }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (registered) unregisterReceiver(screen);
        stopForeground(true);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
