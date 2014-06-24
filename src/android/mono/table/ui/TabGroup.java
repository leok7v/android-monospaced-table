package android.mono.table.ui;

import android.content.*;
import android.graphics.*;
import android.mono.table.app.*;
import android.mono.table.etc.*;
import android.view.*;
import android.widget.*;

import java.util.*;

public class TabGroup extends LinearLayout implements View.OnClickListener {

    private final ArrayList<View> tabs = new ArrayList<View>();
    private final LinearLayout buttons;
    private final Paint paint = new Paint();

    public TabGroup(Context context) {
        super(context);
        paint.setColor(C.NC_LTBLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        setOrientation(VERTICAL);
        buttons = new LinearLayout(context);
        buttons.setOrientation(HORIZONTAL);
        addView(buttons, C.MATCH_WRAP);
        buttons.setBackgroundColor(Color.WHITE);
    }

    public void addTab(String label, View tab) {
        tabs.add(tab);
        Button b = new Button(getContext()) {
            public void draw(Canvas c) {
                super.draw(c);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                if (isSelected()) {
                    c.drawLine(0, 0, 0, h, paint);
                    c.drawLine(0, 0, w, 0, paint);
                    c.drawLine(w, h, w, 0, paint);
                }
            }
        };
        b.setText(label);
        b.setBackgroundResource(0);
        b.setTextColor(C.NC_GREEN);
        b.setBackgroundColor(C.NC_DKGRAY);
        b.setTypeface(((Act)getContext()).getTypeface());
        b.setTextSize(C.PIXEL, ((Act)getContext()).getTextSizePixels());
        int px = Math.round(util.unitToPixels(C.POINT, ((Act)getContext()).getTextSizePoints()));
        b.setPadding(px, px / 2, px, px / 2);
        buttons.addView(b);
        b.setOnClickListener(this);
        if (getChildCount() == 1) {
            addView(tab);
            requestLayout();
            b.setTextColor(C.NC_OFFWHITE);
            b.setBackgroundColor(C.NC_BLUE);
            b.setSelected(true);
        }
    }

    public void onClick(View v) {
        if (v instanceof Button) {
            for (int i = 0; i < buttons.getChildCount(); i++) {
                Button c = (Button)buttons.getChildAt(i);
                c.setTextColor(C.NC_GREEN);
                c.setBackgroundColor(C.NC_DKGRAY);
                c.setSelected(false);
            }
            Button b = (Button)v;
            for (int i = 0; i < buttons.getChildCount(); i++) {
                if (b == buttons.getChildAt(i)) {
                    if (getChildCount() > 1) {
                        removeViewAt(1);
                    }
                    addView(tabs.get(i));
                    requestLayout();
                    b.setTextColor(C.NC_OFFWHITE);
                    b.setBackgroundColor(C.NC_BLUE);
                    b.setSelected(true);
                    break;
                }
            }
        }
    }

}
