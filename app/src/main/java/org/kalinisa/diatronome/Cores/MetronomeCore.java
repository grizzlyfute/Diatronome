package org.kalinisa.diatronome.Cores;

import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class MetronomeCore
  extends BaseCore
{
  public static final int HANDLER_MSG_TICK = 1;
  public static final int HANDLER_MSG_PLAY = 2;
  public static final int HANDLER_MSG_TEMPO = 3;

  // public static final int BEATCONFIG_SKIP = 0;
  public static final int BEATCONFIG_OFF = 1;
  public static final int BEATCONFIG_SUBDIV = 2;
  public static final int BEATCONFIG_NORMAL = 3;
  public static final int BEATCONFIG_ACCENT = 4;

  public static final int MAX_BEATCONFIG = 20;

  public static final int DELAY_MS_ON_BPM_CHANGE = 200;
  private static final int AUDIO_DURATION_MS = 130;
  // 8000, 22050, 44100, ....
  private static final int AUDIO_BIT_RATE = AudioInterceptor.AudioTrack_getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);

  public static final int BPM_MIN = 1;
  public static final int BPM_MAX = 320;

  // Settings
  int m_pitchMainSkb = 0;
  int m_pitchAccentSkb = 0;
  int m_pitchSubdivisionSkb = 0;
  float m_refPitch = 0;
  int m_waveform = 0;
  int m_tempoBpm = 0;
  int[] m_beatsConfig = new int[0];
  boolean m_isBidirectionalNeedle = false;

  // Internal Working
  private boolean m_regenerateTone = false;
  private int m_currentTick = 0;
  private final long[] m_tapLast;
  private int m_tapIndex = 0;
  private Timer m_timer = null;
  AudioTrack m_audioTrack = null;
  private byte[] m_waveAccent;
  private byte[] m_waveSubdiv;
  private byte[] m_waveMiddle;
  private final byte[] m_waveSilence;
  private Semaphore m_mutexTick;
  private boolean m_isReschedule = false;

  private static MetronomeCore s_instance;
  private MetronomeCore()
  {
    super();

    m_beatsConfig = new int[0];
    m_tapLast = new long[5];
    m_mutexTick = new Semaphore(1, false);

    m_waveSilence = new byte[AUDIO_DURATION_MS*AUDIO_BIT_RATE/1000];
    m_audioTrack = PlayNoteCore.newAudioTrack(m_waveSilence.length, AudioTrack.MODE_STREAM);
  }

  public static MetronomeCore getInstance()
  {
    if (s_instance == null)
    {
      s_instance = new MetronomeCore();
    }
    return s_instance;
  }

  private boolean mutexTryAcquire(Semaphore mutex, long timeoutMs)
  {
    boolean result = false;
    try
    {
      if (timeoutMs > 0)
      {
        result = mutex.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
      }
      else
      {
        mutex.acquire();
        result = true;
      }
    }
    catch (InterruptedException e)
    {
      result = false;
    }
    return result;
  }

  private void mutextRelease(Semaphore mutex)
  {
    mutex.release();
  }

  public void setRefPitch(float refPitch)
  {
    m_refPitch = refPitch;
    m_regenerateTone = true;
  }

  public void setPitchAccentSkb(int pitchAccentSKb)
  {
    // keep deferred to be sure to have the ref pitch for computation
    m_pitchAccentSkb = pitchAccentSKb;
    m_regenerateTone = true;
  }

  public void setPitchMainSkb(int pitchMainSkb)
  {
    // keep deferred to be sure to have the ref pitch for computation
    m_pitchMainSkb = pitchMainSkb;
    m_regenerateTone = true;
  }

  public void setPitchSubdivisionSkb(int pitchSubdivisionSkb)
  {
    // keep deferred to be sure to have the ref pitch for computation
    m_pitchSubdivisionSkb = pitchSubdivisionSkb;
    m_regenerateTone = true;
  }

  public void setWaveForm(String waveformStr)
  {
    m_waveform = PlayNoteCore.strWaveFormToInt(waveformStr);
    m_regenerateTone = true;
  }

  public void setIsBidirectionalNeedle(boolean isBidirectional)
  {
    m_isBidirectionalNeedle = isBidirectional;
  }
  public boolean getIsBidirectionalNeedle()
  {
    return m_isBidirectionalNeedle;
  }

  private float seekBarToPitch(int skb)
  {
    // values are 0, 1, 2, 3, 4, 5; 3 = ref pitch
    float ret = 0;
    if (skb < 0) ret = 0;
    else if (skb == 0) ret = 1*m_refPitch / 4; // 0.25
    else if (skb == 1) ret = 2*m_refPitch / 4; // 0.5
    else if (skb == 2) ret = 3*m_refPitch / 4; // 0.75
    else if (skb == 3) ret = 4*m_refPitch / 4; // 1
    else if (skb == 4) ret = 3*m_refPitch / 2; // 1.5
    else if (skb == 5) ret = 4*m_refPitch / 2; // 2
    else if (skb == 6) ret = 6*m_refPitch / 2; // 3
    else ret = m_refPitch;
    return ret;
  }

  private byte[] generatePcmWithFadeInOut(float pitch)
  {
    short[] pcm = null;
    pcm = PlayNoteCore.generatePcm(pitch, AUDIO_DURATION_MS, m_waveform);
    PlayNoteCore.fadeInFilter(pcm, 5);
    PlayNoteCore.fadeOutFilter(pcm, 5);
    return PlayNoteCore.toAudioBytes(pcm);
  }

  // Work under tick mutex
  private void setupTone()
  {
    float pitch = 0.0f;
    short[] pcm = null;
    // All the parameter are know to generate sound track
    if (m_regenerateTone && m_waveform > 0 && m_refPitch > 0)
    {
      // PlayNoteCore.releaseAudioTrack(m_audioTrack);

      // Increase the duration to have an integer number of period
      pitch = seekBarToPitch(m_pitchAccentSkb);
      if (pitch > 0)
      {
        m_waveAccent = generatePcmWithFadeInOut(pitch);
      }
      else
      {
        m_waveAccent = m_waveSilence;
      }

      pitch = seekBarToPitch(m_pitchMainSkb);
      if (pitch > 0)
      {
        m_waveMiddle = generatePcmWithFadeInOut(pitch);
      }
      else
      {
        m_waveMiddle = m_waveSilence;
      }

      pitch = seekBarToPitch(m_pitchSubdivisionSkb);
      if (pitch > 0)
      {
        m_waveSubdiv = generatePcmWithFadeInOut(pitch);
      }
      else
      {
        m_waveSubdiv = m_waveSilence;
      }
    }
  }

  public int getTempoBpm()
  {
    return m_tempoBpm;
  }
  public void setTempoBpm(int value)
  {
    // Prevent extra tick on recreate
    if (m_tempoBpm == value) return;
    m_tempoBpm = value;
    if (getIsPlaying())
    {
      // Rescheduled according to the new period.
      // Do not replay the same tick nor it will break animation
      scheduleTick(true);
    }
  }

  public int getPeriodMs()
  {
    int tempo = getTempoBpm();
    if (tempo > 0)
      return (int)(60.0 * 1000) / tempo;
    else
      return 0;
  }

  public void tap()
  {
    // Min tempo is 1 bpm
    int length = m_tapLast.length;
    long period = 0;
    int n = 0;
    m_tapLast[m_tapIndex] = System.currentTimeMillis();

    for (int i = 0; i < length - 1; i++)
    {
      int current = (m_tapIndex - i + length) % length;
      int previous = (current - 1 + length) % length;
      if (m_tapLast[previous] > 0 && m_tapLast[current] > 0 &&
          m_tapLast[current] - m_tapLast[previous] < 30000)
      {
        period += (m_tapLast[current] - m_tapLast[previous]);
        n++;
      }
    }
    m_tapIndex = (m_tapIndex + 1) % length;

    if (n > 0)
    {
      int bpm = (int)((double)60*1000 / ((double)(period)/n));
      if (bpm > BPM_MIN && bpm <= BPM_MAX)
      {
        sendMessage(HANDLER_MSG_TEMPO, bpm, 0);
      }
    }
  }

  public int[] getBeatsConfig() { return m_beatsConfig; }
  public void setBeatsConfig(int[] beatsConfig)
  {
    m_beatsConfig = beatsConfig;
  }

  public int getDivision()
  {
    return m_beatsConfig.length;
  }
  public int getSubDivision()
  {
    // Search the maximum interval with subdivision.
    int cnt = 0;
    int subDiv = 0;
    boolean hassubdiv = false;
    for (int i = 0; i < m_beatsConfig.length; i++)
    {
      hassubdiv |= (m_beatsConfig[i] == BEATCONFIG_SUBDIV);
      if (m_beatsConfig[i] == BEATCONFIG_SUBDIV ||
          m_beatsConfig[i] == BEATCONFIG_OFF)
      {
        cnt++;
        if (cnt > subDiv) subDiv = cnt;
      }
      else
      {
        cnt = 0;
      }
    }

    subDiv += 1;

    if (m_beatsConfig.length <= 1) subDiv = 1;
    else if (subDiv == 1) subDiv = 4;
    else if (subDiv == 2 && hassubdiv) subDiv = 8;
    else if (subDiv == 2) subDiv = 2;
    else if (subDiv == 3) subDiv = 8;
    else if (subDiv == 4) subDiv = 16;
    else subDiv = 16;

    return subDiv;
  }

  public boolean getIsPlaying()
  {
    return m_timer != null;
  }
  public int getCurrentTick() { return m_currentTick; }

  public void stop()
  {
    // Do not acquire mutex, this may be call from onDestroy with the same thread,
    // provoking auto mutual exclusion
    if (m_timer != null)
    {
      m_timer.cancel();
      m_timer.purge();
      m_timer = null;
      sendMessage(HANDLER_MSG_TICK, -1, -1);
      sendMessage(HANDLER_MSG_PLAY, 0, 0);
      if (m_audioTrack != null &&
          m_audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
      {
        m_audioTrack.stop();
      }
    }
  }

  public void play()
  {
    try
    {
      if (!mutexTryAcquire(m_mutexTick, -1)) return;
      if (m_regenerateTone)
      {
        setupTone();
        m_regenerateTone = false;
      }
      if (m_audioTrack != null)
      {
        m_audioTrack.play();
      }
      m_currentTick = -1;
    }
    finally
    {
      m_mutexTick.release();
    }
    scheduleTick(false);
    sendMessage(HANDLER_MSG_PLAY, 1, 0);
  }

  private void scheduleTick(boolean delayed)
  {
    if (m_isReschedule)
    {
      return;
    }
    try
    {
      long periodMs = getPeriodMs();
      if (!mutexTryAcquire(m_mutexTick, periodMs)) return;
      m_isReschedule = true;

      if (m_timer != null)
      {
        m_timer.cancel();
        m_timer.purge();
      }

      if (periodMs > AUDIO_DURATION_MS)
      {
        // scheduleAtFixedRate throw an exception after cancelled
        m_timer = new Timer();
        m_timer.scheduleAtFixedRate(new TimerTask()
        {
          @Override
          public void run() { tick(); }
        }, (delayed ? DELAY_MS_ON_BPM_CHANGE : 0), periodMs);
      }
      else
      {
        m_timer = null;
      }
    }
    finally
    {
      m_isReschedule = false;
      m_mutexTick.release();
    }
  }

  // Under the tick mutex
  private void writeAudioTrack(byte[] wave, int len)
  {
    int r = 0;
    int offset = 0;
    if (m_audioTrack == null) return;

    while (len > 0 && r >= 0)
    {
      r = m_audioTrack.write(wave, offset, Math.min(len, wave.length - offset));
      if (r >= 0)
      {
        offset = (offset + r) % wave.length;
        len -= r;
      }
      else
      {
        Log.w (SettingsCore.getInstance().getClass().getName(), "Can not write audio track error: " + r);
      }
      if (r == 0)
      {
        try { Thread.sleep(10); }
        catch (java.lang.InterruptedException ignored) {}
      }
    }
  }

  private void tick()
  {
    try
    {
      if (!mutexTryAcquire(m_mutexTick, getPeriodMs())) return;

      // Setup next
      m_currentTick++;
      if (m_currentTick >= m_beatsConfig.length) m_currentTick = 0;

      if (m_currentTick > m_beatsConfig.length) m_currentTick = m_currentTick % m_beatsConfig.length;

      // Notify the UI
      sendMessage(HANDLER_MSG_TICK, m_currentTick, m_beatsConfig[m_currentTick]);

      if (m_audioTrack != null)
      {
        // Flush needs audio to be stopped
        m_audioTrack.pause();
        m_audioTrack.flush();
        m_audioTrack.play();
      }

      // Play Sound
      switch (m_beatsConfig[m_currentTick])
      {
        case MetronomeCore.BEATCONFIG_ACCENT:
          if (m_waveAccent != null)
          {
            writeAudioTrack(m_waveAccent, m_waveAccent.length);
          }
          break;
        case MetronomeCore.BEATCONFIG_NORMAL:
          if (m_waveMiddle != null)
          {
            writeAudioTrack(m_waveMiddle, m_waveMiddle.length);
          }
          break;
        case MetronomeCore.BEATCONFIG_SUBDIV:
          if (m_waveSubdiv != null)
          {
            writeAudioTrack(m_waveSubdiv, m_waveSubdiv.length);
          }
          break;
        case MetronomeCore.BEATCONFIG_OFF:
          if (m_waveSilence != null)
          {
            writeAudioTrack(m_waveSilence, m_waveSilence.length);
          }
          break;
        default:
          // Do nothing
          break;
      }

      // Write a silence until the next tick
      if (m_waveSilence != null)
      {
        writeAudioTrack(m_waveSilence, (getPeriodMs() - AUDIO_DURATION_MS) * AUDIO_BIT_RATE / 1000);
      }
    }
    finally
    {
      m_mutexTick.release();
    }
  }
}
