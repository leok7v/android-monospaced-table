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
import android.mono.table.app.*;
import android.view.*;
import android.widget.*;

import java.util.*;

import static android.mono.table.etc.util.*;

public class TabGroup extends LinearLayout implements View.OnClickListener {

    private final ArrayList<View> tabs = new ArrayList<View>(8);
    private final Paint paint = new Paint();
    private LinearLayout buttons;
    private Button active;

    public TabGroup(Context context) {
        super(context);
        setOrientation(VERTICAL);
        paint.setColor(C.NC_LTBLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
    }

    public void addTab(String label, View tab) {
        if (buttons == null) {
            buttons = new LinearLayout(getContext()) {
                public void draw(Canvas c) {
                    super.draw(c);
                    int w = getWidth() - 1;
                    int h = getHeight() - 1;
                    if (active != null) {
                        c.drawLine(0, h, active.getLeft(), h, paint);
                        c.drawLine(active.getRight(), h, w + 1, h, paint);
                    } else {
                        c.drawLine(0, h, w + 1, h, paint);
                    }
                }
            };
            buttons.setOrientation(HORIZONTAL);
            addView(buttons, C.MATCH_WRAP);
            buttons.setBackgroundColor(C.NC_DKGRAY);
        }
        tabs.add(tab);
        Button b = new Button(getContext()) {
            public void draw(Canvas c) {
                super.draw(c);
                int w = getWidth() - 1;
                int h = getHeight() - 1;
                if (isSelected()) {
                    c.drawLine(0, 0, 0, h + 1, paint);
                    c.drawLine(0, 0, w, 0, paint);
                    c.drawLine(w, 0, w, h + 1, paint);
                }
            }
        };
        b.setText(label);
        b.setBackgroundResource(0);
        b.setTypeface(G.monospaced.getTypeface());
        b.setTextSize(C.POINT, C.MONO_TYPEFACE_SIZE_IN_POINTS);
        float pixels = unitToPixels(C.POINT, C.MONO_TYPEFACE_SIZE_IN_POINTS);
        b.setTextSize(C.PIXEL, pixels);
        int px = Math.round(pixels);
        b.setPadding(px, px / 2, px, px / 2);
        // in some themes (namely Holo) minHeight will be set to (pretty arbitrary) 48px see:
        // http://stackoverflow.com/questions/16467006/holo-theme-make-my-button-bigger
        b.setMinHeight(-1);
        b.setMinimumHeight(-1);
        deactivate(b);
        b.setTextColor(C.NC_OFFWHITE);
        b.setBackgroundColor(C.NC_DKGRAY);
        buttons.addView(b, C.WRAP_WRAP);
        b.setOnClickListener(this);
        if (getChildCount() == 1) {
            activate(b, tab);
        }
    }

    private void activate(Button b, View tab) {
        if (getChildCount() > 1) {
            removeViewAt(1);
        }
        addView(tab, C.WRAP_WRAP);
        b.setTextColor(C.NC_GOLD);
        b.setBackgroundColor(C.NC_BLUE);
        b.setSelected(true);
        active = b;
    }

    private void deactivate(Button b) {
        b.setTextColor(C.NC_OFFWHITE);
        b.setBackgroundColor(C.NC_DKGRAY);
        b.setSelected(false);
    }

    public void onClick(View v) {
        if (v instanceof Button) {
            for (int i = 0; i < buttons.getChildCount(); i++) {
                deactivate((Button)buttons.getChildAt(i));
            }
            Button b = (Button)v;
            for (int i = 0; i < buttons.getChildCount(); i++) {
                if (b == buttons.getChildAt(i)) {
                    activate(b, tabs.get(i));
                    break;
                }
            }
        }
    }

}
