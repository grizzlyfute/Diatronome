package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorTriangle extends ASoundGenerator
{
  public SoundGeneratorTriangle() { }

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
      soundPcm[i] = (short)(range * (double)(i % n) / n + min);
    }

    return soundPcm;
  }
}
