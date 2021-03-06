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

import static android.mono.table.etc.util.*;

public class Act extends BaseActivity {

    private DataModel words;
    private DataModel cpu;
    private DataModel mem;
    private DataModel jvm;
    private TableView tableCPU;
    private TableView tableMem;
    private TableView tableJVM;

    private final Runnable invalidateCPU = new Runnable() { public void run() { tableCPU.invalidate(); } };
    private final Runnable invalidateMem = new Runnable() { public void run() { tableMem.invalidate(); } };
    private final Runnable invalidateJVM = new Runnable() { public void run() { tableJVM.invalidate(); } };

    private final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(C.WRAP_WRAP) {{ setMargins(9, 7, 9, 7); }};
    private final int[] repaint = new int[1];
    private CheckBox cbx;
    private boolean running;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        words = new Words(this);
        mem = new MemInfo();
        cpu = new CPU();
        jvm = new MemJVM();
        words.open(R.raw.words);
        jvm.open(repaint, new String[]{"draw"});
        mem.open("/proc/meminfo");
        cpu.open("/proc/stat");
        words.update(wordsLoaded);
        setContentView(createLinerLayout(LinearLayout.VERTICAL));
    }

    public void onResume() {
        super.onResume();
        running = true;
        if (cv.getChildCount() > 0) {
            jvm.update(invalidateJVM);
            post(updateProcFS);
        }
    }

    public void onPause() {
        super.onPause();
        running = false;
    }

    public void onDestroy() {
        cv.removeAllViews();
        close(jvm);
        close(mem);
        close(cpu);
        close(words);
        super.onDestroy();
    }

    private final Runnable wordsLoaded = new Runnable() { public void run() { wordsLoaded(); } };

    private void wordsLoaded() { mem.update(memUpdate); }

    private final Runnable memUpdate = new Runnable() { public void run() { memUpdated(); } };

    private void memUpdated() { cpu.update(cpuUpdated); }

    private final Runnable cpuUpdated = new Runnable() { public void run() { cpuUpdated(); } };

    private void cpuUpdated() {
        if (cv.getChildCount() == 0) {
            createViews();
        } else {
            post(invalidateCPU);
            post(invalidateMem);
        }
    }

    private final Runnable updateProcFS = new Runnable() {
        public void run() {
            if (running) {
                cpu.update(invalidateCPU);
                mem.update(invalidateMem);
                postDelayed(updateProcFS, C.PROCFS_REFRESH_IN_MILLIS);
            }
        }
    };

    private void createViews() {
        assertion(cv.getChildCount() == 0);
        addJavaMemoryAndRedrawButtonPanel();
        LinearLayout hl = createLinerLayout(LinearLayout.HORIZONTAL);
        hl.addView(createWordsPanel(), lp);
        hl.addView(createProcFSPanel(), lp);
        cv.addView(hl, lp);
        post(updateProcFS);
    }

    private void addJavaMemoryAndRedrawButtonPanel() {
        LinearLayout hl = createLinerLayout(LinearLayout.HORIZONTAL);
        tableJVM = new TableView(this) {
            public void draw(Canvas canvas) {
                super.draw(canvas);
                if (cbx.isChecked() && running) {
                    repaint[0]++;
                    jvm.update(invalidateJVM);
                }
            }
        }.setModel(new PaintTableModel(jvm, G.monospaced) {
            public int justify(int c, int r) { return TableModel.RIGHT_JUSTIFIED; }
            protected int color(int c, int r) { return r == 0 ? C.NC_VERDIGRIS : C.NC_GOLD; }
        });
        hl.addView(scrollableTable(tableJVM));
        cbx = createCheckBox(hl);
        hl.setPadding(5, 5, 5, 5);
        cv.addView(hl, lp);
    }

    private View createWordsPanel() {
        return scrollableTable(new TableView(this).setModel(new PaintTableModel(words, G.monospaced) {
            public int justify(int c, int r) {
                return c == 0 ? TableModel.LEFT_JUSTIFIED : TableModel.RIGHT_JUSTIFIED;
            }
            protected int color(int c, int r) {
                return c == 0 ? C.NC_GOLD : (c == 1 ? C.NC_LTBLUE : C.NC_VERDIGRIS);
            }
        }));
    }

    private ViewGroup createProcFSPanel() {
        tableCPU = new TableView(this).setModel(new PaintTableModel(cpu, G.monospaced) {
            public int justify(int c, int r) { return c == 0 ? TableModel.LEFT_JUSTIFIED : TableModel.RIGHT_JUSTIFIED; }
            protected int color(int c, int r) { return c == 0 ? C.NC_GOLD : (c == 1 ? C.NC_LTBLUE : C.NC_VERDIGRIS); }
        });
        tableMem = new TableView(this).setModel(new PaintTableModel(mem, G.monospaced) {
            public int justify(int c, int r) { return c != 1 ? TableModel.LEFT_JUSTIFIED : TableModel.RIGHT_JUSTIFIED; }
            protected int color(int c, int r) { return c == 0 ? C.NC_GOLD : (c == 1 ? C.NC_LTBLUE : C.NC_VERDIGRIS); }
        });
        TabGroup tg = new TabGroup(this);
        tg.setBackgroundColor(C.NC_DKBLUE);
        tg.addTab("CPU", scrollableTable(tableCPU));
        tg.addTab("Mem", scrollableTable(tableMem));
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

    private CheckBox createCheckBox(ViewGroup parent) {
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
