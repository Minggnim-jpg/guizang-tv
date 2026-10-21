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
        return "重启：" + (enabled(c)?"开":"关") + " · 唤醒：" + (wakeEnabled(c)?"开":"关");
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
        boolean[] choices = { enabled(a), wakeEnabled(a) };
        new AlertDialog.Builder(a).setTitle("启动适配（实验）")
                .setMultiChoiceItems(new String[]{"完整重启后进入归藏", "待机唤醒后进入归藏（常驻服务）"}, choices,
                        (d, which, checked) -> choices[which] = checked)
                .setPositiveButton("保存", (d, which) -> {
                    if (!prefs(a).edit().putBoolean("enabled", choices[0])
                            .putBoolean("wake_enabled", choices[1]).putBoolean("screen_off", false).commit()) return;
                    Intent service = new Intent(a, StartupService.class);
                    // Cancel queued startup work even if a toggle is changed while a launch is pending.
                    a.stopService(service);
                    try {
                        if (choices[1]) a.startForegroundService(service);
                        android.widget.Toast.makeText(a, "已保存 · " + summary(a), android.widget.Toast.LENGTH_LONG).show();
                    } catch (RuntimeException blocked) {
                        status(a, "启动服务被系统阻止：" + blocked.getClass().getSimpleName());
                        android.widget.Toast.makeText(a, "开关已保存，但服务未启动，请查看说明", android.widget.Toast.LENGTH_LONG).show();
                    }
                })
                .setNeutralButton("说明", (d, which) -> new AlertDialog.Builder(a).setTitle("适配边界")
                        .setMessage("默认关闭；不接管 Home，不更改系统默认桌面。待机唤醒使用前台服务，无轮询。\n\n"
                                + "小米若强制停止应用，可能收不到唤醒事件，必须实机验证。\n\n"
                                + prefs(a).getString("status", "尚未测试完整重启或待机唤醒"))
                        .setPositiveButton("返回", (x, y) -> show(a)).show())
                .setNegativeButton("取消", null).show();
    }
    static void dump(Context c, java.io.PrintWriter out) {
        out.println("BootStart: supported=" + supported() + " enabled=" + enabled(c)
                + " wake_enabled=" + wakeEnabled(c) + " status=" + prefs(c).getString("status", "none")
                + " last_event=" + prefs(c).getString("last_event", "none")
                + " event_boot=" + prefs(c).getInt("event_boot", -1)
                + " event_elapsed=" + prefs(c).getLong("event_elapsed", -1));
    }
}
