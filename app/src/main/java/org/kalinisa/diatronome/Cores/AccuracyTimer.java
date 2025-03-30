package org.kalinisa.diatronome.Cores;

/* This class provided a more precise timer than java.util.Timer.
 * The java.util.Timer is single thread. A single test using
 * If a task take too munch time, other are shifted.
 * This single test show a min of 14 and a max of 29 ms for java.util.Timer,
 * and -19.5, 20.5 for this object
 * public static void main(String[] args)
  {
    //java.util.Timer timer = new java.util.Timer();
    AccuracyTimer timer = new AccuracyTimer();

    TimerTask task1 = new TimerTask() { public void run()
    {
      long timeNs = System.nanoTime();
      System.out.print("" + (timeNs - m_lastTimeNs) / 1000000.0);
      System.out.println("");
      m_lastTimeNs = timeNs;
    }};
    TimerTask noise = new TimerTask() { public void run()
    {
      try { Thread.sleep (10); }
      catch ( InterruptedException e) {}
    }};

    timer.scheduleAtFixedRate(task1, 0, 20);
    timer.scheduleAtFixedRate(noise, 0, 1);
  }
 */

public class AccuracyTimer extends Thread
{
  interface AccuracyTimerTask extends Runnable
  {
    void interrupt();
  }

  private final long NB_MS_IN_NS = 1000000L;
  private AccuracyTimerTask m_task = null;
  private long m_waitTime = 0;
  private long m_periodTime = 0;
  private boolean m_reloadTimer = false;

  public AccuracyTimer()
  { }

  private boolean sleepNs(long ns)
  {
    boolean ret = true;
    // % modulus not working with long.
    long ms = ns / NB_MS_IN_NS;
    int ns_int = (int)(ns - ms * NB_MS_IN_NS);

    if (Thread.interrupted()) return false;
    try
    {
      Thread.sleep (ms, ns_int);
    }
    catch (InterruptedException e)
    {
      ret = false;
    }

    return ret;
  }

  @Override
  public void run()
  {
    boolean runCondition = true;
    long startTime = 0, endTime = 0, waitTime = 0, periodTime = 0;
    Runnable task = null;
    startTime = System.nanoTime();
    while (runCondition)
    {
      synchronized (this)
      {
        if (m_reloadTimer)
        {
          waitTime = m_waitTime;
          periodTime = m_periodTime;
          task = m_task;
          m_reloadTimer = false;
        }
      }

      // Run
      if (waitTime <= 0 && task != null)
      {
        task.run();
        waitTime += periodTime;
      }
      else
      {
        runCondition = sleepNs(waitTime);
      }
      runCondition &= !interrupted();

      // Interrupted for reload
      if (!runCondition && m_reloadTimer)
      {
        runCondition = true;
      }

      // Compute new date
      endTime = System.nanoTime();
      waitTime -= endTime - startTime;
      startTime = endTime;
    }
  }

  // Contrary to java.utils.Timer, this allow reschedule without exception
  public void scheduleAtFixedRate(AccuracyTimerTask task, long delayMs, long periodMs)
  {
    AccuracyTimerTask oldTask = null;
    synchronized (this)
    {
      oldTask = m_task;
      m_task = task;
      m_waitTime = delayMs * NB_MS_IN_NS;
      m_periodTime = periodMs * NB_MS_IN_NS;
      m_reloadTimer = true;
    }
    try
    {
      if (isAlive()) interrupt();
      // else NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING or TERMINATED
      else if (Thread.currentThread().getState() == State.NEW ||
        Thread.currentThread().getState() == State.RUNNABLE) start();
    }
    catch (IllegalThreadStateException e)
    {
      android.util.Log.w (getClass().getName(), "Illegal thread state " + Thread.currentThread().getState());
    }
    if (oldTask != null) oldTask.interrupt();
  }

  public synchronized void cancel()
  {
    m_reloadTimer = false;
    interrupt();
    if (m_task != null)
    {
      m_task.interrupt();
    }
    try
    {
      join(1000);
      if (isAlive())
      {
        android.util.Log.w (getClass().getName(), "Timer thread still alive");
        interrupt();
      }
    }
    catch (InterruptedException e)
    { }
  }

  public void purge()
  {
    // Compatibility with java.util.Timer
  }
}
