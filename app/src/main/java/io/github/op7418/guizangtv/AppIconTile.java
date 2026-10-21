package io.github.op7418.guizangtv;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** Bounded native tile; no per-frame blur, shaders or bitmap allocation. */
final class AppIconTile extends FrameLayout {
    private final boolean light;
    AppIconTile(Context context,String id,boolean light,int iconDp){
        super(context);this.light=light;
        ImageView mark=new ImageView(context);
        mark.setImageDrawable(HomeIcons.get(context,id,light?0xFF272B2D:0xFFF5F4EF));
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LayoutParams p=new LayoutParams(dp(iconDp),dp(iconDp),android.view.Gravity.CENTER);
        addView(mark,p);setClipChildren(false);focus(false,false);
    }
    void focus(boolean selected,boolean animate){
        GradientDrawable background=new GradientDrawable();
        background.setColor(light?(selected?0xDDE6E4E0:0xBBDDDCD6):(selected?0xE6424746:0xB3323837));
        background.setCornerRadius(dp(15));
        background.setStroke(dp(selected?1.5f:0),selected?(light?0xFF30383A:0xFFF5F4EF):Color.TRANSPARENT);
        setBackground(background);setElevation(dp(selected?5:0));
        float scale=selected?1.08f:1f;animate().cancel();
        if(animate)animate().scaleX(scale).scaleY(scale).setDuration(140).start();
        else{setScaleX(scale);setScaleY(scale);}
    }
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
