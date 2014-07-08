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

import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.mono.table.etc.*;
import android.mono.table.etc.Text;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;

import static android.mono.table.etc.util.*;

/* Words expect word list to be a raw uncompressed ASCII text resource with non-empty words separated by
   a single "\n" character. The extension ".smf" is used instead of ".txt" to prevent Android "aapt" from
   zip compressing words list. See:
   http://stackoverflow.com/questions/11276112/is-it-possible-to-add-specific-files-uncompressed-to-a-android-apk-using-build-x
   https://groups.google.com/forum/#!topic/android-developers/A5B03t9EKQI
   https://groups.google.com/forum/#!topic/android-ndk/gnSkxRzJzB0
*/

public final class Words implements DataModel {

    private final Context context;
    private final Rect[] bounds = new Rect[2];
    private final Rect rc = new Rect();
    private char[] chars;
    private int[] offsets;
    private int[] lengths; // is necessary because of trailing "\n" at the end of the file and a bit faster too
    private AssetFileDescriptor afd;
    private FileChannel channel;
    private final Text number = new Text();
    private final CharArray word = new CharArray();
    private boolean once;

    public Words(Context ctx) {
        context = ctx;
    }

    public void open(Object... args) {
        afd = context.getResources().openRawResourceFd((Integer)args[0]);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(afd.getFileDescriptor());
            channel = new FileInputStream(afd.getFileDescriptor()).getChannel();
        } finally {
            util.close(fis);
        }
    }

    public void close() {
        if (afd != null) { // AssetFileDescriptor does not implement Closeable
            try { afd.close(); } catch (IOException e) { trace(e); }
            afd = null;
        }
        util.close(channel);
    }

    public void update(final Runnable done) {
        if (!once) {
            once = true;
            new Thread() {
                public void run() {
                    read();
                    if (done != null) {
                        post(done);
                    }
                }
            }.start();
        } else {
            if (done != null) {
                done.run();
            }
        }
    }

    private char[] array() {
        return chars;
    }

    private int offset(int i) {
        return offsets[i];
    }

    private int length(int i) {
        return lengths[i];
    }

    public final int columns() {
        return 2;
    }

    public final int rows() {
        return offsets.length;
    }

    public TextInterface getText(int c, int r) {
        if (c == 0) {
            word.wrap(array(), offset(r), length(r));
            return word;
        } else {
            number.reset();
            number.append(length(r), 10);
            return number;
        }
    }

    public Rect bounds(int c, int r, Paint paint) {
        if (bounds[c] == null) {
            measure(paint);
        }
        return bounds[c];
    }

    private void read() {
        // about 3 times faster than BufferedReader readLine (500 versus 1500 milliseconds)
        try {
            int len = (int)afd.getLength();
            MappedByteBuffer mem = channel.map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), len);
            assertion(!mem.hasArray(), "otherwise can and should use mem.array() and mem.arrayOffset()");
            byte[] bytes = new byte[len];
            mem.get(bytes); // ~40 milliseconds
            Charset utf8 = Charset.forName("UTF-8");
            CharBuffer cb = utf8.decode(ByteBuffer.wrap(bytes, 0, len));
            chars = cb.array();
            len = cb.length();
            // see: http://en.wikipedia.org/wiki/Byte_order_mark#Representations_of_byte_order_marks_by_encoding
            int offset0 = bytes[0] == (byte)(0xEF) && bytes[1] == (byte)(0xBB) && bytes[2] == (byte)(0xBF) ? 3 : 0;
            int offset1 = chars[0] == (char)(0xFEFF) ? 1 : 0;
            assertion(bytes.length - offset0 == len - offset1, "ASCII assumption for words list did not hold");
            // ~370 milliseconds
            int count = 0;
            for (int i = 0; i < len; i++) {
                count += chars[i] == '\n' ? 1 : 0;
            }
            boolean hasLastLineBreak = chars[len - 1] == '\n';
            count += !hasLastLineBreak ? 1 : 0;
            offsets = new int[count];
            lengths = new int[count];
            int n = 0;
            int start = offset1;
            for (int i = offset1; i < len; i++) {
                if (chars[i] == '\n') {
                    offsets[n] = start;
                    lengths[n] = i - 1 - start;
                    n++;
                    start = i + 1;
                }
            }
            if (!hasLastLineBreak) {
                offsets[n] = start;
                lengths[n] = len - start;
                n++;
            }
            assertion(n == count);
            assertion(offsets[n - 1] + lengths[n - 1] == len || offsets[n - 1] + lengths[n - 1] == len - 1,
                      "offsets[n - 1]=" + offsets[n - 1] + " lengths[n - 1]=" + lengths[n - 1] + " len=" + len);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void measure(Paint paint) {
        // measuring all 32,565 words takes up to 8 seconds. Thus we will measure only last 325 (1%)
        // longest words of them (assuming words list is ordered alphabetically)
        assertion(bounds[0] == null);
        bounds[0] = new Rect();
        int n = rows();
        int s = n - n / 100;
        Rect b = bounds[0];
        for (int i = s; i < n; i++) {
            paint.getTextBounds(array(), offset(i), length(i), rc);
            b.left = Math.min(b.left, rc.left);
            b.top = Math.min(b.top, rc.top);
            b.right = Math.max(b.right, rc.right);
            b.bottom = Math.max(b.bottom, rc.bottom);
        }
        b.right /= 2; /* ~ `average' width. long words will be clipped in half */
        assertion(bounds[1] == null);
        bounds[1] = new Rect();
        Text text = Texts.getBuffer();
        try {
            int k = length(n-1);
            text.append(k, 10);
            paint.getTextBounds(text.array(), 0, k, bounds[1]);
        } finally {
            Texts.recycle(text);
        }
    }

}
