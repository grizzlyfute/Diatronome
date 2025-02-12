package com.tuner.diatronome.Fragment;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.tuner.diatronome.Cores.Utils;
import com.tuner.diatronome.R;
import com.tuner.diatronome.Ui.ColorChooserPreference;

import java.util.HashMap;
import java.util.Map;

public class ColorChooserPreferenceFragment
  extends PreferenceDialogFragmentCompat
{
  private final String SAVE_STATE_VALUE = ColorChooserPreferenceFragment.class + ".value";
  private final HashMap<Integer, Integer> m_viewIdColor;
  private int m_selectedColor;
  ViewGroup m_layout;

  private final int[] m_colorList;

  public ColorChooserPreferenceFragment()
  {
    // Require public nop arg constructor
    m_colorList = new int[9];
    m_viewIdColor = new HashMap<Integer, Integer>();
    m_selectedColor = 0;
    m_layout = null;
  }

  public ColorChooserPreferenceFragment(Context context)
  {
    m_viewIdColor = new HashMap<Integer, Integer>();
    m_selectedColor = 0;
    m_layout = null;

    // Get theme attributes
    Resources resources = context.getResources();
    // Color by colum. [1] = row1, col0; [3] = row0, col3
    m_colorList = new int[]
    {
      // Column 0
      ResourcesCompat.getColor(resources, R.color.color_primary_00, null),
      ResourcesCompat.getColor(resources, R.color.color_primary_01, null),
      ResourcesCompat.getColor(resources, R.color.color_primary_02, null),

      // Column 1
      ResourcesCompat.getColor(resources, R.color.color_primary_03, null),
      ResourcesCompat.getColor(resources, R.color.color_primary_04, null),
      ResourcesCompat.getColor(resources, R.color.color_primary_05, null),

      // Column 2
      ResourcesCompat.getColor(resources, R.color.color_primary_06, null),
      ResourcesCompat.getColor(resources, R.color.color_primary_07, null),
      ResourcesCompat.getColor(resources, R.color.color_primary_08, null),
    };
  }

  private ColorChooserPreference getColorChooserPreference()
  {
    return (ColorChooserPreference)this.getPreference();
  }

  @NonNull
  public static ColorChooserPreferenceFragment newInstance(@NonNull String key, Context context)
  {
    final ColorChooserPreferenceFragment fragment = new ColorChooserPreferenceFragment(context);
    final Bundle args = new Bundle(1);
    args.putString(ARG_KEY, key);
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null)
    {
      // if it is first run after installation - get the default value
      m_selectedColor = getColorChooserPreference().getValue();
    }
    else
    {
      // if not - there is a saved value
      m_selectedColor = savedInstanceState.getInt(SAVE_STATE_VALUE);
    }
  }

  // save the value
  @Override
  public void onSaveInstanceState(@NonNull Bundle outState)
  {
    super.onSaveInstanceState(outState);
    outState.putInt(SAVE_STATE_VALUE, m_selectedColor);
  }

  @Override
  protected void onBindDialogView(@NonNull View view)
  {
    super.onBindDialogView(view);

    m_selectedColor = getColorChooserPreference().getValue();
    m_layout = view.findViewById(R.id.pref_color_button_layout);
    if (m_layout == null)
    {
      throw new IllegalStateException("Dialog view must contain a layout pref_color_button_layout with id");
    }

    int selected = -1;
    int color;
    View.OnClickListener listener = new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        selectOne(v.getId());
      }
    };

    int index = 0;
    for (int i = 0; i < m_layout.getChildCount(); i++)
    {
      View item = m_layout.getChildAt(i);
      if (!(item instanceof Button)) continue;
      // m_colorList is transposed
      color = m_colorList[(index / 3) + (index % 3) * 3] | 0xFF000000;
      if (color == m_selectedColor)
      {
        selected = item.getId();
      }
      m_viewIdColor.put (Integer.valueOf(item.getId()), color);
      item.setOnClickListener(listener);
      index++;
    }

    selectOne(selected);
  }

  private void selectOne(int selectedId)
  {
    float radius = 0;
    View item = null;
    int color = 0;
    Integer id = null;
    final Resources r = getResources();

    m_selectedColor = 0;

    // Unselected all unless the clicked view
    for (Map.Entry<Integer, Integer> entry : m_viewIdColor.entrySet())
    {
      id = entry.getKey();
      if (m_viewIdColor.containsKey(id))
      {
        color = m_viewIdColor.get(id).intValue();
      }

      item = m_layout.findViewById(id.intValue());
      if (item == null) continue;
      if (radius < 1)
      {
        item.measure(
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
          View.MeasureSpec.makeMeasureSpec(item.getRootView().getHeight(), View.MeasureSpec.EXACTLY));
        radius = item.getMeasuredWidth() / 2.0f;
      }

      // Do not reuse
      GradientDrawable shape = new GradientDrawable();
      shape.mutate();
      shape.setShape(GradientDrawable.RECTANGLE);
      shape.setCornerRadii(new float[] { radius, radius, radius, radius, radius, radius, radius, radius });
      shape.setStroke(3, Color.DKGRAY);
      shape.setGradientType(GradientDrawable.LINEAR_GRADIENT);
      shape.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);

      shape.setColors(new int[] { color, (color & 0xCCFFFFFF)});

      if (id.intValue() == selectedId)
      {
        m_selectedColor = entry.getValue();
        shape.setStroke((int) Utils.dpToPixels(r, 8), Color.LTGRAY);
        item.setSelected(true);
      }
      else
      {
        shape.setStroke((int)Utils.dpToPixels(r,4), Color.DKGRAY);
        item.setSelected(false);
      }
      item.setBackground(shape);
      // item.invalidate();
    }
  }

  // What to do when the dialog is closed
  @Override
  public void onDialogClosed(boolean positiveResult)
  {
    // m_thisView.clearFocus();
    if (positiveResult)
    {
      getColorChooserPreference().setValue(m_selectedColor);
    }
  }

  // https://issuetracker.google.com/issues/181793702
  // https://stackoverflow.com/questions/69504256/preferencedialog-in-preferenceactivity-target-fragment-must-implement-targetfr
  @Override
  @SuppressWarnings("deprecation")
  public void setTargetFragment(@Nullable Fragment fragment, int requestCode) {
    super.setTargetFragment(fragment, requestCode);
  }
}