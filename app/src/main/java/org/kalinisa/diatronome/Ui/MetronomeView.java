package org.kalinisa.diatronome.Ui;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import org.kalinisa.diatronome.Cores.BasicAnimator;
import org.kalinisa.diatronome.Cores.MetronomeCore;
import org.kalinisa.diatronome.Cores.Utils;

public class MetronomeView extends View
{
  private int m_colorMain;
  private int[] m_beatConfig;
  private int m_beatSelected;
  private boolean m_isReverse;
  private float m_needleAngleDeg;
  private int m_touchSelected;

  private static float s_savedAngle;
  private static boolean s_savedIsReverse;

  private final Paint m_paintBgOff_off;
  private final Paint m_paintBgOff_on;
  private final Paint m_paintBgSubdiv_off;
  private final Paint m_paintBgSubdiv_on;
  private final Paint m_paintBgNormal_off;
  private final Paint m_paintBgNormal_on;
  private final Paint m_paintBgAccent_off;
  private final Paint m_paintBgAccent_on;
  private final Paint m_paintBgSelected;
  private final Paint m_paintLine;
  private final Paint m_paintLineDash;
  private final Paint m_paintNeedle;
  private final Paint m_paintBase;
  private final Paint m_paintText;

  private final Path m_pathNeedle;
  private final Path m_pathBeat;

  private final RectF m_posNeedle;
  private final RectF m_posBase;
  private final RectF m_poClipOut;

  private final BasicAnimator m_animator;

  public interface MetronomeViewTouchListener
  {
    // 100 is the center
    // 0..n is the config zone
    void onTouchArea(int num);
  }
  private MetronomeViewTouchListener m_touchListener = null;
  public void setMetronomeViewTouchListener(MetronomeViewTouchListener touchListener)
  {
    m_touchListener = touchListener;
  }

  public MetronomeView(Context context, AttributeSet attrs)
  {
    super(context, attrs);

    final Resources r = getResources();

    // Default for design
    m_beatConfig = new int[]
    {
      MetronomeCore.BEATCONFIG_ACCENT, MetronomeCore.BEATCONFIG_SUBDIV, MetronomeCore.BEATCONFIG_SUBDIV,
      MetronomeCore.BEATCONFIG_NORMAL, MetronomeCore.BEATCONFIG_SUBDIV, MetronomeCore.BEATCONFIG_SUBDIV,
      MetronomeCore.BEATCONFIG_OFF,    MetronomeCore.BEATCONFIG_SUBDIV, MetronomeCore.BEATCONFIG_OFF,
      MetronomeCore.BEATCONFIG_NORMAL, MetronomeCore.BEATCONFIG_OFF,    MetronomeCore.BEATCONFIG_NORMAL,
    };
    // Designer
    m_beatSelected = 5;
    m_needleAngleDeg = -180.0f*7/12 - 180.0f/24.0f;
    m_isReverse = false;
    m_touchSelected = -1;

    // Colors
    int colorTextPrimary;
    m_colorMain = Color.BLUE;
    TypedValue typedValue = new TypedValue();
    TypedArray arr = context.obtainStyledAttributes(typedValue.data, new int[]
    {
        android.R.attr.textColorPrimary,
        // android.R.attr.colorBackground,
    });
    try
    {
      colorTextPrimary = arr.getColor(0, Color.BLACK);
      // m_colorBackground = arr.getColor(1, Color.WHITE);
    }
    finally
    {
      arr.recycle();
    }

    // Positions
    m_posNeedle = new RectF();
    m_posBase = new RectF();
    m_poClipOut = new RectF();

    m_pathNeedle = new Path();
    m_pathBeat = new Path();

    // Graphics
    m_paintBgOff_off = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintBgOff_off.setStyle(Paint.Style.FILL);

    m_paintBgOff_on = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintBgOff_on.setStyle(Paint.Style.FILL);

    m_paintBgSubdiv_off = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintBgSubdiv_off.setStyle(Paint.Style.FILL);

    m_paintBgSubdiv_on = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintBgSubdiv_on.setStyle(Paint.Style.FILL);

    m_paintBgNormal_off = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintBgNormal_off.setStyle(Paint.Style.FILL);

    m_paintBgNormal_on = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintBgNormal_on.setStyle(Paint.Style.FILL);

    m_paintBgAccent_off = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintBgAccent_off.setStyle(Paint.Style.FILL);

    m_paintBgAccent_on = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintBgAccent_on.setStyle(Paint.Style.FILL);
    m_paintBgAccent_on.setStrokeWidth(Utils.dpToPixels(r, 20));
    m_paintBgAccent_on.setStrokeJoin(Paint.Join.MITER);
    m_paintBgAccent_on.setStrokeMiter(1.0f);
    m_paintBgAccent_on.setAntiAlias(true);

    m_paintBgSelected = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintBgSelected.setStyle(Paint.Style.FILL);
    m_paintBgSelected.setStrokeWidth(Utils.dpToPixels(r, 20));
    m_paintBgSelected.setAntiAlias(true);

    m_paintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintLine.setStyle(Paint.Style.STROKE);
    m_paintLine.setColor(colorTextPrimary);
    m_paintLine.setStrokeWidth(Utils.dpToPixels(r, 4));

    m_paintLineDash = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintLineDash.setStyle(Paint.Style.STROKE);
    m_paintLineDash.setColor(colorTextPrimary);
    m_paintLineDash.setStrokeWidth(Utils.dpToPixels(r, 4));
    m_paintLineDash.setPathEffect(new DashPathEffect(
      new float[] {Utils.dpToPixels(r, 10), Utils.dpToPixels(r, 20)},
      0f));

    m_paintNeedle = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintNeedle.setStyle(Paint.Style.FILL);

    m_paintBase = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintBase.setColor (colorTextPrimary);
    m_paintBase.setStrokeWidth(Utils.dpToPixels(r, 20));
    m_paintBase.setStrokeJoin(Paint.Join.MITER);
    m_paintBase.setStrokeMiter(1.0f);
    m_paintBase.setAntiAlias(true);
    m_paintBase.setStyle(Paint.Style.STROKE);
    // m_paintBase.setShadowLayer(12, 0, 0, Color.BLACK);

    m_paintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    m_paintText.setColor (colorTextPrimary);
    m_paintText.setTextSize(Utils.dpToPixels(r, 32));
    m_paintText.setTextAlign(Paint.Align.CENTER);

    // Animator
    m_animator = new BasicAnimator();
    m_animator.addUpdateListener(new BasicAnimator.AnimatorUpdateListener()
    {
      @Override
      public void onAnimationUpdate(float animatedValueDegree)
      {
        onAnimated(animatedValueDegree);
      }
    });

    // Utilities
    updateColor();
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

    float baseStrokeWidth = m_paintBase.getStrokeWidth()/2;
    float baseRadius = Math.min(w/4.0f, Utils.dpToPixels(r, 64));
    float needleHeight = Math.max(0, Math.min (sizeW/2 - baseRadius - baseStrokeWidth, sizeH - baseRadius - baseStrokeWidth));
    float needleBaseRadius = Utils.dpToPixels(r, 24) / 2;

    float center_rot_x = (float)w / 2;
    float center_rot_y = h - paddingBottom;

    m_poClipOut.left = 0;
    m_poClipOut.right = w;
    m_poClipOut.top = 0;
    m_poClipOut.bottom = h;

    m_posBase.left = center_rot_x - baseRadius;
    m_posBase.right = center_rot_x + baseRadius;
    m_posBase.bottom = center_rot_y + baseRadius;
    m_posBase.top = center_rot_y - baseRadius;

    // Consider now origin at center_rot_x, center_rot_y.
    // baseRadius is now the outer base radius
    baseRadius += baseStrokeWidth;

    m_posNeedle.left = 0 - needleBaseRadius;
    m_posNeedle.right = 0 + needleBaseRadius;
    m_posNeedle.bottom = -baseRadius;
    m_posNeedle.top = m_posNeedle.bottom - needleHeight;

    float needleSwapAngle = (float)Math.toDegrees(Math.asin(needleBaseRadius/baseRadius));
    m_pathNeedle.reset();
    m_pathNeedle.moveTo(m_posNeedle.left, m_posNeedle.bottom);
    m_pathNeedle.lineTo(m_posNeedle.centerX(), m_posNeedle.top);
    m_pathNeedle.lineTo(m_posNeedle.right, m_posNeedle.bottom);
    m_pathNeedle.setFillType(Path.FillType.EVEN_ODD);
    m_pathNeedle.arcTo(new RectF(
        -baseRadius,
        -baseRadius,
        +baseRadius,
        +baseRadius),
       -90+needleSwapAngle,
      -2*needleSwapAngle);
    m_pathNeedle.close();

    float infinity = 3*Math.max (w, h);
    int len = m_beatConfig.length > 0 ? m_beatConfig.length : 180;
    m_pathBeat.reset();
    m_pathBeat.setFillType(Path.FillType.EVEN_ODD);
    m_pathBeat.moveTo(baseRadius,0);
    m_pathBeat.moveTo(baseRadius,0);
    m_pathBeat.lineTo (baseRadius + infinity, 0);
    //noinspection UnaryPlus
    m_pathBeat.arcTo(new RectF(
        -baseRadius - infinity,
        -baseRadius - infinity,
        +baseRadius + infinity,
        +baseRadius + infinity),
      0, -180.0f/len);
     m_pathBeat.arcTo(new RectF( // Auto lineTo
         -baseRadius,
         -baseRadius,
         +baseRadius,
         +baseRadius),
       -180.0f/len, 180.0f/len);
    m_pathBeat.close();

    // Recompute gradient
    updateColor();
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    super.onDraw(canvas);
    // Erase previous
    // canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

    final float infinity = 3*Math.max (getWidth(), getHeight());

    for (int beat = 0; beat < m_beatConfig.length; beat++)
    {
      // int angleBeat = m_isReverse ? m_beatConfig.length - beat - 1 : beat;

      canvas.save();
      canvas.translate(m_posBase.centerX(), m_posBase.centerY());
      if (m_isReverse) canvas.scale(-1, 1);
      canvas.rotate(-180.0f + 180.0f * (beat + 1) / m_beatConfig.length);

      // Background
      Paint tmpPaint = null;
      if (beat == m_beatSelected)
      {
        if (m_beatConfig[beat] == MetronomeCore.BEATCONFIG_OFF)
          tmpPaint = m_paintBgOff_on;
        else if (m_beatConfig[beat] == MetronomeCore.BEATCONFIG_SUBDIV)
          tmpPaint = m_paintBgSubdiv_on;
        else if (m_beatConfig[beat] == MetronomeCore.BEATCONFIG_NORMAL)
          tmpPaint = m_paintBgNormal_on;
        else if (m_beatConfig[beat] == MetronomeCore.BEATCONFIG_ACCENT)
          tmpPaint = m_paintBgAccent_on;
        else
          tmpPaint = m_paintBgNormal_on;
      }
      else
      {
        if (m_beatConfig[beat] == MetronomeCore.BEATCONFIG_OFF)
          tmpPaint = m_paintBgOff_off;
        else if (m_beatConfig[beat] == MetronomeCore.BEATCONFIG_SUBDIV)
          tmpPaint = m_paintBgSubdiv_off;
        else if (m_beatConfig[beat] == MetronomeCore.BEATCONFIG_NORMAL)
          tmpPaint = m_paintBgNormal_off;
        else if (m_beatConfig[beat] == MetronomeCore.BEATCONFIG_ACCENT)
          tmpPaint = m_paintBgAccent_off;
        else
          tmpPaint = m_paintBgNormal_off;
      }
      if (m_touchSelected == beat)
      {
        tmpPaint = m_paintBgSelected;
      }
      canvas.drawPath(m_pathBeat, tmpPaint);

      // Line
      //noinspection ConstantConditions
      if (beat >= 0 && beat < m_beatConfig.length - 1)
      {
        if (beat < m_beatConfig.length - 1 &&
            m_beatConfig[beat + 1] == MetronomeCore.BEATCONFIG_SUBDIV)
          tmpPaint = m_paintLineDash;
        else
          tmpPaint = m_paintLine;
        canvas.drawLine (m_posBase.width() / 2 + m_paintBase.getStrokeWidth() / 2, 0,
          m_posBase.width() / 2 + infinity, 0, tmpPaint);
      }
      canvas.restore();
    }

    // Needle
    canvas.save();
    canvas.translate(m_posBase.centerX(), m_posBase.centerY());
    canvas.rotate(m_needleAngleDeg + 90);
    canvas.drawPath(m_pathNeedle, m_paintNeedle);
    canvas.restore();

    // Base
    if (m_touchSelected == 100)
    {
      canvas.drawArc(m_posBase, 0, - 180, false, m_paintBgSelected);
    }
    else
    {
      canvas.drawArc(m_posBase, 0, - 180, false, m_paintBase);
    }
    canvas.drawText((m_beatSelected >= 0 ? "" + (m_beatSelected+1) : "" + m_beatConfig.length),
      m_posBase.centerX(), (2*m_posBase.centerY() + m_posBase.top) / 3, m_paintText);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event)
  {
    double x = event.getX();
    double y = event.getY();
    final double epsilon = 0.001;
    int selected = -1;

    if (event.getAction() != MotionEvent.ACTION_DOWN &&
        event.getAction() != MotionEvent.ACTION_UP &&
        event.getAction() != MotionEvent.ACTION_BUTTON_RELEASE)
    {
      return false;
    }

    // Change center
    x -= m_posBase.centerX();
    y -= m_posBase.centerY();
    y *= -1;

    // Change to polar coordinate
    double radius, theta;
    radius = Math.sqrt(x * x + y * y);
    if (x > epsilon)
    {
      theta = Math.atan (y / x);
    }
    else if (x < epsilon)
    {
      if (y < 0)
        theta = -Math.PI + Math.atan (y / x);
      else
        theta = Math.PI + Math.atan (y / x);
    }
    else if (y > 0) theta = Math.PI;
    else theta = -Math.PI;

    // Get the part
    if (theta > 0 && theta < Math.PI)
    {
      if (radius < m_posBase.width() / 2)
      {
        selected = 100;
      }
      else
      {
        selected = m_beatConfig.length - (int)(Math.ceil(theta*m_beatConfig.length/Math.PI));
      }
    }
    else
    {
      //noinspection ConstantConditions
      selected = -1;
    }

    if (event.getAction() == MotionEvent.ACTION_UP)
    {
      m_touchSelected = -1;
      if (m_touchListener != null && selected >= 0)
      {
        m_touchListener.onTouchArea(selected);
      }
    }
    else
    {
      m_touchSelected = selected;
    }

    if (selected >= 0)
    {
      // Schedules a repaint.
      invalidate();
    }

    return selected >= 0;
  }

  public void setBeatConfig(int[] config)
  {
    m_beatConfig = config;
    if (m_beatConfig.length <= 0)
    {
      m_beatConfig = new int[0];
    }
    m_beatSelected = -1;
    m_needleAngleDeg = -180;
    m_isReverse = false;

    onSizeChanged(getWidth(), getHeight(), getWidth(), getHeight());
    // invalidate();
    // On next loop event
    this.postInvalidate();
  }

  public void setAnimatedAngle(double angleDeg, long durationMs)
  {
    float wantedDegree;
    if (angleDeg > 0) wantedDegree = -(float)angleDeg;
    else wantedDegree = (float)angleDeg;
    if (m_isReverse) wantedDegree = -180 - wantedDegree;

    m_animator.cancel();
    if (durationMs > 0)
    {
      m_animator.setFloatValues(m_needleAngleDeg, wantedDegree);
      m_animator.setDuration(durationMs);
      m_animator.start();
    }
    else
    {
      onAnimated(wantedDegree);
    }
  }

  public float getAngle()
  {
    return m_needleAngleDeg;
  }
  public void setAngle(float angleDeg)
  {
    m_needleAngleDeg = angleDeg;

    this.postInvalidate();
  }

  private void onAnimated(float angle)
  {
    setAngle(angle);
    s_savedAngle = m_needleAngleDeg;
    s_savedIsReverse = m_isReverse;
  }

  public void restoreAnimatedSavedData()
  {
    m_needleAngleDeg = s_savedAngle;
    m_isReverse = s_savedIsReverse;
  }

  public int getBeatSelected()
  {
    return m_beatSelected;
  }
  public void setBeatSelected(int beatSelected)
  {
    m_beatSelected = beatSelected;
    if (beatSelected < 0) m_animator.cancel();

    // invalidate();
    // On next loop event
    this.postInvalidate();
  }

  public boolean getIsReverse()
  {
    return m_isReverse;
  }
  public void setIsReverse(boolean isReverse)
  {
    m_isReverse = isReverse;
    // invalidate();
    // On next loop event
    this.postInvalidate();
  }

  public void setColorMain(int color)
  {
    m_colorMain = color;
    updateColor();
  }

  private void updateColor()
  {
    int colorDark = Utils.darker(m_colorMain);
    int colorLight = Utils.lighter(m_colorMain);

    m_paintBgOff_off.setColor(0xC0000000);
    m_paintBgOff_on.setColor(0x80404040);
    m_paintBgSubdiv_off.setColor(Color.TRANSPARENT);
    m_paintBgSubdiv_on.setColor(Color.GRAY & 0x80FFFFFF);
    m_paintBgNormal_off.setColor(Color.TRANSPARENT);
    m_paintBgNormal_on.setColor(Color.GRAY & 0x80FFFFFF);
    m_paintBgAccent_off.setColor(colorDark | 0x80000000);
    m_paintBgAccent_on.setColor(colorLight);
    m_paintBgSelected.setColor(0x80808080);

    m_paintLineDash.setColor(Utils.lighter(m_colorMain));

    // 0 is East, 0.25 is South
    Shader shader = new SweepGradient(m_posNeedle.centerX(), m_posNeedle.top,
      new int[] {m_colorMain, m_colorMain, colorDark, m_colorMain, m_colorMain},
      new float[] { 0f, 0.25f - 0.1f*m_posNeedle.width()/m_posNeedle.height(), 0.25f, 0.25f, 1.0f } );
    m_paintNeedle.setShader(shader);
  }
}
