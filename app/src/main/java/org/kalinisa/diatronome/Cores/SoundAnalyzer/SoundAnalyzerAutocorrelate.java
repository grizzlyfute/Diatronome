package org.kalinisa.diatronome.Cores.SoundAnalyzer;

public class SoundAnalyzerAutocorrelate implements ISoundAnalyzer
{
  private final int AUDIO_SAMPLE_RATE;

  public SoundAnalyzerAutocorrelate(int audioSampleRate)
  {
    AUDIO_SAMPLE_RATE = audioSampleRate;
  }

  // https://www.cycfi.com/2018/03/fast-and-efficient-pitch-detection-bitstream-autocorrelation/
  // https://github.com/cycfi/bitstream_autocorrelation/blob/master/bcf2.cpp
  public double getPitch(double[] x)
  {
    final int nBits = 64;
    final int bitRes = 8;
    double old = 0;
    long[] zeroCrossBits = new long[x.length];
    int[] bitCount = new int [x.length* nBits /(2*bitRes)];
    int shift = 0, index = 0;
    long v;

    // Convert to bit, increase signal resolution by interpolating 0 crossing
    for (int i = 0; i < x.length; i++)
    {
      if ((old < 0 && x[i] > 0) ||
        (old > 0 && x[i] < 0))
      {
        // Linear interpolation to get the exact crossing on the bit resolution
        // a = (new - old) / (nBits), b = old+0, y=a*x+b take i-i (old) as origin
        v = (int)(-old* bitRes /(x[i] - old));
        v = -(1L << v); // ~((1L << v) - 1);
        if (old < x[i]) v = ~v;
      }
      else if (x[i] < -0.0001)
      {
        v = 0;
      }
      else
      {
        v = -1;
      }
      old = x[i];
      v &= ((1L << bitRes) - 1);
      zeroCrossBits[index] <<= bitRes;
      zeroCrossBits[index] |= v;
      shift++;
      if (shift >= nBits / bitRes)
      {
        shift = 0;
        index++;
      }
    }

    shift = 0;
    index = 0;
    for (int pos = 0; pos < bitCount.length; pos++)
    {
      bitCount[pos] = 0;
      for (int i = 0; i < zeroCrossBits.length/2; i++)
      {
        // Rotate
        v = zeroCrossBits[i + index] >>> shift;
        v |= zeroCrossBits[i + index + 1] << (nBits - shift);
        // Auto-correlation with XOR
        v = zeroCrossBits[i] ^ v;
        // Bit count
        while (v != 0)
        {
          if ((v & 1) != 0) bitCount[pos]++;
          v >>>= 1; // Do not use the signed operator '>>'
        }
      }

      shift++;
      if (shift >= nBits)
      {
        shift = 0;
        index++;
      }
    }

    // Find the local min of the auto-correlation
    index = 0;
    shift = 0;
    v = 0;
    for (int i = 2; i < bitCount.length; i++)
    {
      if (bitCount[i - 1] <= nBits &&
        (bitCount[i - 2] > bitCount[i - 1]) &&
        bitCount[i - 1] < bitCount[i])
      {
        v += i - 1 - shift;
        shift = i - 1;
        index++;
      }
    }

    return ((double) AUDIO_SAMPLE_RATE * bitRes *v/index);
  }
}
