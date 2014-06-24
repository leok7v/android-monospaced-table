/*  Copyright (c) 2012, Leo Kuznetsov
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this
      list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

    * Neither the name of the {organization} nor the names of its
      contributors may be used to endorse or promote products derived from
      this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
    FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
    DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
    CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package android.mono.table.etc;

import android.content.res.*;
import android.os.*;
import android.util.*;
import android.view.*;

import java.io.*;
import java.util.*;

import static android.view.View.MeasureSpec.*;

@SuppressWarnings({"unused"})
public class util {

    private static final Looper mainLooper = Looper.getMainLooper();
    private static final Handler mainHandler = new Handler(mainLooper);
    private static final Thread mainThread = mainLooper.getThread();
    private static final String[] NONE = new String[]{null};
    private static final String lineSeparator = System.getProperty("line.separator");
    private static final int lineSeparatorChar = lineSeparator.charAt(0);
    private static boolean systemStreamsRedirected;

    static {
        redirectSystemStreams();
    }

    private util() {}

    public static void invalidateAll(View v) {
        if (v != null) {
            v.invalidate();
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup)v;
                int n = vg.getChildCount();
                for (int i = 0; i < n; i++) {
                    invalidateAll(vg.getChildAt(i));
                }
            }
        }
    }

    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static boolean equals(CharSequence ca1, CharSequence ca2) {
        if (ca1 != null && ca2 != null && ca1.length() == ca2.length()) {
            int n = ca1.length();
            for (int i = 0; i < n; i++) {
                if (ca1.charAt(i) != ca2.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
        return equals(ca1, ca2);
    }

    public static float unitToPixels(int unit, float size) {
        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        if (dm.xdpi == 75 && dm.widthPixels >= 1920 && dm.heightPixels >= 1000) {
            dm.xdpi = 100; // most probably unrecognized HDMI monitor with dpi > 90
            dm.ydpi = 100;
        }
        return TypedValue.applyDimension(unit, size, dm);
    }

    public static boolean isMainThread() {
        return mainThread == Thread.currentThread();
    }

    public static void assertion(boolean b) {
        if (!b) {
            StackTraceElement caller = getCallersCaller(4);
            Class cc = forName(caller.getClassName());
            trace0(cc.getPackage().getName(), cc, caller.getMethodName(), caller.getLineNumber(),
                   "assertion failed");
            rethrow(new AssertionError());
        }
    }

    public static void assertion(boolean b, CharSequence msg) {
        if (!b) {
            StackTraceElement caller = getCallersCaller(4);
            Class cc = forName(caller.getClassName());
            trace0(cc.getPackage().getName(), cc, caller.getMethodName(), caller.getLineNumber(),
                   "assertion failed: \"", msg, "\"");
            rethrow(new AssertionError(msg));
        }
    }

    public static void rethrow(Throwable t) {
        if (t != null) {
            throw t instanceof Error ? (Error)t : new Error(t);
        }
    }

    public static void trace(CharSequence... params) {
        if (params != null && params.length > 0) {
            StackTraceElement caller = getCallersCaller(4);
            Class cc = forName(caller.getClassName());
            trace0(cc.getPackage().getName(), cc,
                   caller.getMethodName(), caller.getLineNumber(), params);
        }
    }

    public static void trace(Throwable t) {
        trace(t, NONE);
    }

    public static StackTraceElement getCallersCaller(int n) {
        return Thread.currentThread().getStackTrace()[n];
    }

    public static Class<?> forName(String n) {
        try { return n == null ? null : Class.forName(n); } catch (ClassNotFoundException e) { return null; }
    }

    public static void trace(Throwable t, CharSequence... params) {
        Text b;
        synchronized (util.class) { b = Texts.getBuffer(); }
        final Text cb = b;
        try {
            PrintStream ps = new PrintStream(new OutputStream() {
                public void write(int oneChar) throws IOException { cb.write(oneChar); }
            });
            if (params != null && params.length > 0) {
                for (CharSequence p : params) {
                    if (p != null) {
                        ps.print(p);
                        ps.print(' ');
                    }
                }
                ps.println();
            }
            t.printStackTrace(ps);
            ps.close();
            trace(cb);
        } finally {
            synchronized (util.class) { Texts.recycle(cb); }
        }
    }

    public static void trace0(String pkg, Class caller, String method, int line, CharSequence... params) {
        if (params != null && params.length > 0) {
            Text cb = null;
            try {
                synchronized (util.class) { cb = Texts.getBuffer(); }
                cb.append(caller.getSimpleName()).append('.').append(method).append(':');
                cb.append(line).append(' ');
                for (CharSequence p : params) {
                    if (p != null) {
                        cb.append(p).append(' ');
                    }
                }
                String s = cb.toString().trim(); // TODO: need native Log function with char[] param
                Log.d(pkg, s);
            } finally {
                synchronized (util.class) { Texts.recycle(cb); }
            }
        }
    }

    private static final HashMap<String, Long> start = new HashMap<String, Long>();
    private static long self_time; // = 0
    private static boolean trace_timestamp;

    public synchronized static long timestamp(String label) { // returns delta in nanoseconds
        if (self_time == 0) {
            self_time = 1;
            timestamp("timestamp-self-delta");
            self_time = timestamp("timestamp-self-delta");
            if (self_time <= 0) {
                self_time = 1;
            }
            trace_timestamp = true;
        }
        long t = System.nanoTime();
        Long s = start.remove(label);
        if (s == null) {
            start.put(label, t);
            return 0;
        } else {
            long delta = t - s;
            delta = delta < 1 ? 1 : delta;
            if (trace_timestamp) {
                System.err.print("time: \""); System.err.print(label); System.err.print("\" ");
                System.err.print(humanReadable(delta)); System.err.println(humanReadableSuffix(delta));
            }
            return delta;
        }
    }

    public static String humanReadableSuffix(long delta) {
        if (delta < 10L * 1000) {
            return " nanoseconds";
        } else if (delta < 10L * 1000 * 1000) {
            return " microseconds";
        } else if (delta < 10L * 1000 * 1000 * 1000) {
            return " milliseconds";
        } else {
            return " seconds";
        }
    }

    public static long humanReadable(long delta) {
        if (delta < 10L * 1000) {
            return delta;
        } else if (delta < 10L * 1000 * 1000) {
            return delta / 1000;
        } else if (delta < 10L * 1000 * 1000 * 1000) {
            return delta / (1000 * 1000);
        } else {
            return delta / (1000 * 1000 * 1000);
        }
    }

    public static void close(Closeable c) {
        if (c != null) {
            try { c.close(); } catch (Throwable ignore) {}
        }
    }

    public static void post(Runnable r) {
        mainHandler.post(r);
    }

    public static void postDelayed(Runnable r, long milliseconds) {
        mainHandler.postDelayed(r, milliseconds);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void postDelayedAndWait(final Runnable r, long milliseconds) {
        final Object lock = new Object();
        Runnable runnable = new Runnable() {
            public void run() { try { r.run(); } finally { synchronized (lock) { lock.notifyAll(); } } }
        };
        synchronized (lock) {
            postDelayed(runnable, milliseconds);
            try { lock.wait(); } catch (InterruptedException e) { trace(e); }
        }
    }

    public static void postAndWait(final Runnable r, long milliseconds) {
        postDelayedAndWait(r, 0);
    }

    public static int measure(int mode, int size, int preferred) {
        return mode == EXACTLY ? size :
              (mode == AT_MOST ? Math.min(preferred, size) : preferred);
    }

    public static void nanosleep(long nanoseconds) {
        try {
            long milliseconds = nanoseconds / 1000000;
            nanoseconds = nanoseconds % 1000000;
            Thread.sleep(milliseconds, (int)nanoseconds);
        } catch (InterruptedException ignore) {
            // just return
        }
    }

    public static void redirectSystemStreams() {
        synchronized (util.class) {
            if (!systemStreamsRedirected) {
                if (lineSeparator.length() != 1) {
                    throw new Error("it might not work on Windows CR/LF");
                }
                System.setOut(new LogPrintStream(new LogOutputStream(), System.out));
                System.setErr(new LogPrintStream(new LogOutputStream(), System.err));
                systemStreamsRedirected = true;
            }
        }
    }

    private static class LogPrintStream extends PrintStream {

        private final PrintStream second;

        public LogPrintStream(OutputStream out, PrintStream was) {
            super(out);
            second = was;
        }

        public void write(int ch)  {
            super.write(ch);
            second.write(ch);
        }

    }

    private static class LogOutputStream extends OutputStream {

        private final Text cb = new Text(1024);

        public LogOutputStream() { }

        public void write(int ch) throws IOException {
            if (ch != lineSeparatorChar && ch != '\n') {
                cb.write(ch);
            } else {
                int n = 4; // variable depth of calls inside java.io.* packages
                StackTraceElement[] s = Thread.currentThread().getStackTrace();
                for (int i = n; i < s.length; i++) {
                    if ("java.io".equals(forName(s[i-1].getClassName()).getPackage().getName()) &&
                       !"java.io".equals(forName(s[i].getClassName()).getPackage().getName())) {
                        n = i;
                        break;
                    }
                }
                while (n < s.length - 1 && forName(s[n].getClassName()).equals(util.class)) {
                    n++;
                }
                StackTraceElement caller = s[n];
                Class cc = forName(caller.getClassName());
                trace0(cc.getPackage().getName(), cc, caller.getMethodName(), caller.getLineNumber(), cb);
                cb.reset();
            }
        }

    }

}
