package org.kalinisa.diatronome.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.kalinisa.diatronome.Cores.SettingsCore;
import org.kalinisa.diatronome.R;
import org.kalinisa.diatronome.Ui.ColorChooserPreference;
import org.kalinisa.diatronome.Ui.NumberPickerPreference;
import org.kalinisa.diatronome.Ui.WaveformPickerPreference;

import java.util.Map;
import java.util.Objects;

public class SettingsFragment
  extends PreferenceFragmentCompat
  implements SharedPreferences.OnSharedPreferenceChangeListener
{
  private static final String DIALOG_FRAGMENT_TAG =
    "androidx.preference.PreferenceFragment.DIALOG";
  private static String m_autoScrollToKey = null;

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
    Context context = getContext();
    if (context == null) return;
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    Map<String, ?> allPreferences = sharedPreferences.getAll();

    // The entry.value is the preference value
    for (Map.Entry<String, ?> entry : allPreferences.entrySet())
    {
      // init summary
      preference = findPreference(entry.getKey());
      updateSummary(preference);
    }

    // About
    Preference about = findPreference("setting_about");
    if (about != null)
    {
      about.setOnPreferenceClickListener(pref ->
      {
        showAboutDialog();
        return true;
      });
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);
    // Auto scroll to a specific position
    RecyclerView recyclerView = getListView();

    if (recyclerView != null && m_autoScrollToKey != null)
    {
      recyclerView.post(() ->
      {
        // scrollToPreference(m_autoScrollToKey); will put the preference at bottom. We will it at top
        Preference pref = findPreference(m_autoScrollToKey);
        if (pref == null) return;
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager == null) return;
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        if (adapter == null) return;

        int position = ((PreferenceGroup.PreferencePositionCallback) adapter)
          .getPreferenceAdapterPosition(pref);

        if (position != RecyclerView.NO_POSITION && layoutManager instanceof LinearLayoutManager)
        {
          LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
          // Backward of 1 step to get title
          if (position > 0) position -= 1;
          linearLayoutManager.scrollToPositionWithOffset(position, 0);
        }
      });
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
    else if (preference instanceof WaveformPickerPreference)
    {
      dialogFragment = WaveformPickerFragment.newInstance(preference.getKey(), getContext());
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
      case SettingsCore.SETTING_TUNER_PITCH_REF:
        unit = "Hz";
        break;

      default:
        unit = "";
        break;
    }

    if (!unit.isEmpty())
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
    else if (p instanceof WaveformPickerPreference)
    {
      summary = ((WaveformPickerPreference)p).getEntry();
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

  public void setAutoScrollToKey(String autoScrollToKey)
  {
    m_autoScrollToKey = autoScrollToKey;
  }

  private void showAboutDialog()
  {
    Activity activity = getActivity();
    if (activity == null) return;
    PackageManager manager = activity.getPackageManager();
    if (manager == null) return;
    PackageInfo info = null;
    String version = null;
    try
    {
      info = manager.getPackageInfo(activity.getPackageName(), 0);
      if (info != null)
      {
        version = info.versionName;
      }
    }
    catch (PackageManager.NameNotFoundException ignored) { }

    Context context = getContext();
    if (context == null) return;
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
    alertDialogBuilder.setCancelable(true);
    alertDialogBuilder.setIcon(R.drawable.ic_launcher);
    alertDialogBuilder.setTitle(getResources().getString(R.string.app_name) + " " + version);
    String msg = getString(R.string.about_content, getString(R.string.about_license), getString(R.string.about_source), getString(R.string.about_support));
    alertDialogBuilder.setMessage(HtmlCompat.fromHtml(msg , HtmlCompat.FROM_HTML_MODE_LEGACY));
    alertDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(DialogInterface dialog, int which)
      {
        dialog.cancel();
      }
    });
    AlertDialog alertDialog = alertDialogBuilder.create();
    alertDialog.show();

    // Make link clickable (to call after show());
    TextView view = ((TextView)alertDialog.findViewById(android.R.id.message));
    if (view != null)
    {
      view.setMovementMethod(LinkMovementMethod.getInstance());
    }
  }
}
