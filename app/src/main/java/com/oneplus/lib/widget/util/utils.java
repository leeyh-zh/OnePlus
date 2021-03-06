package com.oneplus.lib.widget.util;

import android.content.Context;
import android.util.TypedValue;

public class utils {
    public static int resolveDefStyleAttr(Context context, int defStyleAttr) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(defStyleAttr, outValue, true);
        return (outValue.resourceId >>> 24) == 1 ? 0 : defStyleAttr;
    }
}
