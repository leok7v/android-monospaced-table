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

    -----------------------------------------------------------------------------

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package android.mono.table.etc;

import static android.mono.table.etc.util.*;

@SuppressWarnings({"unused"})
public class utf8 {

    private static final char REPLACEMENT_CHAR = (char)0xfffd;

    // https://android.googlesource.com/platform/libcore/+/master/libart/src/main/java/java/lang/String.java
    // public String(byte[] data, int offset, int byteCount, Charset charset) { ...

    public static int decode(byte[] d, int offset, int byteCount, char[] v, int s, int len) {
        assertion(v.length + s >= byteCount);
        int idx = offset;
        final int last = offset + byteCount;
        final int limit = s + len;
        outer:
        while (idx < last) {
            byte b0 = d[idx++];
            if (s >= limit) { return -1; }
            if ((b0 & 0x80) == 0) {
                // 0xxxxxxx
                // Range:  U-00000000 - U-0000007F
                int val = b0 & 0xff;
                v[s++] = (char)val;
            } else if (((b0 & 0xe0) == 0xc0) || ((b0 & 0xf0) == 0xe0) ||
                    ((b0 & 0xf8) == 0xf0) || ((b0 & 0xfc) == 0xf8) || ((b0 & 0xfe) == 0xfc)) {
                int utfCount = 1;
                if ((b0 & 0xf0) == 0xe0) {
                    utfCount = 2;
                } else if ((b0 & 0xf8) == 0xf0) {
                    utfCount = 3;
                } else if ((b0 & 0xfc) == 0xf8) {
                    utfCount = 4;
                } else if ((b0 & 0xfe) == 0xfc) {
                    utfCount = 5;
                }
                // 110xxxxx (10xxxxxx)+
                // Range:  U-00000080 - U-000007FF (count == 1)
                // Range:  U-00000800 - U-0000FFFF (count == 2)
                // Range:  U-00010000 - U-001FFFFF (count == 3)
                // Range:  U-00200000 - U-03FFFFFF (count == 4)
                // Range:  U-04000000 - U-7FFFFFFF (count == 5)
                if (idx + utfCount > last) {
                    v[s++] = REPLACEMENT_CHAR;
                    break;
                }
                // Extract usable bits from b0
                int val = b0 & (0x1f >> (utfCount - 1));
                for (int i = 0; i < utfCount; i++) {
                    byte b = d[idx++];
                    if ((b & 0xC0) != 0x80) {
                        v[s++] = REPLACEMENT_CHAR;
                        idx--; // Put the input char back
                        continue outer;
                    }
                    // Push new bits in from the right side
                    val <<= 6;
                    val |= b & 0x3f;
                }
                // Note: Java allows overlong char
                // specifications To disallow, check that val
                // is greater than or equal to the minimum
                // value for each count:
                //
                // count    min value
                // -----   ----------
                //   1           0x80
                //   2          0x800
                //   3        0x10000
                //   4       0x200000
                //   5      0x4000000
                // Allow surrogate values (0xD800 - 0xDFFF) to
                // be specified using 3-byte UTF values only
                if ((utfCount != 2) && (val >= 0xD800) && (val <= 0xDFFF)) {
                    v[s++] = REPLACEMENT_CHAR;
                    continue;
                }
                // Reject chars0 greater than the Unicode maximum of U+10FFFF.
                if (val > 0x10FFFF) {
                    v[s++] = REPLACEMENT_CHAR;
                    continue;
                }
                // Encode chars0 from U+10000 up as surrogate pairs
                if (val < 0x10000) {
                    v[s++] = (char)val;
                } else {
                    int x = val & 0xffff;
                    int u = (val >> 16) & 0x1f;
                    int w = (u - 1) & 0xffff;
                    int hi = 0xd800 | (w << 6) | (x >> 10);
                    int lo = 0xdc00 | (x & 0x3ff);
                    v[s++] = (char)hi;
                    if (s >= limit - 1) { return -1; }
                    v[s++] = (char)lo;
                }
            } else {
                // Illegal values 0x8*, 0x9*, 0xa*, 0xb*, 0xfd-0xff
                v[s++] = REPLACEMENT_CHAR;
            }
        }
        return s;
    }

    // https://code.google.com/p/android-source-browsing/source/browse/common/unicode/utf.h?repo=platform--external--icu4c
    // https://code.google.com/p/android-source-browsing/source/browse/common/unicode/utf16.h?repo=platform--external--icu4c

    private static boolean U16_IS_SURROGATE(int c) {
        return (((c) & 0xfffff800) == 0xd800);
    }

    private static boolean U16_IS_SURROGATE_LEAD(int c) {
        return (((c) & 0x400) == 0);
    }

    private static boolean U16_IS_SURROGATE_TRAIL(int c) {
        return (((c) & 0x400) != 0);
    }

    private static final long U16_SURROGATE_OFFSET = ((0xd800L<<10) + 0xdc00 - 0x10000);

    private static int U16_GET_SUPPLEMENTARY(char lead, char trail) {
        return (int)(((((long)lead) & 0xFFFF) << 10) + ((((long)trail) & 0xFFFF) - U16_SURROGATE_OFFSET));
    }

    // https://android.googlesource.com/platform/libcore/+/jb-mr2-release/luni/src/main/native/java_nio_charset_Charsets.cpp

    public static int encode(char[] chars, int offset, int length, byte[] out, int s, int len) {
        final int end = offset + length;
        final int limit = s + len;
        assertion(limit <= out.length);
        for (int i = offset; i < end; ++i) {
            int ch = chars[i];
            if (ch < 0x80) {
                // One byte.
                if (s >= limit) {
                    return -1;
                }
                out[s++] = (byte)ch;
            } else if (ch < 0x800) {
                if (s >= limit - 2) {
                    return -1;
                }
                out[s++] = (byte)((ch >> 6) | 0xc0);
                out[s++] = (byte)((ch & 0x3f) | 0x80);
            } else if (U16_IS_SURROGATE(ch)) {
                // A supplementary character.
                char high = (char) ch;
                char low = (i + 1 != end) ? chars[i + 1] : 0;
                if (!U16_IS_SURROGATE_LEAD(high) || !U16_IS_SURROGATE_TRAIL(low)) {
                    if (s >= limit) {
                        return -1;
                    }
                    out[s++] = '?';
                    continue;
                }
                // Now we know we have a *valid* surrogate pair, we can consume the low surrogate.
                ++i;
                ch = U16_GET_SUPPLEMENTARY(high, low);
                if (s >= limit - 4) {
                    return -1;
                }
                out[s++] = (byte)((ch >> 18) | 0xf0);
                out[s++] = (byte)(((ch >> 12) & 0x3f) | 0x80);
                out[s++] = (byte)(((ch >> 6) & 0x3f) | 0x80);
                out[s++] = (byte)((ch & 0x3f) | 0x80);
            } else {
                if (s >= limit - 3) {
                    return -1;
                }
                out[s++] = (byte)((ch >> 12) | 0xe0);
                out[s++] = (byte)(((ch >> 6) & 0x3f) | 0x80);
                out[s++] = (byte)((ch & 0x3f) | 0x80);
            }
        }
        return s;
    }

/*
    private static final String hello = "안녕하세요 你好 世界 العالممرح Привет Мир!";
    private static final char[] helloChars = hello.toCharArray();
    private static final byte[] helloBytes = hello.getBytes();
    private static byte[] bytes = new byte[80];
    private static char[] chars = new char[80];

    public static void smokeTest() {
        int k = decode(helloBytes, 0, helloBytes.length, chars, 0, chars.length);
        assertion(k == helloChars.length);
        if (k > 0) {
            for (int i = 0; i < k; i++) {
                assertion(helloChars[i] == chars[i]);
            }
            int n = encode(chars, 0, k, bytes, 0, bytes.length);
            assertion(n == helloBytes.length);
            for (int i = 0; i < n; i++) {
                assertion(helloBytes[i] == bytes[i]);
            }
        }
        final int N = 1000;
        timestamp("decode");
        for (int i = 0; i < N; i++) { decode(helloBytes, 0, helloBytes.length, chars, 0, chars.length); }
        timestamp("decode");

        timestamp("encode");
        for (int i = 0; i < N; i++) { encode(chars, 0, k, bytes, 0, chars.length); }
        timestamp("encode");
    }

//  time: "decode" 109 microseconds
//  time: "encode" 81 microseconds

    static {
        smokeTest();
    }
*/

}
