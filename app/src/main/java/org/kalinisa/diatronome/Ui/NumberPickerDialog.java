package org.kalinisa.diatronome.Ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.kalinisa.diatronome.R;

/*
    m_numberDialog = new NumberPickerDialog();
    m_numberDialog.setMin(0);
    m_numberDialog.setMax(100);
    m_numberDialog.setTitle(R.string.title);
    m_numberDialog.setOnValidateListener(new NumberPickerDialog.NumberPickerDialogListener()
    {
      @Override
      public void onValidateNum(int num)
      {
        setData(num);
      }
    });
    Button btn = m_root.findViewById(R.id.buttonId);
    if (btn != null)
    {
      btn.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          m_numberDialog.setValue(InitialValue);
          m_numberDialog.show(getChildFragmentManager(), "tag_dialog_number");
        }
      });
 */

public class NumberPickerDialog extends DialogFragment
{
  public interface NumberPickerDialogListener
  {
    void onValidateNum(int num);
  }
  private int m_value = 0;
  private int m_min = Integer.MIN_VALUE, m_max = Integer.MAX_VALUE;
  private EditText m_viewNumber = null;
  private SeekBar m_seekBar = null;
  private int m_idTitle = 0, m_idIcon = 0;
  NumberPickerDialogListener m_listenerOnValidate = null;

  public NumberPickerDialog()
  {
    super();
  }

  public void setTitle(int idTitle)
  {
    m_idTitle = idTitle;
  }

  public void setIcon(int idIcon)
  {
    m_idIcon = idIcon;
  }

  public void setOnValidateListener(NumberPickerDialogListener listener)
  {
    m_listenerOnValidate = listener;
  }

  public int getValue()
  {
    return m_value;
  }
  @SuppressLint("SetTextI18n")
  public void setValue(int value)
  {
    if (value > m_max)
    {
      value = m_max;
    }
    if (value < m_min)
    {
      value = m_min;
    }
    m_value = value;
    if (m_seekBar != null)
    {
      m_seekBar.setProgress(m_value - m_min);
    }
    if (m_viewNumber != null)
    {
      m_viewNumber.setText("" + m_value);
      m_viewNumber.setSelection(0, m_viewNumber.getText().length());
    }
  }

  public int getMin()
  {
    return m_min;
  }
  public void setMin(int min)
  {
    m_min = min;
    if (m_max > m_min && m_seekBar != null) m_seekBar.setMax(m_max - m_min);
    setValue(m_value);
  }

  public int getMax()
  {
    return m_max;
  }
  public void setMax(int max)
  {
    m_max = max;
    if (m_max > m_min && m_seekBar != null) m_seekBar.setMax(m_max - m_min);
    setValue(m_value);
  }

  // Call on 'Dialog.show'
  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState)
  {
    LayoutInflater inflater = requireActivity().getLayoutInflater();
    View root = inflater.inflate(R.layout.uc_number_picker, null);
    initView(root);

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    if (m_idTitle > 0) builder.setTitle(m_idTitle);
    if (m_idIcon > 0) builder.setIcon(m_idIcon);
    builder.setNegativeButton(android.R.string.cancel, null);
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(DialogInterface dialog, int which)
      {
        if (which == DialogInterface.BUTTON_POSITIVE && m_listenerOnValidate != null)
        {
          m_listenerOnValidate.onValidateNum(getValue());
        }
      }
    });
    builder.setView(root);
    Dialog dialog = builder.create();
    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    return dialog;
  }

  @SuppressLint("SetTextI18n")
  public void initView(@NonNull View root)
  {
    // Inflate the layout to use as a dialog or embedded fragment.
    m_viewNumber = root.findViewById(R.id.txtNumPicker);
    m_seekBar = root.findViewById(R.id.skbNumberPicker);
    if (m_viewNumber == null || m_seekBar == null)
    {
      throw new IllegalStateException("Incomplete layout");
    }

    m_viewNumber.setText("" + m_value);
    m_viewNumber.addTextChangedListener(new TextWatcher()
    {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after)
      { }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        try
        {
          m_value = Integer.parseInt(m_viewNumber.getText().toString());
        }
        catch (java.lang.NumberFormatException e)
        {
          m_value = 0;
        }
        if (m_value < m_min)
        {
          m_value = m_min;
          m_seekBar.setProgress(0);
        }
        else if (m_value > m_max)
        {
          m_value = m_max;
          m_seekBar.setProgress(m_seekBar.getMax());
        }
        else
        {
          m_seekBar.setProgress(m_value - m_min);
        }
      }

      @Override
      public void afterTextChanged(Editable s)
      { }
    });

    // Auto select view text
    m_viewNumber.setSelection(0, m_viewNumber.getText().length());

    // Auto start virtual keyboard.
    if (m_viewNumber.requestFocus())
    {
      Activity activity = getActivity();
      if (activity != null)
      {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        // imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        imm.showSoftInput(m_viewNumber, InputMethodManager.SHOW_IMPLICIT);
      }
    }

    m_seekBar.setMax(m_max - m_min);
    m_seekBar.setProgress(m_value - m_min);
    m_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
    {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
      {
        if (fromUser)
        {
          m_value = m_min + progress;
          setValue (m_value);
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar)
      {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar)
      {
      }
    });

    Button btnSub = root.findViewById(R.id.btnNumPickerSub);
    if (btnSub != null)
    {
      btnSub.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          setValue(m_value - 1);
        }
      });
    }

    Button btnAdd = root.findViewById(R.id.btnNumPickerAdd);
    if (btnAdd != null)
    {
      btnAdd.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          setValue(m_value + 1);
        }
      });
    }
  }

  public static void hideSoftKeyboard(Activity activity)
  {
    if (activity != null)
    {
      InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
      View view = activity.findViewById(android.R.id.content);
      imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
  }

  @Override
  public void onDismiss(DialogInterface dialog)
  {
    if (m_viewNumber != null)
    {
      m_viewNumber.clearFocus();
    }
    hideSoftKeyboard(getActivity());
    super.onDismiss(dialog);
  }
}
