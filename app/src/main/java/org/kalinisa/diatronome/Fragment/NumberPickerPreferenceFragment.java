package org.kalinisa.diatronome.Fragment;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceDialogFragmentCompat;

import org.kalinisa.diatronome.Ui.NumberPickerDialog;
import org.kalinisa.diatronome.Ui.NumberPickerPreference;

public class NumberPickerPreferenceFragment
  extends PreferenceDialogFragmentCompat
{
  private final String SAVE_STATE_VALUE = NumberPickerPreferenceFragment.class + ".value";
  private final NumberPickerDialog m_numberPickerDialog;

  private NumberPickerPreferenceFragment()
  {
    m_numberPickerDialog = new NumberPickerDialog();
  }

  private NumberPickerPreference getNumberPickerPreference()
  {
    return (NumberPickerPreference)this.getPreference();
  }

  @NonNull
  public static NumberPickerPreferenceFragment newInstance(@NonNull String key)
  {
    final NumberPickerPreferenceFragment fragment = new NumberPickerPreferenceFragment();
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
      // If it is first run after installation - get the default value
      m_numberPickerDialog.setValue(getNumberPickerPreference().getValue());
    }
    else
    {
      // If not - there is a saved value
      m_numberPickerDialog.setValue(savedInstanceState.getInt(SAVE_STATE_VALUE));
    }
  }

  // save the value
  @Override
  public void onSaveInstanceState(@NonNull Bundle outState)
  {
    super.onSaveInstanceState(outState);
    outState.putInt(SAVE_STATE_VALUE, m_numberPickerDialog.getValue());
  }

  @Override
  protected void onBindDialogView(@NonNull View view)
  {
    super.onBindDialogView(view);

    m_numberPickerDialog.setMin(getNumberPickerPreference().getMin());
    m_numberPickerDialog.setMax(getNumberPickerPreference().getMax());
    m_numberPickerDialog.setValue(getNumberPickerPreference().getValue());

    m_numberPickerDialog.initView(view);
  }

  // What to do when the dialog is closed
  @Override
  public void onDialogClosed(boolean positiveResult)
  {
    if (positiveResult)
    {
      getNumberPickerPreference().setValue(m_numberPickerDialog.getValue());
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