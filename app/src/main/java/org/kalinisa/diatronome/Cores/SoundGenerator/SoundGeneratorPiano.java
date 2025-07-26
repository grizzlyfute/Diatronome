package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorPiano extends ASoundGenerator
{
  public SoundGeneratorPiano() { }
  private final double attenuator = 0.003;

  public boolean isContinous()
  {
    return false;
  }

  public double hintDurationMs(double freq)
  {
    return 1000 * Math.log (0.01) / (- 2 * Math.PI * attenuator * freq);
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
      att = 0.33*hintDurationMs(frequency) * attenuator / durationMs;

    for (i = 0; i < numSample; i++)
    {
      soundDouble[i] = 0;
      for (n = 1; n <= 6; n++)
      {
        soundDouble[i] += (Math.sin (w * n * frequency * i)) * Math.exp (-w * att * frequency * i) / (Math.pow(2, n-1));
      }
      soundDouble[i] += Math.pow(soundDouble[i], 3);
      soundDouble[i] *= 1 + 16 * ((double)i / AudioUtils.AUDIO_SAMPLE_RATE_HZ) * Math.exp(-6 * (double)i / AudioUtils.AUDIO_SAMPLE_RATE_HZ);
    }

    return toShortPcm(soundDouble);
  }
}
