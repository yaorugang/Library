package com.yaorugang.coco;

import android.graphics.PointF;

/**
 * Created by Rugang on 2017/4/4.
 */

public class DirectionTool
{
    /**
     * 判断是否向右滑动。判断标准为：如果向右滑动的方向与X轴夹角小于等于30度（无论向右上还是右下），
     * 并且滑动的直线距离大于15dp，则认定为向右的有效滑动。
     * @param start 滑动起点坐标
     * @param end 滑动结束坐标
     * @return true if swiped to right, otherwise false will be returned
     */
    public static boolean isSwipeRight(PointF start, PointF end)
    {
        if (end.x <= start.x)
            return false;

        float distanceX = Math.abs(end.x - start.x);
        float distanceY = Math.abs(end.y - start.y);

        double distance = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));   // 滑动的直线距离
        double degree = Math.toDegrees(Math.atan(distanceY / distanceX));   // 滑动方向与 X 轴的夹角

        return (degree <= 30 && distance > Converter.dpToPx(15));
    }

    /**
     * 判断是否向上滑动。判断标准为：如果向上滑动的方向与X轴夹角大于60度，也就是与Y轴夹角小于30度（无论左上还是右上），
     * 并且滑动的直线距离大于15dp，则认定为向上的有效滑动。
     * @param start 滑动起点坐标
     * @param end 滑动结束坐标
     * @return true if swiped to up, otherwise false will be returned
     */
    public static boolean isSwipeUp(PointF start, PointF end)
    {
        if (end.y >= start.y)
            return false;

        float distanceX = Math.abs(end.x - start.x);
        float distanceY = Math.abs(end.y - start.y);

        double distance = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));   // 滑动的直线距离
        double degree = Math.toDegrees(Math.atan(distanceY / distanceX));   // 滑动方向与 X 轴的夹角

        return (degree > 60 && distance > Converter.dpToPx(15));
    }

    /**
     * 判断是否向下滑动。判断标准为：如果向下滑动的方向与X轴夹角大于60度，也就是与Y轴夹角小于30度（无论左下还是右下），
     * 并且滑动的直线距离大于15dp，则认定为向下的有效滑动。
     * @param start 滑动起点坐标
     * @param end 滑动结束坐标
     * @return true if swiped to down, otherwise false will be returned
     */
    public static boolean isSwipeDown(PointF start, PointF end)
    {
        if (end.y <= start.y)
            return false;

        float distanceX = Math.abs(end.x - start.x);
        float distanceY = Math.abs(end.y - start.y);

        double distance = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));   // 滑动的直线距离
        double degree = Math.toDegrees(Math.atan(distanceY / distanceX));   // 滑动方向与 X 轴的夹角

        return (degree > 60 && distance > Converter.dpToPx(15));
    }
}
