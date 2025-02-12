package com.tuner.diatronome.Fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.TwoStatePreference;

import com.tuner.diatronome.Cores.SettingsCore;
import com.tuner.diatronome.R;
import com.tuner.diatronome.Ui.ColorChooserPreference;
import com.tuner.diatronome.Ui.NumberPickerPreference;

import java.util.Map;
import java.util.Objects;

public class SettingsFragment
  extends PreferenceFragmentCompat
  implements SharedPreferences.OnSharedPreferenceChangeListener
{
  private static final String DIALOG_FRAGMENT_TAG =
    "androidx.preference.PreferenceFragment.DIALOG";

  public SettingsFragment()
  { }

  @Override
  public void onResume()
  {
    super.onResume();
    // Several Listener may be assigned (and should unassigned on pause)
    Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onPause()
  {
    Objects.requireNonNull(getPreferenceManager().getSharedPreferences()).unregisterOnSharedPreferenceChangeListener(this);
    super.onPause();
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
  {
    setPreferencesFromResource(R.xml.preferences, rootKey);

    // Object implementing Preference.OnPreferenceChangeListener may not be called on custom preference
    // when register with setOnPreferenceChangeListener. (onValue about to change)
    // Prefer SharedPreferences.OnSharedPreferenceChangeListener which is working better

    Preference preference = null;

    // Read all
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
    Map<String, ?> allPreferences = sharedPreferences.getAll();

    // The entry.value is the preference value
    for (Map.Entry<String, ?> entry : allPreferences.entrySet())
    {
      // init summary
      preference = findPreference(entry.getKey());
      updateSummary(preference);
    }
  }

  @SuppressWarnings("deprecation")
  private void showFragmentAsDialog(DialogFragment dialogFragment)
  {
    FragmentManager fragmentManager = getParentFragmentManager();
    fragmentManager.setFragmentResultListener("resultKey", this, new FragmentResultListener()
    {
      @Override
      public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundleResult)
      {
        @SuppressWarnings("unused") String result = bundleResult.getString("resultKey");
      }
    });

    //noinspection deprecation
    dialogFragment.setTargetFragment(this, 0);
    // fragmentManager.putFragment(preference.getExtras(), DIALOG_FRAGMENT_TAG, dialogFragment);
    dialogFragment.show(fragmentManager, DIALOG_FRAGMENT_TAG);
  }

  @Override
  public void onDisplayPreferenceDialog(@NonNull Preference preference)
  {
    // Check if dialog is already showing
    if (getParentFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null)
    {
      return;
    }

    DialogFragment dialogFragment;
    if (preference instanceof ColorChooserPreference)
    {
      dialogFragment = ColorChooserPreferenceFragment.newInstance(preference.getKey(), getContext());
    }
    else if (preference instanceof NumberPickerPreference)
    {
      dialogFragment = NumberPickerPreferenceFragment.newInstance(preference.getKey());
    }
    else
    {
      dialogFragment = null;
    }

    if (dialogFragment != null)
    {
      showFragmentAsDialog(dialogFragment);
    }
    else
    {
      super.onDisplayPreferenceDialog(preference);
    }
  }

  private String getPreferenceUnit(Preference p)
  {
    String unit = "";
    if (p == null) return unit;
    String key = p.getKey();
    //noinspection SwitchStatementWithTooFewBranches
    switch (key)
    {
      case SettingsCore.SETTING_PITCH_REF:
        unit = "Hz";
        break;

      default:
        unit = "";
        break;
    }

    if (!"".equals(unit))
    {
      unit = " " + unit.trim();
    }

    return unit;
  }

  @SuppressLint("DefaultLocale")
  private void updateSummary(Preference p)
  {
    String summary = "";

    if (p instanceof ListPreference)
    {
      CharSequence entry = ((ListPreference)p).getEntry();
      if (entry != null)
      {
        summary = String.format("%s", entry);
      }
    }
    // CheckBoxPreferences, SwitchPreference
    else if (p instanceof TwoStatePreference)
    {
      summary = String.format(((TwoStatePreference)p).isChecked() ?
        getString(R.string.common_yes) :
        getString(R.string.common_no));
    }
    else if (p instanceof ColorChooserPreference)
    {
      summary = String.format("#%08x", ((ColorChooserPreference)p).getValue());
    }
    else if (p instanceof NumberPickerPreference)
    {
      summary = String.format("%d", ((NumberPickerPreference)p).getValue());
    }
    else if (p instanceof SeekBarPreference)
    {
      summary = String.format("%d", ((SeekBarPreference)p).getValue());
    }
    else if (p instanceof EditTextPreference)
    {
      summary = ((EditTextPreference)p).getText();
    }
    else
    {
      summary = "??";
    }

    if (p != null)
    {
      summary += getPreferenceUnit(p);
      p.setSummary(summary);
    }
  }

  // When preference committed
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
  {
    // android.util.Log.d("Preference", "onSharedPreferenceChanged " + key);

    SettingsCore.analyzePreference(sharedPreferences, key);

    Preference preference = findPreference(key);
    if (preference != null)
    {
      updateSummary(preference);
    }
  }
}
