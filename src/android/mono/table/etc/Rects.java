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

import android.graphics.*;

/** Rects class manages set of rectangles that could be recycled
    to minimize GC load. Only usable from application main thread.
    Not thread safe. Synchronize externally if needed.
 */

@SuppressWarnings({"unused"})
public class Rects {

    private static final Stack<Rect> ri = new Stack<Rect>(32);
    private static final Stack<RectF> rf = new Stack<RectF>(16);

    public static Rect getRect() {
        Rect r = ri.pop();
        if (r != null) {
            r.left = r.top = r.right = r.bottom = 0;
        } else {
            r = new Rect();
        }
        return r;
    }

    public static RectF getRectF() {
        RectF r = rf.pop();
        if (r != null) {
            r.left = r.top = r.right = r.bottom = 0;
        } else {
            r = new RectF();
        }
        return r;
    }

    public static void recycle(Rect r) {
        ri.push(r);
    }

    public static void recycle(RectF r) {
        rf.push(r);
    }

    public static RectF getRectF(float left, float top, float right, float bottom) {
        RectF r = getRectF();
        r.left = left;
        r.top = top;
        r.right = right;
        r.bottom = bottom;
        return r;
    }

    public static Rect getRect(int left, int top, int right, int bottom) {
        Rect r = getRect();
        r.left = left;
        r.top = top;
        r.right = right;
        r.bottom = bottom;
        return r;
    }

    public static RectF getRectF(RectF s) {
        RectF r = getRectF();
        r.left = s.left;
        r.top = s.top;
        r.right = s.right;
        r.bottom = s.bottom;
        return r;
    }

    public static Rect getRect(Rect s) {
        Rect r = getRect();
        r.left = s.left;
        r.top = s.top;
        r.right = s.right;
        r.bottom = s.bottom;
        return r;
    }

    private Rects() { }
}
