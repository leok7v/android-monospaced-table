package android.mono.table.app;

import android.app.*;
import android.graphics.*;
import android.mono.table.etc.*;

import static android.mono.table.etc.util.*;
import static java.lang.String.format;

public class App extends Application {

    private static final int N = 1000;
    private static volatile boolean logging = true;
    private static volatile int count;

    private static void log(CharSequence m) {
        if (m != null) {
            trace(m);
        }
    }

    private static void log1() {
        timestamp("log1");
        count = 0;
        for (int i = 0; i < N; i++) {
            if (logging) { log(format("Hello %s i=%d logging=%b", "World", i, logging)); }
            count++;
        }
        timestamp("log1");
    }

    private static void log2() {
        timestamp("log2");
        for (int i = 0; i < N; i++) {
            log(logging ? format("Hello %s i=%d logging=%b", "World", i, logging) : null);
            count++;
        }
        timestamp("log2");
    }

// "log1" 628 milliseconds ~== 0.628 nanoseconds per "if () { log(); }"
// "log2" 3468 milliseconds ~= 3.468 nanoseconds per "log( ? );"

    private static void testLoggingAndAssertions() {
        if (logging) { log(format("Hello %s i=%d logging=%b", "World", 123, logging)); }
        log(logging ? format("Hello %s i=%d logging=%b", "World", 321, logging) : null);
        logging = false;
        count = 0;
        log1();
        assertion(count == N ? true : format("expected count=%d == %d", count, N));
        logging = false;
        count = 0;
        log2();
        assertion(count == N ? true : format("expected count=%d == %d", count, N));
    }

    public void onCreate() {
        super.onCreate();
        testLoggingAndAssertions();
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
