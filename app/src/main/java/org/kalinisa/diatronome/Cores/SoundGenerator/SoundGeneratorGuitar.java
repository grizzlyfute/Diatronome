package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public class SoundGeneratorGuitar extends ASoundGenerator
{
  public SoundGeneratorGuitar() { }
  // Time ~= 0.5s
  private final double attenuator = 0.993;

  public boolean isContinuous()
  {
    return false;
  }

  public double hintDurationMs(double freq)
  {
    // attenuator^((duration=numSample/sampleRate) / (buf=sampleRate/frequency)) <= 0.2
    return freq > 0 ? 1000*Math.log(0.2) / (Math.log(attenuator) * freq) : 0;
  }

  // Karplus-Strong algorithm
  // Low pass filter + progressive damping
  // Use noise to simulate mechanical hit
  public short[] generatePcm(double frequency, double durationMs)
  {
    if (frequency <= 0) return new short[0];
    final int numSample = AudioUtils.getAudioSampleLen(durationMs);
    final double[] soundDouble = new double[numSample];
    int i;
    int index = 0, next;

    double[] buffer = new double[(int)(AudioUtils.AUDIO_SAMPLE_RATE_HZ / frequency)];
    for (i = 0; i < buffer.length; i++)
    {
      buffer[i] = Math.random() * 2 - 1;
    }

    for (i = 1; i < numSample; i++)
    {
      next = (index + 1) % buffer.length;
      soundDouble[i] = (buffer[index] + buffer[next]) / 2;
      buffer[index] = soundDouble[i] * attenuator;
      index = next;
    }

    return toShortPcm(soundDouble);
  }
}