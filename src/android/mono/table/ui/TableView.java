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

    private void adjust(float x, float y, Drawable d, RectF rc) {
        rc.left = x;
        rc.top = y;
        rc.right = rc.left + d.getMinimumWidth();
        rc.bottom = rc.top + d.getMinimumHeight();
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (!getLocalVisibleRect(visible) || visible.width() <= 0 || visible.height() <= 0) {
            return;
        }
        visible.left += getPaddingLeft();
        visible.top += getPaddingTop();
        visible.right -= getPaddingRight();
        visible.bottom -= getPaddingBottom();
        RectF rc = Rects.getRectF();
        canvas.save();
        try {
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
                        adjust(x, y, d, rc);
                        d.setBounds((int)Math.floor(x), (int)Math.floor(y), Math.round(rc.right), Math.round(rc.bottom));
                        canvas.clipRect(visible, Region.Op.REPLACE);
                        Rect b = model.bounds(c, r); // based on rc because text bounds are vertically negative
                        canvas.clipRect(x, y, x + b.width(), y + b.height(), Region.Op.INTERSECT);
                        d.draw(canvas);
                        x += widths[c];
                    } finally {
                        model.recycle(d);
                    }
                }
                y += height;
            }
        } finally {
            Rects.recycle(rc);
            canvas.restore();
        }
    }

}
