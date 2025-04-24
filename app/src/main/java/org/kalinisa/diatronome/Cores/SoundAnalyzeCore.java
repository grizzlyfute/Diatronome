package org.kalinisa.diatronome.Cores;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import org.kalinisa.diatronome.Cores.SoundAnalyzer.ISoundAnalyzer;
import org.kalinisa.diatronome.Cores.SoundAnalyzer.SoundAnalyzerEnvelop;
import org.kalinisa.diatronome.Cores.SoundAnalyzer.SoundAnalyzerFourier;
import org.kalinisa.diatronome.Cores.SoundAnalyzer.SoundAnalyzerPeakDetector;
import org.kalinisa.diatronome.Cores.SoundAnalyzer.SoundAnalyzerYin;

public class SoundAnalyzeCore
  extends BaseCore
  implements Runnable
{
  // ---------------------------------------------------------------------------
  // Types
  // ---------------------------------------------------------------------------
  public static class NeedleParameters
  {
    // range: [-1, 1]. -1 = -50c, +1 = +50c
    public double accuracy;
    // range: [0 - 20000]
    public double frequency;
    // 0 = C, 1 = C#/Db, ...
    public int note;
    // range: [0 - 10]
    public int octave;
    // range: [0, 1]
    public double intensity;
  }

  // ---------------------------------------------------------------------------
  // Constants
  // ---------------------------------------------------------------------------
  public static final int HANDLER_MSG_UPDATE_NEEDLE = 1;
  // 44100,22050,16000,8000;
  public static final int AUDIO_SAMPLE_RATE = 44100;
  private static final int NOTE_PERSISTENCE_MS = 3000;
  private static final int MIN_LOOP_ROUND_MS = 10;

  // ---------------------------------------------------------------------------
  // Members
  // ---------------------------------------------------------------------------
  private static SoundAnalyzeCore s_instance;
  private Thread m_thread = null;
  private double m_thresholdIntensity = 0.01;
  private int m_algoToUse = 1;
  private boolean m_isSettingsChanged;
  private AudioRecord m_recorder = null;

  // ---------------------------------------------------------------------------
  // Methods
  // ---------------------------------------------------------------------------

  private SoundAnalyzeCore()
  { }

  public static SoundAnalyzeCore getInstance()
  {
    if (s_instance == null)
    {
      s_instance = new SoundAnalyzeCore();
    }
    return s_instance;
  }

  private interface iChangeSettingDouble { void change(double value); }
  private void changeSettingDouble(iChangeSettingDouble fn, double value)
  {
    // or use java.util.concurrent.locks.Lock as mutex
    synchronized (this)
    {
      fn.change(value);
      m_isSettingsChanged = true;
    }
  }

  public void setThreshold(int value)
  {
    // Compute intensity from int range [0-10]
    // 0: 0.75, 5: 0.01, 10 = 0, model, f(value) = k*(exp((10-value)*u) - 1)
    // k ~= 0.000136986; u ~= 0.860813
    changeSettingDouble(x -> m_thresholdIntensity = x, 0.000136986 * (Math.exp((10 - value) * 0.860813) - 1));
  }

  public void setAlgo(int algo)
  {
    changeSettingDouble(x -> m_algoToUse = (int)x, algo);
  }

  public void startFromUi()
  {
    if (m_thread != null)
    {
      stopFromUi();
    }

    try
    {
      final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
      final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

      if (m_recorder == null && AudioUtils.IsAudioWorking())
      {
        m_recorder = new AudioRecord
        (
          MediaRecorder.AudioSource.MIC,
          AUDIO_SAMPLE_RATE,
          CHANNEL_IN,
          ENCODING,
          // audioBufferSize % frameSizeInBytes (=channel*encoding) == 0
          // 4096 =  buffer size for c0 Acquisition -16.31 Hz, power of 2)
          Math.max(4096, AudioUtils.AudioRecord_getMinBufferSize(AUDIO_SAMPLE_RATE, CHANNEL_IN, ENCODING))
        );
      }

      // new Thread (new Runnable { public void run() { ... } }).start();
      m_thread = new Thread(this);
      if (m_recorder != null)
      {
        m_thread.start();
      }
    }
    catch (SecurityException e)
    {
      android.util.Log.e (this.getClass().getName(), "Missing record permission", e);
    }
  }

  public void stopFromUi()
  {
    if (m_thread != null)
    {
      m_thread.interrupt();
      // s_thread.join (1000);
      m_thread = null;
    }
    if (m_recorder != null)
    {
      m_recorder.stop();
      m_recorder.release();
      m_recorder = null;
    }
  }

  private void doMessage(double frequency, double intensity)
  {
    if (getHandler() != null)
    {
      double cents;
      NeedleParameters parameters = new NeedleParameters();
      Utils.Holder<Integer> holderOctave = new Utils.Holder<Integer> (0);
      Utils.Holder<Integer> holderNote = new Utils.Holder<Integer> (0);

      if (frequency > 0 && intensity > 0)
      {
        PlayNoteCore.getInstance().getNearNoteFromFreq(frequency, holderOctave, holderNote);
        double theoreticalFreq = PlayNoteCore.getInstance().getFrequency(holderOctave.value, holderNote.value);
        cents = PlayNoteCore.getInstance().diffCents(frequency, theoreticalFreq);
      }
      else
      {
        cents = 0;
        frequency = 0;
        intensity = 0;
        holderOctave.value = -1;
        holderNote.value = -1;
      }

      // range: [-1, 1]
      parameters.accuracy = cents / 50;
      // range: [0 - 20000]
      parameters.frequency = frequency;
      // range: [0, 10]
      parameters.octave = holderOctave.value;
      // 0 = C, 1 = C#/Dbb, ...
      parameters.note = holderNote.value;
      // range: [0, 1]
      parameters.intensity = intensity;

      sendMessage(HANDLER_MSG_UPDATE_NEEDLE, parameters);
    }
  }

  private int prepareDataToDouble(final short[] readBuffer,
                                  int offset, int lenRead,
                                  double[] signal)
  {
    double x;
    while (lenRead > 0)
    {
      x = 2.0*((double)readBuffer[offset] - Short.MIN_VALUE) / (double)(Short.MAX_VALUE - Short.MIN_VALUE) - 1.0;
      signal[offset] = x;
      offset++;
      --lenRead;
    }
    return offset;

  }

  private double rootMeanSquare(final double[] data)
  {
    double rms = 0;
    for (double x : data)
    {
      rms += x*x;
    }
    return Math.sqrt(rms/data.length);
  }

  private boolean isAllFrequenciesNear(final double[] frequenciesBuffer, double frequency)
  {
    boolean isFreqNear = true;
    for (int i = 0; i < frequenciesBuffer.length; i++)
    {
      if (Math.abs(PlayNoteCore.getInstance().diffCents(frequenciesBuffer[i], frequency)) >= 100)
      {
        isFreqNear = false;
        break;
      }
    }
    return isFreqNear;
  }

  public void run()
  {
    int lenRead = 0;
    int lenByteToRead = 0;
    int offsetBuffer = 0;
    double thresholdIntensity = 0;
    long tsLastLoop = 0;
    long lastMessageTs = 0;
    double[] freqSmooth = new double[5];
    double[] intensitySmooth = new double[freqSmooth.length];
    int smoothInd = 0;
    double smoothDelta = 0;
    double frequency = 0;
    double intensity = 0;
    ISoundAnalyzer soundAnalyzer = null;

    // Buffer len have to be a power of 2 for fourier fast transform
    // The lowest know frequency is about 16.3125 Hz (C0). The period is 0.061 second
    // Buffer size should be 2^ceil(log2(sampleRate * minPeriod)).
    // For 16.3125 Hz, the buffer is 4096, needing 0.093 s to be acquired
    lenByteToRead = (int)Math.pow (2,
      (int)Math.ceil(Math.log(AUDIO_SAMPLE_RATE * (1.0/16.3125)) / Math.log(2)));
    short[] readBuffer = new short[lenByteToRead];
    double[] signal = new double[lenByteToRead];

    // Force initial read settings
    synchronized (this) { m_isSettingsChanged = true; }
    // Go
    if (m_recorder != null)
    {
      try
      {
        m_recorder.startRecording();
      }
      catch (java.lang.IllegalStateException e)
      {
        m_recorder = null;
        return;
      }
    }

    doMessage (0, 0);

    while (!Thread.interrupted())
    {
      // Wait for data | display
      if (lenByteToRead > 0)
      {
        long diff = System.currentTimeMillis() - tsLastLoop;
        tsLastLoop += diff;
        diff = (long)Math.max((1000 * lenByteToRead / AUDIO_SAMPLE_RATE) - diff, MIN_LOOP_ROUND_MS);
        if (diff > 0)
        {
          try
          {
            //noinspection BusyWait
            Thread.sleep(diff);
          }
          catch (InterruptedException exn)
          {
            // null: m_thread.interrupt();
            break;
          }
        }
      }

      // Update settings
      synchronized (this)
      {
        if (m_isSettingsChanged)
        {
          m_isSettingsChanged = false;
          thresholdIntensity = m_thresholdIntensity;
          switch(m_algoToUse)
          {
            default:
            case 1:
              soundAnalyzer = new SoundAnalyzerEnvelop(AUDIO_SAMPLE_RATE);
              break;
            case 2:
              soundAnalyzer = new SoundAnalyzerYin(AUDIO_SAMPLE_RATE);
              break;
            case 3:
              soundAnalyzer = new SoundAnalyzerFourier(AUDIO_SAMPLE_RATE);
              break;
            case 4:
              soundAnalyzer = new SoundAnalyzerPeakDetector(AUDIO_SAMPLE_RATE);
              break;
          }
        }
      }

      // Extract data
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
      {
        // Non blocking may allow to precompute during acquisition
        lenRead = m_recorder.read (readBuffer, offsetBuffer, lenByteToRead, AudioRecord.READ_NON_BLOCKING);
      }
      else
      {
        lenRead = m_recorder.read (readBuffer, offsetBuffer, lenByteToRead);
      }

      // Pre-computation.
      if (lenRead >= 0)
      {
        offsetBuffer = prepareDataToDouble (readBuffer, offsetBuffer, Math.min (lenByteToRead, lenRead), signal);
        
        // Simulate input, noisy signal at 442 Hz
        if (lenByteToRead >= lenRead)
        {
          lenByteToRead -= lenRead;
        }
        else
        {
          lenByteToRead = 0;
        }
      }
      else
      {
        Log.e (this.getClass().getName(), "Fail to read: " + lenRead);
      }

      // Analyze signal
      if (offsetBuffer >= readBuffer.length)
      {
        intensity = rootMeanSquare(signal);
        if (intensity > thresholdIntensity)
        {
          frequency = soundAnalyzer.getPitch(signal);

          // Store in smooth buffer
          freqSmooth[smoothInd] = frequency;
          intensitySmooth[smoothInd] = intensity;
          smoothInd++;
          smoothInd %= freqSmooth.length;

          frequency = 0;
          intensity = 0;
          smoothDelta = 0;
          lenRead = 0;
          for (int i = 0; i < freqSmooth.length; i++)
          {
            frequency += freqSmooth[i];
            intensity += intensitySmooth[i];
            smoothDelta += Math.abs (freqSmooth[i] - freqSmooth[(i + 1) % freqSmooth.length]);
          }

          // +5% is approximately one half tone
          if (smoothDelta / freqSmooth.length <= 0.05 * (frequency / freqSmooth.length))
          {
            frequency = frequency / freqSmooth.length;
            intensity = intensity / freqSmooth.length;
          }
          else
          {
            frequency = 0;
            intensity = 0;
          }
        }

        if (intensity > thresholdIntensity && frequency > 0)
        {
          doMessage (frequency, intensity);
          lastMessageTs = System.currentTimeMillis();
        }
        else
        {
          if (lastMessageTs > 0 && lastMessageTs + NOTE_PERSISTENCE_MS < System.currentTimeMillis())
          {
            doMessage (0, 0);
            lastMessageTs = 0;
          }
        }

        // Reset buffer
        offsetBuffer = 0;
        lenByteToRead = readBuffer.length;
      }
    }
  }
}
