package com.tuner.diatronome.Cores.SoundAnalyzer;

public interface ISoundAnalyzer
{
  // Return the main pitch frequency of the signal.
  // Signal is expected between -1.0 and 1.0
  double getPitch(final double[] signal);
}
