package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorSine extends ASoundGenerator
{
  public SoundGeneratorSine() { }

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
    final double w = getWaveFactor();
    int i;

    if (durationMs <= 0) return new short[0];
    if (frequency <= 0) return new short[AudioUtils.getAudioSampleLen(durationMs)];

    for (i = 0; i < numSample; i++)
    {
      soundPcm[i] = (short)((range / 2) * (Math.sin (w * frequency * i) + 1) + min);
    }

    return soundPcm;
  }
}
