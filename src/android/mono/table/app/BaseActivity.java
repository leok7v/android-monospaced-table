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

package android.mono.table.app;

import android.app.*;
import android.os.*;
import android.view.*;

public abstract class BaseActivity extends Activity {

    protected ViewGroup cv;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setContentView(ViewGroup view) {
        cv = view;
        super.setContentView(view);
    }

    public void onStart() {
        super.onStart();
        if (cv instanceof TopView) {
            ((TopView)cv).onStart();
        }
    }

    protected void onPause() {
        if (cv instanceof TopView) {
            ((TopView)cv).onPause();
        }
        super.onPause();
    }

    public void onDestroy() {
        if (cv instanceof TopView) {
            ((TopView)cv).onDestroy();
        }
        cv = null;
        super.onDestroy();
    }

    public void onResume() {
        if (cv instanceof TopView) {
            ((TopView)cv).onResume();
        }
        super.onResume();
    }

    public void onStop() {
        if (cv instanceof TopView) {
            ((TopView)cv).onStop();
        }
        super.onStop();
    }

    protected void onRestart() {
        if (cv instanceof TopView) {
            ((TopView)cv).onRestart();
        }
        super.onRestart();
    }

}
