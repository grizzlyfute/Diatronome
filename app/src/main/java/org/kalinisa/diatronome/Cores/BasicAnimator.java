package org.kalinisa.diatronome.Cores;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/* The ValueAnimator take the system wide areAnimatorsEnabled. If this value is
 * false, the value animator is not working, resulting to a needle jump.
 * The valueAnimator is nice for graphical effect, not for render a move.
 */
public class BasicAnimator
{
  private final List<AnimatorUpdateListener> m_listeners;
  private float m_begin;
  private float m_end;
  private long m_countMax;
  private long m_count;
  private long m_frameDelayMs;
  private long m_durationMs;
  private long m_lastTimerTime;
  private Timer m_timer;
  private Semaphore m_mutex;

  public static final int PERIOD_FPS_60 = 16;
  public static final int PERIOD_FPS_30 = 33;
  public static int PERIOD_FPS_25 = 40;

  public interface AnimatorUpdateListener
  {
    void onAnimationUpdate (float value);
  }

  public BasicAnimator()
  {
    m_listeners = new ArrayList<AnimatorUpdateListener>();
    m_begin = 0;
    m_end = 1;
    m_count = 0;
    m_countMax = 1;
    m_frameDelayMs = PERIOD_FPS_30;
    m_durationMs = 0;
    m_timer = null;
    m_mutex = new Semaphore(1);
  }

  public void start()
  {
    if (m_timer != null)
    {
      // Force run end
      m_count = m_countMax;
      timerTask();
      cancel();
    }

    if (m_frameDelayMs <= 0) m_frameDelayMs = 40;
    m_count = 0;
    m_countMax = m_durationMs / m_frameDelayMs;
    m_lastTimerTime = System.currentTimeMillis();
    if (m_countMax <= 0) m_countMax = 1;

    if (m_timer != null) m_timer.cancel();

    m_timer = new Timer();
    m_timer.schedule(new TimerTask()
    {
      @Override
      public void run()
      {
        timerTask();
      }
    }, 0, m_frameDelayMs);
  }

  public void cancel()
  {
    try
    {
      m_mutex.tryAcquire(m_frameDelayMs, TimeUnit.MILLISECONDS);
      if (m_timer != null)
      {
        m_timer.cancel();
        m_timer = null;
      }
    }
    catch (InterruptedException e)
    { }
    finally
    {
      m_mutex.release();
    }

  }

  private void timerTask()
  {
    try
    {
      // Use mutex instead synchronized to abort if too loaded
      try
      {
        if (!m_mutex.tryAcquire(m_frameDelayMs, TimeUnit.MILLISECONDS)) return;
      }
      catch (InterruptedException e)
      {
        return;
      }

      // For slow device, count faster
      long ts = System.currentTimeMillis();
      long diffTs = ts - m_lastTimerTime;
      if (m_count > 0 && diffTs > m_frameDelayMs && m_frameDelayMs > 0)
      {
        // round count add.
        m_count += (diffTs + m_frameDelayMs/2) / m_frameDelayMs;
      }
      else
      {
        m_count += 1;
      }
      m_lastTimerTime = ts;

      if (m_count > m_countMax)
      {
        m_count = m_countMax;
        cancel();
      }

      final float current = m_begin + (m_end - m_begin) * m_count / m_countMax;
      for (AnimatorUpdateListener animator : m_listeners)
      {
        animator.onAnimationUpdate(current);
      }
    }
    finally
    {
      m_mutex.release();
    }
  }

  public void setDuration(long durationMs)
  {
    m_durationMs = durationMs;
  }

  public void setFloatValues(float begin, float end)
  {
    m_begin = begin;
    m_end = end;
  }

  public void setFrameDelay(long delayMs)
  {
    m_frameDelayMs = delayMs;
  }

  public void addUpdateListener(AnimatorUpdateListener listener)
  {
    m_listeners.add(listener);
  }
}
