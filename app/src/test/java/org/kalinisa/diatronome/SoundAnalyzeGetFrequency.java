package org.kalinisa.diatronome;

import org.kalinisa.diatronome.Cores.SoundAnalyzer.ISoundAnalyzer;
import org.kalinisa.diatronome.Cores.SoundAnalyzer.SoundAnalyzerEnvelop;

import org.junit.Test;
import static org.junit.Assert.*;

public class SoundAnalyzeGetFrequency
{
  private double PERCENTACCEPTANCE = 0.002;
  private double FREQREF = 442.0;
  private int AUDIO_SAMPLE_RATE = 44100;

  ISoundAnalyzer m_soundAnalyzer;

  public SoundAnalyzeGetFrequency()
  {
    m_soundAnalyzer = new SoundAnalyzerEnvelop(AUDIO_SAMPLE_RATE);
    // m_soundAnalyzer = new SoundAnalyzerAutocorrelate(AUDIO_SAMPLE_RATE);
    // m_soundAnalyzer = new SoundAnalyzerFourier(AUDIO_SAMPLE_RATE);
    // m_soundAnalyzer = new SoundAnalyzerPeakDetector(AUDIO_SAMPLE_RATE);
    // m_soundAnalyzer = new SoundAnalyzerYin(AUDIO_SAMPLE_RATE);
  }

  private void assertFrequency (double expected, double actual)
  {
    assertTrue("Got frequency " + actual + " Hz, expected " + expected + " Hz",
      Math.abs (actual - expected) / expected <= PERCENTACCEPTANCE);
  }

  @Test
  public void sinusPerfect()
  {
    double[] signal = new double[SoundAnalyzeGetFrequencyData.SIGNAL_LEN];
    SoundAnalyzeGetFrequencyData.generatorSinus(signal, FREQREF);
    double frequency = m_soundAnalyzer.getPitch(signal);
    assertFrequency (FREQREF, frequency);
  }

  @Test
  public void squarePerfect()
  {
    double[] signal = new double[SoundAnalyzeGetFrequencyData.SIGNAL_LEN];
    SoundAnalyzeGetFrequencyData.generatorSquare(signal, FREQREF);
    double frequency = m_soundAnalyzer.getPitch(signal);
    assertFrequency (FREQREF, frequency);
  }

  @Test
  public void sawSoothPerfect()
  {
    double[] signal = new double[SoundAnalyzeGetFrequencyData.SIGNAL_LEN];
    SoundAnalyzeGetFrequencyData.generatorSawSooth(signal, FREQREF);
    double frequency = m_soundAnalyzer.getPitch(signal);
    assertFrequency (FREQREF, frequency);
  }

  @Test
  public void sinusFiltered()
  {
    double[] signal = new double[SoundAnalyzeGetFrequencyData.SIGNAL_LEN];
    SoundAnalyzeGetFrequencyData.signalSin442(signal);
    double frequency = m_soundAnalyzer.getPitch(signal);
    assertFrequency (FREQREF, frequency);
  }

  @Test
  public void squareFiltered()
  {
    double[] signal = new double[SoundAnalyzeGetFrequencyData.SIGNAL_LEN];
    SoundAnalyzeGetFrequencyData.signalSquare442(signal);
    double frequency = m_soundAnalyzer.getPitch(signal);
    assertFrequency (FREQREF, frequency);
  }

  @Test
  public void triangleFiltered()
  {
    double[] signal = new double[SoundAnalyzeGetFrequencyData.SIGNAL_LEN];
    SoundAnalyzeGetFrequencyData.signalTriangle442(signal);
    double frequency = m_soundAnalyzer.getPitch(signal);
    assertFrequency (FREQREF, frequency);
  }
}
