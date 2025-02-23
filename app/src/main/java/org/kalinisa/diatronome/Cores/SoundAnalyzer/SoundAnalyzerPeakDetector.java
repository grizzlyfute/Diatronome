package org.kalinisa.diatronome.Cores.SoundAnalyzer;

public class SoundAnalyzerPeakDetector implements ISoundAnalyzer
{
  private final int AUDIO_SAMPLE_RATE;

  public SoundAnalyzerPeakDetector(int audioSampleRate)
  {
    AUDIO_SAMPLE_RATE = audioSampleRate;
  }

  public double getPitch(final double[] signal)
  {
    double[] norm = new double[signal.length];
    System.arraycopy(signal, 0, norm, 0, signal.length);
    normalize(norm, 0, norm.length);
    double[] correlated = autocorrelate(norm);
    return frequencyByPeakDetector(correlated);
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

  // Signal is expected between (-1.0 and 1.0)
  private double[] autocorrelate(final double[] in)
  {
    double[] out = new double[in.length];
    // double u = 0*average(in);
    double u = 0;
    double d = 0;
    for (int i = 0; i < in.length; i++) d += (in[i] - u);

    for (int t = 0; t < in.length; t++)
    {
      out[t] = 0;
      for (int i = 0; i < in.length - t; i++)
      {
        // u = expected mean = 0; R(t) = sum(x[i] - u)*(conjugate(x[i - t]) - u)/x.length
        out[t] += (in[t] - u) * (in[t + i] - u);
        // Can use Average Magnitude Differential Function
      }
      out[t] /= d;
      //out[t] /= in.length;
    }
    return out;
  }

  private int findLocalPeak(final double[] x, int from, double threshold)
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

  // return the index of the maximum around i for a polynom
  private double interpolatePolynome(final double[] x, int i)
  {
    if (i <= 0 || i >= x.length - 1) return i;
    // Polynomial 2nd order interpolation (Lagrange)
    // a*x*x + b*x + c = P(x),
    // with (P(i-1) = x[i-1], P(i) = x[i], P(i + 1) = x[i+1]
    double a = 0, b = 0;
    a = x[i - 1]/2 - x[i] + x[i + 1]/2;
    b = ((-1 - 2*i)*x[i - 1])/2 + 2*i*x[i] + ((1 - 2*i)*x[1 + i])/2;
    // c = ((i + i*i)*x[i -1])/2 + ((2 - 2*i*i)*x[i])/2 + ((-i + i*i)*x[i + 1])/2;
    // We are looking for an extrema => null derivative => 2*a*x + b = 0
    if (Math.abs(a) >= 0.00001)
    {
      return -b/(2*a);
    }
    else
      return i - 0.5;
  }

  private double frequencyByPeakDetector(final double[] in)
  {
    int first = 0, last = 0, next = 0;
    double oldPeak = 0, newPeak = 0;
    int n = 0;
    double delta = 0;
    // Min spacing = sample / Max Frequency
    final int minSpacing = AUDIO_SAMPLE_RATE /10000;

    while (first < in.length)
    {
      if (in[first] > 0.75)
      {
        next = findLocalPeak(in, first, 0);
        newPeak = interpolatePolynome(in, next);
        if (oldPeak >= 1)
        {
          delta += (newPeak - oldPeak);
          n++;
        }
        oldPeak = newPeak;

        if (next < first + minSpacing)
          first += minSpacing;
        else
          first = next + 1;
      }
      else
      {
        first++;
      }
    }

    if (n <= 0) return 0.0;

    delta /=n;
    return AUDIO_SAMPLE_RATE / delta;
  }
}
