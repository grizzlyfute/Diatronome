package org.kalinisa.diatronome.Ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.kalinisa.diatronome.Cores.MetronomeCore;
import org.kalinisa.diatronome.R;

public class TimeSignatureDialog extends DialogFragment
{
  private Spinner m_spinnerDivision = null;
  private Spinner m_spinnerSubDivision = null;
  private NumberPickerDialog m_numberDialog = null;
  private int m_doUpdateListener = 0;

  public TimeSignatureDialog()
  { }

  // dialog = new(); dialog.show();
  public interface TimeSignatureDialogueListener
  {
    void onValidateTimeSig(int[] beatConfig);
  }
  TimeSignatureDialogueListener m_listener;

  public void setonValidateTimeSig(TimeSignatureDialogueListener listener)
  {
    m_listener = listener;
  }

  private int searchItemInSpinner(Spinner spinner, String item)
  {
    int index = -1;
    for (index = 0; index < spinner.getCount(); index++)
    {
      if (spinner.getItemAtPosition(index).toString().equals(item))
      {
        break;
      }
    }
    if (index < spinner.getCount()) return index;
    else return -1;
  }

  private AdapterView.OnItemSelectedListener m_onSpinnerSelected = new AdapterView.OnItemSelectedListener()
  {
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
      // Prevent updating on init
      if (m_doUpdateListener < 2) ++m_doUpdateListener;
      else if (m_listener != null)
      {
        m_listener.onValidateTimeSig(getTimeConfiguration());
      }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
      // Do nothing
    }
  };

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState)
  {
    LayoutInflater inflater = requireActivity().getLayoutInflater();
    View root = inflater.inflate(R.layout.dialog_timesignatures, null);
    initView(root);

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(R.string.metronome_timesignature);
    // builder.setNegativeButton(android.R.string.cancel, null);
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(DialogInterface dialog, int which)
      {
        /* if (m_listener != null)
        {
          m_listener.onValidateTimeSig(getTimeConfiguration());
        } */
      }
    });
    builder.setView(root);
    return builder.create();
  }

  private void initView(View root)
  {
    TableLayout tableBtnTimeSigLayout = root.findViewById(R.id.tlpTimeSignatures);
    for (int i = 0; i < tableBtnTimeSigLayout.getChildCount(); i++)
    {
      if (tableBtnTimeSigLayout.getChildAt(i) instanceof TableRow)
      {
        TableRow row = (TableRow)tableBtnTimeSigLayout.getChildAt(i);
        for (int j = 0; j < row.getChildCount(); j++)
        {
          if (row.getChildAt(j) instanceof Button)
          {
            Button btn = (Button)row.getChildAt(j);
            btn.setOnClickListener(btnClickListener);
          }
        }
      }
    }

    m_doUpdateListener = 0;

    m_spinnerDivision = (Spinner)root.findViewById(R.id.numTimeDivisions);
    if (m_spinnerDivision != null)
    {
      String[] arraySpinner = new String[MetronomeCore.MAX_BEATCONFIG];
      for (int i = 0; i < arraySpinner.length; i++)
      {
        arraySpinner[i] = "" + (i + 1);
      };

      ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_dropdown_item, arraySpinner);
      m_spinnerDivision.setAdapter (adapter);
      int selected = adapter.getPosition("" + MetronomeCore.getInstance().getDivision());
      if (selected < 0) selected = 5;

      m_spinnerDivision.setSelection(selected);
      m_spinnerDivision.setOnItemSelectedListener(m_onSpinnerSelected);

      // Disable auto focus
      //m_spinnerDivision.setFocusable(true);
      //m_spinnerDivision.setFocusableInTouchMode(true);
    }

    m_spinnerSubDivision = (Spinner)root.findViewById(R.id.numTimeSubDivisions);
    if (m_spinnerSubDivision != null)
    {
      String[] arraySpinner = new String[]
      {
        "1", "2", "4", "8", "16",
      };
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_spinner_dropdown_item, arraySpinner);
      m_spinnerSubDivision.setAdapter (adapter);
      int selected = adapter.getPosition("" + MetronomeCore.getInstance().getSubDivision());
      if (selected < 0) selected = 2;

      m_spinnerSubDivision.setSelection(selected);
      m_spinnerSubDivision.setOnItemSelectedListener(m_onSpinnerSelected);

      // Disable auto focus
      //m_spinnerSubDivision.setFocusable(true);
      //m_spinnerSubDivision.setFocusableInTouchMode(true);
    }
  }

  private final View.OnClickListener btnClickListener = new View.OnClickListener()
  {
    @Override
    public void onClick(View v)
    {
      if (v.getTag() instanceof String)
      {
        String tag = (String)v.getTag();
        String[] split = tag.split(":");

        m_doUpdateListener = 0;
        if (m_spinnerDivision != null)
        {
          m_spinnerDivision.setSelection(searchItemInSpinner(m_spinnerDivision, split[0].trim()));
        }

        if (m_spinnerSubDivision != null)
        {
          m_spinnerSubDivision.setSelection(searchItemInSpinner(m_spinnerSubDivision, split[1].trim()));
        }
        m_doUpdateListener = 2;

        if (m_listener != null)
        {
          m_listener.onValidateTimeSig(getTimeConfiguration());
        }
      }
      v.setSelected(true);
    }
  };

  public int getDivision()
  {
    String selectedItem = null;
    if (m_spinnerDivision != null)
    {
      selectedItem = (String)m_spinnerDivision.getSelectedItem();
    }
    if (selectedItem == null) selectedItem = "0";
    return Integer.parseInt(selectedItem);
  }
  public void setDivision(int value)
  {
    if (m_spinnerDivision != null)
    {
      m_spinnerDivision.setSelection(searchItemInSpinner(m_spinnerDivision, "" + value));
    }
  }

  @SuppressWarnings("unused")
  public int getSubDivision()
  {
    String selectedItem = null;
    if (m_spinnerSubDivision != null)
    {
      selectedItem = (String)m_spinnerSubDivision.getSelectedItem();
    }
    if (selectedItem == null) selectedItem = "0";
    return Integer.parseInt(selectedItem);
  }
  public void setSubDivision (int value)
  {
    if (m_spinnerSubDivision != null)
    {
      m_spinnerSubDivision.setSelection(searchItemInSpinner(m_spinnerSubDivision, "" + value));
    }
  }

  public int[] getTimeConfiguration()
  {
    int div = getDivision();
    int subDiv = getSubDivision();
    boolean isHalfTime = false;

    if (subDiv == 2 && div > 3) isHalfTime = true;

    // Irregular
    if (div == 5 && subDiv == 8) subDiv = 3;
    else if (div == 8 && subDiv == 1) subDiv = 3;
    // Normal
    else if (subDiv == 4 || subDiv == 1) subDiv = 1;
    else if (subDiv == 2) subDiv = 2;
    // Tertiary
    else if (subDiv == 8 && (div % 3 == 0)) subDiv = 3;
    // Eight
    else if (subDiv == 8) subDiv = 2;
    else if (subDiv == 16) subDiv = 4;
    // Default
    else subDiv = 1;

    int[] ret = new int[div];
    if (subDiv <= 0) subDiv = 1;
    ret[0] = MetronomeCore.BEATCONFIG_ACCENT;
    for (int i = 1; i < ret.length; i++)
    {
      if (i % subDiv == 0
        // Regroup the last isolated with its comrade
          && (subDiv <= 1 || ret.length - i > 1)
      )
      {
        ret[i] = MetronomeCore.BEATCONFIG_NORMAL;
      }
      else if (isHalfTime)
      {
        ret[i] = MetronomeCore.BEATCONFIG_OFF;
      }
      else
      {
        ret[i] = MetronomeCore.BEATCONFIG_SUBDIV;
      }
    }
    return ret;
  }
}

