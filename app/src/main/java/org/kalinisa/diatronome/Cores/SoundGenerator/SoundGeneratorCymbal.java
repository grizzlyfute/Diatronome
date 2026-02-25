package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorCymbal extends ASoundGenerator
{
  public SoundGeneratorCymbal() { }
  // Time ~= 0.5s
  private final double attenuator = 0.003;

  public boolean isContinuous()
  {
    return false;
  }

  public double hintDurationMs(double freq)
  {
    // exp(-freq * attenuator * t) <= 0.1
    return -1000 * Math.log(0.1) / (freq * attenuator);
  }

  public short[] generatePcm(double frequency, double durationMs)
  {
    if (frequency <= 0) return new short[0];
    final int numSample = AudioUtils.getAudioSampleLen(durationMs);
    double[] soundDouble = new double[numSample];
    int i;
    final double w = getWaveFactor();

    double[] partialFrequency = new double[] { 1.0, 2.3, 3.1, 4.7, 6.2 };
    double filtered, previousNoise = 0, noise;
    double a = 1 - w * frequency;
    double decay = 0;
    double amplitude = 0;
    double phaseModulation = 0;

    for (i = 0; i < soundDouble.length; i++)
    {
      double t = (double)i / AudioUtils.AUDIO_SAMPLE_RATE_HZ;
      soundDouble[i] = 0;
      // Sub harmonics
      for (int k = 0; k < partialFrequency.length; k++)
      {
        phaseModulation = 2 * Math.PI * (2*k-1) / (partialFrequency.length + 1);
        decay = frequency*partialFrequency[k] * attenuator;
        amplitude = Math.max (1 - 0.2*k / partialFrequency.length, 0.1);
        // Non-correlate harmonics, with independent decay
        soundDouble[i] += amplitude * Math.sin(w * frequency * partialFrequency[k] * i + phaseModulation) * Math.exp (-decay * t);
        // Adding saturate
        soundDouble[i] = Math.tanh(1.1 * soundDouble[i]);
      }

      noise = Math.random() * 2 - 1;
      // Colored noise
      noise = 0.3 * noise + 0.7 * previousNoise;
      // Infinite Impulse Response filter (IIFR) first order - high pass
      // |H(e^j*w*t)| = sqrt(1 + a^2 - 2*a*cos(w))
      // Fc = ((1−a) / (2*Pi)) * SampleRate = (1-a) / w
      filtered = noise - a * previousNoise;
      previousNoise = noise;
      amplitude = 3.5;
      decay = 4*frequency*partialFrequency[0] * attenuator;
      soundDouble[i] += filtered * amplitude * Math.exp (-decay * t);
    }

    return toShortPcm(soundDouble);
  }
}