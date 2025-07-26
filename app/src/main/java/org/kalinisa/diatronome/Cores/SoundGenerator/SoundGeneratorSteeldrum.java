package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorSteeldrum extends ASoundGenerator
{
  public SoundGeneratorSteeldrum() { }
  // Sound will be about 100 ms
  private final double attenuator = 2.5;

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
    int i;

    for (i = 0; i < numSample; i++)
    {
      // We can approximate by
      // Math.sin(n * w * frequency * i) / Math.pow(n, 1.5)
      // or  Math.sin(x) + 0.3 * Math.sin(2 * w * frequency * i + 0.5) + 0.15 * Math.sin(3 * w * frequency * i + 1.0)
      soundDouble[i] = Math.tanh(3*Math.sin(w * frequency * i));
      soundDouble[i] *= (Math.exp (-attenuator * w * i)+ + Math.exp(-0.25 * attenuator * w * i));
    }

    return toShortPcm(soundDouble);
  }
}
