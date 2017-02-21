package com.yaorugang.coco;

import android.content.res.Resources;

/**
 * Created by yaorugang on 4/27/2016.
 */
public class Converter
{
    public static double dpToPx(double dp)
    {
        return (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static double pxToDp(double px)
    {
        return (px / Resources.getSystem().getDisplayMetrics().density);
    }
}
