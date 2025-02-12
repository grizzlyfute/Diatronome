package com.tuner.diatronome.Cores.SoundAnalyzer;

// https://sound.eti.pg.gda.pl/student/eim/synteza/leszczyna/index_ang.htm
// https://www.guitarpitchshifter.com/algorithm.html

public class SoundAnalyzerFourier implements ISoundAnalyzer
{
  private final int AUDIO_SAMPLE_RATE;
  private final FFT m_fft;

  public SoundAnalyzerFourier(int audioSampleRate)
  {
    AUDIO_SAMPLE_RATE = audioSampleRate;
    m_fft = new FFT(4096);
  }

  public double getPitch(final double[] signal)
  {
    double[] img = new double[signal.length];
    double[] real = new double[signal.length];

    for (int i = 0; i < signal.length; i++)
    {
      real[i] = signal[i] * window(i, real.length);
      img[i] = 0;
    }

    m_fft.fft(real, img);

    return fourierToFrequency(real, img);
  }

  // Ensure signal begin and end at zero,
  // so the function is periodic and is candidate to the Fourier transformation
  // Caution: unappropriated windows may generate unwanted sub-harmonic
  private double window(int index, int length)
  {
    double x = (double)index/(double)length;

    // Hann window
    return 0.5 - 0.5*Math.cos(2*Math.PI*x);
  }

  private double magnitude (double real, double img)
  {
    return Math.sqrt(real*real + img*img);
  }

  private double interpolateGaussian(
    double t1, double g1,
    double t2, double g2,
    double t3, double g3)
  {
    final double epsilon = 0.001;
    if (Math.abs (g1) < epsilon || Math.abs (g2) < epsilon || Math.abs (g3) < epsilon) return t2;
    if (t1 < epsilon || t2 < epsilon || t3 < epsilon) return t2;
    // Do interpolation to find the real frequency peak
    // Gaussian interpolation are better than parabolic.
    // Let a gaussian G(t) = exp(a*(t - u)^2 + h), with (g(freq_n) = peak_n). We know G(n), G(n-1) and G(n+1)
    // (a = -1/(2*sigma^2), h = ln(1/(sigma*2_PI))
    // ln (G(t1)/G(t2)) = ln(exp(a*(t1 - u)^2 + h - a*(t2 - u)^2 - h)) = a*((t1 - u)^2 - (t2-u)^2)
    // ln (G(t1)/G(t2)) / ln (G(t3)/G(t2)) = ln_ratio
    //   = a*((t1 - u)^2 - (t2-u)^2) / a*((t3 - u)^2 - (t2-u)^2)
    //   = ((t1 - u)^2 - (t2-u)^2) / ((t3 - u)^2 - (t2-u)^2) = ln_ratio
    // (t1^2 - 2*t1*u + u^2 - t2^2 + 2*t2*u - u^2) = ln_ratio*(t3^2 - 2*t3*u + u^2 - t2^2 + 2*t2*u - u^2)
    // (t1^2 - t2^2 - 2*(t1 - t2)*u) = ln_ratio*(t3^2 - t2^2 - 2*(t3 - t2)*u)
    // t1^2 - t2^2 - ln_ratio*(t3^2 - t2^2) = - 2*ln_ratio*(t3 - t2)*u) +  2*(t1 - t2)*u
    // t1^2 - t2^2 - ln_ratio*(t3^2 - t2^2) = (2*(t1 - t2) - 2*ln_ratio*(t3 - t2))*u)
    // u = (t1^2 - t2^2 - ln_ratio*(t3^2 - t2^2)) / (2*(t1 - t2) - 2*ln_ratio*(t3 - t2))
    // a = ln(G(t1)/g(t2)) / ((t1 - u)^2 - (t2-u)^2)
    // h = ln(G(t2)) - a*(t2 - u)^2
    double x = Math.log(g1/g2) / Math.log(g3/g2);
    double u = (t1*t1 - t2*t2 - x * (t3*t3 - t2*t2)) / (2 * ((t1 - t2) - x * (t3 - t2)));
    // double a = Math.log(g1/g2) / (Math.pow(t1 - u, 2) - Math.pow(t2 - u, 2));
    // double h = Math.log(g2) - a*Math.pow(t2 - u, 2);

    // The gaussian is maximal at u
    return !Double.isNaN(u) ? u : t2;
  }

  private double fourierToFrequency(
    double[] real, double[] img)
  {
    // Compute the Harmonic product spectrum
    double[] hps = new double[real.length/2];
    // for (int i = 0; i < hps.length; i++) hps[i] = 1.0;
    double[] m = new double[real.length/2];
    int index_max = 0;

    // /2 -> to match the Nyquist frequency. Part from n/2+1 to n is the conjugate,
    // ie the mirror of part 0 to n/2 if n/2, n is reported to the negative frequency spectrum
    // f(0), f(1), f(2), ... f(n) = -f(n/2), -f(n/2) +1, ..., f(-1), f(0), f(1), ... f(n/2)
    int len = real.length/2;

    for (int i = 0; i < len; i++)
    {
      // https://fr.wikipedia.org/wiki/S%C3%A9rie_de_Fourier
      // https://www.mathworks.com/help/matlab/ref/fft.html
      // c(n) = coef fourier complex, a, b : coef real
      // c(n) = x + i*y = 1/2*(a(n) -i*b(n)), c(-n) x - i*y = conjugate(c(n)) = 1/2(a(n)+ib(n))
      // a(n) = c(n) + c(-n), b(n) = i*(c(n) - c(-n)). (because signal is real)
      // In polar (r*exp(-i*phase), r = magnitude = a*a + b*b, phase = arctan(b/a))
      m[i] = magnitude(real[i], img[i]);
    }

    for (int i = 10; i < len; i++)
    {
      if (m[i] <= 20.0) continue;
      hps[i] = m[i];
      for (int j = 1; j < i+10; j++)
      {
        if (i*j < len)
        {
          if (m[i*j] >= 10.0)
            hps[i] *= m[i*j];
        }
        else
        {
          break;
        }
      }
      if (hps[i] > hps[index_max])
      {
        index_max = i;
      }
    }

    if (index_max <= 0 || index_max >= m.length) return 0;

    return interpolateGaussian
    (
      // Fourier frequencies associated to the 3 best peak
      ((double)(index_max - 1) * AUDIO_SAMPLE_RATE / real.length), m[index_max - 1],
      ((double)(index_max + 0) * AUDIO_SAMPLE_RATE / real.length), m[index_max + 0],
      ((double)(index_max + 1) * AUDIO_SAMPLE_RATE / real.length), m[index_max + 1]
    );
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

  private double fourierToFrequency1(
    double[] real, double[] img,
    double freqLowPass, double freqHighPass)
  {
    // /2 -> to match the Nyquist frequency. Part from n/2+1 to n is the conjugate,
    // ie the mirror of part 0 to n/2 if n/2, n is reported to the negative frequency spectrum
    // f(0), f(1), f(2), ... f(n) = -f(n/2), -f(n/2) +1, ..., f(-1), f(0), f(1), ... f(n/2)
    final int from = Math.max (0, (int)freqLowPass * real.length / AUDIO_SAMPLE_RATE);
    final int to = Math.max(from, Math.min (real.length/2, (int)freqHighPass * real.length / AUDIO_SAMPLE_RATE));
    int index;
    // modulus[i2] >= modulus[i1]; modulus[i2] >= modulus[i3]; i1 <= i2 <= i3; m, 3 best max
    // int  i1 = 0, i2 = 0, i3 = 0; // i1 < i2 < i3;
    int[] ind = new int[3];

    double[] modulus = new double[to - from + 1];
    for (index = from; index < to; index++)
    {
      // https://fr.wikipedia.org/wiki/S%C3%A9rie_de_Fourier
      // https://www.mathworks.com/help/matlab/ref/fft.html
      // c(n) = coef fourier complex, a, b : coef real
      // c(n) = x + i*y = 1/2*(a(n) -i*b(n)), c(-n) x - i*y = conjugate(c(n)) = 1/2(a(n)+ib(n))
      // a(n) = c(n) + c(-n), b(n) = i*(c(n) - c(-n)). (because signal is real)
      // In polar (r*exp(-i*phase), r = magnitude = a*a + b*b, phase = arctan(b/a))
      modulus[index - from] = magnitude(real[index], img[index]);

      // In positive spectre (0 ->length/2), multiply by 2 the frequency (to match the conjugate part, negative frequency)
      // Because a(n) = c(n) + c(-n), and as signal is real c(-n) = c(n)
      modulus[index - from] *= 2;
    }

    // Find the 3 best max
    index = 0;
    while (index < modulus.length)
    {
      // https://dspillustrations.com/pages/posts/misc/spectral-leakage-zero-padding-and-frequency-resolution.html
      // -> spectral leakage
      index = findLocalPeak(modulus, index, 0);

      // Is greater than one of max
      int j = 0;
      for (int i = 0; i < ind.length; i++)
      {
        if (modulus[index] > modulus[ind[i]])
        {
          j = 1;
          break;
        }
      }
      // Index take place of the min
      if (j > 0)
      {
        j = 0;
        for (int i = 1; i < ind.length; i++)
        {
          if (modulus[ind[i]] <= modulus[ind[j]])
            j = i;
        }
        ind[j] = index;
      }

      index++;
    }
    java.util.Arrays.sort(ind);
    // No local before/after
    if (ind[0] == ind[1]) ind[0] = 0;
    if (ind[1] == ind[2]) ind[2] = modulus.length - 1;
    if (ind[1] <= 0) return 0;

    return interpolateGaussian
      (
        // Fourier frequencies associated to the 3 best peak
        ((double)(ind[0] + from) * AUDIO_SAMPLE_RATE / real.length), modulus[ind[0]],
        ((double)(ind[1] + from) * AUDIO_SAMPLE_RATE / real.length), modulus[ind[1]],
        ((double)(ind[2] + from) * AUDIO_SAMPLE_RATE / real.length), modulus[ind[2]]
      );
  }
}

/*
 *  Copyright 2006-2007 Columbia University.
 *
 *  This file is part of MEAPsoft.
 *
 *  MEAPsoft is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  MEAPsoft is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MEAPsoft; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA
 *
 *  See the file "COPYING" for the text of the license.
 */
class FFT
{
  int n, m;

  // Lookup tables. Only need to recompute when size of FFT changes.
  double[] cos;
  double[] sin;

  public FFT(int n)
  {
    this.n = n;
    this.m = (int) (Math.log(n) / Math.log(2));

    // Make sure n is a power of 2
    if (n != (1 << m))
      throw new RuntimeException("FFT length must be power of 2");

    // precompute tables
    cos = new double[n / 2];
    sin = new double[n / 2];

    for (int i = 0; i < n / 2; i++)
    {
      cos[i] = Math.cos(-2 * Math.PI * i / n);
      sin[i] = Math.sin(-2 * Math.PI * i / n);
    }
  }

  /***************************************************************
   * fft.c
   * Douglas L. Jones
   * University of Illinois at Urbana-Champaign
   * January 19, 1992
   * http://cnx.rice.edu/content/m12016/latest/
   *
   *   fft: in-place radix-2 DIT DFT of a complex input
   *
   *   input:
   * n: length of FFT: must be a power of two
   * m: n = 2**m
   *   input/output
   * x: double array of length n with real part of data
   * y: double array of length n with imag part of data
   *
   *   Permission to copy and use this program is granted
   *   as long as this header is included.
   ****************************************************************/
  // FFT by bit-reversal permutation
  public void fft(double[] x, double[] y)
  {
    int i, j, k, n1, n2, a;
    double c, s, t1, t2;

    // Bit-reverse
    j = 0;
    n2 = n / 2;
    for (i = 1; i < n - 1; i++)
    {
      n1 = n2;
      while (j >= n1)
      {
        j = j - n1;
        n1 = n1 / 2;
      }
      j = j + n1;

      if (i < j)
      {
        t1 = x[i];
        x[i] = x[j];
        x[j] = t1;
        t1 = y[i];
        y[i] = y[j];
        y[j] = t1;
      }
    }

    // FFT
    n1 = 0;
    n2 = 1;

    for (i = 0; i < m; i++)
    {
      n1 = n2;
      n2 = n2 + n2;
      a = 0;

      for (j = 0; j < n1; j++)
      {
        c = cos[a];
        s = sin[a];
        a += 1 << (m - i - 1);

        for (k = j; k < n; k = k + n2)
        {
          t1 = c * x[k + n1] - s * y[k + n1];
          t2 = s * x[k + n1] + c * y[k + n1];
          x[k + n1] = x[k] - t1;
          y[k + n1] = y[k] - t2;
          x[k] = x[k] + t1;
          y[k] = y[k] + t2;
        }
      }
    }
  }
}
