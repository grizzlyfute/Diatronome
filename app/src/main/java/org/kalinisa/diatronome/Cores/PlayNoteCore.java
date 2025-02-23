package org.kalinisa.diatronome.Cores;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

public class PlayNoteCore
  extends BaseCore
{
  private static PlayNoteCore s_instance;
  private int m_waveForm = 0;
  private boolean m_useFlatSharp = false;
  private int m_temperament = 0;
  private double[] m_freqRatio = new double[]
    { 1.0, 1.0, 9.0/8.0, 9.0/8.0, 5.0/4.0, 4.0/3.0, 4.0/3.0, 3.0/2.0, 3.0/2.0, 5.0/3.0, 5.0/3.0, 15.0/8.0, 15.0/8.0, 2.0};
  private double m_refPitch = 440;
  private int m_currentNote = 0;
  private int m_currentOctave = 0;
  private AudioTrack m_audioTrack = null;
  private int m_audioPosEnd = 0;
  private int FADE_IN_OUT_MS = 10;

  // final int AUDIO_BIT_RATE = 44100,22050,16000,8000;
  private static final int AUDIO_BIT_RATE = AudioInterceptor.AudioTrack_getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
  private static final int AUDIO_FORMAT = AudioFormat.CHANNEL_OUT_STEREO;
  private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

  private final static int WAVEFORM_SINE = 1;
  private final static int WAVEFORM_TRIANGLE = 2;
  private final static int WAVEFORM_SAWTOOTH = 3;
  private final static int WAVEFORM_SQUARE = 4;

  private PlayNoteCore()
  {
    super();
  }

  public static PlayNoteCore getInstance()
  {
    if (s_instance == null)
    {
      s_instance = new PlayNoteCore();
    }
    return s_instance;
  }

  static private int getAudioFrameSize()
  {
    int frameSize = 1;
    if (AUDIO_FORMAT == AudioFormat.CHANNEL_OUT_DEFAULT ||
      AUDIO_FORMAT == AudioFormat.CHANNEL_OUT_MONO)
    {
      frameSize = 1;
    }
    else if (AUDIO_FORMAT == AudioFormat.CHANNEL_OUT_STEREO)
    {
      frameSize = 2;
    }
    if (AUDIO_ENCODING == AudioFormat.ENCODING_PCM_8BIT)
    {
      frameSize *= 1;
    }
    else
    {
      frameSize *= 2;
    }
    return frameSize;
  }

  @SuppressWarnings("deprecation")
  public static AudioTrack newAudioTrack(int audioLen, int AUDIO_MODE)
  {
    int frameSize = getAudioFrameSize();

    // Buffer size should be greater than MinSize, and multiple of (ChannelCount * frameSizeInByte)
    // Where frameSizeInByte = 1 if 8 BITS, 2 is 16 BITS, ChannelCount = 1 if mono, 2 is stereo
    final int AUDIO_BUFFER_SIZE = Math.max(
      frameSize * ((audioLen + (frameSize - 1)) / frameSize),
      AudioInterceptor.AudioTrack_getMinBufferSize(AUDIO_BIT_RATE, AUDIO_FORMAT, AUDIO_ENCODING));

    AudioTrack audioTrack = null;
    if (!AudioInterceptor.IsAudioWorking())
    {
      audioTrack = null;
    }
    else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
    {
      audioTrack = new AudioTrack(
        (new AudioAttributes.Builder())
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build(),
        (new AudioFormat.Builder())
          .setEncoding(AUDIO_ENCODING)
          .setSampleRate(AUDIO_BIT_RATE)
          .setChannelMask(AUDIO_FORMAT)
          .build(),
        AUDIO_BUFFER_SIZE,
        AUDIO_MODE,
        AudioManager.AUDIO_SESSION_ID_GENERATE);
    }
    else
    {
      audioTrack = new AudioTrack(
        AudioManager.STREAM_MUSIC,
        AUDIO_BIT_RATE,
        AUDIO_FORMAT,
        AUDIO_ENCODING,
        AUDIO_BUFFER_SIZE,
        AUDIO_MODE);
    }
    return audioTrack;
  }

  public static byte[] toAudioBytes(final short[] pcm)
  {
    //noinspection ConstantConditions
    byte audioByte[] = new byte[pcm.length *
      (AUDIO_ENCODING == AudioFormat.ENCODING_PCM_8BIT ? 1 : 2) *
      (AUDIO_FORMAT == AudioFormat.CHANNEL_OUT_STEREO ? 2 : 1)];
    int index_byte = 0;

    // Caution : Do not do  audioByte[index_byte++] = audioByte[index_byte - 2] because the left operand (index_byte++) will be executed before right operand (index_byte - 2)
    for (int i = 0; i < pcm.length; i++)
    {
      // in 16 bit wav PCM, first byte is the low order byte
      audioByte[index_byte] = (byte)((pcm[i] >> 0) & 0xFF);
      index_byte++;
      //noinspection ConstantConditions
      if (AUDIO_ENCODING == AudioFormat.ENCODING_PCM_16BIT)
      {
        audioByte[index_byte] = (byte)((pcm[i] >> 8) & 0xFF);
        index_byte++;
      }
      //noinspection ConstantConditions
      if (AUDIO_FORMAT == AudioFormat.CHANNEL_OUT_STEREO)
      {
        audioByte[index_byte] = audioByte[index_byte - 2];
        index_byte++;
        //noinspection ConstantConditions
        if (AUDIO_ENCODING == AudioFormat.ENCODING_PCM_16BIT)
        {
          audioByte[index_byte] = audioByte[index_byte - 2];
          index_byte++;
        }
      }
    }
    return audioByte;
  }

  public static short[] generatePcm(double frequency, double durationMs, int waveForm)
  {
    if (frequency <= 0 || durationMs <= 0) return null;

    // Generate sample
    // The duration become inaccurate. This allow to have an integer number of periods, and avoid audio glitch
    // nbr periods * period duration * bitrate
    int numSample = (int)(Math.ceil(Math.ceil(frequency * durationMs / 1000.0) * AUDIO_BIT_RATE / frequency));

    short[] soundPcm = new short[numSample];
    int i, n;
    double range = (double)(Short.MAX_VALUE) - (double)(Short.MIN_VALUE);
    double min = (double)(Short.MIN_VALUE);

    switch (waveForm)
    {
      case WAVEFORM_SINE:
        for (i = 0; i < numSample; i++)
        {
          soundPcm[i] = (short)((range / 2) * (Math.sin (2 * Math.PI * i * frequency / AUDIO_BIT_RATE) + 1) + min);
        }
        break;
      case WAVEFORM_TRIANGLE:
        n = (int)(AUDIO_BIT_RATE / frequency);
        for (i = 0; i < numSample; i++)
        {
          soundPcm[i] = (short)(range * (double)(i % n) / n + min);
        }
        break;
      case WAVEFORM_SAWTOOTH:
        n = (int)(AUDIO_BIT_RATE / (2*frequency));
        for (i = 0; i < numSample; i++)
        {
          if (i % (2*n) < n)
          {
            soundPcm[i] = (short)(range * (double)(i % n) / n + min);
          }
          else
          {
            soundPcm[i] = (short)(range * (1.0 - (double)(i % n) / n) + min);
          }
        }
        break;
      case WAVEFORM_SQUARE:
        n = (int)(AUDIO_BIT_RATE / (2*frequency));
        for (i = 0; i < numSample; i++)
        {
          if (i % (2*n) < n)
          {
            soundPcm[i] = (short)(min + range);
          }
          else
          {
            soundPcm[i] = (short)min;
          }
        }
        break;
      default:
        for (i = 0; i < numSample; i++)
        {
          soundPcm[i] = (short)(Math.random() * range + min);
        }
        break;
    }
    return soundPcm;
  }

  public static void fadeOutFilter(short[] audio, int filterDurationInMs)
  {
    final int filterDurationInSamples = Math.min(filterDurationInMs * AUDIO_BIT_RATE / 1000, audio.length);
    double fadeAmplification = 0;
    int j;

    for (int i = 0; i < filterDurationInSamples; i++)
    {
      j = audio.length - filterDurationInSamples + i;
      fadeAmplification = (1 - (double)i/filterDurationInSamples);
      audio[j] = (short)(fadeAmplification * audio[j]);
    }
  }

  public static void fadeInFilter(short[] audio, int filterDurationInMs)
  {
    final int filterDurationInSamples = Math.min(filterDurationInMs * AUDIO_BIT_RATE / 1000, audio.length);
    double fadeAmplification = 0;

    final int fadeOutOffset = audio.length - filterDurationInSamples;
    for (int i = 0 ; i < filterDurationInSamples; i++)
    {
      fadeAmplification = (double)i/filterDurationInSamples;
      audio[i] = (short)(fadeAmplification * audio[i]);
    }
  }

  public static void releaseAudioTrack(AudioTrack audioTrack)
  {
    if (audioTrack == null) return;
    if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED)
    {
      audioTrack.stop();
      // audioTrack.flush();
    }
    audioTrack.release();
  }

  public synchronized void stopPlaying()
  {
    m_currentOctave = -1;
    m_currentNote = -1;
    if (m_audioTrack != null && m_audioTrack.getState() == AudioTrack.STATE_INITIALIZED)
    {
      // Set the sequence to end
      m_audioTrack.pause();
      // Disable looping, do fad out to avoid glitch
      // m_audioTrack.setPlaybackHeadPosition(m_audioPosEnd);
      m_audioTrack.setLoopPoints(0, m_audioPosEnd, 0);
      m_audioTrack.play();
      // Wait for playing finish
      try
      {
        do
        {
          Thread.sleep (FADE_IN_OUT_MS);
        } while (m_audioTrack.getPlaybackHeadPosition() < m_audioPosEnd);
      }
      catch (InterruptedException e)
      {
        // Do nothing
      }
      m_audioTrack.stop();
    }

    releaseAudioTrack(m_audioTrack);
    m_audioTrack = null;
  }

  public synchronized void startPlaying(int octave, int note)
  {
    final double frequency = getFrequency(octave, note);
    final int frameSize = getAudioFrameSize();
    final short[] pcm = generatePcm(frequency, 1000 / frequency, m_waveForm);
    final short[] pcmStart = generatePcm(frequency, FADE_IN_OUT_MS, m_waveForm);
    final short[] pcmEnd = generatePcm(frequency, FADE_IN_OUT_MS, m_waveForm);
    fadeInFilter(pcmStart, FADE_IN_OUT_MS);
    fadeOutFilter(pcmEnd, FADE_IN_OUT_MS);
    byte[] audioPcm = new byte[frameSize * (pcmStart.length + pcm.length + pcmEnd.length)];
    byte[] audioPcmInBytes = null;
    int start, stop = 0;

    stopPlaying();

    m_currentOctave = octave;
    m_currentNote = note;

    audioPcmInBytes = toAudioBytes(pcmStart);
    System.arraycopy(audioPcmInBytes, 0, audioPcm, 0, audioPcmInBytes.length);
    start = pcmStart.length;

    audioPcmInBytes = toAudioBytes(pcm);
    System.arraycopy(audioPcmInBytes, 0, audioPcm, frameSize * start, audioPcmInBytes.length);
    stop = start + pcm.length;

    audioPcmInBytes = toAudioBytes(pcmEnd);
    System.arraycopy(audioPcmInBytes, 0, audioPcm, frameSize * stop, audioPcmInBytes.length);
    m_audioPosEnd = stop + pcmEnd.length;

    m_audioTrack = newAudioTrack(audioPcm.length, AudioTrack.MODE_STATIC);
    if (m_audioTrack != null && audioPcm != null)
    {
      // In static mode, write all in same time
      m_audioTrack.write (audioPcm, 0, audioPcm.length);
      // 0 <= start < end < audioBuffSize / frameSizeInByte
      int result = m_audioTrack.setLoopPoints(start, stop, -1);
      if (result != AudioTrack.SUCCESS)
      {
        android.util.Log.e(this.getClass().getName(), "Failed to initialized AudioTrack: " + result);
      }
      if (m_audioTrack.getState() == AudioTrack.STATE_INITIALIZED)
      {
        m_audioTrack.play();
      }
    }

    // Need to transpose and get wave form option
  }

  public boolean isPlaying(int octave, int note)
  {
    return octave == m_currentOctave && note == m_currentNote;
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
      default:
        waveform = WAVEFORM_SINE;
        break;
    }
    return waveform;
  }

  public void setWaveForm(String waveForm)
  {
    m_waveForm = strWaveFormToInt(waveForm);
  }

  public void setUseFlatSharp(Boolean useFlatSharp)
  {
    m_useFlatSharp = useFlatSharp;
    if (m_freqRatio == null)
    {
      setTemperament(m_temperament);
    }
  }

  public void setTemperament(int temperament)
  {
    if (temperament == m_temperament && m_temperament != 0) return;
    m_temperament = temperament;
    // Tone Equals Temperament (12-TET) / Equals Division Octave (12-EDO)
    if (temperament == 12)
    {
      double edo = Math.pow(2, 1.0/12.0);
      m_freqRatio = new double[]
      {
        Math.pow(edo,  0), // C
        Math.pow(edo,  1), // C#
        Math.pow(edo,  2), // D
        Math.pow(edo,  3), // D#
        Math.pow(edo,  4), // E
        Math.pow(edo,  5), // F
        Math.pow(edo,  6), // F#
        Math.pow(edo,  7), // G
        Math.pow(edo,  8), // G#
        Math.pow(edo,  9), // A
        Math.pow(edo, 10), // A#
        Math.pow(edo, 11), // B
        Math.pow(edo, 12), // C
      };
    }
    // Pythagorean (near 53 EDO) / Pure 3/2
    else if (temperament == 53)
    {
      m_freqRatio = new double[]
      {
        1.0,
        Math.pow(3, 7)/Math.pow(2, 11),
        Math.pow(3, 2)/Math.pow(2, 3),
        Math.pow(2, 5)/Math.pow(3, 3),
        Math.pow(3, 4)/Math.pow(2, 6),
        4.0/3.0,
        Math.pow(3, 6)/Math.pow(2, 9),
        3.0/2.0,
        Math.pow(2, 7)/Math.pow(3, 4),
        Math.pow(3, 3)/Math.pow(2, 4),
        Math.pow(2, 4)/Math.pow(3, 2),
        Math.pow(3, 5)/Math.pow(2, 7),
        2.0
      };
    }
    // Zarlino/Heimholtz - Pythagorean Just (close to 41 EDO)
    else if (temperament == 41)
    {
      m_freqRatio = new double[]
      {
        1.0,         // C=1
        25.0/24.0,   // C#=A*(3th)/2
        9.0/8.0,     // D=G*(5th)/2
        75.0/64.0,   // D#=B*(3th)/2
        5.0/4.0,     // E=1*(3th), 3th = 5/4
        4.0/3.0,     // F=1*(4th), 4th = 4/3
        45.0/32.0,   // F#=G*(5th)*(3th)/2
        3.0/2.0,     // G=1*(5th), 5th = 3/2
        25.0/16.0,   // G#=E*(3th)
        5.0/3.0,     // A=F*(4th)
        225.0/128.0, // A#=F#*(3th)
        15.0/8.0,    // B=G*(3th)
        2,
      };
    }
    // Meantone 1/4 ~ Tricesimoprimal - 31 TET|EDO / Pure5/4
    else if (temperament == 31)
    {
      // Fifth temperament interval
      double x = Math.pow(5, 1.0/4.0); // (3/2)*(80/81)^(1/4)
      m_freqRatio = new double[]
      {
        Math.pow (x,  0) * Math.pow (2,  0), // C
        Math.pow (x, -5) * Math.pow (2,  3), // C#
        Math.pow (x,  2) * Math.pow (2, -1), // D
        Math.pow (x, -3) * Math.pow (2,  2), // D#
        Math.pow (x,  4) * Math.pow (2, -2), // E
        Math.pow (x, -1) * Math.pow (2,  1), // F
        Math.pow (x,  6) * Math.pow (2, -3), // F# (or x^-6 * 2^4)
        Math.pow (x,  1) * Math.pow (2,  0), // G
        Math.pow (x, -4) * Math.pow (2,  3), // G#
        Math.pow (x,  3) * Math.pow (2, -1), // A
        Math.pow (x, -2) * Math.pow (2,  2), // A#
        Math.pow (x,  5) * Math.pow (2, -2), // B
        Math.pow (x,  0) * Math.pow (2,  1), // C
      };
    }
    // Meantone 1/5 ~ Perfect fifth 43 TET|EDO
    else if (temperament == 43)
    {
      double x = Math.pow(15.0/2.0, 1.0/5.0); // (3/2) / (81/80)^(1/5)
      m_freqRatio = new double[]
      {
        Math.pow (x,  0) * Math.pow (2,  0), // C
        Math.pow (x, -5) * Math.pow (2,  3), // C#
        Math.pow (x,  2) * Math.pow (2, -1), // D
        Math.pow (x, -3) * Math.pow (2,  2), // D#
        Math.pow (x,  4) * Math.pow (2, -2), // E
        Math.pow (x, -1) * Math.pow (2,  1), // F
        Math.pow (x,  6) * Math.pow (2, -3), // F# (or x^-6 * 2^4)
        Math.pow (x,  1) * Math.pow (2,  0), // G
        Math.pow (x, -4) * Math.pow (2,  3), // G#
        Math.pow (x,  3) * Math.pow (2, -1), // A
        Math.pow (x, -2) * Math.pow (2,  2), // A#
        Math.pow (x,  5) * Math.pow (2, -2), // B
        Math.pow (x,  0) * Math.pow (2,  1), // C
      };
    }
    // Meantone 1/3 ~ 19 TET|EDO  / Pure 6/5
    else if (temperament == 19)
    {
      double x = Math.pow(2187.0/640.0, 1.0/3.0);// (3/2) / (81/80)^(1/3)
      m_freqRatio = new double[]
      {
        Math.pow (x,  0) * Math.pow (2,  0), // C
        Math.pow (x, -5) * Math.pow (2,  3), // C#
        Math.pow (x,  2) * Math.pow (2, -1), // D
        Math.pow (x, -3) * Math.pow (2,  2), // D#
        Math.pow (x,  4) * Math.pow (2, -2), // E
        Math.pow (x, -1) * Math.pow (2,  1), // F
        Math.pow (x,  6) * Math.pow (2, -3), // F# (or x^-6 * 2^4)
        Math.pow (x,  1) * Math.pow (2,  0), // G
        Math.pow (x, -4) * Math.pow (2,  3), // G#
        Math.pow (x,  3) * Math.pow (2, -1), // A
        Math.pow (x, -2) * Math.pow (2,  2), // A#
        Math.pow (x,  5) * Math.pow (2, -2), // B
        Math.pow (x,  0) * Math.pow (2,  1), // C
      };
    }
    // Chromatic
    else if (temperament == 108)
    {
      double edo = Math.pow(2, 1.0/17.0);
      double syntonic = diffCents(1, edo) / 9.0;
      if (m_useFlatSharp)
      {
        m_freqRatio = new double[]
        {
          Math.pow(edo,  0), // C
          Math.pow(edo,  0) + syntonic, // C#
          Math.pow(edo,  2), // D
          Math.pow(edo,  2) + syntonic, // D#
          Math.pow(edo,  4), // E
          Math.pow(edo,  5), // F
          Math.pow(edo,  5) + syntonic, // F#
          Math.pow(edo,  7), // G
          Math.pow(edo,  7) + syntonic, // G#
          Math.pow(edo,  9), // A
          Math.pow(edo,  9) + syntonic, // A#
          Math.pow(edo, 11), // B
          Math.pow(edo, 12), // C
        };
      }
      else
      {
        m_freqRatio = new double[]
        {
          Math.pow(edo,  0), // C
          Math.pow(edo,  2) - syntonic, // Db
          Math.pow(edo,  2), // D
          Math.pow(edo,  4) - syntonic, // Eb
          Math.pow(edo,  4), // E
          Math.pow(edo,  5), // F
          Math.pow(edo,  7) - syntonic, // Gb
          Math.pow(edo,  7), // G
          Math.pow(edo,  9) - syntonic, // Ab
          Math.pow(edo,  9), // A
          Math.pow(edo, 11) - syntonic, // Bb
          Math.pow(edo, 11), // B
          Math.pow(edo, 12), // C
        };
      }
    }
    // 15 TET, 15 EDO
    else if (temperament == 15)
    {
      double edo = Math.pow(2, 1.0/15.0);
      if (m_useFlatSharp)
      {
        m_freqRatio = new double[]
        {
          Math.pow(edo,  0), // C
          Math.pow(edo,  1), // C#
          Math.pow(edo,  3), // D
          Math.pow(edo,  4), // D#
          Math.pow(edo,  6), // E
          Math.pow(edo,  7), // F
          Math.pow(edo,  8), // F#
          Math.pow(edo,  9), // G
          Math.pow(edo, 10), // G#
          Math.pow(edo, 12), // A
          Math.pow(edo, 13), // A#
          Math.pow(edo, 14), // B
          Math.pow(edo, 15), // C
        };
      }
      else
      {
        m_freqRatio = new double[]
        {
          Math.pow(edo,  0), // C
          Math.pow(edo,  2), // Db
          Math.pow(edo,  3), // D
          Math.pow(edo,  5), // Eb
          Math.pow(edo,  6), // E
          Math.pow(edo,  7), // F
          Math.pow(edo,  8), // F#
          Math.pow(edo,  9), // G
          Math.pow(edo, 11), // Ab
          Math.pow(edo, 12), // A
          Math.pow(edo, 13), // A#
          Math.pow(edo, 14), // B
          Math.pow(edo, 15), // C
        };
      }
    }
    // 17 TET, 17 EDO
    else if (temperament == 17)
    {
      double edo = Math.pow(2, 1.0/17.0);
      if (m_useFlatSharp)
      {
        m_freqRatio = new double[]
        {
          Math.pow(edo,  0), // C
          Math.pow(edo,  2), // C#
          Math.pow(edo,  3), // D
          Math.pow(edo,  5), // D#
          Math.pow(edo,  6), // E
          Math.pow(edo,  7), // F
          Math.pow(edo,  9), // F#
          Math.pow(edo, 10), // G
          Math.pow(edo, 12), // G#
          Math.pow(edo, 13), // A
          Math.pow(edo, 15), // A#
          Math.pow(edo, 16), // B
          Math.pow(edo, 17), // C
        };
      }
      else
      {
        m_freqRatio = new double[]
        {
          Math.pow(edo,  0), // C
          Math.pow(edo,  1), // Db
          Math.pow(edo,  3), // D
          Math.pow(edo,  4), // Eb
          Math.pow(edo,  6), // E
          Math.pow(edo,  7), // F
          Math.pow(edo,  8), // Gb
          Math.pow(edo, 10), // G
          Math.pow(edo, 11), // Ab
          Math.pow(edo, 13), // A
          Math.pow(edo, 14), // Bb
          Math.pow(edo, 16), // B
          Math.pow(edo, 17), // C
        };
      }
    }
  }

  public void setRefPitch(float refPitch)
  {
    // A4
    if (refPitch > 0)
      m_refPitch = refPitch;
  }

  public double getFrequency(int octave, int note)
  {
    double freq = 0;
    if (note < 0 || octave < 0) return 0;
    octave += note / 12;
    note %= 12;
    // 9 is note A. (C_ref = ratio1 = m_refPitch / m_freqRatio[9])
    freq = m_refPitch * m_freqRatio[note] / m_freqRatio[9];
    freq = freq * Math.pow (2, octave - 4);
    return freq;
  }

  public double diffCents(double freq1, double freq2)
  {
    return 1200 * Math.log(freq1/freq2) / Math.log(2);
  }

  public void getNearNoteFromFreq(double freq, Utils.Holder<Integer> holderOctave, Utils.Holder<Integer> holderNote)
  {
    /* Dichotomous search seem more efficient than Math.log
    // log(freq) = number of octave for the fundamental of 1
    int octave = (int)(Math.log(freq) / Math.log(2));
    freq /= (2 << octave);
    octave += 4 + (Math.log(m_refPitch) / Math.log(2));
    int note = 0;
    while (m_freqRatio[note] < freq) note++;
    outOctaveNote[0] = octave;
    outOctaveNote[1] = note;
    */

    // 12 * octave + note
    int min = 0 * 9 + 0, max = 12 * 9 + 11;
    int mid = 0;
    double f = 0;
    while (min < max)
    {
      mid = (min + max) /2;
      f = getFrequency(mid / 12, mid % 12);
      if (f < freq)
      {
        // Because we are searching an interval, not an exact value
        if (min == mid) min = mid+1;
        else min = mid;
      }
      else
      {
        max = mid;
      }
    }

    if (f < freq) min-=1;
    else max+=1;
    if (freq - getFrequency(min / 12, min % 12) > getFrequency(max / 12, max % 12) - freq)
      mid = max;
    else
      mid = min;

    if (holderOctave != null)
    {
      holderOctave.value = mid / 12;
    }
    if (holderNote != null)
    {
      holderNote.value = mid % 12;
    }
  }
}
