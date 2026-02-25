package org.kalinisa.diatronome.Ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceViewHolder;

import org.kalinisa.diatronome.R;

public class WaveformPickerPreference extends DialogPreference
{
  private final String DEF_WAVEFORM = "SINE";
  public String m_value = null;
  public String m_defaultValue = DEF_WAVEFORM;
  private CharSequence[] m_entries = null;
  private CharSequence[] m_entriesValues = null;

  public WaveformPickerPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
  {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context, attrs);
  }

  public WaveformPickerPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  public WaveformPickerPreference(@NonNull Context context, @Nullable AttributeSet attrs)
  {
    super(context, attrs);
    init(context, attrs);
  }

  public WaveformPickerPreference(@NonNull Context context)
  {
    super(context);
    init(context, null);
  }

  private void init(@NonNull Context context, @Nullable AttributeSet attrs)
  {
    // override the widget layout
    // setWidgetLayoutResource(R.layout.pref_waveform_picker);
    // override the all view
    // setLayoutResource(R.layout.pref_waveform_picker);

    setDialogLayoutResource(R.layout.uc_waveform);
    // setDialogIcon(iconId);
    // setDialogIcon(iconId);
    // setDialogTitle("Title");
    // setDialogMessage("Message");
    // setNegativeButtonText ("Cancel");
    // setPositiveButtonText("Ok");

    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.WaveformPickerPreference);
    m_entries = ta.getTextArray(R.styleable.WaveformPickerPreference_entries);
    m_entriesValues = ta.getTextArray(R.styleable.WaveformPickerPreference_entryValues);
    ta.recycle();
  }

  public CharSequence[] getEntryValues()
  {
    return m_entriesValues;
  }

  public CharSequence[] getEntries()
  {
    return m_entries;
  }

  public String getValue()
  {
    return m_value;
  }

  public String getEntry()
  {
    String result = "";
    int i;
    for (i = 0; i < m_entriesValues.length; i++)
    {
      if (m_entriesValues[i].equals(m_value))
      {
        break;
      }
    }
    if (i < m_entriesValues.length && i < m_entries.length)
    {
      result = m_entries[i].toString();
    }
    return result;
  }

  public void setValue(String value)
  {
    m_value = value;
    persistString(value);
    notifyChanged();
  }

  @Override
  protected @Nullable Object onGetDefaultValue(@NonNull TypedArray ta, int i)
  {
    m_defaultValue = ta.getString(i);
    if (m_defaultValue == null) m_defaultValue = DEF_WAVEFORM;
    return m_defaultValue;
  }

  @Override
  protected void onSetInitialValue(@Nullable Object defaultValue)
  {
    if (defaultValue instanceof String)
    {
      m_value = getPersistedString((String)defaultValue);
    }
    else
    {
      m_value = getPersistedString(m_defaultValue);
    }
    // Force to store the value, so will be included on load default value
    if (getPreferenceManager() != null && getPreferenceManager().getSharedPreferences() != null &&
      !getPreferenceManager().getSharedPreferences().getAll().containsKey (getKey()))
    {
      setValue (m_value);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
  {
    super.onBindViewHolder(holder);

    // Do nothing
  }
}
