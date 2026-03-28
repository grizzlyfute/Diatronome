package org.kalinisa.diatronome.Cores;

import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import org.kalinisa.diatronome.Cores.SoundGenerator.ASoundGenerator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

  public static final int BPM_MIN = 1;
  public static final int BPM_MAX = 320;
  public static final int DELAY_MS_ON_BPM_CHANGE = 200;
  private static final int FADEINOUT_MS = 3;
  // Should be less than min period, ie 60 * 1000 / MAX_BPM
  // Min audio duration
  private static final int AUDIO_DURATION_MS = 100;
  // According to https://developer.android.com/ndk/guides/audio/audio-latency?hl=en#validate,
  // audio latency in less than 20 ms for device having "pro" feature and less than 45 for device having "low latency".
  // In practical case, Samsung S10E have pro feature and start latency measured is 14 ms. With 20 ms for low latency,
  // recording at 320 bpm show a difference of 15 ms between two tick length. This not appears with 45 ms latency, so device lies.
  // This constant should not be greater than 1000 * 60 / BPM_MAX - AUDIO_DURATION_MS = (87 ms)
  // (unless audio track may throw exception when tick flush because it continues writing).
  public final static int AUDIO_LATENCY_MS =
    Math.min (
      45 + 20, // Theoretical + some margin
    1000 * 60 / BPM_MAX - AUDIO_DURATION_MS);

  // Settings
  int m_pitchMainSkb = 0;
  int m_pitchAccentSkb = 0;
  int m_pitchSubdivisionSkb = 0;
  float m_refPitch = 0;
  ASoundGenerator m_soundGeneratorAccent = null;
  ASoundGenerator m_soundGeneratorMain = null;
  ASoundGenerator m_soundGeneratorSubdivision = null;
  int m_tempoBpm = 0;
  int[] m_beatsConfig = new int[0];
  boolean m_isBidirectionalNeedle = false;

  // Internal Working
  private boolean m_regenerateToneAccent = false;
  private boolean m_regenerateToneMain = false;
  private boolean m_regenerateToneSubdivision = false;
  private final AtomicInteger m_currentTick = new AtomicInteger(0);
  private int m_forceTickOnBpmChange = -1;
  private final long[] m_tapLast;
  private int m_tapIndex = 0;
  private AccuracyTimer m_timer = null;
  AudioTrack m_audioTrack = null;
  private byte[] m_waveAccent;
  private byte[] m_waveSubdiv;
  private byte[] m_waveMiddle;
  private byte[] m_waveSilence;
  private final Semaphore m_mutexTick;
  private boolean m_isReschedule = false;
  private boolean m_forceInterrupt = false;

  private static MetronomeCore s_instance;
  private MetronomeCore()
  {
    super();

    m_beatsConfig = new int[0];
    m_tapLast = new long[5];
    m_mutexTick = new Semaphore(1, false);

    // In static mode, when audio is stopped (or play silent), hardware set 0 during 7 ms and release the line.
    // On release signal is to 0.5 [rang: -1: 1], probably due changing output gain which creating an op-amp DC offset.
    // This generate a low bass beat (clip). After, a capacitor is gently discharging and signal value come back to zero
    // We can not avoid the signal raise, unless doing loop and write some constant not 0. But waste CPU time.
    // In streaming mode, we have to ensure that buffer never under run, but it is not creating clip.
    // Keep min buffer size to be reactive. (nb : start playing time depend of this buffer size. lower is better)
    m_audioTrack = AudioUtils.newAudioTrack(0, AudioTrack.MODE_STREAM);

    m_audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener()
    {
      @Override
      public void onMarkerReached(AudioTrack track)
      {
        // m_currentTick is updated in another thread, witch holding for a long time the mutex.
        final int currentTick = m_currentTick.get();
        // Notify UI.
        if (currentTick >= 0 && currentTick < m_beatsConfig.length)
        {
          sendMessage(HANDLER_MSG_TICK, currentTick, m_beatsConfig[currentTick]);
        }
      }

      @Override
      public void onPeriodicNotification(AudioTrack track)
      {
        // Not used
      }
    });
  }

  public static MetronomeCore getInstance()
  {
    if (s_instance == null)
    {
      s_instance = new MetronomeCore();
    }
    return s_instance;
  }

  public void setRefPitch(float refPitch)
  {
    m_refPitch = refPitch;
    // Ensure ref pitch not diverge if playing too strong with ref
    if (m_refPitch < 415 - 3 || m_refPitch > 466 + 3) m_refPitch = 440;
    m_regenerateToneAccent = true;
    m_regenerateToneMain = true;
    m_regenerateToneSubdivision = true;
  }

  public void setPitchAccentSkb(int pitchAccentSKb)
  {
    // keep deferred to be sure to have the ref pitch for computation
    m_pitchAccentSkb = pitchAccentSKb;
    m_regenerateToneAccent = true;
  }

  public void setPitchMainSkb(int pitchMainSkb)
  {
    // keep deferred to be sure to have the ref pitch for computation
    m_pitchMainSkb = pitchMainSkb;
    m_regenerateToneMain = true;
  }

  public void setPitchSubdivisionSkb(int pitchSubdivisionSkb)
  {
    // keep deferred to be sure to have the ref pitch for computation
    m_pitchSubdivisionSkb = pitchSubdivisionSkb;
    m_regenerateToneSubdivision = true;
  }


  public void setWaveFormAccent(String waveformStr)
  {
    m_soundGeneratorAccent = ASoundGenerator.factory(waveformStr);
    m_regenerateToneAccent = true;
  }

  public void setWaveFormMain(String waveformStr)
  {
    m_soundGeneratorMain = ASoundGenerator.factory(waveformStr);
    m_regenerateToneMain = true;
  }

  public void setWaveFormSubdivision(String waveformStr)
  {
    m_soundGeneratorSubdivision = ASoundGenerator.factory(waveformStr);
    m_regenerateToneSubdivision = true;
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
    else if (skb == 0) ret = 1 * m_refPitch / 4; // 0.25
    else if (skb == 1) ret = 2 * m_refPitch / 4; // 0.5
    else if (skb == 2) ret = 3 * m_refPitch / 4; // 0.75
    else if (skb == 3) ret = 4 * m_refPitch / 4; // 1
    else if (skb == 4) ret = 3 * m_refPitch / 2; // 1.5
    else if (skb == 5) ret = 4 * m_refPitch / 2; // 2
    else if (skb == 6) ret = 6 * m_refPitch / 2; // 3
    else ret = m_refPitch;
    return ret;
  }

  private byte[] generatePcmWithFadeInOut(ASoundGenerator generator, float pitch)
  {
    short[] pcm = null;
    if (pitch > 0)
    {
      double durationMs = AUDIO_DURATION_MS;
      if (!generator.isContinuous())
      {
        durationMs = Math.min (getPeriodMs() - 2*AUDIO_LATENCY_MS, (int)generator.hintDurationMs(pitch));
        durationMs = Math.max (AUDIO_DURATION_MS, durationMs);
      }

      // Ensure tone duration correspond of the period duration. This avoid "pop" by half period
      int nbPeriods = (int)Math.floor(durationMs * pitch / 1000);
      durationMs = 1000 * nbPeriods / pitch;

      pcm = generator.generatePcm(pitch, durationMs);
      // Avoid clips generated by amplification
      AudioUtils.fadeInFilter(pcm, FADEINOUT_MS);
      AudioUtils.fadeOutFilter(pcm, FADEINOUT_MS);
      return AudioUtils.toAudioBytes(pcm);
    }
    else
    {
      return new byte[AudioUtils.getAudioByteLen(AUDIO_DURATION_MS)];
    }
  }

  // Work under tick mutex
  private void setupTone()
  {
    float pitch = 0.0f;
    short[] pcm = null;
    // Wait for input before generation
    if (m_refPitch <= 0) return;
    // All the parameter are know to generate sound track
    if (m_waveSilence == null)
    {
      m_waveSilence = generatePcmWithFadeInOut(null,-1);
    }
    if (m_regenerateToneAccent && m_soundGeneratorAccent != null)
    {
      // Increase the duration to have an integer number of period
      pitch = seekBarToPitch(m_pitchAccentSkb);
      if (pitch > 0)
      {
        m_waveAccent = generatePcmWithFadeInOut(m_soundGeneratorAccent, pitch);
      }
      else
      {
        m_waveAccent = m_waveSilence;
      }
      m_regenerateToneAccent = false;
    }
    if (m_regenerateToneMain && m_soundGeneratorMain != null)
    {
      pitch = seekBarToPitch(m_pitchMainSkb);
      if (pitch > 0)
      {
        m_waveMiddle = generatePcmWithFadeInOut(m_soundGeneratorMain, pitch);
      }
      else
      {
        m_waveMiddle = m_waveSilence;
      }
      m_regenerateToneMain = false;
    }
    if (m_regenerateToneSubdivision && m_soundGeneratorSubdivision != null)
    {
      pitch = seekBarToPitch(m_pitchSubdivisionSkb);
      if (pitch > 0)
      {
        m_waveSubdiv = generatePcmWithFadeInOut(m_soundGeneratorSubdivision, pitch);
      }
      else
      {
        m_waveSubdiv = m_waveSilence;
      }
      m_regenerateToneSubdivision = false;
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
    int currentTick = m_currentTick.get();
    if (getIsPlaying() && currentTick >= 0)
    {
      sendMessage(HANDLER_MSG_TICK, currentTick, m_beatsConfig[currentTick]);
      // Rescheduled according to the new period.
      // Do not replay the same tick nor it will break animation
      scheduleTick(DELAY_MS_ON_BPM_CHANGE);
    }

    // Recompute tone according new tempo
    if (!m_soundGeneratorAccent.isContinuous())
      m_regenerateToneAccent = true;
    if (!m_soundGeneratorMain.isContinuous())
      m_regenerateToneMain = true;
    if (!m_soundGeneratorSubdivision.isContinuous())
      m_regenerateToneSubdivision = true;
    setupTone();
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
    boolean hasSubDiv = false;
    for (int i = 0; i < m_beatsConfig.length; i++)
    {
      hasSubDiv |= (m_beatsConfig[i] == BEATCONFIG_SUBDIV);
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
    else if (subDiv == 2 && hasSubDiv) subDiv = 8;
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
  public int getCurrentTick() { return m_currentTick.get(); }

  public void stop()
  {
    // Do not acquire mutex, this may be call from onDestroy with the same thread,
    // provoking auto mutual exclusion
    if (m_timer != null)
    {
      try
      {
        Utils.mutexTryAcquire(m_mutexTick, 300);
        // Do not matter how the mutex is taken or not. We force the stop
        // Cancel will brutally stop the timer
        if (m_audioTrack != null &&
          m_audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
        {
          m_audioTrack.stop();
        }
        m_timer.cancel();
        m_timer.purge();
        m_timer = null;
        sendMessage(HANDLER_MSG_TICK, -1, -1);
        sendMessage(HANDLER_MSG_PLAY, 0, 0);
        m_currentTick.set(-1);
        m_forceTickOnBpmChange = -1;
      }
      // If we cancel the timer during job
      finally
      {
        m_mutexTick.release();
      }
    }
  }

  public void play()
  {
    try
    {
      if (!Utils.mutexTryAcquire(m_mutexTick, 100)) return;
      if (m_regenerateToneAccent || m_regenerateToneMain || m_regenerateToneSubdivision)
      {
        setupTone();
      }
      if (m_timer == null)
      {
        m_timer = new AccuracyTimer();
      }
      if (m_audioTrack != null)
      {
        m_audioTrack.play();
        // Avoid under run when start playing
        writeAudioTrack(m_waveSilence, AudioUtils.getAudioByteLen(AUDIO_LATENCY_MS), AUDIO_LATENCY_MS);
      }
      m_currentTick.set(-1);
    }
    catch (InterruptedException ignored)
    {
      // Do nothing
    }
    finally
    {
      m_mutexTick.release();
    }
    scheduleTick(0);
    sendMessage(HANDLER_MSG_PLAY, 1, 0);
  }

  private void scheduleTick(int delayMs)
  {
    if (m_isReschedule)
    {
      return;
    }
    long periodMs = getPeriodMs();
    if (!Utils.mutexTryAcquire(m_mutexTick, periodMs)) return;
    try
    {
      m_isReschedule = true;
      m_forceTickOnBpmChange = m_currentTick.get();

      if (periodMs > AUDIO_DURATION_MS)
      {
        m_timer.scheduleAtFixedRate(new AccuracyTimer.AccuracyTimerTask()
        {
          @Override
          public void run() { tick(); }
          @Override
          public synchronized void interrupt() { m_forceInterrupt = true; }
        }, delayMs, periodMs);
      }
      else
      {
        if (m_timer != null)
        {
          m_timer.cancel();
          m_timer.purge();
          m_timer = null;
        }
      }
    }
    finally
    {
      m_isReschedule = false;
      m_mutexTick.release();
    }
  }

  // Thread.interrupt is ignored when doing m_audioTrack.write
  private void checkForceInterrupt()
    throws java.lang.InterruptedException
  {
    if (m_forceInterrupt || Thread.interrupted())
    {
      m_forceInterrupt = false;
      throw new InterruptedException("Thread interrupted");
    }
  }

  // Under the tick mutex. Caution: this is a blocking function
  // Timeout guarantee the audio not taking late. If audio is late, the UI will lag
  private void writeAudioTrack(byte[] wave, int len, long timeoutMs)
    throws java.lang.InterruptedException
  {
    int r = 0;
    int offset = 0;
    long tsEnd = 0;
    long tsStart = System.currentTimeMillis();
    if (m_audioTrack == null || wave == null) return;
    int wavelen = wave.length;

    // Ensure multiple of frame size, else write return 0
    final int frameSize = AudioUtils.getAudioFrameSize();
    len = (len / frameSize) * frameSize;
    wavelen = (wavelen / frameSize) * frameSize;

    while (len > 0 && r >= 0 && timeoutMs > AUDIO_LATENCY_MS)
    {
      checkForceInterrupt();
      // Remaining size before loop
      r = Math.min (wavelen - offset, len);
      // To manage timeout properly, write by block
      r = Math.min (r, 4096);
      if (r <= frameSize) break;
      r = m_audioTrack.write(wave, offset, r);
      if (r >= 0)
      {
        offset = (offset + r) % wavelen;
        len -= r;
      }
      else
      {
        Log.w (SettingsCore.getInstance().getClass().getName(), "Can not write audio track error: " + r);
      }
      tsEnd = System.currentTimeMillis();
      timeoutMs -= (tsEnd - tsStart);
      tsStart = tsEnd;

      // Buffer full or non blocking write
      if (r == 0)
      {
        Thread.sleep(Math.min (10, timeoutMs));
      }
    }
  }

  private void tick()
  {
    byte[] waveCurrent = null;
    final long stopWatch = System.currentTimeMillis();
    long elapsedTime = 0;
    int remainingBytes = 0;
    int currentTick = 0;
    double durationMs = 0;
    final int periodMs = getPeriodMs();
    if (m_beatsConfig.length <= 0) return;
    if (!Utils.mutexTryAcquire(m_mutexTick, periodMs)) return;
    try
    {
      // Setup next
      if (m_forceTickOnBpmChange > 0)
      {
        currentTick = m_forceTickOnBpmChange;
        m_forceTickOnBpmChange = -1;
      }
      else
      {
        currentTick = m_currentTick.get();
        currentTick++;
      }
      if (currentTick >= m_beatsConfig.length) currentTick = 0;
      m_currentTick.set(currentTick);

      // Play Sound
      switch (m_beatsConfig[currentTick])
      {
        case MetronomeCore.BEATCONFIG_ACCENT:
          waveCurrent = m_waveAccent;
          break;
        case MetronomeCore.BEATCONFIG_NORMAL:
          waveCurrent = m_waveMiddle;
          break;
        case MetronomeCore.BEATCONFIG_SUBDIV:
          waveCurrent = m_waveSubdiv;
          break;
        case MetronomeCore.BEATCONFIG_OFF:
          waveCurrent = m_waveSilence;
          break;
        default:
          waveCurrent = m_waveSilence;
          break;
      }

      if (m_audioTrack == null)
      {
        sendMessage(HANDLER_MSG_TICK, currentTick, m_beatsConfig[currentTick]);
        m_mutexTick.release();
        return;
      }
      else if (getIsPlaying())
      {
        // Flush needs audio to be stopped
        m_audioTrack.pause();
        m_audioTrack.flush();
        m_audioTrack.play();
        m_audioTrack.setNotificationMarkerPosition(0);
      }

      // Warm-up audio by writing silent. The audio may start from 0 to 45 ms after write, or never start if buffer have not enough data
      // In first part, measure warm-up time. When,writing the second part, we adjust frame to have exactly a latency time before the clip
      writeAudioTrack(m_waveSilence, AudioUtils.getAudioByteLen(AUDIO_LATENCY_MS / 2), AUDIO_LATENCY_MS);
      elapsedTime = System.currentTimeMillis() - stopWatch;

      // Wait for audio starting
      while (m_audioTrack.getPlaybackHeadPosition() <= 0 && elapsedTime < AUDIO_LATENCY_MS)
      {
        checkForceInterrupt();
        Thread.sleep (1);
        elapsedTime = System.currentTimeMillis() - stopWatch;
      }

      // Compute the remaining bytes to write to have constant warm-up time.
      remainingBytes = AudioUtils.getAudioByteLen(
        // Total time
        AUDIO_LATENCY_MS -
        // Elapsed time
        (int)elapsedTime -
        // Remaining time to play for warmup byte
          (AUDIO_LATENCY_MS/2 -
          m_audioTrack.getPlaybackHeadPosition()*AudioUtils.getAudioFrameSize()/AudioUtils.getAudioByteLen(1))
      );
      // Rewrite silence frame to have exactly the remaining of audio latency duration
      if (remainingBytes > 0)
      {
        writeAudioTrack(m_waveSilence, remainingBytes, AUDIO_LATENCY_MS);

        // Notify UI when tick begin
        m_audioTrack.setNotificationMarkerPosition((AudioUtils.getAudioByteLen(AUDIO_LATENCY_MS/2) + remainingBytes)/AudioUtils.getAudioFrameSize());
      }
      else
      {
        // Immediate UI notification is safer
        sendMessage(HANDLER_MSG_TICK, currentTick, m_beatsConfig[currentTick]);
      }

      // Write the tick track
      durationMs = Math.min (periodMs - AUDIO_LATENCY_MS, AudioUtils.getDurationMs(waveCurrent.length));
      writeAudioTrack(waveCurrent, Math.min (waveCurrent.length, AudioUtils.getAudioByteLen(durationMs)), (long)durationMs);

      // Write silence until the next tick.
      durationMs = periodMs - durationMs - AUDIO_LATENCY_MS;
      remainingBytes = AudioUtils.getAudioByteLen(durationMs);
      writeAudioTrack(m_waveSilence, remainingBytes, (long)durationMs);
      // Should exit before audio buffer reach the end
    }
    catch (InterruptedException e)
    {
      // Do nothing
    }
    finally
    {
      m_mutexTick.release();
    }
  }
}
