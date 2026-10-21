package io.github.op7418.guizangtv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Build;

/** Fork-only ASTP0 startup: independently opt in to boot and screen-on. */
public final class BootStart {
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("boot-start", Context.MODE_PRIVATE); }
    static boolean supported() { return StartupPolicy.supported(Build.MODEL, Build.VERSION.SDK_INT); }
    static boolean enabled(Context c) { return prefs(c).getBoolean("enabled", false); }
    static boolean keepAdb(Context c) { return prefs(c).getBoolean("keep_adb", false); }
    static boolean adbGranted(Context c) { return c.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)==android.content.pm.PackageManager.PERMISSION_GRANTED; }
    static void ensureAdb(Context c) {
        if(!supported()||!keepAdb(c)||!adbGranted(c))return;
        try {
            if(android.provider.Settings.Global.getInt(c.getContentResolver(),android.provider.Settings.Global.ADB_ENABLED,0)==0){
                boolean ok=android.provider.Settings.Global.putInt(c.getContentResolver(),android.provider.Settings.Global.ADB_ENABLED,1);
                event(c,ok?"已自动恢复 ADB 调试":"系统拒绝恢复 ADB 调试");
            }
        }catch(RuntimeException error){event(c,"恢复 ADB 失败："+error.getClass().getSimpleName());}
    }
    static void refreshService(Context c) {
        Intent intent=new Intent(c,StartupService.class);
        try {
            if(supported()&&(wakeEnabled(c)||keepAdb(c)))c.startForegroundService(intent);
            else c.stopService(intent);
        }catch(RuntimeException error){status(c,"启动服务失败："+error.getClass().getSimpleName());}
    }
    static boolean wakeEnabled(Context c) { return prefs(c).getBoolean("wake_enabled", false); }
    static void status(Context c, String value) {
        prefs(c).edit().putString("status", value).apply();
        android.util.Log.i("GuiZangBoot", value);
    }
    static void event(Context c, String value) {
        prefs(c).edit().putString("last_event", value)
                .putInt("event_boot", android.provider.Settings.Global.getInt(c.getContentResolver(), "boot_count", -1))
                .putLong("event_elapsed", android.os.SystemClock.elapsedRealtime()).apply();
        android.util.Log.i("GuiZangBoot", value);
    }
    static String summary(Context c) {
        return "重启：" + (enabled(c)?"开":"关") + " · 唤醒：" + (wakeEnabled(c)?"开":"关") + " · ADB：" + (keepAdb(c)?"开":"关");
    }
    // The old package-based Xiaomi hook must not bypass the opt-in service or cause duplicate launches.
    static void enter(Activity a) { }
    static void confirm(Activity a) {
        String reason = a.getIntent().getStringExtra("startup.reason");
        if (reason != null && a.hasWindowFocus()) {
            a.getIntent().removeExtra("startup.reason");
            status(a, reason + "：已进入前台");
        }
    }
    static void show(Activity a) {
        if (!supported()) {
            new AlertDialog.Builder(a).setTitle("启动适配（实验）")
                    .setMessage("此定制版仅为 MiTV-ASTP0 / Android 9 准备，其他机型未启用。")
                    .setPositiveButton("返回", null).show();
            return;
        }
        boolean[] choices = { enabled(a), wakeEnabled(a), keepAdb(a) };
        new AlertDialog.Builder(a).setTitle("启动适配（实验）")
                .setMultiChoiceItems(new String[]{"完整重启后进入哈怪鱼", "待机唤醒后进入哈怪鱼（常驻服务）", "保持 ADB 调试（需电脑授权）"}, choices,
                        (d, which, checked) -> choices[which] = checked)
                .setPositiveButton("保存", (d, which) -> {
                    if (!prefs(a).edit().putBoolean("enabled", choices[0])
                            .putBoolean("wake_enabled", choices[1]).putBoolean("keep_adb", choices[2]).putBoolean("screen_off", false).commit()) return;
                    Intent service = new Intent(a, StartupService.class);
                    // Cancel queued startup work even if a toggle is changed while a launch is pending.
                    a.stopService(service);
                    try {
                        ensureAdb(a);
                        if (choices[1]||choices[2]) a.startForegroundService(service);
                        android.widget.Toast.makeText(a, "已保存 · " + summary(a), android.widget.Toast.LENGTH_LONG).show();
                    } catch (RuntimeException blocked) {
                        status(a, "启动服务被系统阻止：" + blocked.getClass().getSimpleName());
                        android.widget.Toast.makeText(a, "开关已保存，但服务未启动，请查看说明", android.widget.Toast.LENGTH_LONG).show();
                    }
                })
                .setNeutralButton("说明", (d, which) -> new AlertDialog.Builder(a).setTitle("适配边界")
                        .setMessage("默认关闭；不接管 Home，不更改系统默认桌面。待机唤醒使用前台服务，无轮询。\n\n"
                                + "小米会清理后台。启动恢复服务需由电脑授权通知监听，仅用于恢复进程，不处理通知内容。\n\n保持 ADB 会自动重新打开调试；要手动关闭调试，请先关闭此选项。\n\n"
                                + prefs(a).getString("status", "尚未测试完整重启或待机唤醒"))
                        .setPositiveButton("返回", (x, y) -> show(a)).show())
                .setNegativeButton("取消", null).show();
    }
    static void dump(Context c, java.io.PrintWriter out) {
        out.println("BootStart: supported=" + supported() + " enabled=" + enabled(c)
                + " wake_enabled=" + wakeEnabled(c) + " keep_adb=" + keepAdb(c) + " adb_granted=" + adbGranted(c) + " revival_bound=" + StartupRevivalListener.connected + " status=" + prefs(c).getString("status", "none")
                + " last_event=" + prefs(c).getString("last_event", "none")
                + " event_boot=" + prefs(c).getInt("event_boot", -1)
                + " event_elapsed=" + prefs(c).getLong("event_elapsed", -1));
    }
}
