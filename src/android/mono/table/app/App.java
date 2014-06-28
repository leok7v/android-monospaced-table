package android.mono.table.app;

import android.app.*;
import android.graphics.*;
import android.mono.table.etc.*;

public class App extends Application {

    public void onCreate() {
        super.onCreate();
        G.monospaced = new Paint();
        G.monospaced.setColor(C.NC_LTBLUE);
        G.monospaced.setStyle(Paint.Style.FILL);
        G.monospaced.setFlags(G.monospaced.getFlags() | Paint.SUBPIXEL_TEXT_FLAG|Paint.ANTI_ALIAS_FLAG|Paint.DEV_KERN_TEXT_FLAG);
        G.monospaced.setTypeface(TypefaceLoader.loadTypeface(this, C.MONO_TYPEFACE_NAME));
        if (!G.monospaced.getTypeface().isBold()) {
            G.monospaced.setFlags(G.monospaced.getFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
        }
        G.monospaced.setTextSize(util.unitToPixels(C.POINT, C.MONO_TYPEFACE_SIZE_IN_POINTS));
    }

}
