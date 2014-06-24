package android.mono.table.ui;

import android.graphics.*;
import android.graphics.drawable.*;

public interface TableModel {
    public static final int
            LEFT_JUSTIFIED = 0,
            CENTERED = 1,
            RIGHT_JUSTIFIED = 2;

    int columns();
    int rows();
    Paint paint(int column, int row);
    Drawable drawable(int column, int row);
    void recycle(Drawable d);
    Rect bounds(int row, int column);
    int justify(int row, int column);
}
