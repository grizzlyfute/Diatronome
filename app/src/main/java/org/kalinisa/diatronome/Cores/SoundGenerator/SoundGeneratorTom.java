package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorTom extends ASoundGenerator
{
  public SoundGeneratorTom() { }
  private final double attenuator = 0.75;

  public boolean isContinuous()
  {
    return false;
  }

  public double hintDurationMs(double freq)
  {
    // return 1000 * Math.log (0.05) / (-2 * Math.PI * attenuator);
    return 125;
  }

  public short[] generatePcm(double frequency, double durationMs)
  {
    final int numSample = AudioUtils.getAudioSampleLen(durationMs);
    final double[] soundDouble = new double[numSample];
    final double w = getWaveFactor();
    int i;
    double f;
    double a;

    for (i = 0; i < numSample; i++)
    {
      soundDouble[i] = 0;
      // Pitch drop
      f = frequency * (1 + 0.3 * Math.exp(-40.0 * i / AudioUtils.AUDIO_SAMPLE_RATE_HZ));
      soundDouble[i] += Math.sin(w * f * i) * Math.exp (-10.0 * i / AudioUtils.AUDIO_SAMPLE_RATE_HZ);
      // Harmonics
      a = Math.exp (-attenuator * w * i);
      soundDouble[i] += a * 1.0 * Math.sin (frequency * w * i);
      soundDouble[i] += a * 0.4 * Math.sin (2*frequency * w * i + 2*Math.PI/3);
      soundDouble[i] += a * 0.15* Math.sin (3*frequency * w * i + 4*Math.PI/3);
    }

    return toShortPcm(soundDouble);
  }
}
