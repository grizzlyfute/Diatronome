package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorLaser extends ASoundGenerator
{
  public SoundGeneratorLaser() { }

  private final double attenuator = 2.0;

  public boolean isContinous()
  {
    return false;
  }

  public double hintDurationMs(double freq)
  {
    return 444;
  }

  public short[] generatePcm(double frequency, double durationMs)
  {
    final int numSample = AudioUtils.getAudioSampleLen(durationMs);
    final double[] soundDouble = new double[numSample];
    final double w = getWaveFactor();
    int i,n;
    double f;

    for (i = 0; i < numSample; i++)
    {
      // Tuner
      if (durationMs < 200)
      {
        f = frequency * (1 + 1 * Math.cos (2.0 * w * i * 1000 / durationMs) * (Math.exp(-1.0 * (1000 / durationMs) * i / AudioUtils.AUDIO_SAMPLE_RATE_HZ)));
      }
      // Synthesizer
      else
      {
        f = 1;
        f += 3*Math.exp(-4.00 * i / AudioUtils.AUDIO_SAMPLE_RATE_HZ);
        f += 0.05*Math.cos (5 * w * i * 1000 / durationMs) * (Math.exp(-2.0 * (1000 / durationMs) * i / AudioUtils.AUDIO_SAMPLE_RATE_HZ));
        f *= frequency;
      }
      soundDouble[i] = Math.sin (w * f * i) * (Math.exp(-w * attenuator * i) + Math.exp(-0.25 * attenuator * w * i));

      // Add reverb
      f = 0;
      n = i - AudioUtils.getAudioSampleLen(11);
      if (n >= 0) f += 0.3 * soundDouble[n];
      n = i - AudioUtils.getAudioSampleLen(13);
      if (n >= 0) f += 0.2 * soundDouble[n];
      n = i - AudioUtils.getAudioSampleLen(17);
      if (n >= 0) f += 0.1 * soundDouble[n];
      soundDouble[i] += f;
    }

    return toShortPcm(soundDouble);
  }
}