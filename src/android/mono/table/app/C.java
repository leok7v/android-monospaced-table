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

import android.util.*;
import android.view.*;

import static android.view.ViewGroup.LayoutParams.*;

@SuppressWarnings({"unused"})
public class C {

    public static final int POINT = TypedValue.COMPLEX_UNIT_PT;
    public static final int PIXEL = TypedValue.COMPLEX_UNIT_PX;

    public static final String MONO_TYPEFACE_NAME = "DejaVuSansMono-Bold";
    public static final float MONO_TYPEFACE_SIZE_IN_POINTS = 8;

    public static final int
            BACKGROUND_COLOR = 0xFF112013,
            // http://en.wikipedia.org/wiki/Norton_Commander#mediaviewer/File:Norton_commander.png
            NC_BLUE      = 0xFF0000aa,
            NC_RED       = 0xFFff5555,
            NC_YELLOW    = 0xFFffff55,
            NC_TURQUOISE = 0xFF00aaaa,
            NC_GREEN     = 0xFF00aa00,
            NC_DKGRAY    = 0xff555555,
            NC_GRAY      = 0xffaaaaaa,
            // http://en.wikipedia.org/wiki/Norton_Commander#mediaviewer/File:Norton_Commander_5.51.png
            NC_GOLD      = 0xFFfcfe54,
            NC_OFFWHITE  = 0xFFfefefe,
            NC_VERDIGRIS = 0xFF04aaac, // TURQUOISE

            NC_DKBLUE = 0xFF0402ac,
            NC_LTBLUE = 0xFF54fefc;
            // http://en.wikipedia.org/wiki/Norton_Commander#mediaviewer/File:Norton_Commander_5.0.png

    public static final ViewGroup.LayoutParams
            WRAP_WRAP   =  new ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT),
            MATCH_MATCH = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT),
            MATCH_WRAP  = new ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT),
            WRAP_MATCH   = new ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT);

    public static final long NANOS_IN_SECOND = 1000000000L;

    public static final int PROCFS_REFRESH_IN_MILLIS = 2 * 1000;

}
