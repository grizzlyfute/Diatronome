package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorTom extends ASoundGenerator
{
  public SoundGeneratorTom() { }
  private final double attenuator = 0.75;

  public boolean isContinous()
  {
    return false;
  }

  public double hintDurationMs(double freq)
  {
    // return 1000 * Math.log (0.05) / (-2 * Math.PI * attenuator);
    return 100;
  }

  public short[] generatePcm(double frequency, double durationMs)
  {
    final int numSample = AudioUtils.getAudioSampleLen(durationMs);
    final double[] soundDouble = new double[numSample];
    final double w = getWaveFactor();
    int i;
    double f;

    for (i = 0; i < numSample; i++)
    {
      // Frequency period 100 ms,
      f = frequency * (1 + 0.1 * Math.sin (w * i * 10*(1000 / durationMs)) * (Math.exp(-10*(1000 / durationMs) * i / AudioUtils.AUDIO_SAMPLE_RATE_HZ)));
      soundDouble[i] = Math.sin (w * f * i) * (Math.exp (-attenuator * w * i) + Math.exp(-0.25 * attenuator * w * i));
    }

    return toShortPcm(soundDouble);
  }
}
