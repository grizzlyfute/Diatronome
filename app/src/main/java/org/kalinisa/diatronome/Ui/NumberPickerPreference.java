package org.kalinisa.diatronome.Ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceViewHolder;

import org.kalinisa.diatronome.R;

public class NumberPickerPreference
  extends DialogPreference
{
  int m_value;
  int m_min;
  int m_max;
  int m_defaultValue;

  public NumberPickerPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
  {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context, attrs);
  }

  public NumberPickerPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  public NumberPickerPreference(@NonNull Context context, @Nullable AttributeSet attrs)
  {
    super(context, attrs);
    init(context, attrs);
  }

  public NumberPickerPreference(@NonNull Context context)
  {
    super(context);
    init(context, null);
  }

  @SuppressWarnings("CommentedOutCode")
  private void init(@NonNull Context context, @Nullable AttributeSet attrs)
  {
    // override the widget layout
    // setWidgetLayoutResource(R.layout.pref_number_picker);
    // override the all view
    // setLayoutResource(R.layout.pref_number_picker);

    setDialogLayoutResource(R.layout.uc_number_picker);
    // setDialogIcon(iconId);
    // setDialogIcon(iconId);
    // setDialogTitle("Title");
    // setDialogMessage("Message");
    // setNegativeButtonText ("Cancel");
    // setPositiveButtonText("Ok");

    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference);
    m_min = ta.getInt(R.styleable.NumberPickerPreference_min, 0);
    m_max = ta.getInt(R.styleable.NumberPickerPreference_max, 1000);
    ta.recycle();

    m_defaultValue = 0;
    m_value = m_defaultValue;
  }

  public int getValue()
  {
    // return getPersistedInt(m_defaultValue);
    return m_value;
  }

  public void setValue(int value)
  {
    m_value = value;
    persistInt(value);
    notifyChanged();
  }

  public int getMax()
  {
    return m_max;
  }

  public int getMin()
  {
    return m_min;
  }

  // **** Override from Preferences

  // Called when a Preference is being inflated and the default value attribute needs to be read. Since different
  // Preference types have different value types, the subclass should get and return the default value which will be
  // its value type.
  @Override
  protected @Nullable Object onGetDefaultValue(@NonNull TypedArray ta, int i)
  {
    m_defaultValue = ta.getInt(i, 0);
    return m_defaultValue;
  }

  // Implement this to set the initial value of the Preference.
  @Override
  protected void onSetInitialValue(@Nullable Object defaultValue)
  {
    if (defaultValue instanceof Integer)
    {
      m_value = getPersistedInt((Integer)defaultValue);
    }
    else if (defaultValue instanceof String)
    {
      m_value = getPersistedInt(Integer.parseInt((String)defaultValue));
    }
    else if (defaultValue instanceof Float)
    {
      m_value = getPersistedInt(((Float) defaultValue).intValue());
    }
    else if (defaultValue instanceof Double)
    {
      m_value = getPersistedInt(((Double) defaultValue).intValue());
    }
    else if (defaultValue instanceof Boolean)
    {
      m_value = getPersistedInt((Boolean) defaultValue ? 1 : 0);
    }
    else
    {
      m_value = getPersistedInt(0);
    }

    // Force to store the value, so will be included on load default value
    if (getPreferenceManager() != null && getPreferenceManager().getSharedPreferences() != null &&
      !getPreferenceManager().getSharedPreferences().getAll().containsKey (getKey()))
    {
      setValue (m_value);
    }
  }

  // call order: onGetDefaultValue -> onSetInitialValue -> onBindViewHolder
  @Override
  public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
  {
    super.onBindViewHolder(holder);

    // holder.view
  }
}
