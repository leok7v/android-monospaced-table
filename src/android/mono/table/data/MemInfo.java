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

import static android.mono.table.etc.util.*;

public final class MemInfo extends ProcFS {

    public MemInfo() {
        super(3);
    }

    protected void parse(Data d, int len, int count, boolean hasLastLineBreak) {
        int c = 0;
        int n = 0;
        int start = 0;
        int i = 0;
        while (i < len) {
            if (c == 0 && d.chars[i] == ':') {
                d.offsets[0][n] = start;
                d.lengths[0][n] = i - start + 1;
                start = i + 1;
                c++;
            } else if (c == 1) {
                while (start < len && d.chars[start] == ' ') {
                    start++;
                }
                i = start;
                while (i < len && d.chars[i] != ' ') {
                    i++;
                }
                d.offsets[1][n] = start;
                d.lengths[1][n] = i - start;
                c++;
                start = i;
            } else if (d.chars[i] == '\n') {
                d.offsets[2][n] = start;
                d.lengths[2][n] = i - start;
                n++;
                start = i + 1;
                c = 0;
            }
            i++;
        }
        if (!hasLastLineBreak) {
            d.offsets[2][n] = start;
            d.lengths[2][n] = len - start;
            n++;
        }
        assertion(n == count);
    }

}
