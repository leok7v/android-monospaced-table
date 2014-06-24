package android.mono.table.ui;

import android.graphics.*;
import android.graphics.drawable.*;
import android.mono.table.etc.*;

import static android.mono.table.etc.util.*;

public abstract class TextRenderer extends Drawable implements TableModel {

    private final char[] ELLIPSIS = new char[]{'\u2026'}; // "..." character

    private int column;
    private int row;
    private int len;
    private TextInterface text;
    private int ellipsis = -1;
    private final float[] widths = new float[1024]; // will accommodate 4K pixels horizontal, 4 pixels wide monospace font
    private float width;
    private float height;
    private final Rect rc = new Rect(); // text bounds
    private final Rect pd = new Rect(); // padding
    private final Rect cb = new Rect(); // clip box

    public void set(int c, int r, TextInterface txt) {
        column = c;
        row = r;
        text = txt;
        len = Math.min(text.length(), widths.length);
        Paint p = paint(c, r);
        if (height == 0) {
            p.getTextBounds(text.array(), text.offset(), len, rc); // expensive ~100 microseconds
            height = rc.height(); // rc.width is not the same as width returned by measureText
        }
        if (widths[len] == 0) { // this is optimization is only valid for MONOSPACED fonts
            widths[len] = paint(c, r).measureText(text.array(), text.offset(), len); // expensive ~100 microseconds
        }
        width = widths[len];
        getPadding(pd);
    }

    public boolean getPadding(Rect p) {
        p.left = p.top = p.right = p.bottom = 4;
        return true;
    }

    public void setAlpha(int alpha) { }

    public void setColorFilter(ColorFilter cf) { }

    public int getOpacity() { return PixelFormat.OPAQUE; }

    public int getIntrinsicWidth() { return Math.round(pd.left + width + pd.right); }

    public int getIntrinsicHeight(){ return Math.round(pd.top + height + pd.bottom); }

    public void draw(Canvas c) {
        c.getClipBounds(cb);
        if (cb.height() > 0 && cb.width() > 0) {
            Paint paint = paint(column, row);
            Rect b = getBounds();
            float x = b.left + pd.left;
            float y = b.top + pd.top + height;
            float w = pd.left + width + pd.right;
            x = adjust(x, w);
            // w - 1 because of rounding errors
            if (cb.width() >= Math.floor(w) - 1 || ellipsis == 0) {
                c.drawText(text.array(), text.offset(), len, x, y, paint); // expensive ~100 microseconds
            } else {
                drawEllipsis(c, paint, x, y);
            }
        }
    }

    private void drawEllipsis(Canvas c, Paint paint, float x, float y) {
        if (ellipsis < 0) {
            ellipsis = (int)Math.ceil(paint.measureText(ELLIPSIS, 0, 1));
        }
        int n = text.length();
        float w = width;
        while (n > 1 && ellipsis > 0 && cb.width() < pd.left + w - 1 + ellipsis + pd.right) {
            n--;
            w = paint.measureText(text.array(), text.offset(), n);
        }
        x = adjust(x, w);
        c.drawText(text.array(), text.offset(), n, x, y, paint);
        if (ellipsis > 0) {
            c.drawText(ELLIPSIS, 0, 1, (int)Math.floor(x + w), y, paint);
        }
    }

    private float adjust(float x, float w) {
        switch (justify(column, row)) {
            case LEFT_JUSTIFIED:  return x;
            case CENTERED:        return x + (cb.width() - w) / 2;
            case RIGHT_JUSTIFIED: return x + cb.width() - w - pd.right;
            default: assertion(false); return x;
        }
    }

}
