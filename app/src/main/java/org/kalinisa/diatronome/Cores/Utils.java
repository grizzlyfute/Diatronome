package org.kalinisa.diatronome.Cores;

import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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

  // Helper for mutex
  public static boolean mutexTryAcquire(Semaphore mutex, long timeoutMs)
  {
    boolean result = false;
    try
    {
      if (timeoutMs > 0)
      {
        result = mutex.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
      }
      else
      {
        mutex.acquire();
        result = true;
      }
    }
    catch (InterruptedException e)
    {
      result = false;
    }
    return result;
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
