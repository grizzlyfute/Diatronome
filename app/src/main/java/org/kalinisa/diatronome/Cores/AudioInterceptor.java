package org.kalinisa.diatronome.Cores;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;

/*
 * The aim of this class is to intercept AudioRecord and AudioTrack not working
 * on emulator. The native SDK may not reply, simply avoiding to launch the view.
 */
public class AudioInterceptor
{
  public static boolean m_isNdkWorking = false;
  public static boolean m_isInitialized = false;

  public static boolean IsAudioWorking()
  {
    if (m_isInitialized) return m_isNdkWorking;
    try
    {
      Thread thread = new Thread(() ->
        AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT));
      thread.start();
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
}
