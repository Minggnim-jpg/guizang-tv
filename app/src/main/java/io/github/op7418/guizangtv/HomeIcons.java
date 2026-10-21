package io.github.op7418.guizangtv;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.LruCache;

/** Distinct single-color marks for known TV apps, original APK artwork in grayscale for others. */
final class HomeIcons {
    private static final LruCache<String, Bitmap> fallback = new LruCache<>(32);
    static Drawable get(Context context,String id,int color){
        int resource;
        switch(id){
            case "hdmi:2":resource=R.drawable.mark_playstation;break;
            case "hdmi:1":resource=R.drawable.mark_appletv;break;
            case "com.xiaodianshi.tv.yst":resource=R.drawable.mark_bilibili;break;
            case "com.ktcp.video":resource=R.drawable.mark_aurora;break;
            case "com.gitvdemo.video":resource=R.drawable.mark_kiwi;break;
            case "com.quark.yun.tv":resource=R.drawable.mark_quark;break;
            case "com.cibn.tv":resource=R.drawable.mark_youku;break;
            case "com.hunantv.license":resource=R.drawable.mark_mango;break;
            case "com.limelight":resource=R.drawable.mark_moonlight;break;
            case "tv.emby.embyatv":resource=R.drawable.mark_emby;break;
            case "com.xiaomi.mitv.smartshare":resource=R.drawable.mark_broadcast;break;
            case "com.mitv.gallery":resource=R.drawable.mark_image;break;
            case "com.mitv.tvhome":resource=R.drawable.mark_house;break;
            case "com.xiaomi.mitv.settings":resource=R.drawable.mark_gear_six;break;
            case "com.xiaomi.mitv.systemui":resource=R.drawable.mark_bell;break;
            case "com.xiaomi.mitv.tvmanager":resource=R.drawable.mark_shield;break;
            case "control:settings":resource=R.drawable.mark_gear_six;break;
            case "control:inputs":resource=R.drawable.mark_monitor_play;break;
            case "control:apps":resource=R.drawable.mark_squares_four;break;
            default:
                if(!id.startsWith("hdmi:")){
                    Drawable icon=fromApk(context,id);
                    if(icon!=null)return icon;
                }
                resource=id.startsWith("hdmi:")?R.drawable.mark_monitor_play:R.drawable.mark_app_window;
        }
        Drawable mark=context.getDrawable(resource).mutate();mark.setTint(color);return mark;
    }
    private static Drawable fromApk(Context c,String pkg){
        try{
            long updated=c.getPackageManager().getPackageInfo(pkg,0).lastUpdateTime;
            String key=pkg+":"+updated;
            Bitmap image=fallback.get(key);
            if(image==null){
                Drawable original=c.getPackageManager().getApplicationIcon(pkg);
                image=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
                Canvas canvas=new Canvas(image);
                ColorMatrix gray=new ColorMatrix();gray.setSaturation(0);
                // Keep luminance details: tinting an opaque legacy icon makes a blank square.
                Paint layer=new Paint(Paint.ANTI_ALIAS_FLAG);
                layer.setColorFilter(new ColorMatrixColorFilter(gray));
                int save=canvas.saveLayer(0,0,128,128,layer);
                int w=Math.max(1,original.getIntrinsicWidth()),h=Math.max(1,original.getIntrinsicHeight());
                float scale=120f/Math.max(w,h);
                int width=Math.round(w*scale),height=Math.round(h*scale);
                original.setBounds((128-width)/2,(128-height)/2,(128+width)/2,(128+height)/2);
                original.draw(canvas);canvas.restoreToCount(save);fallback.put(key,image);
            }
            return new BitmapDrawable(c.getResources(),image);
        }catch(android.content.pm.PackageManager.NameNotFoundException|RuntimeException unavailable){return null;}
    }
}
