package com.yaorugang.coco;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Rugang on 2017/6/19.
 *
 * 此类提供一些便捷的Debug函数，用来帮助开发调试
 */

public class DebugTool
{
    /**
     * 打印View Hierarchy，以缩进的方式显示层层视图结构
     * @param tag debug tag
     * @param viewGroup 要打印的 View Hierarchy 的跟节点，不可以为null
     */
    public static void printViewHierarchy(String tag, @NonNull ViewGroup viewGroup)
    {
        printViewGroup(tag, viewGroup, 0);
    }

    private static void printViewGroup(String tag, ViewGroup viewGroup, int level)
    {
        String levelString = getLevelString(level);
        Log.d(tag, levelString + viewGroup.toString());

        levelString = getLevelString(level + 1);
        for (int i = 0; i <viewGroup.getChildCount(); i++)
        {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup)
                printViewGroup(tag, (ViewGroup)child, level + 1);
            else
                Log.d(tag, levelString + child.toString());
        }
    }

    private static String getLevelString(int level)
    {
        String str = "";
        while (level > 0)
        {
            str += "......";
            level--;
        }

        return str;
    }
}
