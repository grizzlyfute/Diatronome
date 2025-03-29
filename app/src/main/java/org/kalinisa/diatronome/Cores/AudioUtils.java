package org.kalinisa.diatronome.Cores;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.audiofx.AutomaticGainControl;
import android.os.Build;

import androidx.annotation.NonNull;

public class AudioUtils
{
  public static boolean m_isNdkWorking = false;
  public static boolean m_isInitialized = false;

  // final int AUDIO_BIT_RATE = 44100,22050,16000,8000;
  public static final int AUDIO_BIT_RATE = AudioTrack_getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
  public static final int AUDIO_FORMAT = AudioFormat.CHANNEL_OUT_STEREO;
  public static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

  public final static int WAVEFORM_SINE = 1;
  public final static int WAVEFORM_TRIANGLE = 2;
  public final static int WAVEFORM_SAWTOOTH = 3;
  public final static int WAVEFORM_SQUARE = 4;

  public static synchronized boolean IsAudioWorking()
  {
    if (m_isInitialized) return m_isNdkWorking;
    try
    {
      Thread thread = new Thread(() ->
        AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
      );
      for (int timeout = 100; !thread.isAlive() && timeout > 0; --timeout)
      {
        Thread.sleep(10);
      }
      thread.join(3000);
      m_isNdkWorking = !thread.isAlive();
      if (!m_isNdkWorking)
      {
        thread.interrupt();
      }
    }
    catch (Exception e)
    {
      m_isNdkWorking = false;
    }
    finally
    {
      m_isInitialized = true;
    }

    return m_isNdkWorking;
  }

  public static int AudioTrack_getNativeOutputSampleRate(int streamType)
  {
    if (IsAudioWorking())
    {
      return AudioTrack.getNativeOutputSampleRate(streamType);
    }
    else
    {
      return 44100;
    }
  }

  public static int AudioTrack_getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat)
  {
    if (IsAudioWorking())
    {
      return AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
    }
    else
    {
      return 1024;
    }
  }

  public static int AudioRecord_getMinBufferSize(int sampleRateInHz, int channelConfig, int audioFormat)
  {
    if (IsAudioWorking())
    {
      return AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
    }
    else
    {
      return 1024;
    }
  }

  static public int getAudioFrameSize()
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

  public static int getAudioByteLen (int durationMs)
  {
    return (getAudioFrameSize() * AUDIO_BIT_RATE * durationMs) / 1000;
  }

  public static int getAudioBufferSize(int audioLen)
  {
    int frameSize = getAudioFrameSize();

    // Buffer size should be greater than MinSize, and multiple of (ChannelCount * frameSizeInByte)
    // Where frameSizeInByte = 1 if 8 BITS, 2 is 16 BITS, ChannelCount = 1 if mono, 2 is stereo
    return Math.max(
      frameSize * ((audioLen + (frameSize - 1)) / frameSize),
      AudioTrack_getMinBufferSize(AUDIO_BIT_RATE, AUDIO_FORMAT, AUDIO_ENCODING));
  }

  @SuppressWarnings("deprecation")
  public static AudioTrack newAudioTrack(int audioLen, int AUDIO_MODE)
  {
    final int AUDIO_BUFFER_SIZE = getAudioBufferSize(audioLen);

    AudioTrack audioTrack = null;
    if (!IsAudioWorking())
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
    byte[] audioByte = new byte[pcm.length *
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

  @NonNull
  public static short[] generatePcm(double frequency, double durationMs, int waveForm)
  {
    if (durationMs <= 0) return new short[0];
    if (frequency <= 0) return new short[getAudioByteLen((int)durationMs)];

    // Generate sample
    // The duration become inaccurate. This allow to have an integer number of periods, and avoid audio glitch
    // nbr periods * period duration * bitrate
    int numSample = (int)(Math.ceil(Math.ceil(frequency * durationMs / 1000.0) * AudioUtils.AUDIO_BIT_RATE / frequency));

    short[] soundPcm = new short[numSample];
    int i, n;
    double range = (double)(Short.MAX_VALUE) - (double)(Short.MIN_VALUE);
    double min = (double)(Short.MIN_VALUE);

    switch (waveForm)
    {
      case WAVEFORM_SINE:
        for (i = 0; i < numSample; i++)
        {
          soundPcm[i] = (short)((range / 2) * (Math.sin (2 * Math.PI * i * frequency / AudioUtils.AUDIO_BIT_RATE) + 1) + min);
        }
        break;
      case WAVEFORM_TRIANGLE:
        n = (int)(AudioUtils.AUDIO_BIT_RATE / frequency);
        for (i = 0; i < numSample; i++)
        {
          soundPcm[i] = (short)(range * (double)(i % n) / n + min);
        }
        break;
      case WAVEFORM_SAWTOOTH:
        n = (int)(AudioUtils.AUDIO_BIT_RATE / (2*frequency));
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
        n = (int)(AudioUtils.AUDIO_BIT_RATE / (2*frequency));
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

  public static void fadeOutFilter(short[] audio, double filterDurationInMs)
  {
    final int filterDurationInSamples = (int)Math.min(filterDurationInMs * AUDIO_BIT_RATE / 1000, audio.length);
    double fadeAmplification = 0;
    int j;

    for (int i = 0; i < filterDurationInSamples; i++)
    {
      j = audio.length - filterDurationInSamples + i;
      fadeAmplification = (1 - (double)i/filterDurationInSamples);
      audio[j] = (short)(fadeAmplification * audio[j]);
    }
  }

  public static void fadeInFilter(short[] audio, double filterDurationInMs)
  {
    final int filterDurationInSamples = (int)Math.min(filterDurationInMs * AUDIO_BIT_RATE / 1000, audio.length);
    double fadeAmplification = 0;

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
      audioTrack.pause();
      // audioTrack.flush();
    }
    audioTrack.release();
  }
}
