package io.github.op7418.guizangtv;

/** Package/class transitions only. No screen text, view tree, keys or periodic polling. */
final class ScreensaverReturnPolicy {
    static final String SAVER="com.mitv.screensaver", STOCK="com.mitv.tvhome";
    enum Action { NONE, SCHEDULE, CANCEL }
    private final String home;
    private String last="";
    private boolean saver, armed, pending;
    ScreensaverReturnPolicy(String home){this.home=home;}
    void homeVisible(){clear();last=home;}
    void clear(){saver=false;armed=false;pending=false;last="";}
    boolean pending(){return pending;}
    boolean armed(){return armed;}
    Action window(String pkg,String cls){
        if(pkg==null||pkg.isEmpty()||"android".equals(pkg)||"com.android.systemui".equals(pkg)
                ||"com.xiaomi.mitv.systemui".equals(pkg))return Action.NONE;
        if(SAVER.equals(pkg)){
            if(!"com.mitv.screensaver.RenderActivity".equals(cls))return Action.NONE;
            if(!saver){armed=home.equals(last);saver=true;pending=false;}
            last=pkg;return Action.NONE;
        }
        if(saver){
            if(STOCK.equals(pkg)&&armed){
                last=pkg;
                // Some exits first show Xiaomi BootActivity; wait for the actual home window.
                if("com.mitv.tvhome.MainActivityUserMode".equals(cls)||"com.mitv.tvhome.MainActivity".equals(cls)){
                    saver=false;pending=true;return Action.SCHEDULE;
                }
                return Action.NONE;
            }
            clear();last=pkg;return Action.CANCEL;
        }
        if(pending&&!STOCK.equals(pkg)){clear();last=pkg;return Action.CANCEL;}
        last=pkg;return Action.NONE;
    }
    boolean claim(){
        if(!pending||!armed||!STOCK.equals(last))return false;
        clear();return true;
    }
}
