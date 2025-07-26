package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorNoise extends ASoundGenerator
{
  public SoundGeneratorNoise() { }

  public boolean isContinous()
  {
    return true;
  }

  public double hintDurationMs(double freq)
  {
    return 3;
  }

  public short[] generatePcm(double frequency, double durationMs)
  {
    final int numSample = AudioUtils.getAudioSampleLen(durationMs);
    final short[] soundPcm = new short[numSample];
    final double range = (double)(Short.MAX_VALUE) - (double)(Short.MIN_VALUE);
    final double min = (double)(Short.MIN_VALUE);
    int i;

    for (i = 0; i < numSample; i++)
    {
      soundPcm[i] = (short)(Math.random() * range + min);
    }

    return soundPcm;
  }
}
