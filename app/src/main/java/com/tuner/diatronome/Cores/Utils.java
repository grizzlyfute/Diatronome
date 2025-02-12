package com.tuner.diatronome.Cores;

import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;

public class Utils
{
  // Used for functions returning several data
  static public class Holder<T>
  {
    public T value;
    public Holder(T value)
    {
      this.value = value;
    }
  }

  // Dp stand for density independent pixels
  public static float dpToPixels(final Resources r, int dp)
  {
    // px = dp *  ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT)
    return TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
  }

  public static int darker(int color)
  {
    final float percent = 0.25f;
    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);
    hsv[1] = percent * (1.0f - hsv[1]) + hsv[1];
    hsv[2] = (1.0f - percent) * hsv[2];
    return Color.HSVToColor(hsv);
  }

  public static int lighter(int color)
  {
    final float percent = 0.25f;
    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);
    hsv[1] = (1.0f - percent) * hsv[1];
    hsv[2] = percent * (1.0f - hsv[2]) + hsv[2];
    return Color.HSVToColor(hsv);
  }
}
