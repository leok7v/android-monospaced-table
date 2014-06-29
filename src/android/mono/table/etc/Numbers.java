/*
 *  Adopted from original package private: java.lang.RealToString.java
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package android.mono.table.etc;

import static android.mono.table.etc.util.*;

public class Numbers {

    private static final ThreadLocal<Numbers> INSTANCE = new ThreadLocal<Numbers>() {
        @Override protected Numbers initialValue() {
            return new Numbers();
        }
    };

    public static final long[] LONG_POWERS_OF_TEN = new long[] {
        1L,
        10L,
        100L,
        1000L,
        10000L,
        100000L,
        1000000L,
        10000000L,
        100000000L,
        1000000000L,
        10000000000L,
        100000000000L,
        1000000000000L,
        10000000000000L,
        100000000000000L,
        1000000000000000L,
        10000000000000000L,
        100000000000000000L,
        1000000000000000000L,
    };

    public static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F'
    };

    public static final int  Double_EXPONENT_BIAS = 1023;
    public static final int  Double_MANTISSA_BITS = 52;
    public static final long Double_SIGN_MASK     = 0x8000000000000000L;
    public static final long Double_EXPONENT_MASK = 0x7ff0000000000000L;
    public static final long Double_MANTISSA_MASK = 0x000fffffffffffffL;

    private static final double invLogOfTenBaseTwo = Math.log(2.0) / Math.log(10.0);

    private static final Text INFINITY = wrap("Infinity");
    private static final Text NEGATIVE_INFINITY = wrap("-Infinity");
    private static final Text NaN = wrap("NaN");
    private static final Text ZERO = wrap("0.0");
    private static final Text NEGATIVE_ZERO = wrap("0.0");
    private static final Text SMALLEST_DOUBLE = wrap("4.9E-324");
    private static final Text NEGATIVE_SMALLEST_DOUBLE = wrap("-4.9E-324");

    private int firstK;

    private final char[] num = new char[64];

    /**
     * An array of decimal digits, filled by longDigitGenerator or bigIntDigitGenerator.
     */
    private final int[] digits = new int[64];

    /**
     * Number of valid entries in 'digits'.
     */
    private int digitCount;

    private Numbers() {
    }

    public static Numbers getInstance() {
        return INSTANCE.get();
    }

    private static Text wrap(String s) {
        Text text = new Text(s.length());
        text.append(s);
        return text;
    }

    public static long parseLong(CharSequence s) {
        return parse(s, 0, s.length(), 10);
    }

    public static long parseLong(CharSequence s, int offset, int length) {
        return parse(s, offset, length, 10);
    }

    private static long parse(CharSequence s, int offset, int length, int radix) {
        int start = offset;
        int end = offset + length;
        while (offset < end && Character.isWhitespace(s.charAt(offset))) {
            offset++;
        }
        boolean negative = offset < end && s.charAt(offset) == '-';
        if (negative) {
            offset++;
        }
        if (offset >= end) {
            throw invalidLong(s, start, length);
        }
        long max = Long.MIN_VALUE / radix;
        long result = 0;
        while (offset < end) {
            int digit = Character.digit(s.charAt(offset++), radix);
            if (digit == -1) {
                throw invalidLong(s, start, length);
            }
            if (max > result) {
                throw invalidLong(s, start, length);
            }
            long next = result * radix - digit;
            if (next > result) {
                throw invalidLong(s, start, length);
            }
            result = next;
        }
        if (!negative) {
            result = -result;
            if (result < 0) {
                throw invalidLong(s, start, length);
            }
        }
        return result;
    }

    private static NumberFormatException invalidLong(CharSequence s, int offset, int length) {
        char[] ca = new char[length];
        for (int i = 0; i < length; i++) {
            ca[i] = s.charAt(offset + i);
        }
        return new NumberFormatException("Invalid long: \"" + new String(ca) + "\"");
    }

    public void appendLong(Text text, long v, int radix) {
        if (radix < Character.MIN_RADIX || radix > 16) {
            radix = 10;
        }
        /*
         * If i is positive, negate it. This is the opposite of what one might
         * expect. It is necessary because the range of the negative values is
         * strictly larger than that of the positive values: there is no
         * positive value corresponding to Integer.MIN_VALUE.
         */
        boolean negative = v < 0;
        if (!negative) {
            v = -v;
        }
        int bufLen = num.length;  // Max chars in result (conservative)
        int cursor = bufLen;
        do {
            long q = v / radix;
            num[--cursor] = DIGITS[(int)(radix * q - v)];
            v = q;
        } while (v != 0);
        if (negative) {
            num[--cursor] = '-';
        }
        for (int i = cursor; i < bufLen; i++) {
            text.append(num[i]);
        }
    }

    private static Text resultOrSideEffect(Text text, Text s) {
        if (text != null) {
            text.append(s);
            return text;
        }
        return s;
    }

    public Text appendDouble(Text text, double d) {
        return convertDouble(text, d);
    }

    private Text convertDouble(Text text, double inputNumber) {
        assertion(text != null);
        long inputNumberBits = Double.doubleToRawLongBits(inputNumber);
        boolean positive = (inputNumberBits & Double_SIGN_MASK) == 0;
        int e = (int) ((inputNumberBits & Double_EXPONENT_MASK) >> Double_MANTISSA_BITS);
        long f = inputNumberBits & Double_MANTISSA_MASK;
        boolean mantissaIsZero = f == 0;
        Text quickResult = null;
        if (e == 2047) {
            if (mantissaIsZero) {
                quickResult = positive ? INFINITY : NEGATIVE_INFINITY;
            } else {
                quickResult = NaN;
            }
        } else if (e == 0) {
            if (mantissaIsZero) {
                quickResult = positive ? ZERO : NEGATIVE_ZERO;
            } else if (f == 1) {
                // special case to increase precision even though 2 * Double_MIN_VALUE is 1.0e-323
                quickResult = positive ? SMALLEST_DOUBLE : NEGATIVE_SMALLEST_DOUBLE;
            }
        }
        if (quickResult != null) {
            return resultOrSideEffect(text, quickResult);
        }
        int p = Double_EXPONENT_BIAS + Double_MANTISSA_BITS; // the power offset (precision)
        int pow;
        int numBits = Double_MANTISSA_BITS;
        if (e == 0) {
            pow = 1 - p; // a denormalized number
            long ff = f;
            while ((ff & 0x0010000000000000L) == 0) {
                ff = ff << 1;
                numBits--;
            }
        } else {
            // 0 < e < 2047
            // a "normalized" number
            f = f | 0x0010000000000000L;
            pow = e - p;
        }
        firstK = digitCount = 0;
        if (-59 < pow && pow < 6 || (pow == -59 && !mantissaIsZero)) {
            longDigitGenerator(f, pow, e == 0, mantissaIsZero, numBits);
        } else {
            bigIntDigitGenerator(f, pow, e == 0, numBits);
        }
        if (inputNumber >= 1e7D || inputNumber <= -1e7D
                || (inputNumber > -1e-3D && inputNumber < 1e-3D)) {
            freeFormatExponential(text, positive);
        } else {
            freeFormat(text, positive);
        }
        return text;
    }

    public Text appendFloat(Text text, float f) {
        return convertFloat(text, f);
    }

    public static final int Float_EXPONENT_BIAS = 127;
    public static final int Float_MANTISSA_BITS = 23;
    public static final int Float_SIGN_MASK     = 0x80000000;
    public static final int Float_EXPONENT_MASK = 0x7f800000;
    public static final int Float_MANTISSA_MASK = 0x007fffff;

    public Text convertFloat(Text text, float inputNumber) {
        assertion(text != null);
        int inputNumberBits = Float.floatToRawIntBits(inputNumber);
        boolean positive = (inputNumberBits & Float_SIGN_MASK) == 0;
        int e = (inputNumberBits & Float_EXPONENT_MASK) >> Float_MANTISSA_BITS;
        int f = inputNumberBits & Float_MANTISSA_MASK;
        boolean mantissaIsZero = f == 0;

        Text quickResult = null;
        if (e == 255) {
            if (mantissaIsZero) {
                quickResult = positive ? INFINITY : NEGATIVE_INFINITY;
            } else {
                quickResult = NaN;
            }
        } else if (e == 0 && mantissaIsZero) {
            quickResult = positive ? ZERO : NEGATIVE_ZERO;
        }
        if (quickResult != null) {
            return resultOrSideEffect(text, quickResult);
        }
        int p = Float_EXPONENT_BIAS + Float_MANTISSA_BITS; // the power offset (precision)
        int pow;
        int numBits = Float_MANTISSA_BITS;
        if (e == 0) {
            pow = 1 - p; // a denormalized number
            if (f < 8) { // want more precision with smallest values
                f = f << 2;
                pow -= 2;
            }
            int ff = f;
            while ((ff & 0x00800000) == 0) {
                ff = ff << 1;
                numBits--;
            }
        } else {
            // 0 < e < 255
            // a "normalized" number
            f = f | 0x00800000;
            pow = e - p;
        }
        firstK = digitCount = 0;
        if (-59 < pow && pow < 35 || (pow == -59 && !mantissaIsZero)) {
            longDigitGenerator(f, pow, e == 0, mantissaIsZero, numBits);
        } else {
            bigIntDigitGenerator(f, pow, e == 0, numBits);
        }
        if (inputNumber >= 1e7f || inputNumber <= -1e7f
                || (inputNumber > -1e-3f && inputNumber < 1e-3f)) {
            freeFormatExponential(text, positive);
        } else {
            freeFormat(text, positive);
        }
        return text;
    }

    private void freeFormatExponential(Text text, boolean positive) {
        int digitIndex = 0;
        if (!positive) {
            text.append('-');
        }
        text.append((char)('0' + digits[digitIndex++]));
        text.append('.');
        int k = firstK;
        int exponent = k;
        while (true) {
            k--;
            if (digitIndex >= digitCount) {
                break;
            }
            text.append((char)('0' + digits[digitIndex++]));
        }
        if (k == exponent - 1) {
            text.append('0');
        }
        text.append('E');
        text.append(exponent, 10);
    }

    private void freeFormat(Text text, boolean positive) {
        int digitIndex = 0;
        if (!positive) {
            text.append('-');
        }
        int k = firstK;
        if (k < 0) {
            text.append('0');
            text.append('.');
            for (int i = k + 1; i < 0; ++i) {
                text.append('0');
            }
        }
        int U = digits[digitIndex++];
        do {
            if (U != -1) {
                text.append((char)('0' + U));
            } else if (k >= -1) {
                text.append('0');
            }
            if (k == 0) {
                text.append('.');
            }
            k--;
            U = digitIndex < digitCount ? digits[digitIndex++] : -1;
        } while (U != -1 || k >= -1);
    }

    private native void bigIntDigitGenerator(long f, int e, boolean isDenormalized, int p);

    private void longDigitGenerator(long f, int e, boolean isDenormalized, boolean mantissaIsZero, int p) {
        long R, S, M;
        if (e >= 0) {
            M = 1l << e;
            if (!mantissaIsZero) {
                R = f << (e + 1);
                S = 2;
            } else {
                R = f << (e + 2);
                S = 4;
            }
        } else {
            M = 1;
            if (isDenormalized || !mantissaIsZero) {
                R = f << 1;
                S = 1l << (1 - e);
            } else {
                R = f << 2;
                S = 1l << (2 - e);
            }
        }
        int k = (int) Math.ceil((e + p - 1) * invLogOfTenBaseTwo - 1e-10);
        if (k > 0) {
            S = S * LONG_POWERS_OF_TEN[k];
        } else if (k < 0) {
            long scale = LONG_POWERS_OF_TEN[-k];
            R = R * scale;
            M = M == 1 ? scale : M * scale;
        }
        if (R + M > S) { // was M_plus
            firstK = k;
        } else {
            firstK = k - 1;
            R = R * 10;
            M = M * 10;
        }
        boolean low, high;
        int U;
        while (true) {
            // Set U to floor(R/S) and R to the remainder, using *unsigned* 64-bit division
            U = 0;
            for (int i = 3; i >= 0; i--) {
                long remainder = R - (S << i);
                if (remainder >= 0) {
                    R = remainder;
                    U += 1 << i;
                }
            }
            low = R < M; // was M_minus
            high = R + M > S; // was M_plus
            if (low || high) {
                break;
            }
            R = R * 10;
            M = M * 10;
            digits[digitCount++] = U;
        }
        if (low && !high) {
            digits[digitCount++] = U;
        } else if (high && !low) {
            digits[digitCount++] = U + 1;
        } else if ((R << 1) < S) {
            digits[digitCount++] = U;
        } else {
            digits[digitCount++] = U + 1;
        }
    }

}
