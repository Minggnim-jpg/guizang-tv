package io.github.op7418.guizangtv;

import android.app.*;
import android.content.*;
import android.database.ContentObserver;
import android.os.*;
import android.provider.Settings;

/** Android 9 startup/wake service. Event-driven, no polling, wake locks or Home interception. */
public final class StartupService extends Service {
    static final String BOOT="startup.boot", RECOVER="startup.recover";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean registered, observing, bootPending;
    private final ContentObserver adbObserver=new ContentObserver(handler){
        @Override public void onChange(boolean selfChange){BootStart.ensureAdb(StartupService.this);}
    };
    private final Runnable afterWake=()->{
        BootStart.ensureAdb(this);
        if(BootStart.wakeEnabled(this)&&interactive())launch("待机唤醒");
        finishIfTemporary();
    };
    private final Runnable afterBoot=()->{
        bootPending=false;
        int boot=bootCount();
        BootStart.ensureAdb(this);
        String skip=BootStartPolicy.skipReason(true,BootStart.enabled(this),boot,
                BootStart.prefs(this).getInt("attempted",-1),SystemClock.elapsedRealtime());
        if(skip!=null)BootStart.status(this,skip);
        else if(!interactive())BootStart.status(this,"开机时屏幕未亮，未请求启动");
        else if(BootStart.prefs(this).edit().putInt("attempted",boot).commit())launch("完整开机");
        finishIfTemporary();
    };
    private final BroadcastReceiver screen=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent intent){
            if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){
                handler.removeCallbacks(afterBoot);handler.removeCallbacks(afterWake);bootPending=false;
                BootStart.prefs(c).edit().putBoolean("screen_off",true).apply();
                BootStart.event(c,"收到关屏事件");
            }else if(Intent.ACTION_SCREEN_ON.equals(intent.getAction())){
                BootStart.event(c,"收到亮屏事件");
                BootStart.ensureAdb(c);recoverWake();
            }
        }
    };
    private int bootCount(){return Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,-1);}
    private boolean interactive(){return getSystemService(PowerManager.class).isInteractive();}
    private void recoverWake(){
        boolean off=BootStart.prefs(this).getBoolean("screen_off",false);
        if(!interactive())return;
        BootStart.prefs(this).edit().putBoolean("screen_off",false).apply();
        if(StartupPolicy.wake(BootStart.wakeEnabled(this),off,true)){
            handler.removeCallbacks(afterWake);handler.postDelayed(afterWake,2000);
        }
    }
    // This service is only enabled on ASTP0 / API 28, before Android 14 service types exist.
    @android.annotation.SuppressLint("ForegroundServiceType")
    @Override public void onCreate(){
        super.onCreate();
        if(!BootStart.supported()){stopSelf();return;}
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("startup","归藏启动服务",NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,FrameHomeActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        startForeground(1,new Notification.Builder(this,"startup")
                .setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("归藏启动与调试服务")
                .setContentText("可在电视与启动设置中关闭").setContentIntent(open).setOngoing(true).build());
        IntentFilter filter=new IntentFilter(Intent.ACTION_SCREEN_ON);filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screen,filter);registered=true;
        getContentResolver().registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED),false,adbObserver);
        observing=true;
        if(!interactive())BootStart.prefs(this).edit().putBoolean("screen_off",true).apply();
        BootStart.ensureAdb(this);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(!BootStart.supported()||(!BootStart.enabled(this)&&!BootStart.wakeEnabled(this)&&!BootStart.keepAdb(this))){
            stopSelf();return START_NOT_STICKY;
        }
        BootStart.ensureAdb(this);
        String action=intent==null?null:intent.getAction();
        boolean recovering=RECOVER.equals(action);
        boolean eligible=StartupPolicy.boot(BootStart.enabled(this),bootCount(),
                BootStart.prefs(this).getInt("attempted",-1),SystemClock.elapsedRealtime());
        if(BOOT.equals(action)||(recovering&&eligible)){
            if(BOOT.equals(action))BootStart.prefs(this).edit().putBoolean("screen_off",false).apply();
            handler.removeCallbacks(afterWake);handler.removeCallbacks(afterBoot);
            bootPending=true;handler.postDelayed(afterBoot,3000);
        }else if(intent==null||recovering){recoverWake();}
        finishIfTemporary();
        return BootStart.wakeEnabled(this)||BootStart.keepAdb(this)?START_STICKY:START_NOT_STICKY;
    }
    private void finishIfTemporary(){if(!bootPending&&!BootStart.wakeEnabled(this)&&!BootStart.keepAdb(this))stopSelf();}
    private void launch(String reason){
        try{
            BootStart.status(this,reason+"：已请求启动，等待前台确认");
            startActivity(new Intent(this,FrameHomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("startup.reason",reason));
        }catch(RuntimeException error){BootStart.status(this,reason+"失败："+error.getClass().getSimpleName());}
    }
    @Override public void onDestroy(){
        handler.removeCallbacksAndMessages(null);
        if(registered)unregisterReceiver(screen);
        if(observing)getContentResolver().unregisterContentObserver(adbObserver);
        stopForeground(true);super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
