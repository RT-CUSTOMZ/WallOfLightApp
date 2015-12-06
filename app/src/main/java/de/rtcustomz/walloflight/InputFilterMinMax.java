package de.rtcustomz.walloflight;


import android.text.InputFilter;
import android.text.Spanned;

/**
 * Input filter to force a integer min and max range
 */
public class InputFilterMinMax implements InputFilter {

    private float min, max;

    public InputFilterMinMax(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public InputFilterMinMax(String min, String max) {
        this.min = Float.parseFloat(min);
        this.max = Float.parseFloat(max);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        try {
            String value = dest.subSequence(0, dstart).toString()
                    + source.subSequence(start, end)
                    + dest.subSequence(dend, dest.length());
            if (value.isEmpty()) {
                return null;
            }
            float input = Float.parseFloat(value);
            if (isInRange(min, max, input)) {
                return null;
            }
        } catch (NumberFormatException nfe) {
        }
        return dest.subSequence(dstart, dend);
    }

    private boolean isInRange(float a, float b, float c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }

}