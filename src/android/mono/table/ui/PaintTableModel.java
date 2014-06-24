package android.mono.table.ui;

import android.graphics.*;
import android.mono.table.data.*;

public abstract class PaintTableModel extends TextTableModel {

    private final Paint paint;

    public PaintTableModel(DataModel dm, Paint p) {
        super(dm);
        paint = p;
    }

    public Paint paint(int c, int r) {
        paint.setColor(color(c, r));
        return paint;
    }

    protected abstract int color(int c, int r);

}
