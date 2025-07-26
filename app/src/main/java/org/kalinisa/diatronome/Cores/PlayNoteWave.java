package org.kalinisa.diatronome.Cores;

import java.util.Iterator;

/**
 * Describe note wave to play
 * Wave will loop from start to end until stop is called
 */
public class PlayNoteWave implements Iterator<Short>
{
  public static class PlayNote
  {
    private int m_octave;
    private int m_note;
    public PlayNote(int octave, int note)
    {
      m_octave = octave;
      m_note = note;
    }
    public int getOctave() { return m_octave;}
    public int getNote() { return m_note;}
    @Override
    public boolean equals(Object o)
    {
      if (o == null) return false;
      if (o.getClass() != this.getClass()) return false;
      PlayNote other = (PlayNote)o;
      return m_octave == other.m_octave &&
        m_note == other.m_note;
    }
    @Override
    public int hashCode() { return m_octave * 12 + m_note; }
  }

  private final int m_start;
  private final int m_stop;
  private final short[] m_audioPcm;
  private int m_pos;
  private final PlayNote m_note;
  private boolean m_isPlaying;
  private double m_gain;
  private final double m_gainFactor;

  public PlayNoteWave(PlayNote note, short[] pcm, int start, int stop, int smoothStopMs)
  {
    m_note = note;
    m_audioPcm = pcm;
    m_pos = 0;
    m_start = start;
    m_stop = stop;
    m_isPlaying = false;

    m_gain = 1;
    m_gainFactor = Math.pow (0.01, 1.0/AudioUtils.getAudioSampleLen(smoothStopMs));
  }

  public Short next()
  {
    short res = 0;
    int pos = m_pos;
    if (pos < m_audioPcm.length)
    {
      res = m_audioPcm[pos];
    }
    pos++;

    if (m_isPlaying)
    {
      // auto loop
      if (pos == m_stop) pos = m_start;
    }
    else
    {
      // Smooth to stop
      m_gain = m_gain * m_gainFactor;

      res = (short)(m_gain*res);
    }
    m_pos = pos;

    return res;
  }

  public boolean hasNext ()
  {
    return m_pos < m_audioPcm.length;
  }

  public PlayNote getPlayNote() { return m_note; }

  public void reset()
  {
    m_pos = 0;
  }

  public boolean isPlaying()
  {
    return m_isPlaying && hasNext();
  }

  public void play()
  {
    m_isPlaying = true;
    m_gain = 1;
  }

  public void stop()
  {
    m_isPlaying = false;
  }
}
