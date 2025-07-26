package org.kalinisa.diatronome.Cores;

import android.media.AudioFormat;
import android.media.AudioTrack;

import org.kalinisa.diatronome.Cores.SoundGenerator.ASoundGenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Semaphore;

public class PlayNoteCore
  extends BaseCore
{
  public static final int PLAY_MODE_STICKY = 1;
  public static final int PLAY_MODE_VOLATILE = 2;
  private final int FADE_IN_OUT_MS = 25;

  private static PlayNoteCore s_instance;
  private ASoundGenerator m_soundGenerator = null;
  private boolean m_useFlatSharp = false;
  private int m_temperament = 0;
  private double[] m_freqRatio = new double[]
    { 1.0, 1.0, 9.0/8.0, 9.0/8.0, 5.0/4.0, 4.0/3.0, 4.0/3.0, 3.0/2.0, 3.0/2.0, 5.0/3.0, 5.0/3.0, 15.0/8.0, 15.0/8.0, 2.0};
  private double m_refPitch = 440;
  private int m_pianoMode = 0;

  private final Semaphore m_currentNotesMutex;
  private Thread m_playNoteThread = null;
  private List<PlayNoteWave> m_waveCurrents = null;
  private final PlayNoteWave m_waveSilentPreventGlitch;

  public interface IAudioAutoStopPlayingListener { void onStopPlaying(PlayNoteWave.PlayNote note); }
  private IAudioAutoStopPlayingListener m_audioAutoStopPlayingListener = null;

  private PlayNoteCore()
  {
    super();
    m_currentNotesMutex = new Semaphore(1, false);

    m_waveCurrents = new ArrayList<PlayNoteWave>();
    m_soundGenerator = ASoundGenerator.factory(ASoundGenerator.WAVEFORM_SINE);

    // Prevent glitch by shutting down audio when changing note.
    m_waveSilentPreventGlitch = new PlayNoteWave(new PlayNoteWave.PlayNote(-1, -1),
      new short[Math.max (AudioUtils.getAudioSampleLen(3000), AudioUtils.getAudioBufferSize(3000)/2)],
      0, 0, FADE_IN_OUT_MS);
    // Playing silent after note ensure we have enough bytes in buffer to start to play
    m_waveSilentPreventGlitch.play();
  }

  public static PlayNoteCore getInstance()
  {
    if (s_instance == null)
    {
      s_instance = new PlayNoteCore();
    }
    return s_instance;
  }

  public void setOnAudioStopListener (IAudioAutoStopPlayingListener listener)
  {
    m_audioAutoStopPlayingListener = listener;
  }

  public void stopAllPlaying()
  {
    stopPlaying(null);
  }

  public void stopPlaying(PlayNoteWave.PlayNote playNote)
  {
    if (m_playNoteThread == null || !m_playNoteThread.isAlive()) return;
    try
    {
      Utils.mutexTryAcquire(m_currentNotesMutex, 10000);

      for (PlayNoteWave wave : m_waveCurrents)
      {
        if (playNote == null ||
            wave.getPlayNote().equals(playNote))
        {
          wave.stop();
        }
      }
    }
    finally
    {
      m_currentNotesMutex.release();
    }
  }

  public void startPlaying(PlayNoteWave.PlayNote playNote)
  {
    if (isPlaying(playNote)) return;
    final double frequency = getFrequency(playNote.getOctave(), playNote.getNote());
    final short[] pcm = m_soundGenerator.generatePcm(frequency, m_soundGenerator.hintDurationMs(frequency));
    int start, stop;
    short[] audioPcm;

    if (m_soundGenerator.isContinous())
    {
      short[] pcmStart = m_soundGenerator.generatePcm(frequency, FADE_IN_OUT_MS);
      short[] pcmEnd = m_soundGenerator.generatePcm(frequency, FADE_IN_OUT_MS);
      AudioUtils.fadeInFilter(pcmStart, FADE_IN_OUT_MS);
      AudioUtils.fadeOutFilter(pcmEnd, FADE_IN_OUT_MS);

      audioPcm = new short[(pcmStart.length + pcm.length + pcmEnd.length)];
      System.arraycopy(pcmStart, 0, audioPcm, 0, pcmStart.length);
      start = pcmStart.length;
      System.arraycopy(pcm, 0, audioPcm, start, pcm.length);
      stop = start + pcm.length;
      System.arraycopy(pcmEnd, 0, audioPcm, stop, pcmEnd.length);
    }
    else
    {
      // Auto loop and last point (silent)
      stop = pcm.length;
      start = stop;
      audioPcm = pcm;

      AudioUtils.fadeInFilter(pcm, FADE_IN_OUT_MS);
      AudioUtils.fadeOutFilter(pcm, FADE_IN_OUT_MS);
    }

    PlayNoteWave playNoteWave = new PlayNoteWave(playNote, audioPcm, start, stop, FADE_IN_OUT_MS);
    playNoteWave.play();

    if (!Utils.mutexTryAcquire(m_currentNotesMutex, 1000)) return;
    try
    {
      m_waveSilentPreventGlitch.reset();
      m_waveCurrents.add (playNoteWave);
    }
    finally
    {
      m_currentNotesMutex.release();
    }

    // Start thread if not started
    if (m_playNoteThread == null || !m_playNoteThread.isAlive())
    {
      m_playNoteThread = new Thread(this::playingNoteRun);
      m_playNoteThread.start();
    }
  }

  public boolean isPlaying(PlayNoteWave.PlayNote playNote)
  {
    boolean ret = false;
    if (!Utils.mutexTryAcquire(m_currentNotesMutex, 1000)) return false;
    try
    {
      for (PlayNoteWave wave : m_waveCurrents)
      {
        if (playNote == null ||
          wave.getPlayNote().equals(playNote))
        {
          if (wave.isPlaying())
          {
            ret = true;
            break;
          }
        }
      }
    }
    finally
    {
      m_currentNotesMutex.release();
    }

    return ret;
  }

  public Collection<PlayNoteWave.PlayNote> getPlayingNoteList()
  {
    List <PlayNoteWave.PlayNote> list = new ArrayList<PlayNoteWave.PlayNote>();
    if (!Utils.mutexTryAcquire(m_currentNotesMutex, 1000)) return list;
    try
    {
      for (PlayNoteWave wave : m_waveCurrents)
      {
        if (wave.isPlaying())
        {
          list.add (wave.getPlayNote());
        }
      }
    }
    finally
    {
      m_currentNotesMutex.release();
    }

    return list;
  }

  // When a note is added, to avoid saturation, the signal must be divided by the number of currently playing notes.
  // When a note is removed, the divider must increase accordingly, based on the number of remaining notes.
  // This causes glitches, as the signal is suddenly multiplied or divided (e.g., by two).
  // To handle this, we maintain min and max values to compute a stable divider - this easy solves the case when a note is added.
  // To smooth out the effect when a note is removed, we track min and max values over a moving window of recent samples,
  // and we regularly force an update of these values. This keeps the divider accurate and avoids abrupt changes.
  double m_pcm_Lastmin = 0, m_pcm_Lastmax = 0;
  int m_pcm_updateMinMax = 0;
  // Will add signal, and adjust level to avoid click adding / removing a sound
  private boolean setPcmFromCurrentWave(short[] pcmOut)
  {
    boolean result = true;
    int count = 0;
    int tmp = 0;
    double div;

    double min = m_pcm_Lastmin, max = m_pcm_Lastmax;

    for (int i = 0; i < pcmOut.length; i++)
    {
      tmp = 0;
      count = 0;

      try
      {
        if (Utils.mutexTryAcquire(m_currentNotesMutex, 300))
        {
          ListIterator<PlayNoteWave> it = m_waveCurrents.listIterator();
          while (it.hasNext())
          {
            PlayNoteWave wave = it.next();
            PlayNoteWave.PlayNote playNote = wave.getPlayNote();
            // Add signals
            if (wave.hasNext())
            {
              tmp += wave.next();
              count++;
            }
            else
            {
              if (m_audioAutoStopPlayingListener != null)
              {
                m_audioAutoStopPlayingListener.onStopPlaying(playNote);
              }
              it.remove();
            }
          }
        }
      }
      finally
      {
        m_currentNotesMutex.release();
      }

      if (count > 0)
      {
        if (tmp > max) max = tmp;
        if (tmp < min) min = tmp;
        if (max > m_pcm_Lastmax) m_pcm_Lastmax = max;
        if (min < m_pcm_Lastmin) m_pcm_Lastmin = min;

        // Divided by count of signal generated glitch because amplitude was suddenly divided.
        // Use previous min / max to do it
        div = Math.max (max / Short.MAX_VALUE, min / Short.MIN_VALUE);
        if (div < 1) div = 1;

        tmp = (int)(tmp / div);
        if (tmp > Short.MAX_VALUE) tmp = Short.MAX_VALUE;
        if (tmp < Short.MIN_VALUE) tmp = Short.MIN_VALUE;
        pcmOut[i] = (short)(tmp);

        if (m_pcm_updateMinMax <= 0)
        {
          // 62 ms is the max period for a wave (16.0 hz)
          m_pcm_updateMinMax = AudioUtils.getAudioByteLen(62);
          min = m_pcm_Lastmin;
          max = m_pcm_Lastmax;
          m_pcm_Lastmin = 0;
          m_pcm_Lastmax = 0;
        }
        else
        {
          m_pcm_updateMinMax--;
        }

        m_waveSilentPreventGlitch.reset();
      }
      else if (m_waveSilentPreventGlitch.hasNext())
      {
        pcmOut[i] = m_waveSilentPreventGlitch.next();
      }
      else
      {
        pcmOut[i] = 0;
        result = false;
      }

      // Copy to second channel if stereo
      if (AudioUtils.AUDIO_FORMAT == AudioFormat.CHANNEL_OUT_STEREO && i < pcmOut.length - 1)
      {
        pcmOut[i + 1] = pcmOut[i];
        i++;
      }
    }

    // Return false if nothing more to play
    return result;
  }

  private void playingNoteRun()
  {
    Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    short[] pcm = new short[AudioUtils.AudioTrack_getMinBufferSize(
        AudioUtils.AUDIO_SAMPLE_RATE_HZ,
        AudioUtils.AUDIO_ENCODING,
        AudioUtils.AUDIO_FORMAT)];

    // Start Audio track
    AudioTrack audioTrack = AudioUtils.newAudioTrack(pcm.length, AudioTrack.MODE_STREAM);
    if (audioTrack == null)
    {
      return;
    }
    try
    {
      audioTrack.play();

      while (setPcmFromCurrentWave(pcm) &&
             !Thread.currentThread().isInterrupted())
      {
        audioTrack.write(pcm, 0, pcm.length);
      }

      // Stop AudioTrack
      audioTrack.stop();
    }
    finally
    {
      AudioUtils.releaseAudioTrack(audioTrack);
    }
  }

  public void setWaveForm(String waveFormStr)
  {
    m_soundGenerator = ASoundGenerator.factory(waveFormStr);
  }

  public void setPianoMode(int mode)
  {
    m_pianoMode = mode;
  }
  public int getPianoMode()
  {
    // Backward compatibility for version <= 1.0.8
    if (m_pianoMode <= 0) return 1;
    return m_pianoMode;
  }

  public boolean isNoteContinuous()
  {
    return m_soundGenerator.isContinous();
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
    else max += 1;
    if (diffCents(freq, getFrequency(min / 12, min % 12)) > diffCents(getFrequency(max / 12, max % 12), freq))
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
