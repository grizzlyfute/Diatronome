package com.tuner.diatronome.Ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.tuner.diatronome.Cores.BasicAnimator;
import com.tuner.diatronome.Cores.Utils;
import com.tuner.diatronome.R;

// See res/values/attrs.xml
public class NeedleView extends View
{
  private int m_colorMain;
  private int m_colorLine1;

  private final Paint m_paintLine1;
  private final Paint m_paintLine2;

  private final Paint m_paintFillNeedle;
  private final Paint m_paintFillRound;
  private final Paint m_paintIndicator;

  private final Path m_pathNeedle;

  private final RectF m_posGradle;
  private final RectF m_posNeedle;
  private final RectF m_posBase;
  private final RectF m_posCutOffNeedle;
  private final RectF m_posIndicator;

  private float m_needleMaxHalfAngleDeg;
  private float m_needleAngleDeg;
  private float m_gradleHeight;
  private final int m_minValue;
  private final int m_maxValue;

  private final float[] m_hsvGood;
  private final float[] m_hsvBad;

  private final BasicAnimator m_animator;

  public NeedleView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    final Resources r = getResources();

    // Get attributes from xml
    TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.NeedleView, 0, 0);
    try
    {
      m_minValue = typedArray.getInt(R.styleable.NeedleView_min, -50);
      m_maxValue = typedArray.getInt(R.styleable.NeedleView_max, +50);
    }
    finally
    {
      typedArray.recycle();
    }

    // Initialize the most objet as possible to optimize redraw

    // Positions
    m_posGradle = new RectF();
    m_posNeedle = new RectF();
    m_posBase = new RectF();
    m_posCutOffNeedle = new RectF();
    m_posIndicator = new RectF();

    m_needleAngleDeg = 0;
    m_gradleHeight = 0;
    m_needleMaxHalfAngleDeg = 1;

    // Colors
    m_colorLine1 = Color.LTGRAY;

    TypedValue typedValue = new TypedValue();
    TypedArray arr = context.obtainStyledAttributes(typedValue.data,
      new int[]{ android.R.attr.textColorPrimary });
    m_colorLine1 = arr.getColor(0, m_colorLine1);
    // Can not get shared preference here (note constructed). Color will updated later
    arr.recycle();

    // Graphics
    m_paintLine1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintLine1.setColor(m_colorLine1);
    m_paintLine1.setStyle(Paint.Style.STROKE);
    m_paintLine1.setStrokeWidth(Utils.dpToPixels(r, 2));
    m_paintLine1.setTextSize(Utils.dpToPixels(r, 22));
    m_paintLine1.setTextAlign(Paint.Align.CENTER);

    m_paintLine2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintLine2.setColor(Color.RED);
    m_paintLine2.setStyle(Paint.Style.STROKE);
    m_paintLine2.setStrokeWidth(Utils.dpToPixels(r, 2));
    m_paintLine2.setTextSize(Utils.dpToPixels(r, 22));
    m_paintLine2.setTextAlign(Paint.Align.CENTER);

    m_paintFillNeedle = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintFillNeedle.setColor(m_colorMain);
    m_paintFillNeedle.setStrokeWidth(3);
    m_paintFillNeedle.setAntiAlias(true);
    m_paintFillNeedle.setStrokeCap(Paint.Cap.ROUND);
    m_paintFillNeedle.setStyle(Paint.Style.FILL);
    //m_paintFillNeedle.setShadowLayer(12, 0, 0, Color.BLACK);

    m_paintFillRound = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintFillRound.setColor(m_colorMain);
    // Crops the arcs circle of the value, with strong stroke line
    m_paintFillRound.setStrokeWidth(Utils.dpToPixels(r, 10));
    m_paintFillRound.setStrokeJoin(Paint.Join.MITER);
    m_paintFillRound.setStrokeMiter(1.0f);
    m_paintFillRound.setAntiAlias(true);
    m_paintFillRound.setStyle(Paint.Style.STROKE);
    // m_paintFillRound.setShadowLayer(12, 0, 0, Color.BLACK);

    m_paintIndicator = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintIndicator.setColor(Color.GREEN);
    m_paintIndicator.setStyle(Paint.Style.FILL);

    // Color indicator
    m_hsvGood = new float[3];
    m_hsvBad = new float[3];
    Color.colorToHSV(Color.GREEN, m_hsvGood);
    Color.colorToHSV(Color.RED, m_hsvBad);

    // Path
    m_pathNeedle = new Path();

    // Animator
    m_animator = new BasicAnimator();
    m_animator.addUpdateListener(new BasicAnimator.AnimatorUpdateListener()
    {
      @Override
      public void onAnimationUpdate(float animatedValueDegree)
      {
        updateNeedleDeg (animatedValueDegree);
      }
    });

    updateNeedleDeg(0);
  }

  // Limit size h to w / 2 to avoid bad ratio
  @Override
  @SuppressWarnings("UnnecessaryLocalVariable")
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    int desiredWidth = widthSize;
    int desiredHeight = heightSize / 2;

    int width;
    int height;

    // Must be this size
    if (widthMode == MeasureSpec.EXACTLY)
      width = widthSize;
      // Can't be bigger than...
    else if (widthMode == MeasureSpec.AT_MOST)
      //noinspection ConstantConditions
      width = Math.min(desiredWidth, widthSize);
      // Be whatever you want
    else // UNSPECIFIED
      width = desiredWidth;

    // Must be this size
    if (heightMode == MeasureSpec.EXACTLY)
      height = heightSize;
      // Can't be bigger than...
    else if (heightMode == MeasureSpec.AT_MOST)
      height = Math.min(3 * widthSize / 2, heightSize);
      //Be whatever you want
    else // UNSPECIFIED
      height = desiredHeight;

    width = Math.max(width, getSuggestedMinimumWidth());
    height = Math.max(height, getSuggestedMinimumHeight());

    // Redraw dimension
    setMeasuredDimension(width, height);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldW, int oldH)
  {
    super.onSizeChanged(w, h, oldW, oldH);

    int paddingLeft = getPaddingLeft();
    int paddingRight = getPaddingRight();
    int paddingTop = getPaddingTop();
    int paddingBottom = getPaddingBottom();

    float sizeW = w - paddingLeft - paddingRight;
    float sizeH = h - paddingTop - paddingBottom;

    final Resources r = getResources();
    m_gradleHeight = Utils.dpToPixels(r, 16);

    Rect textBounds = new Rect();
    m_paintLine1.getTextBounds("-00", 0, "-00".length(), textBounds);
    float arbitraryRaiseCenter = (w > h) ? ((float)h/5.0f)*((float)h/w) : Utils.dpToPixels(r, 0);
    float baseRadius = Utils.dpToPixels(r, 16) - m_paintFillRound.getStrokeWidth() / 2;
    float needleBaseRadius = baseRadius + m_paintFillRound.getStrokeWidth() / 2 + Utils.dpToPixels(r, 4);
    float radius =
      sizeH -
        // Space between gradle and text
        (m_gradleHeight + m_paintLine1.getTextSize() / 2) -
        // Text size from gradle
        textBounds.height() -
        // Half round of base
        baseRadius -
        // Arbitrary rotation center raise
        arbitraryRaiseCenter;
    float center_rot_x = (float) w / 2;
    float center_rot_y = h - paddingBottom - baseRadius - arbitraryRaiseCenter;

    // The sizeH is the hypotenuse of the triangle (extremity of the arc to needle center)
    if ((sizeW / 2) < radius)
      m_needleMaxHalfAngleDeg = (float) Math.toDegrees(Math.asin(((sizeW - (sizeH - radius)) / 2) / radius));
    else
      m_needleMaxHalfAngleDeg = 90;
    m_needleMaxHalfAngleDeg = Math.max(0.1f, Math.min(75, m_needleMaxHalfAngleDeg));

    // Center on screen, height on h - margins
    m_posNeedle.left = center_rot_x - needleBaseRadius;
    m_posNeedle.top = center_rot_y - radius + m_gradleHeight * 2 / 3;
    m_posNeedle.right = center_rot_x + needleBaseRadius;
    m_posNeedle.bottom = center_rot_y;

    m_posCutOffNeedle.left = paddingLeft;
    m_posCutOffNeedle.top = paddingTop;
    m_posCutOffNeedle.right = paddingLeft + sizeW;
    m_posCutOffNeedle.bottom = center_rot_y;

    // Top anchored, rounded, center on rotation (screen middle, needle bottom
    m_posGradle.left = center_rot_x - radius;
    m_posGradle.top = center_rot_y - radius;
    m_posGradle.right = center_rot_x + radius;
    m_posGradle.bottom = center_rot_y + radius;

    // Center on bottom needle, half circle
    m_posBase.left = center_rot_x - baseRadius;
    m_posBase.top = center_rot_y - baseRadius;
    m_posBase.right = center_rot_x + baseRadius;
    m_posBase.bottom = center_rot_y + baseRadius;

    m_posIndicator.left = center_rot_x - 3*baseRadius;
    m_posIndicator.top = center_rot_y + 3*baseRadius;
    m_posIndicator.right = center_rot_x + 3*baseRadius;
    m_posIndicator.bottom = center_rot_y + 3.5f*baseRadius;

    m_pathNeedle.reset();
    m_pathNeedle.setFillType(Path.FillType.EVEN_ODD);
    m_pathNeedle.moveTo(m_posNeedle.left, m_posNeedle.bottom);
    m_pathNeedle.lineTo(m_posNeedle.centerX(), m_posNeedle.top);
    m_pathNeedle.arcTo(new RectF(
      m_posNeedle.left,
        m_posNeedle.bottom - needleBaseRadius,
        m_posNeedle.right,
        m_posNeedle.bottom + needleBaseRadius),
      0, -180);
    m_pathNeedle.close();

    // Recompute gradient
    setColorMain(m_colorMain);
  }

  // No allocation here
  @Override
  protected void onDraw(Canvas canvas)
  {
    super.onDraw(canvas);
    final int ratioPercent = 15;
    final Resources r = getResources();

    final float radius = (m_posGradle.bottom - m_posGradle.top) / 2;
    // The view is very small
    if (radius <= Utils.dpToPixels(r, 20))
    {
      float m = Utils.dpToPixels(r, 8);
      float w = getWidth() - 2 * m;
      float h = getHeight() / 3.0f;
      for (int i = 0; i <= 10; i++)
      {
        if (i <= 10 * ratioPercent / 100 || i >= 10 * (100 - ratioPercent) / 100)
        {
          canvas.drawLine(m + w * i / 10.0f, h - m_gradleHeight / 2, m + w * i / 10.0f, h + m_gradleHeight / 2, m_paintLine2);
        }
        else if (i == 10 / 2)
        {
          canvas.drawLine(m + w * i / 10.0f, h - m_gradleHeight*1.5f, m + w * i / 10.0f, h + m_gradleHeight*1.5f, m_paintLine1);
        }
        else
        {
          canvas.drawLine(m + w * i / 10.0f, h - m_gradleHeight / 2, m + w * i / 10.0f, h + m_gradleHeight / 2, m_paintLine1);
        }
      }
      canvas.drawLine(m, h, m + w, h, m_paintLine1);
      canvas.drawCircle(m + w / 2 + (1 + m_needleAngleDeg / m_needleMaxHalfAngleDeg),
        h, Utils.dpToPixels(r, 8), m_paintFillNeedle);
      return;
    }

    // The gradle
    canvas.drawArc(
      m_posGradle,
      -m_needleMaxHalfAngleDeg - 90 + 2 * m_needleMaxHalfAngleDeg * 0.0f,
      2 * m_needleMaxHalfAngleDeg * ratioPercent * 1.0f / 100,
      false,
      m_paintLine2);
    canvas.drawArc(
      m_posGradle,
      -m_needleMaxHalfAngleDeg - 90 + 2 * m_needleMaxHalfAngleDeg * ratioPercent * 1.0f / 100,
      2 * m_needleMaxHalfAngleDeg * (100 - 2 * ratioPercent) * 1.0f / 100,
      false,
      m_paintLine1);
    canvas.drawArc(
      m_posGradle,
      -m_needleMaxHalfAngleDeg - 90 + 2 * m_needleMaxHalfAngleDeg * (100 - ratioPercent) * 1.0f / 100,
      2 * m_needleMaxHalfAngleDeg * ratioPercent * 1.0f / 100,
      false,
      m_paintLine2);

    // The gradle number
    Paint painter = null;

    boolean isLargeScreen = (canvas.getWidth() >=
      // 10 labels of 3 characters in the circle
      10 * (3 * m_paintLine1.getTextSize()) / (Math.toRadians(2 * m_needleMaxHalfAngleDeg)));
    final int stepMax = 10 * (isLargeScreen ? 4 : 2);
    for (int i = 0; i <= stepMax; i++)
    {
      // Save work with stack...
      canvas.save();
      // 0, 0 is the intersection of the arc and the gradle
      canvas.translate(m_posGradle.centerX(), m_posGradle.centerY());
      canvas.rotate(-m_needleMaxHalfAngleDeg + i * 2 * m_needleMaxHalfAngleDeg / stepMax);
      canvas.translate(0, -radius);
      // Caution : inverted coordinate y. Can not flip horizontal (canvas.scale(1, -1, w/2, h/2) because text will draw reverted
      // Flip horizontally to not have reverted coordinate
      // canvas.scale(1f, -1f, canvas.getWidth() / 2f, canvas.getHeight() / 2f);

      if (i <= stepMax * ratioPercent / 100 || i >= stepMax * (100 - ratioPercent) / 100)
      {
        painter = m_paintLine2;
      }
      else
      {
        painter = m_paintLine1;
      }

      // Middle
      if (i == stepMax /2)
      {
        canvas.drawLine(-0.1f, m_gradleHeight*1.5f, -0.1f, -m_gradleHeight*1.5f / 2, painter);
        canvas.drawLine(0.1f, m_gradleHeight*1.5f, 0.1f, -m_gradleHeight*1.5f / 2, painter);
      }
      else if ((isLargeScreen && i % 4 == 0) || (!isLargeScreen && i % 2 == 0))
      {
        // Big gradle
        canvas.drawLine(0, m_gradleHeight, 0, -m_gradleHeight / 2, painter);
      }
      else
      {
        // Small gradle
        canvas.drawLine(0, m_gradleHeight / 2, 0, -0, painter);
      }

      // Label. One over two if small screen
      //noinspection ConstantConditions
      if ((isLargeScreen && i % 4 == 0) || (!isLargeScreen && i % (2 * 2) == 0) ||
        i == 0 || i == stepMax)
      {
        canvas.drawText("" + (m_minValue + i * (m_maxValue - m_minValue) / stepMax),
          0, -(m_gradleHeight + painter.getTextSize() / 2), m_paintLine1);
      }
      canvas.restore();
    }

    // The needle
    // Hide the overflow part if rotated
    // canvas.clipRect(m_posCutOffNeedle);

    canvas.save();
    canvas.rotate(m_needleAngleDeg,
      m_posNeedle.centerX(), m_posNeedle.bottom);
    canvas.drawPath(m_pathNeedle, m_paintFillNeedle);

    // Base of the needle
    canvas.drawArc(m_posBase, 0, 360, false, m_paintFillRound);

    canvas.restore();

    // The indicator
    // Disabled for now
    // canvas.drawArc(m_posIndicator, 0, 360, false, m_paintIndicator);
  }

  public void setColorMain(int color)
  {
    m_colorMain = color;

    m_paintFillRound.setColor(color);
    m_paintFillNeedle.setColor(color);
    int colorDark = Utils.darker(m_colorMain);

    // 0 is East, 0.25 is South
    Shader shader = new SweepGradient(m_posNeedle.centerX(), m_posNeedle.top,
      new int[] {m_colorMain, m_colorMain, colorDark, m_colorMain, m_colorMain},
      new float[] { 0f, 0.25f - 0.1f*m_posNeedle.width()/m_posNeedle.height(), 0.25f, 0.25f, 1.0f } );
    m_paintFillNeedle.setShader(shader);
    m_paintFillRound.setShader(shader);
    // m_needleMaxHalfAngleDeg is never null
    updateAccuracy (m_needleAngleDeg / m_needleMaxHalfAngleDeg);
  }
  public int getColorMain() { return m_colorMain; }

  public void updateAccuracy (double accuracy)
  {
    accuracy = Math.min(1, Math.max(-1, accuracy));
    float wantedDegree = (float) (m_needleMaxHalfAngleDeg * accuracy);

    m_animator.cancel();
    m_animator.setFloatValues(m_needleAngleDeg, wantedDegree);
    m_animator.setDuration((long)(500*Math.abs(m_needleAngleDeg - wantedDegree)/(2*m_needleMaxHalfAngleDeg)));
    m_animator.start();
  }

  private float linearInterpolate(float a, float b, float proportion)
  {
    return (a + ((b - a) * proportion));
  }

  private int getAngleColor()
  {
    float proportion = Math.abs (m_needleAngleDeg / m_needleMaxHalfAngleDeg);
    float[] hsv = new float[3];
    for (int i = 0; i < 3; i++)
    {
      hsv[i] = linearInterpolate(m_hsvGood[i], m_hsvBad[i], proportion);
    }
    return Color.HSVToColor(hsv);
  }

  private void updateNeedleDeg (float degree)
  {
    if (Math.abs(m_needleAngleDeg - degree) < 0.001) return;
    m_needleAngleDeg = degree;

    m_paintIndicator.setColor (getAngleColor());

    // One next event loop (do not do this.invalidate();)
    this.postInvalidate();
  }
}