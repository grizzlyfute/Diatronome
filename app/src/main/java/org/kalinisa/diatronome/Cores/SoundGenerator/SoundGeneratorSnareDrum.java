package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorSnareDrum extends ASoundGenerator
{
  public SoundGeneratorSnareDrum() { }
  private final double attenuator = 3.2;

  public boolean isContinous()
  {
    return false;
  }

  public double hintDurationMs(double freq)
  {
    return 1000 * Math.log (0.01) / (-2 * Math.PI * attenuator);
  }

  public short[] generatePcm(double frequency, double durationMs)
  {
    final int numSample = AudioUtils.getAudioSampleLen(durationMs);
    final double[] soundDouble = new double[numSample];
    final double w = getWaveFactor();
    int i,n;
    double f;
    // J2
    double[] bessel = new double[] {5.1356, 8.4172, 11.6198, 14.7960, 17.9598 };

    for (i = 0; i < numSample; i++)
    {
      soundDouble[i] = 0;
      f = frequency * (1 + 0.02*2*(Math.random() - 0.5));
      soundDouble[i] += Math.sin (w * frequency * i);

      for (n = 0; n < bessel.length; n++)
      {
        soundDouble[i] += (1.0 / bessel.length) * Math.sin (w * bessel[n] * frequency * i + n) / Math.pow(n+1, 1.2);
      }

      soundDouble[i] += 1.0 * 2 * (Math.random() - 0.5);
      soundDouble[i] *= Math.exp(-w * attenuator * i);
    }


    /* for (i = 0; i < numSample; i++)
    {
      soundDouble[i] = 0;
      f = frequency * (1 + 0.02*2*(Math.random() - 0.5));
      soundDouble[i] += Math.sin (w * f * i);

      for (n = 0; n < bessel.length; n++)
      {
        soundDouble[i] += (1.0 / bessel.length) * Math.sin (w * bessel[n] * frequency * i + n) / Math.pow(n+1, 1.2);
      }
      soundDouble[i] *= Math.exp(-w * attenuator * i);
    } */

    return toShortPcm(soundDouble);
  }
}