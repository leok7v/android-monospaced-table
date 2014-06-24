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

import java.io.*;
import java.util.*;

@SuppressWarnings({"unused"})
public class Text extends CharArrayWriter implements TextInterface {

    private final Formatter formatter = new Formatter(this);

    public Text() {
        this(512); /* 0.5KB reserved for formatter character array (CharArrayWriter default is 32 characters */
    }

    public Text(int capacity) {
        super(capacity);
    }

    public static boolean startsWith(CharSequence cs1, CharSequence cs2) {
        if (cs1.length() >= cs2.length()) {
            int n = cs2.length();
            for (int i = 0; i < n; i++) {
                if (cs1.charAt(i) != cs2.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns direct access to the formatted character array buffer of the length() characters buffer;
     * The buffer can change if reset() / format() append() are called,
     * If you want to obtain a copy-snapshot of content call toCharArray() which returns a copy.
     * @return direct character array reference
     */
    public char[] array() {
        return buf;
    }

    public int offset() {
        return 0;
    }

    public Formatter format(String format, Object... args) {
        return formatter.format(format, args);
    }

    public int length() {
        return count;
    }

    public char charAt(int index) {
        return buf[index];
    }

    public CharSequence subSequence(int start, int end) {
        return new CharSubSequence(buf, start, end);
    }

    public CharArrayWriter append(CharSequence csq, int start, int end) {
        if (csq == null) {
            write("null", 0, 4);
        } else if (csq instanceof Text) {
            Text cb = (Text)csq;
            write(cb.array(), 0, cb.length());
        } else {
            for (int i = 0; i < csq.length(); i++) {
                super.append(csq.charAt(i));
            }
        }
        return this;
    }

    public CharArrayWriter append(char[] ca, int start, int end) {
        if (ca == null) {
            write("null", 0, 4);
        } else {
            write(ca, start, end - start);
        }
        return this;
    }

    public CharArrayWriter append(char[] ca) {
        append(ca, 0, ca.length);
        return this;
    }

    public CharArrayWriter append(String s) {
        if (s == null) {
            write("null", 0, 4);
        } else {
            write(s, 0, s.length());
        }
        return this;
    }

    public CharArrayWriter append(double v) {
        Numbers.getInstance().appendDouble(this, v);
        return this;
    }

    public CharArrayWriter append(float v) {
        return Numbers.getInstance().appendFloat(this, v);
    }


    public CharArrayWriter append(long v, int radix) {
        Numbers.getInstance().appendLong(this, v, radix);
        return this;
    }

    public CharArrayWriter append(byte b, int radix) {
        return append((long)b, radix);
    }

    public CharArrayWriter append(short s, int radix) {
        return append((long)s, radix);
    }

    public CharArrayWriter append(int i, int radix) {
        return append((long)i, radix);
    }

    public CharArrayWriter append(byte b) {
        return append((long)b, 10);
    }

    public CharArrayWriter append(short s) {
        return append((long)s, 10);
    }

    public CharArrayWriter append(int i) {
        return append((long)i, 10);
    }

    public CharArrayWriter append(long v) {
        return append(v, 10);
    }

    public boolean equals(Object o) {
        return o instanceof CharSequence ? util.equals(this, (CharSequence)o) : super.equals(o);
    }

    private static class CharSubSequence implements CharSequence {

        private final int start;
        private final int end;
        private final char[] buf;

        CharSubSequence(char[] b, int s, int e) {
            buf = b;
            start = s;
            end = e;
        }

        public int length() {
            return end - start;
        }

        public char charAt(int index) {
            return buf[start + index];
        }

        public CharSequence subSequence(int s, int e) {
            return new CharSubSequence(buf, start + s, start + e);
        }
    }

}
