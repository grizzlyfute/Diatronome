package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorSawtooth extends ASoundGenerator
{
  public SoundGeneratorSawtooth() { }

  public boolean isContinous()
  {
    return true;
  }

  public double hintDurationMs(double freq)
  {
    return (freq > 0 ? 1000.0 / freq : 0);
  }

  public short[] generatePcm(double frequency, double durationMs)
  {
    final int numSample = getWaveLenMultipleOfPeriod(frequency, durationMs);
    final short[] soundPcm = new short[numSample];
    final double range = (double)(Short.MAX_VALUE) - (double)(Short.MIN_VALUE);
    final double min = (double)(Short.MIN_VALUE);
    int i, n;

    if (durationMs <= 0) return new short[0];
    if (frequency <= 0) return new short[AudioUtils.getAudioSampleLen(durationMs)];

    n = (int)((AudioUtils.AUDIO_SAMPLE_RATE_HZ) / (2*frequency));
    for (i = 0; i < numSample; i++)
    {
      if (i % (2*n) < n)
      {
        soundPcm[i] = (short)(range * (double)(i % n) / n + min);
      }
      else
      {
        soundPcm[i] = (short)(range * (1.0 - (double)(i % n) / n) + min);
      }
    }

    return soundPcm;
  }
}
