package io.github.op7418.guizangtv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class StartupReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || !BootStart.supported()) return;
        BootStart.event(context, "收到系统完整开机广播");
        if (BootStart.enabled(context) || BootStart.wakeEnabled(context)) {
            try {
            context.startForegroundService(new Intent(context, StartupService.class)
                    .setAction(StartupService.BOOT));
            } catch (RuntimeException blocked) {
                BootStart.status(context, "开机服务被系统阻止：" + blocked.getClass().getSimpleName());
            }
        } else { BootStart.status(context, "自启动开关关闭，未请求启动"); }
    }
}
