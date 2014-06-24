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

public class JavaMem implements DataModel {

    private final static String[] header = {"free", "total", "max KB", "draw"};
    private final Text[] head = new Text[] { new Text(32), new Text(32), new Text(32), new Text(32) };
    private final Text[] text = new Text[] { new Text(32), new Text(32), new Text(32), new Text(32) };
    private int[] repaint;
    private Rect bounds;

    public void open(Object... args) {
        repaint = (int[])args[0];
        for (int i = 0; i < text.length; i++) {
            head[i].append(header[i]);
        }
    }

    public void close() { }

    public void update(Runnable done) {
        Runtime rt = Runtime.getRuntime();
        for (int i = 0; i < text.length; i++) {
            text[i].reset();
            switch (i) {
                case 0: text[i].append(rt.freeMemory() / 1024); break;
                case 1: text[i].append(rt.totalMemory() / 1024); break;
                case 2: text[i].append(rt.maxMemory() / 1024); break;
                case 3: text[i].append(repaint[0]); break;
                default: throw new Error("unexpected column " + i);
            }
        }
        if (done != null) {
            done.run();
        }
    }

    public int columns() {
        return 4;
    }

    public int rows() {
        return 2;
    }

    public TextInterface getText(int c, int r) {
        return r == 0 ? head[c] : text[c];
    }

    public Rect bounds(int c, int r, Paint paint) {
        if (bounds == null) {
            bounds = new Rect();
            String s = "9999999"; // 9,999MB
            paint.getTextBounds(s, 0, s.length(), bounds);
        }
        return bounds;
    }

}
