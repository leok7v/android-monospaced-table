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

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.mono.table.etc.*;

import static android.view.View.MeasureSpec.*;

public class TableView extends View {

    private float[] widths;
    private float   height;
    private final Rect visible = new Rect();
    private TableModel model;

    public TableView(Context context) {
        super(context);
    }

    public TableView setModel(TableModel m) {
        model = m;
        return this;
    }

    protected void onMeasure(int wms, int hms) {
        Rect rc = Rects.getRect();
        Rect pd = Rects.getRect();
        float width = 0;
        try {
            int columns = model.columns();
            if (widths == null || widths.length != columns) {
                widths = new float[columns];
            }
            height = 0;
            for (int c = 0; c < columns; c++) {
                Rect bounds = model.bounds(c, 0);
                widths[c] = bounds.width();
                width += widths[c];
                height = Math.max(height, bounds.height());
            }
        } finally {
            Rects.recycle(rc);
            Rects.recycle(pd);
        }
        int w = (int)Math.ceil(getPaddingLeft() + width + getPaddingRight());
        int h = (int)Math.ceil(getPaddingTop() + model.rows() * height + getPaddingBottom());
        setMeasuredDimension(util.measure(getMode(wms), getSize(wms), w),
                             util.measure(getMode(hms), getSize(hms), h));
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (getLocalVisibleRect(visible) && visible.width() > 0 && visible.height() > 0) {
            canvas.save();
            try { drawTable(canvas); } finally { canvas.restore(); }
        }
    }

    private void drawTable(Canvas canvas) {
        visible.left += getPaddingLeft();
        visible.top += getPaddingTop();
        visible.right -= getPaddingRight();
        visible.bottom -= getPaddingBottom();
        int firstVisibleRow = (int)Math.floor(visible.top / height);
        int lastVisibleRow = (int)Math.ceil((visible.bottom + height) / height);
        float y = getPaddingTop() + firstVisibleRow * height;
        int columns = model.columns();
        int rows = Math.min(model.rows(), lastVisibleRow);
        canvas.clipRect(visible);
        for (int r = firstVisibleRow; r < rows; r++) {
            float x = getPaddingLeft();
            for (int c = 0; c < columns; c++) {
                Drawable d = model.drawable(c, r);
                try {
                    int mw = d.getMinimumWidth();
                    int mh = d.getMinimumHeight();
                    d.setBounds((int)Math.floor(x), (int)Math.floor(y), Math.round(x + mw), Math.round(y + mh));
                    canvas.clipRect(visible, Region.Op.REPLACE);
                    Rect b = model.bounds(c, r);
                    if (b.width() != widths[c]) {
                        requestLayout();
                        return;
                    }
                    canvas.clipRect(x, y, x + b.width(), y + b.height()); // intersect
                    d.draw(canvas);
                    x += widths[c];
                } finally {
                    model.recycle(d);
                }
            }
            y += height;
        }
    }

}
