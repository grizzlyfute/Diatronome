package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorOrgan extends ASoundGenerator
{
  public SoundGeneratorOrgan() { }

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
    final int numSample = AudioUtils.getAudioSampleLen(durationMs);
    final double[] soundDouble = new double[numSample];
    final double w = getWaveFactor();
    int i,n;
    double phaseModulation;

    for (i = 0; i < numSample; i++)
    {
      soundDouble[i] = 0;
      // Simulate the wind
      for (n = 1; n <= 4; n++)
      {
        phaseModulation = 2*Math.PI * (n-1) / 5;
        soundDouble[i] += Math.sin(n * w * frequency * i + phaseModulation)/Math.pow(n, 1.1);
      }
    }

    return toShortPcm(soundDouble);
  }
}
