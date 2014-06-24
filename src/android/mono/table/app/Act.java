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

package android.mono.table.app;

import android.graphics.*;
import android.mono.table.*;
import android.mono.table.data.*;
import android.mono.table.ui.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.mono.table.etc.*;

import static android.mono.table.etc.util.*;

public class Act extends BaseActivity {

    private static final int CPU_REFRESH_IN_MILLIS = 5 * 1000;

    private DataModel words;
    private DataModel cpu;
    private DataModel sysMem;
    private DataModel javaMem;
    private long cpuNanos = System.nanoTime();
    private TableView tableCPU;
    private TableView tableSysMem;
    private TableView tableJavaMem;
    private final Paint paint = new Paint();
    private final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(C.WRAP_WRAP) {{ setMargins(9, 7, 9, 7); }};
    private final int[] repaint = new int[1];
    private CheckBox cbx;
    private boolean running;
    private Typeface typeface;

    public final Typeface getTypeface() {
        return typeface;
    }

    public final float getTextSizePoints() {
        return 8;
    }

    public final float getTextSizePixels() {
        return unitToPixels(C.POINT, getTextSizePoints());
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        typeface = TypefaceLoader.loadTypeface(this, "DejaVuSansMono-Bold");
        words = new Words(this);
        sysMem = new SysMem();
        cpu = new CPU();
        javaMem = new JavaMem();
        paint.setColor(C.NC_LTBLUE);
        paint.setStyle(Paint.Style.FILL);
        paint.setFlags(paint.getFlags() | Paint.SUBPIXEL_TEXT_FLAG|Paint.ANTI_ALIAS_FLAG|Paint.DEV_KERN_TEXT_FLAG);
        if (!typeface.isBold()) {
            paint.setFlags(paint.getFlags() | Paint.FAKE_BOLD_TEXT_FLAG);
        }
        paint.setTypeface(typeface);
        paint.setTextSize(getTextSizePixels());
        setContentView(createLinerLayout(LinearLayout.VERTICAL));
    }

    public void onStart() {
        super.onStart();
        running = true;
        javaMem.open((Object)repaint);
        words.open(R.raw.words);
        sysMem.open("/proc/meminfo");
        cpu.open("/proc/stat");
        javaMem.update(null);
        cpu.update(null);
        words.update(wordsLoaded);
    }

    public void onPause() {
        super.onPause();
        running = false;
        close(words);
        close(sysMem);
        close(sysMem);
        close(cpu);
    }

    private final Runnable cpuLoaded = new Runnable() { public void run() { cpuLoaded(); } };

    private void cpuLoaded() {
        if (running) {
            createViews();
        }
    }

    private final Runnable memInfoLoaded = new Runnable() { public void run() { memInfoLoaded(); } };

    private void memInfoLoaded() {
        if (running) {
            cpu.update(cpuLoaded);
        }
    }

    private final Runnable wordsLoaded = new Runnable() { public void run() { wordsLoaded(); } };

    private void wordsLoaded() {
        if (running) {
            sysMem.update(memInfoLoaded);
        }
    }

    private final Runnable invalidateCPU = new Runnable() {
        public void run() {
            long now = System.nanoTime();
            long delay = CPU_REFRESH_IN_MILLIS - (now - cpuNanos) * 1000 / C.NANOS_IN_SECOND;
            tableCPU.postInvalidateDelayed(Math.min(delay, CPU_REFRESH_IN_MILLIS));
            cpuNanos = now;
        }
    };

    private final Runnable invalidateSysMem = new Runnable() {
        public void run() {
            tableSysMem.invalidate();
        }
    };

    private final Runnable invalidateJavaMem = new Runnable() {
        public void run() {
            tableJavaMem.invalidate();
        }
    };

    private void createViews() {
        addJavaMemoryAndRedrawButtonPanel();
        LinearLayout hl = createLinerLayout(LinearLayout.HORIZONTAL);
        addWordsPanel(hl);
        hl.addView(addCpuAndSysMemoryPanel(), lp);
        cv.addView(hl, lp);
    }

    private void addJavaMemoryAndRedrawButtonPanel() {
        LinearLayout hl = createLinerLayout(LinearLayout.HORIZONTAL);
        tableJavaMem = new TableView(this) {
            public void draw(Canvas canvas) {
                super.draw(canvas);
                if (cbx.isChecked()) {
                    repaint[0]++;
                    javaMem.update(invalidateJavaMem);
                }
            }
        }.setModel(new PaintTableModel(javaMem, paint) {
            public int justify(int c, int r) { return TableModel.RIGHT_JUSTIFIED; }
            protected int color(int c, int r) { return r == 0 ? C.NC_VERDIGRIS : C.NC_GOLD; }
        });
        hl.addView(scrollableTable(tableJavaMem));
        cbx = addCheckBox(hl);
        hl.setPadding(5, 5, 5, 5);
        cv.addView(hl, lp);
    }

    private void addWordsPanel(ViewGroup parent) {
        parent.addView(scrollableTable(new TableView(this).setModel(new PaintTableModel(words, paint) {
            public int justify(int c, int r) {
                return c == 0 ? TableModel.LEFT_JUSTIFIED : TableModel.RIGHT_JUSTIFIED;
            }
            protected int color(int c, int r) {
                return c == 0 ? C.NC_GOLD : (c == 1 ? C.NC_LTBLUE : C.NC_VERDIGRIS);
            }
        })));
    }

    private ViewGroup addCpuAndSysMemoryPanel() {
        tableCPU = new TableView(this) {
            public void draw(Canvas canvas) {
                super.draw(canvas);
                cpu.update(invalidateCPU);
            }
        }.setModel(new PaintTableModel(cpu, paint) {
            public int justify(int c, int r) { return c == 0 ? TableModel.LEFT_JUSTIFIED : TableModel.RIGHT_JUSTIFIED; }
            protected int color(int c, int r) { return c == 0 ? C.NC_GOLD : (c == 1 ? C.NC_LTBLUE : C.NC_VERDIGRIS); }
        });
        tableSysMem = new TableView(this) {
            public void draw(Canvas canvas) {
                super.draw(canvas);
                if (cbx.isChecked()) {
                    sysMem.update(invalidateSysMem);
                }
            }
        }.setModel(new PaintTableModel(sysMem, paint) {
            public int justify(int c, int r) { return c != 1 ? TableModel.LEFT_JUSTIFIED : TableModel.RIGHT_JUSTIFIED; }
            protected int color(int c, int r) { return c == 0 ? C.NC_GOLD : (c == 1 ? C.NC_LTBLUE : C.NC_VERDIGRIS); }
        });
        TabGroup tg = new TabGroup(this);
        tg.setBackgroundColor(C.NC_DKBLUE);
        tg.addTab("CPU", scrollableTable(tableCPU));
        tg.addTab("Mem", scrollableTable(tableSysMem));
        return tg;
    }

    private LinearLayout createLinerLayout(int orientation) {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(orientation);
        ll.setBackgroundColor(Color.BLACK);
        return ll;
    }

    private ViewGroup scrollableTable(TableView tv) {
        tv.setBackgroundColor(C.NC_DKBLUE);
        tv.setPadding(5, 5, 5, 5);
        ScrollView sv = new ScrollView(this);
        sv.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // IMPORTANT - otherwise TableView is cached
        sv.setFillViewport(true); // to draw background in whole viewport of scroll view
        sv.setOverScrollMode(ScrollView.OVER_SCROLL_ALWAYS);
        sv.addView(tv);
        sv.setPadding(3, 3, 3, 3);
        return sv;
    }

    private CheckBox addCheckBox(ViewGroup parent) {
        CheckBox cbx = new CheckBox(this);
        cbx.setChecked(true);
        cbx.setText("Redraw Continuously");
        cbx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { invalidateAll(cv); }
        });
        parent.addView(cbx, lp);
        return cbx;
    }

}

/* Other Typeface options:
//      http://en.wikipedia.org/wiki/Bitstream_Vera
//      http://www.fontsquirrel.com/fonts/Bitstream-Vera-Sans-Mono
//      http://en.wikipedia.org/wiki/DejaVu_fonts
//      http://www.fontsquirrel.com/fonts/dejavu-sans-mono
//      typeface = TypefaceLoader.loadTypeface(this, "pcsenior");
//      typeface = TypefaceLoader.loadTypeface(this, "VeraMono-Bold");
//      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
*/