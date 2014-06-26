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

package android.mono.table.data;

import android.graphics.*;
import android.mono.table.app.*;
import android.mono.table.etc.*;

import static android.mono.table.etc.util.*;

public final class CPU extends ProcFS {

    // in User HERTZ see: see: https://www.kernel.org/doc/Documentation/filesystems/proc.txt
    public static class Stats {
        int cpu;
        long user;
        long nice;
        long system;
        long idle;
        long iowait;
        long irq;
        long softirq;
        float load; // %%
        long nanos; // System.nanoTime() timestamp
    }

    private final int ap = Runtime.getRuntime().availableProcessors();
    private final Stats[][] stats = new Stats[2][];
    private final Stats[] delta = new Stats[ap + 1];
    private final Text text = new Text(32);
    private final TextWrap buf = new TextWrap();
    private int measuring;

    public CPU() {
        super(8);
        for (int i = 0; i < delta.length; i++) {
            delta[i] = new Stats();
        }
        for (int k = 0; k < stats.length; k++) {
            stats[k] = new Stats[delta.length];
            for (int i = 0; i < stats[k].length; i++) {
                stats[k][i] = new Stats();
            }
        }
    }

    public int columns() {
        return ap + 2;
    }

    public int rows() { return 9; }

    public TextInterface getText(int c, int r) {
        Stats[] st = delta;
        text.reset();
        if (measuring > 0) {
            text.append("100.00%"); // the longest
        } else if (c == 0) {
            switch (r) {
                case 0: text.append("cpu"); break;
                case 1: text.append("user"); break;
                case 2: text.append("nice"); break;
                case 3: text.append("system"); break;
                case 4: text.append("idle"); break;
                case 5: text.append("iowait"); break;
                case 6: text.append("irq"); break;
                case 7: text.append("softirq"); break;
                case 8: text.append("load"); break;
            }
        } else {
            c = c - 1;
            switch (r) {
                case 0: if (st[c].cpu < 0) { text.append("all"); } else { text.append(st[c].cpu); } break;
                case 1: text.append(st[c].user); break;
                case 2: text.append(st[c].nice); break;
                case 3: text.append(st[c].system); break;
                case 4: text.append(st[c].idle); break;
                case 5: text.append(st[c].iowait); break;
                case 6: text.append(st[c].irq); break;
                case 7: text.append(st[c].softirq); break;
                case 8: text.append(st[c].load); text.append('%'); break;
            }
        }
        return text;
    }

    public void measure(Paint paint) {
        measuring++;
        super.measure(paint);
        measuring--;
    }

/*
    https://www.kernel.org/doc/Documentation/filesystems/proc.txt
    cpu  8586298 1913 1515090 75456849 5603 831 16 0 0 0
    cpu0 5170237 939 846929 36632011 1871 817 14 0 0 0
    cpu1 3416060 973 668160 38824837 3731 13 2 0 0 0
*/

    protected void parse(Data d, int len, int count, boolean hasLastLineBreak) {

        count = Math.min(count, ap + 1);
        int c = 0;
        int n = 0;
        int start = 0;
        int i = 0;
        while (i < len && n < count) {
            if (c < d.offsets.length && d.chars[i] != '\n') {
                while (start < len && d.chars[start] != '\n' && Character.isWhitespace(d.chars[start])) {
                    start++;
                }
                i = start;
                while (i < len && !Character.isWhitespace(d.chars[i])) {
                    i++;
                }
                d.offsets[c][n] = start;
                d.lengths[c][n] = i - start;
                start = i;
                c++;
            } else if (d.chars[i] == '\n') {
                if (c < d.offsets.length) {
                    d.offsets[c][n] = start;
                    d.lengths[c][n] = i - start;
                }
                n++;
                start = i + 1;
                c = 0;
            }
            i++;
        }
        if (!hasLastLineBreak && c < d.offsets.length) {
            d.offsets[c][n] = start;
            d.lengths[c][n] = len - start;
            n++;
        }
        assertion(n == count);
        textToStats(d, n);
    }

    private void textToStats(Data d, int n) {
        Stats[] st = stats[ix];
        for (int c = 0; c < n; c++) {
            st[c].user = st[c].nice = st[c].system = st[c].idle = st[c].iowait = st[c].irq = st[c].softirq = 0;
            st[c].load = 0;
            st[c].nanos = System.nanoTime();
        }
        for (int c = 0; c < n; c++) {
            int cpu = -1;
            int rows = Math.min(d.offsets.length, 8); // ignore: steal, guest, guest_nice
            for (int r = 0; r < rows; r++) {
                buf.wrap(d.chars, d.offsets[r][c], d.lengths[r][c]);
                if (r == 0 && Text.startsWith(buf, "cpu")) {
                    cpu = c == 0 ? 0 : (int)Numbers.parseLong(buf, 3, buf.length() - 3) + 1;
                }
                if (cpu >= 0) {
                    switch (r) {
                        case 0: st[cpu].cpu = cpu - 1; break;
                        case 1: st[cpu].user = Numbers.parseLong(buf, 0, buf.length()); break;
                        case 2: st[cpu].nice = Numbers.parseLong(buf, 0, buf.length()); break;
                        case 3: st[cpu].system = Numbers.parseLong(buf, 0, buf.length()); break;
                        case 4: st[cpu].idle = Numbers.parseLong(buf, 0, buf.length()); break;
                        case 5: st[cpu].iowait = Numbers.parseLong(buf, 0, buf.length()); break;
                        case 6: st[cpu].irq = Numbers.parseLong(buf, 0, buf.length()); break;
                        case 7: st[cpu].softirq = Numbers.parseLong(buf, 0, buf.length()); break;
                        default: assertion(false, "r=" + r);
                    }
                }
            }
            if (cpu >= 0) {
                if (stats[1 - ix][0].user > 0) {
                    st[cpu].load = load(cpu, stats[1 - ix], st);
                }
            }
        }
        delta(st);
    }

    private void delta(Stats[] st) {
        Stats[] prev = stats[1 - ix];
        for (int cpu = 0; cpu < delta.length; cpu++) {
            Stats dt = delta[cpu];
            dt.cpu = st[cpu].cpu; // not a diff
            dt.nanos = st[cpu].nanos - prev[cpu].nanos;
            long milliseconds = dt.nanos * 1000 / C.NANOS_IN_SECOND;
            if (milliseconds > 0) {
                dt.user = (st[cpu].user - prev[cpu].user) * 1000 / milliseconds;
                dt.nice = (st[cpu].nice - prev[cpu].nice) * 1000 / milliseconds;
                dt.system = (st[cpu].system - prev[cpu].system) * 1000 / milliseconds;
                dt.idle = (st[cpu].idle - prev[cpu].idle) * 1000 / milliseconds;
                dt.iowait = (st[cpu].iowait - prev[cpu].iowait) * 1000 / milliseconds;
                dt.irq = (st[cpu].irq - prev[cpu].irq) * 1000 / milliseconds;
                dt.softirq = (st[cpu].softirq - prev[cpu].softirq) * 1000 / milliseconds;
                dt.load = st[cpu].load; // not a diff
            } else {
                dt.user = dt.nice = dt.system = dt.idle = dt.iowait = dt.irq = dt.softirq = 0;
                dt.load = 0;
                dt.nanos = System.nanoTime();
            }
        }
    }

    private float load(int cpu, Stats[] stats0, Stats[] stats1) {
        Stats s0 = stats0[cpu];
        Stats s1 = stats1[cpu];
        long busy_delta = (s1.user + s1.nice + s1.system + s1.irq + s1.softirq) -
                          (s0.user + s0.nice + s0.system + s0.irq + s0.softirq);
        long idle_delta = (s1.idle + s1.iowait) - (s0.idle + s0.iowait);
        long total = busy_delta + idle_delta;
        return total == 0 ? 0 : Math.round(busy_delta * 1000f / total) / 10f;
    }

}
