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

package android.mono.table.etc;

import android.content.*;
import android.graphics.*;
import android.text.*;

import java.util.*;

public class TypefaceLoader {

    private static final String[] extensions = {"ttf", "otf"};
    private static final HashMap<String, Typeface> cache = new HashMap<String, Typeface>(16);

    public static Typeface loadTypeface(Context context, String fontFamily) {
        if (TextUtils.isEmpty(fontFamily)) {
            return null;
        }
        Typeface typeface = cache.get(fontFamily);
        if (typeface == null) {
            for (String ext : extensions) {
                try {
                    typeface = Typeface.createFromAsset(context.getAssets(), String.format("fonts/%s.%s", fontFamily, ext));
                } catch (Throwable t) {
                    // ignore
                }
            }
        }
        if (typeface != null) {
            cache.put(fontFamily, typeface);
        }
        return typeface;
    }

}
