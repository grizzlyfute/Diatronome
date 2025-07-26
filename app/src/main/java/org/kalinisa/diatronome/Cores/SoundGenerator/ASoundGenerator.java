package org.kalinisa.diatronome.Cores.SoundGenerator;

import org.kalinisa.diatronome.Cores.AudioUtils;

public abstract class ASoundGenerator
{
  public abstract short[] generatePcm(double frequency, double durationMs);

  // All continuous sound shall start and stop by zero
  public abstract boolean isContinous();
  public abstract double hintDurationMs(double freq);

  public final static int WAVEFORM_SINE = 1;
  public final static int WAVEFORM_TRIANGLE = 2;
  public final static int WAVEFORM_SAWTOOTH = 3;
  public final static int WAVEFORM_SQUARE = 4;
  public final static int WAVEFORM_PIANO = 5;
  public final static int WAVEFORM_ORGAN = 6;
  public final static int WAVEFORM_DRUM = 7;
  public final static int WAVEFORM_TOM = 8;
  public final static int WAVEFORM_SNAREDRUM = 9;
  public final static int WAVEFORM_STEELDRUM = 10;
  public final static int WAVEFORM_BELL = 11;
  public final static int WAVEFORM_LASER = 12;

  public static ASoundGenerator factory (int waveForm)
  {
    ASoundGenerator result = null;
    switch (waveForm)
    {
      case WAVEFORM_SINE:
        result = new SoundGeneratorSine();
        break;

      case WAVEFORM_TRIANGLE:
        result = new SoundGeneratorTriangle();
        break;

      case WAVEFORM_SAWTOOTH:
        result = new SoundGeneratorSawtooth();
        break;

      case WAVEFORM_SQUARE:
        result = new SoundGeneratorSquare();
        break;

      case WAVEFORM_PIANO:
        result = new SoundGeneratorPiano();
        break;

      case WAVEFORM_ORGAN:
        result = new SoundGeneratorOrgan();
        break;

      case WAVEFORM_DRUM:
        result = new SoundGeneratorDrum();
        break;

      case WAVEFORM_TOM:
        result = new SoundGeneratorTom();
        break;

      case WAVEFORM_SNAREDRUM:
        result = new SoundGeneratorSnareDrum();
        break;

      case WAVEFORM_STEELDRUM:
        result = new SoundGeneratorSteeldrum();
        break;

      case WAVEFORM_BELL:
        result = new SoundGeneratorBell();
        break;

      case WAVEFORM_LASER:
        result = new SoundGeneratorLaser();
        break;

      default:
        result = new SoundGeneratorNoise();
        break;
    }
    return result;
  }

  public static ASoundGenerator factory (String waveFormStr)
  {
    return factory(strWaveFormToInt(waveFormStr));
  }

  public static int strWaveFormToInt(String waveFormStr)
  {
    int waveform;
    switch (waveFormStr)
    {
      case "SINE":
        waveform = WAVEFORM_SINE;
        break;
      case "TRIANGLE":
        waveform = WAVEFORM_TRIANGLE;
        break;
      case "SAWTOOTH":
        waveform = WAVEFORM_SAWTOOTH;
        break;
      case "SQUARE":
        waveform = WAVEFORM_SQUARE;
        break;
      case "PIANO":
        waveform = WAVEFORM_PIANO;
        break;
      case "ORGAN":
        waveform = WAVEFORM_ORGAN;
        break;
      case "DRUM":
        waveform = WAVEFORM_DRUM;
        break;
      case "TOM":
        waveform = WAVEFORM_TOM;
        break;
      case "SNAREDRUM":
        waveform = WAVEFORM_SNAREDRUM;
        break;
      case "STEELDRUM":
        waveform = WAVEFORM_STEELDRUM;
        break;
      case "BELL":
        waveform = WAVEFORM_BELL;
        break;
      case "LASER":
        waveform = WAVEFORM_LASER;
        break;
      default:
        waveform = WAVEFORM_SINE;
        break;
    }
    return waveform;
  }

  protected int getWaveLenMultipleOfPeriod(double frequency, double durationMs)
  {
    if (frequency <= 0) return 0;
    // Generate sample
    // The duration become inaccurate. This allow to have an integer number of periods, and avoid audio glitch
    // nbr periods * period duration * bitrate
    return (int)(Math.ceil(Math.ceil(frequency * durationMs / 1000.0) * AudioUtils.AUDIO_SAMPLE_RATE_HZ / frequency));
  }

  protected double getWaveFactor()
  {
    return 2 * Math.PI / AudioUtils.AUDIO_SAMPLE_RATE_HZ;
  }

  protected short[] toShortPcm(double[] values)
  {
    short[] pcm = new short[values.length];
    double min = 0, max = 0;
    int i;
    for (i = 0; i < values.length; i++)
    {
      if (values[i] < min) min = values[i];
      if (values[i] > max) max = values[i];
    }
    for (i = 0; i < values.length; i++)
    {
      pcm[i] = (short)(Short.MIN_VALUE + (values[i] - min) * (Short.MAX_VALUE - Short.MIN_VALUE) / (max - min));
    }
    return pcm;
  }
}
