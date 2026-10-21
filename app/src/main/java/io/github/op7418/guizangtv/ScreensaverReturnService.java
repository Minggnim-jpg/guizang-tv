package io.github.op7418.guizangtv;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import java.util.*;

/** Opt-in metadata-only accessibility service scoped to Xiaomi screensaver round trips. */
public final class ScreensaverReturnService extends AccessibilityService {
    private static ScreensaverReturnService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ScreensaverReturnPolicy policy;
    private final Runnable restore=()->{
        if(!enabled(this)||!getSystemService(PowerManager.class).isInteractive()||FrameHomeActivity.homeResumed){cancel();return;}
        if(policy!=null&&policy.claim()){
            try{
                BootStart.status(this,"屏保退出：已请求返回，等待前台确认");
                startActivity(new Intent(this,FrameHomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra("startup.reason","屏保退出"));
            }catch(RuntimeException error){BootStart.status(this,"屏保返回失败："+error.getClass().getSimpleName());}
        }
    };
    static boolean enabled(Context c){return BootStart.prefs(c).getBoolean("screensaver_return",false);}
    static boolean connected(){return instance!=null;}
    /** Called only after explicit opt-in; preserves every other accessibility service. */
    static void sync(Context c){
        if(!BootStart.supported()||c.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)!=PackageManager.PERMISSION_GRANTED)return;
        ComponentName self=new ComponentName(c,ScreensaverReturnService.class);
        String raw=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        List<String> entries=new ArrayList<>();boolean found=false;
        for(String entry:(raw==null?"":raw).split(":")){
            if(entry.isEmpty()||"null".equals(entry))continue;
            if(self.equals(ComponentName.unflattenFromString(entry))){found=true;if(enabled(c))entries.add(entry);}
            else entries.add(entry);
        }
        if(enabled(c)&&!found)entries.add(self.flattenToString());
        if(found!=enabled(c)){
            Settings.Secure.putString(c.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,String.join(":",entries));
            if(enabled(c))Settings.Secure.putInt(c.getContentResolver(),Settings.Secure.ACCESSIBILITY_ENABLED,1);
        }
    }
    static void resumed(){
        if(instance!=null&&instance.policy!=null){instance.handler.removeCallbacks(instance.restore);instance.policy.homeVisible();}
    }
    static void cancelForSleep(){if(instance!=null)instance.cancel();}
    private void cancel(){handler.removeCallbacks(restore);if(policy!=null)policy.clear();}
    @Override protected void onServiceConnected(){
        if(!BootStart.supported()||!enabled(this)){disableSelf();return;}
        instance=this;policy=new ScreensaverReturnPolicy(getPackageName());
        if(FrameHomeActivity.homeResumed)policy.homeVisible();
        BootStart.event(this,"屏保返回窗口监听已连接");
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(!enabled(this)||policy==null||event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)return;
        String pkg=event.getPackageName()==null?"":event.getPackageName().toString();
        String cls=event.getClassName()==null?"":event.getClassName().toString();
        // Deliberately do not call getText(), getSource(), getWindows() or getRootInActiveWindow().
        boolean before=policy.armed();
        ScreensaverReturnPolicy.Action action=policy.window(pkg,cls);
        if(!before&&policy.armed())BootStart.event(this,"系统屏保从哈怪鱼桌面进入，已记录返回目标");
        if(action==ScreensaverReturnPolicy.Action.CANCEL)handler.removeCallbacks(restore);
        if(action==ScreensaverReturnPolicy.Action.SCHEDULE){handler.removeCallbacks(restore);handler.postDelayed(restore,600);}
    }
    @Override public void onInterrupt(){cancel();}
    @Override public void onDestroy(){cancel();if(instance==this)instance=null;super.onDestroy();}
}
