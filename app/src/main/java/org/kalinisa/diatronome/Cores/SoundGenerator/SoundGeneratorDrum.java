package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorDrum extends ASoundGenerator
{
  public SoundGeneratorDrum() { }
  private final double attenuator = 3.0;
  public boolean isContinous()
  {
    return false;
  }

  public double hintDurationMs(double freq)
  {
    return Math.max(38, 1000 * Math.log (0.01) / (-2 * Math.PI * attenuator));
  }

  public short[] generatePcm(double frequency, double durationMs)
  {
    if (durationMs < 0) return new short[0];
    final int numSample = AudioUtils.getAudioSampleLen(durationMs);
    final double[] soundDouble = new double[numSample];
    final double w = getWaveFactor();
    final int startAttenuation = AudioUtils.getAudioSampleLen(38);
    // Solutions of x^2+d^2(y)/dx^2 + x*dy/dx + (x^2-a^2)*y = 0;
    // https://mathworld.wolfram.com/BesselFunctionZeros.html
    final double[] bessel =
      // J_0(x)
      new double[] {2.4048, 5.5201, 8.6537, 11.7915, 14.9309 };
      // J_1(x)
      // new double[] {3.8317, 7.0156, 10.1735, 13.3237, 16.4706 };
      // J_2(x)
      // new double[] {5.1356, 8.4172, 11.6198, 14.7960, 17.9598 };
      // J_3(x)
      // new double[] {6.3802, 9.7610, 13.0152, 16.2235, 19.4094 };
      // J_4(x)
      // new double[] {7.5883, 11.0647, 14.3725, 17.6160, 20.8269 };
      // J_5(x)
      // new double[] {8.7715, 12.3386, 15.7002, 18.9801, 22.2178 };

    int i,n;
    double f;

    frequency /= bessel[0];

    for (i = 0; i < numSample; i++)
    {
      soundDouble[i] = 2*Math.sin (w * frequency * i);
      for (n = 0; n < bessel.length; n++)
      {
        f = frequency * bessel[n];
        soundDouble[i] += Math.sin (w * f * i) / (n + 1);
      }

      if (i > startAttenuation)
      {
        soundDouble[i] *= Math.exp (-attenuator * w * (i - startAttenuation));
      }
    }

    return toShortPcm(soundDouble);
  }
}
