package org.kalinisa.diatronome.Fragment;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import org.kalinisa.diatronome.Cores.SettingsCore;
import org.kalinisa.diatronome.Cores.UiCore;
import org.kalinisa.diatronome.R;
import org.kalinisa.diatronome.Ui.NeedleView;
import org.kalinisa.diatronome.Ui.NumberPickerDialog;
import org.kalinisa.diatronome.databinding.FragmentTunerBinding;

public class TunerFragment extends Fragment
{
  private FragmentTunerBinding m_binding;
  private AlertDialog.Builder m_transpositionDialog;
  private NumberPickerDialog m_pitchSettingDialog = null;

    @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState
  )
  {
    m_binding = FragmentTunerBinding.inflate(inflater, container, false);

    m_transpositionDialog = new AlertDialog.Builder(getContext());
    m_transpositionDialog.setIcon(R.drawable.icon_transposition);
    m_transpositionDialog.setTitle(R.string.preference_transposition);

    m_pitchSettingDialog = new NumberPickerDialog();
    // Duplicated with settings min/max
    m_pitchSettingDialog.setMin(250);
    m_pitchSettingDialog.setMax(630);
    m_pitchSettingDialog.setValue (
      PreferenceManager.getDefaultSharedPreferences(getContext()).getInt (SettingsCore.SETTING_PITCH_REF, 0));
    m_pitchSettingDialog.setIcon(R.drawable.icon_pitch);
    m_pitchSettingDialog.setTitle(R.string.preference_pitch);
    m_pitchSettingDialog.setOnValidateListener(new NumberPickerDialog.NumberPickerDialogListener()
    {
      @Override
      public void onValidateNum(int num)
      {
        SettingsCore.updateSettingFromUi (getContext(), SettingsCore.SETTING_PITCH_REF, num);
      }
    });

    Button btnNote = m_binding.getRoot().findViewById(R.id.btnNote);
    if (btnNote != null)
    {
      btnNote.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          m_transpositionDialog.setSingleChoiceItems(R.array.array_transposition, (UiCore.getInstance().getTransposition() + 12)%12, new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
              String[] transpositionData = getResources().getStringArray(R.array.array_transposition_data);
              //noinspection StatementWithEmptyBody
              if (which == DialogInterface.BUTTON_POSITIVE ||
                  which == DialogInterface.BUTTON_NEGATIVE ||
                  which == DialogInterface.BUTTON_NEUTRAL)
              {
                // Ignore
              }
              else if (which >= 0 && which < transpositionData.length)
              {
                String newTransposition = transpositionData[which];
                SettingsCore.updateSettingFromUi (getContext(), SettingsCore.SETTING_TRANSPOSITION, newTransposition);
                dialog.cancel();
              }
              else
              {
                android.util.Log.e(getString(R.string.app_name), "Out of bound transposition");
              }
            }
          });

          m_transpositionDialog.show();
        }
      });
    }

    Button btnMeasure = m_binding.getRoot().findViewById(R.id.btnMeasure);
    if (btnNote != null)
    {
      btnMeasure.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          // DialogFragment.show() will take care of adding the fragment
          // in a transaction.  We also want to remove any currently showing
          // dialog, so make our own transaction and take care of that here.
          FragmentTransaction ft = getChildFragmentManager().beginTransaction();
          Fragment prev = getChildFragmentManager().findFragmentByTag("dialog");
          if (prev != null) {
            ft.remove(prev);
          }
          ft.addToBackStack(null);

          // Create and show the dialog.
           m_pitchSettingDialog.show(ft, "dialog");
        }
      });
    }

    NeedleView needleView = m_binding.getRoot().findViewById(R.id.viewNeedle);
    if (needleView != null)
    {
      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
      needleView.setColorMain(sharedPreferences.getInt(SettingsCore.SETTING_COLOR, needleView.getColorMain()));
    }

    return m_binding.getRoot();
  }

  public void onViewCreated(@NonNull View view, Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);
  }

  @Override
  public void onDestroyView()
  {
    super.onDestroyView();
    m_binding = null;
  }
}
