package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorBell extends ASoundGenerator
{
  public SoundGeneratorBell() { }

  private final double attenuator = 0.50;

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
    int i, n;
    double att = attenuator;
    // Remove resonator if asked time is short
    if (durationMs < hintDurationMs(frequency)/2 && durationMs > 0)
      att = hintDurationMs(frequency) * attenuator / durationMs;

    for (i = 0; i < numSample; i++)
    {
      soundDouble[i] = 0;
      for (n = 1; n < 8; n++)
      {
        soundDouble[i] += Math.sin (w * frequency * Math.pow(n, Math.sqrt(2)) * i) *
          Math.exp(-w * att * i) / n;
      }
    }

    return toShortPcm(soundDouble);
  }
}
