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
