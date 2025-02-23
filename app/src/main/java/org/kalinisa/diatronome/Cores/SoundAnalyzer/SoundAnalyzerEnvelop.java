package org.kalinisa.diatronome.Cores.SoundAnalyzer;

public class SoundAnalyzerEnvelop implements ISoundAnalyzer
{
  private final int AUDIO_SAMPLE_RATE;

  public SoundAnalyzerEnvelop(int audioSampleRate)
  {
    AUDIO_SAMPLE_RATE = audioSampleRate;
  }

  public double getPitch(final double[] signal)
  {
    double[] x = new double[signal.length];
    System.arraycopy(signal, 0, x, 0, signal.length);
    normalize(x, 0, x.length);
    return envelop(x);
  }

  protected void normalize(double[] x, int from, int to)
  {
    to = Math.min (to, x.length);
    double max = x[from], min = x[from];
    for (int i = from + 1; i < to; i++)
    {
      if (x[i] > max) max = x[i];
      if (x[i] < min) min = x[i];
    }
    for (int i = from; i < to; i++)
    {
      if (max - min > 0)
      {
        x[i] = 2*(x[i] - min) / (max - min) - 1;
      }
    }
  }

  protected double average(final double[] data, int from, int to)
  {
    double s = 0;
    for (int i = from; i < to; i++)
    {
      s += data[i];
    }
    return (to > from ? s / (to - from) : 0);
  }

  protected double stdDev (final double[] v, int from, int to)
  {
    double mean = 0;
    double stddev = 0;
    for (int i = from; i < to; i++)
    {
      mean += v[i];
      stddev += v[i]*v[i];
    }
    if (to > from)
    {
      mean = mean / (to - from);
      stddev = Math.sqrt(stddev/(to - from) - mean*mean);
    }

    return stddev;
  }

  private int findZero(final double[] x, int from)
  {
    double old = 0;
    while (from < x.length)
    {
      if ((x[from] < -0.001 && old > 0.001) ||
        (x[from] > 0.001 && old <- 0.0001)) break;
      else
      {
        old = x[from];
        from++;
      }
    }
    return from;
  }

  private int findExtrema(final double[] x, int from, int to)
  {
    int m = from;
    to = Math.min (to, x.length);
    for (int i = from; i < to ; i++)
    {
      if (Math.abs(x[i]) > Math.abs(x[m]))
      {
        m = i;
      }
    }
    return m;
  }

  private double getPeakByPolynomialInterpolation(
    double x1, double y1,
    double x2, double y2,
    double x3, double y3)
  {
    // We searching a, b, c for polynomial p(x) = a*x*x + b*x + c. Lagrange coefficients may help, but prefer matrix solving
    // [x1*x1, x1, 1]   [a]   [y1]
    // [x2*x2, x2, 1] * [b] = [y2]
    // [x3*x3, x3, 1]   [c]   [y3]
    double x1x2 = x1 - x2;
    double x2x3 = x2 - x3;
    double x1x3 = x1 - x3;

    if (Math.abs(x1x2 * x1x3 * x2x3) <= 0.0001) return 0;

    double a = y1 / (x1x2 * x1x3) - y2 / (x1x2 * x2x3) + y3 / (x1x3 * x2x3);
    double b = -y1 * (x2 + x3) / (x1x2 * x1x3) + y2 * (x1 + x3) / (x1x2 * x2x3) - y3 * (x1 + x2) / (x1x3 * x2x3);
    // double c = y1 * x2 * x3 / (x1x2*x1x3) - y2 * x1 * x3 / (x1x2 * x2x3) + y3 * x1 * x2 / (x1x3 * x2x3);

    if (Math.abs (a) <= 0.0001) return x2;
    // Derivative dp(x) / dx = 2x + b = 0
    return -b / (2 * a);
  }

  private int findLocalPeak(final double[] x, int from)
  {
    double old = x[from];
    int sign = 0, oldSign = 0;
    int i;

    for (i = from + 1; i < x.length; i++)
    {
      if (x[i] >= old) sign = 1;
      else sign = -1;
      if (oldSign > 0 && sign < 0) break;
      old = x[i];
      oldSign = sign;
    }

    return i - 1;
  }

  // Expected a normalized signal
  private double envelop(final double[] signal)
  {
    double[] x = new double[signal.length];
    double[] y = new double[signal.length];
    int len = 0;
    double v1 = 0, v2 = 0, value = 0;

    int p1 = 0, p2 = 0, p3 = 0;

    // SignalBank.signalTriangle442(signal);

    // Find Envelop
    p1 = findZero(signal, 0);
    p2 = 0;
    len = 0;
    while (p2 < signal.length)
    {
      p2 = findZero(signal, p1);
      p1 = findExtrema(signal, p1, p2);
      if (p1 >= signal.length -1) break;
      // if (Math.abs(signal[p1]) > threshold)
      {
        // Interpolate here to increase accuracy.
        x[len] = getPeakByPolynomialInterpolation(p1-1, signal[p1-1], p1, signal[p1],p1+1, signal[p1+1]); // pos;
        y[len] = signal[p1];
        len++;
      }
      p1 = p2;
    }
    if (len <= 0) return 0;

    // Integrator - low pass filter
    v1 = 0;
    for (p1 = 0; p1 < len; p1++)
    {
      if (y[p1] >= v1)
      {
        v1 = y[p1];
        // y[p1] = 1;
      }
      else
      {
        v1 *= 0.90;
        y[p1] = v1;
      }
    }

    // Recenter signal and normalize
    p1 = 0;
    while (p1 < len)
    {
      p2 = findLocalPeak(y, p1);
      if (p2 < len)
      {
        normalize(y, p1, p2);
      }
      if (p2 != p1) p1 = p2;
      else p1 = p2 + 1;
    }

    // Get the period between two max
    p1 = 0; p2 = 1; p3 = 0;
    len -= 1;
    double[] periods = new double[len];
    v1 = x[p2];
    while (p2 < len)
    {
      v2 = x[p2];
      if (y[p2] > 0.95) // Threshold
      {
        value = v2 - v1;
        if (p1 > 0 && value > AUDIO_SAMPLE_RATE /10000 && value < AUDIO_SAMPLE_RATE /16)
        {
          periods[p3++] = value;
        }
        p1 = p2;
        v1 = v2;
      }
      p2++;
    }

    // Get max period, and sum all subperiod lower than the half of this period (debounce)
    value = 0;
    for (p1 = 0; p1 < len; p1++)
    {
      if (periods[p1] > value)
      {
        value = periods[p1];
      }
    }
    for (p1 = 0; p1 < len; p1++)
    {
      if (p1 > 0 &&
        periods[p1] + periods[p1 - 1] < value * 1.5 &&
        periods[p1 - 1] < periods[p1]/2)
      {
        periods[p1] += periods[p1 - 1];
        periods[p1 - 1] = 0;
      }
    }

    // Do a mean, removing invalid value
    double stddev = 0;
    double mean = 0;
    p1 = 0;
    do
    {
      // Remove 0
      java.util.Arrays.sort(periods);
      while (p1 < periods.length && periods[p1] <= 0) p1++;

      stddev = stdDev (periods, p1, periods.length);
      mean = average (periods, p1, periods.length);

      for (p2 = p1; p2 < periods.length; p2++)
      {
        if (Math.abs (periods[p2] - mean) > stddev)
        {
          periods[p2] = 0;
        }
      }
      p1++;
    } while (stddev > 1 && p1 < periods.length);
    value = mean;

    if (value <= 0.001) return 0;
    return (double) AUDIO_SAMPLE_RATE /value;
  }
}

