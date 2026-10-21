package io.github.op7418.guizangtv;

import android.content.*;

/** Explicit shell-only configuration. DUMP permission protects the exported receiver. */
public final class StartupConfigReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(!BootStart.supported())return;
        android.content.SharedPreferences.Editor e=BootStart.prefs(context).edit();
        for(String name:new String[]{"enabled","wake_enabled","keep_adb"})
            if(intent.hasExtra(name))e.putBoolean(name,intent.getBooleanExtra(name,false));
        if(!e.commit()){setResultCode(0);setResultData("Save failed");return;}
        BootStart.ensureAdb(context);BootStart.refreshService(context);
        setResultCode(-1);setResultData(BootStart.summary(context));
    }
}
