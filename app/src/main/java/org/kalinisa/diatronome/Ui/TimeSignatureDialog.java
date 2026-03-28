package org.kalinisa.diatronome.Ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.AttributeSet;
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

import java.util.Objects;

public class TimeSignatureDialog extends DialogFragment
{
  public static class SameSelectSpinner extends androidx.appcompat.widget.AppCompatSpinner
  {
    public SameSelectSpinner(Context context) { super(context); }
    public SameSelectSpinner(Context context, AttributeSet attrs) { super(context, attrs); }
    public SameSelectSpinner(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }

    @Override
    public void setSelection(int position, boolean animate) {
      boolean sameSelected = position == getSelectedItemPosition();
      super.setSelection(position, animate);
      if (sameSelected) {
        // Spinner does not call the OnItemSelectedListener if the same item is selected, so do it manually now
        Objects.requireNonNull(getOnItemSelectedListener()).onItemSelected(this, getSelectedView(), position, getSelectedItemId());
      }
    }

    @Override
    public void setSelection(int position) { setSelection(position, false); }
  }

  private Spinner m_spinnerDivision = null;
  private Spinner m_spinnerSubDivision = null;
  private Button m_btnTimeConfiguration = null;
  private int m_ignoreListenerCallNb = 0;

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

  private final AdapterView.OnItemSelectedListener m_onSpinnerSelected = new AdapterView.OnItemSelectedListener()
  {
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
      // Prevent updating on init
      if (m_ignoreListenerCallNb > 0) --m_ignoreListenerCallNb;
      else updateTimeConfiguration(true);
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
        // Do nothing
      }
    });
    builder.setView(root);
    return builder.create();
  }

  private void initView(View root)
  {
    Context context = getContext();
    if (context == null) return;

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

    m_ignoreListenerCallNb = 2;

    m_spinnerDivision = (Spinner)root.findViewById(R.id.numTimeDivisions);
    if (m_spinnerDivision != null)
    {
      String[] arraySpinner = new String[MetronomeCore.MAX_BEATCONFIG];
      for (int i = 0; i < arraySpinner.length; i++)
      {
        arraySpinner[i] = "" + (i + 1);
      };

      ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, arraySpinner);
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
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, arraySpinner);
      m_spinnerSubDivision.setAdapter (adapter);
      int selected = adapter.getPosition("" + MetronomeCore.getInstance().getSubDivision());
      if (selected < 0) selected = 2;

      m_spinnerSubDivision.setSelection(selected);
      m_spinnerSubDivision.setOnItemSelectedListener(m_onSpinnerSelected);

      // Disable auto focus
      //m_spinnerSubDivision.setFocusable(true);
      //m_spinnerSubDivision.setFocusableInTouchMode(true);
    }

    m_btnTimeConfiguration = (Button)root.findViewById(R.id.btnTimeConfigurations);
    if (m_btnTimeConfiguration != null)
    {
      m_btnTimeConfiguration.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          // Rotate configuration by sending the same
          updateTimeConfiguration(true);
        }
      });
    }

    updateTimeConfiguration(false);
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

        m_ignoreListenerCallNb = 0;
        // Disable listener to not call three time getTimeConfiguration().
        // Loop event, so disabling member will not work because listener executed after
        if (m_spinnerDivision != null)
        {
          m_ignoreListenerCallNb++;
          m_spinnerDivision.setSelection(searchItemInSpinner(m_spinnerDivision, split[0].trim()));
        }

        if (m_spinnerSubDivision != null)
        {
          m_ignoreListenerCallNb++;
          m_spinnerSubDivision.setSelection(searchItemInSpinner(m_spinnerSubDivision, split[1].trim()));
        }

        updateTimeConfiguration(true);
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

  private int m_lastDivHash = 0;
  private int m_lastDivCnt = 0;
  private void updateTimeConfiguration(boolean cyclingIrregular)
  {
    // Cycling config if irregular
    if (cyclingIrregular)
    {
      int hash = getDivision() * 128 + getSubDivision();
      if (m_lastDivHash != hash)
      {
        m_lastDivCnt = 0;
        m_lastDivHash = hash;
      }
      else
      {
        m_lastDivCnt++;
      }
    }

    // Update Ui
    if (m_listener != null)
    {
      int[] timeConfiguration = getTimeConfiguration();
      m_listener.onValidateTimeSig(timeConfiguration);
      m_btnTimeConfiguration.setText (timeConfigurationToString(timeConfiguration));
    }
  }
  public int[] getTimeConfiguration()
  {
    int div = getDivision();
    int subDiv = getSubDivision();
    int cnt;
    int i;
    boolean isHalfTime = false;
    int[] ret = null;

    if (subDiv == 2 && div > 3) isHalfTime = true;
    // Irregular
    if (div == 5 && subDiv == 8) subDiv = 3;
    else if (div == 8 && subDiv == 1) subDiv = 3;
    // Particular
    else if (div == 1 && subDiv >= 4) subDiv = 2;
    // Normal
    else if (subDiv == 4 || subDiv == 1) subDiv = 1;
    else if (subDiv == 2) subDiv = 2;
    // Eight tertiary
    else if (subDiv == 8 && (div % 3 == 0)) subDiv = 3;
    // Eight binary
    else if (subDiv == 8) subDiv = 2;
    // Sixteen
    else if (subDiv == 16) subDiv = 4;
    // Default
    else subDiv = 1;

    ret = new int[div];
    for (i = 0; i < ret.length; i++)
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

    // Cycle irregulars
    cnt = 0;
    for (i = 0; i < ret.length; i++)
    {
      if (ret[i] == MetronomeCore.BEATCONFIG_NORMAL) cnt++;
    }
    if (cnt > 0)
    {
      cnt = m_lastDivCnt % cnt;
    }
    for (i = 0; i < ret.length && cnt >= 0; i++)
    {
      if (ret[i] == MetronomeCore.BEATCONFIG_NORMAL)
      {
        cnt--;
      }
    }
    if (cnt <= 0 && i < ret.length && i > 0)
    {
      i -= 1;
      int[] temp = new int[ret.length];
      System.arraycopy(ret, i, temp, 0, ret.length - i);
      System.arraycopy(ret, 0, temp, ret.length - i, i);
      ret = temp;
    }

    // Set the first beat as accent
    ret[0] = MetronomeCore.BEATCONFIG_ACCENT;

    return ret;
  }

  private void timeConfigurationToStringAppend(StringBuilder builder, int cnt, int mult, boolean forceMultiplier)
  {
    if (mult > 4 || (forceMultiplier && mult > 1))
    {
      builder.append(cnt);
      builder.append("×");
      builder.append(mult);
      builder.append("+");
    }
    else
    {
      while (mult > 0)
      {
        builder.append(cnt);
        builder.append("+");
        --mult;
      }
    }
  }

  private String timeConfigurationToString(int[] beatConfig)
  {
    int i = 0, j = 0;
    int cnt = 0;
    int multiplier = 1;
    int last = -1;
    boolean forceMultiplier = beatConfig.length > 16;
    StringBuilder builder = new StringBuilder();
    for (i = 0; i < beatConfig.length; i++)
    {
      for (j = i + 1; j < beatConfig.length; j++)
      {
        if (beatConfig[j] == MetronomeCore.BEATCONFIG_NORMAL ||
            beatConfig[j] == MetronomeCore.BEATCONFIG_ACCENT)
        {
          break;
        }
      }
      cnt = j - i;
      i = j - 1;

      if (cnt == last)
      {
        multiplier += 1;
      }
      else if (last > 0)
      {
        timeConfigurationToStringAppend(builder, last, multiplier, forceMultiplier);
        multiplier = 1;
      }
      last = cnt;
    }
    timeConfigurationToStringAppend(builder, cnt, multiplier, forceMultiplier);
    builder.deleteCharAt(builder.length() - 1);
    if (builder.length() == 1)
    {
      builder.append ("+...+");
      builder.append(builder.charAt(0));
    }

    return builder.toString();
  }
}
