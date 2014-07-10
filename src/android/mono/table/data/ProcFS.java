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
import android.mono.table.etc.*;

import java.io.*;
import java.util.concurrent.atomic.*;

import static android.mono.table.etc.util.*;

public abstract class ProcFS implements DataModel {

    protected static final class Data {
        public byte[] bytes;
        public char[] chars;
        public int[][] offsets;
        public int[][] lengths;
    }

    private RandomAccessFile raf;
    protected volatile int ix;
    private final AtomicInteger ai = new AtomicInteger();
    private final Data data[] = new Data[2];
    private final CharArray charArray = new CharArray();
    private Rect[] bounds;
    private final Rect rc = new Rect();
    private final Runnable updater = new Runnable() { public void run() { updater(); } };
    private final Runnable read = new Runnable() { public void run() { read(); } };
    private final Runnable QUIT = new Runnable() { public void run() { } };
    private final Runnable[] queue = new Runnable[1];
    private Thread thread;
    private Runnable done;

    public ProcFS(int columns) {
        for (int i = 0; i < data.length; i++) {
            data[i] = new Data();
            data[i].bytes = new byte[4096]; /* most procFS files on my Android are shorter */
            data[i].lengths = new int[columns][];
            data[i].offsets = new int[columns][];
        }
    }

    protected abstract void parse(Data d, int len, int count, boolean hasLastLineBreak);

    public void open(Object... args) {
        try {
            assertion(thread == null && raf == null);
            raf = new RandomAccessFile((String)args[0], "r");
            thread = new Thread(updater);
            thread.start();
        } catch (FileNotFoundException e) {
            throw new Error("failed to open " + args[0]);
        }
    }

    public void close() {
        synchronized (queue) { queue[0] = QUIT; queue.notify(); }
        try { thread.join(); } catch (InterruptedException e) { e.printStackTrace(); }
        util.close(raf);
        raf = null;
        thread = null;
    }

    public void update(Runnable updated) {
        done = updated;
        synchronized (queue) { queue[0] = read; queue.notify(); }
    }

    public int columns() {
        return data[1 - ix].offsets.length;
    }

    public int rows() {
        return data[1 - ix].offsets[0].length;
    }

    public TextInterface getText(int c, int r) {
        charArray.wrap(data[1 - ix].chars, data[1 - ix].offsets[c][r], data[1 - ix].lengths[c][r]);
        return charArray;
    }

    public Rect bounds(int c, int r, Paint paint) {
        if (bounds == null || bounds[c] == null) {
            measure(paint);
        }
        return bounds[c];
    }

    public void measure(Paint paint) {
        int n = rows();
        bounds = new Rect[columns()];
        for (int c = 0; c < bounds.length; c++) {
            assertion(bounds[c] == null);
            bounds[c] = new Rect();
            Rect b = bounds[c];
            for (int r = 0; r < n; r++) {
                TextInterface ti = getText(c, r);
                paint.getTextBounds(ti.array(), ti.offset(), ti.length(), rc);
                b.left = Math.min(b.left, rc.left);
                b.top = Math.min(b.top, rc.top);
                b.right = Math.max(b.right, rc.right);
                b.bottom = Math.max(b.bottom, rc.bottom);
            }
        }
    }

    private void updater() {
        try {
            for (;;) {
                Runnable q = null;
                while (q == null) {
                    synchronized (queue) {
                        q = queue[0];
                        if (q == null) {
                            queue.wait();
                        } else {
                            queue[0] = null;
                        }
                    }
                }
                if (q == QUIT) {
                    break;
                }
                q.run();
                synchronized (queue) {
                    if (done != null) {
                        post(done);
                        done = null;
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void read() {
        Data d = data[ix];
        try {
            int len = 0;
            int remains = d.bytes.length;
            int offset = 0;
            raf.seek(0);
            for (;;) {
                int k = raf.read(d.bytes, offset, remains);
                if (k < 0) {
                    break;
                }
                len += k;
                remains -= k;
                offset += k;
                if (remains == 0) {
                    byte[] realloc = new byte[d.bytes.length * 3 / 2];
                    System.arraycopy(d.bytes, 0, realloc, 0, d.bytes.length);
                    remains = realloc.length - d.bytes.length;
                    d.bytes = realloc;
                }
            }
            if (d.chars == null || d.chars.length != len) {
                d.chars = new char[len];
            }
            utf8.decode(d.bytes, 0, len, d.chars, 0, d.chars.length);
            boolean hasLastLineBreak = d.chars[len - 1] == '\n';
            int count = countLines(d, len, hasLastLineBreak);
            parse(d, len, count, hasLastLineBreak);
            ix = ai.incrementAndGet() % 2;
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private int countLines(Data d, int len, boolean hasLastLineBreak) {
        int count = 0;
        for (int i = 0; i < len; i++) {
            count += d.chars[i] == '\n' ? 1 : 0;
        }
        count += !hasLastLineBreak ? 1 : 0;
        for (int c = 0; c < d.offsets.length; c++) {
            if (d.offsets[c] == null || d.offsets[c].length != count) {
                d.offsets[c] = new int[count];
            }
            if (d.lengths[c] == null || d.lengths[c].length != count) {
                d.lengths[c] = new int[count];
            }
        }
        return count;
    }

}
