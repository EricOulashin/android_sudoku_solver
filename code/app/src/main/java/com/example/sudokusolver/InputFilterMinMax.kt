// https://stackoverflow.com/questions/14212518/is-there-a-way-to-define-a-min-and-max-value-for-edittext-in-android

package com.example.sudokusolver

import android.text.InputFilter
import android.text.Spanned


class InputFilterMinMax(pMin: Int, pMax: Int): InputFilter {
    public final override fun filter(source: CharSequence, start: Int, end: Int, dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        try {
            var input: Int = Integer.parseInt(dest.toString() + source.toString())
            if (isInRange(mMin, mMax, input))
                return null
        }
        catch (nfe: NumberFormatException) { }
        return ""
    }


    private val mMin: Int = pMin
    private val mMax: Int = pMax

    private fun isInRange(a: Int, b: Int, c: Int): Boolean {
        return if (b > a) (c >= a && c <= b) else (c >= b && c <= a)
    }
}