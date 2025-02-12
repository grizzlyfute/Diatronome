package com.tuner.diatronome.Cores.SoundAnalyzer;

/**
 * @author Joren Six
 *An implementation of the YIN pitch tracking algorithm.
 *See <a href="http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf">the YIN paper.</a>
 *
 *Implementation originally based on <a href="http://aubio.org">aubio</a>
 *
 *
 * Updated by Emlyn O'Regan to work in the PitchDetect sample project for Android.
 * I removed all the realtime features (which are tied in with javax libraries, not good for Dalvik), and
 * modified Yin to be called with a byte buffer to be analyzed using the getPitch() method. So
 * just create yourself a Yin, then call getPitch(bytes) when you're ready.
 *
 * Also converted it to use an array of Shorts instead of Floats.
 *
 * Original implementation is here: http://tarsos.0110.be/artikels/lees/YIN_Pitch_Tracker_in_JAVA
 */
public class SoundAnalyzerYin implements ISoundAnalyzer
{
  /**
   * Used to start and stop real time annotations.
   */
//	private static Yin yinInstance;

  /**
   * The YIN threshold value (see paper)
   */
  private final double threshold = 0.15;

  //	private int bufferSize;
//	private int overlapSize;
  private final float sampleRate;
  /**
   * A boolean to start and stop the algorithm.
   * Practical for real time processing of data.
   */
//	private volatile boolean running;

  /**
   * The original input buffer
   */
  private short[] inputBuffer;

  /**
   * The buffer that stores the calculated values.
   * It is exactly half the size of the input buffer.
   */
  private float[] yinBuffer;

  public SoundAnalyzerYin(int audioSampleRate) {
    this.sampleRate = audioSampleRate;
//		bufferSize = 1024;
//		overlapSize = bufferSize/2;//half of the buffer overlaps
//		running = true;
//		inputBuffer = new short[bufferSize];
//		yinBuffer = new float[bufferSize/2];
  }

  /**
   * Implements the difference function as described
   * in step 2 of the YIN paper
   */
  private void difference(){
    int j,tau;
    float delta;
    for(tau=0;tau < yinBuffer.length;tau++){
      yinBuffer[tau] = 0;
    }
    for(tau = 1 ; tau < yinBuffer.length ; tau++){
      for(j = 0 ; j < yinBuffer.length ; j++){
        delta = (float)inputBuffer[j] - (float)inputBuffer[j+tau];
        yinBuffer[tau] += delta * delta;
      }
    }
  }

  /**
   * The cumulative mean normalized difference function
   * as described in step 3 of the YIN paper
   * <br><code>
   * yinBuffer[0] == yinBuffer[1] = 1
   * </code>
   *
   */
  private void cumulativeMeanNormalizedDifference(){
    int tau;
    yinBuffer[0] = 1;
    //Very small optimization in comparison with AUBIO
    //start the running sum with the correct value:
    //the first value of the yinBuffer
    float runningSum = yinBuffer[1];
    //yinBuffer[1] is always 1
    yinBuffer[1] = 1;
    //now start at tau = 2
    for(tau = 2 ; tau < yinBuffer.length ; tau++){
      runningSum += yinBuffer[tau];
      yinBuffer[tau] *= tau / runningSum;
    }
  }

  /**
   * Implements step 4 of the YIN paper
   */
  private int absoluteThreshold(){
    //Uses another loop construct
    //than the AUBIO implementation
    for(int tau = 1;tau<yinBuffer.length;tau++){
      if(yinBuffer[tau] < threshold){
        while(tau+1 < yinBuffer.length &&
          yinBuffer[tau+1] < yinBuffer[tau])
          tau++;
        return tau;
      }
    }
    //no pitch found
    return -1;
  }

  /**
   * Implements step 5 of the YIN paper. It refines the estimated tau value
   * using parabolic interpolation. This is needed to detect higher
   * frequencies more precisely.
   *
   * @param tauEstimate
   *            the estimated tau value.
   * @return a better, more precise tau value.
   */
  private float parabolicInterpolation(int tauEstimate) {
    float s0, s1, s2;
    int x0 = (tauEstimate < 1) ? tauEstimate : tauEstimate - 1;
    int x2 = (tauEstimate + 1 < yinBuffer.length) ? tauEstimate + 1 : tauEstimate;
    if (x0 == tauEstimate)
      return (yinBuffer[tauEstimate] <= yinBuffer[x2]) ? tauEstimate : x2;
    if (x2 == tauEstimate)
      return (yinBuffer[tauEstimate] <= yinBuffer[x0]) ? tauEstimate : x0;
    s0 = yinBuffer[x0];
    s1 = yinBuffer[tauEstimate];
    s2 = yinBuffer[x2];
    //fixed AUBIO implementation, thanks to Karl Helgason:
    //(2.0f * s1 - s2 - s0) was incorrectly multiplied with -1
    return tauEstimate + 0.5f * (s2 - s0 ) / (2.0f * s1 - s2 - s0);
  }

  /**
   * The main flow of the YIN algorithm. Returns a pitch value in Hz or -1 if
   * no pitch is detected using the current values of the input buffer.
   *
   * @return a pitch value in Hz or -1 if no pitch is detected.
   */
  public float getPitch(short[] aInputBuffer)
  {
    inputBuffer = aInputBuffer;
    yinBuffer = new float[inputBuffer.length/2];

    int tauEstimate = -1;
    float pitchInHertz = -1;

    //step 2
    difference();

    //step 3
    cumulativeMeanNormalizedDifference();

    //step 4
    tauEstimate = absoluteThreshold();

    //step 5
    if(tauEstimate != -1){
      float betterTau = parabolicInterpolation(tauEstimate);

      //step 6
      //TODO Implement optimization for the YIN algorithm.
      //0.77% => 0.5% error rate,
      //using the data of the YIN paper
      //bestLocalEstimate()

      //conversion to Hz
      pitchInHertz = sampleRate/betterTau;
    }

    return pitchInHertz;
  }

  public double getPitch(final double[] signal)
  {
    short[] buffer = new short[signal.length];
    for (int i = 0; i < buffer.length; i++)
    {
      // signal[i] = 2.0*((double)buffer[i] - Short.MIN_VALUE) / (double)(Short.MAX_VALUE - Short.MIN_VALUE) - 1.0;
      buffer[i] = (short)(signal[i] * Short.MAX_VALUE);
    }
    return (double)(getPitch(buffer));
  }
}