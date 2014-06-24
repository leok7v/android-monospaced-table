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

package android.mono.table.ui;

import android.graphics.*;
import android.graphics.drawable.*;
import android.mono.table.data.*;

import static android.mono.table.etc.util.assertion;

public abstract class TextTableModel extends TextRenderer implements TableModel {

    protected final DataModel data;
    protected int inuse;
    private final Rect pd = new Rect();
    private final Rect bounds = new Rect();

    public TextTableModel(DataModel dm) {
        data = dm;
    }

    public int columns() {
        return data.columns();
    }

    public int rows() {
        return data.rows();
    }

    public abstract Paint paint(int c, int r);

    public Rect bounds(int c, int r) {
        Paint paint = paint(c, r);
        bounds.set(data.bounds(c, r, paint));
        set(c, r, data.getText(c, r));
        getPadding(pd);
        bounds.left -= pd.left;
        bounds.top -= pd.top;
        bounds.right += pd.right;
        bounds.bottom += pd.bottom;
        return bounds;
    }

    public Drawable drawable(int c, int r) {
        assertion(inuse == 0);
        inuse++;
        set(c, r, data.getText(c, r));
        return this;
    }

    public void recycle(Drawable d) {
        assertion(inuse == 1);
        inuse--;
    }

}
