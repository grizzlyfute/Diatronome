package org.kalinisa.diatronome.Fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import org.kalinisa.diatronome.Cores.MetronomeCore;
import org.kalinisa.diatronome.Cores.MetronomePlaybackService;
import org.kalinisa.diatronome.Cores.SettingsCore;
import org.kalinisa.diatronome.R;
import org.kalinisa.diatronome.Ui.MetronomeView;
import org.kalinisa.diatronome.Ui.TimeSignatureDialog;
import org.kalinisa.diatronome.databinding.FragmentMetronomeBinding;

public class MetronomeFragment extends Fragment
{
  private final TimeSignatureDialog m_timeSignatureDialog;

  View m_root;
  MetronomeView m_metronomeView;
  View.OnFocusChangeListener m_txtBpmFocusListener;

  public MetronomeFragment()
  {
    m_root = null;
    m_timeSignatureDialog = new TimeSignatureDialog();
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
  }

  static int getNumbers(String s)
  {
    int result = 0;
    String[] n = s.split("");
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < n.length; i++)
    {
      if((n[i].matches("[0-9]+"))) sb.append(n[i]);
      else if (sb.length() > 0) break;
    }
    if (sb.length() > 0)
    {
      result = Integer.parseInt(sb.toString());
    }
    return result;
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState)
  {
    // Inflate the layout for this fragment
    m_root = FragmentMetronomeBinding.inflate(inflater, container, false).getRoot();
    m_metronomeView = m_root.findViewById(R.id.viewMetronome);

    m_timeSignatureDialog.setonValidateTimeSig(new TimeSignatureDialog.TimeSignatureDialogueListener()
    {
      @Override
      public void onValidateTimeSig(int[] beatConfig)
      {
        if (m_metronomeView != null)
        {
          m_metronomeView.setBeatConfig(beatConfig);
        }
        MetronomeCore.getInstance().setBeatsConfig(beatConfig);
        SettingsCore.updateSettingFromUi(getContext(), SettingsCore.SETTING_METRONOME_BEATSIG, beatSigToPref(beatConfig));
      }
    });

    Button btnBeats = m_root.findViewById(R.id.btnMetronomeBeat);
    if (btnBeats != null)
    {
      btnBeats.setOnClickListener(new View.OnClickListener()
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
          // ft.commit() is already call (?)

          // Create and show the dialog.
          m_timeSignatureDialog.show(ft, "dialog");
        }
      });
    }

    Button btnSub = m_root.findViewById(R.id.btnMetronomeSub);
    if (btnSub != null)
    {
      btnSub.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          setTempo(MetronomeCore.getInstance().getTempoBpm() - 1);
        }
      });
    }

    Button btnAdd = m_root.findViewById(R.id.btnMetronomeAdd);
    if (btnAdd != null)
    {
      btnAdd.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          setTempo(MetronomeCore.getInstance().getTempoBpm() + 1);
        }
      });
    }

    EditText txtBpm = m_root.findViewById(R.id.btnMetronomeTempo);
    if (txtBpm != null)
    {
      txtBpm.setOnEditorActionListener(new TextView.OnEditorActionListener()
      {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
        {
          boolean ret = false;
          switch (actionId)
          {
            case EditorInfo.IME_ACTION_DONE:
            case EditorInfo.IME_ACTION_NEXT:
            case EditorInfo.IME_ACTION_PREVIOUS:
              setTempo(getNumbers(v.getText().toString()));
              v.clearFocus();
              v.setCursorVisible(false);

              // the setOnFocusChangeListener prevent keyboard to automatically show
              InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
              imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

              ret = true;
              break;
          }
          return ret;
        }
      });

      txtBpm.setOnFocusChangeListener(new View.OnFocusChangeListener()
      {
        @SuppressLint("SetTextI18n")
        @Override
        public void onFocusChange(View v, boolean hasFocus)
        {
          EditText textView = (EditText) v;
          if (hasFocus)
          {
            textView.setText("" + getNumbers(textView.getText().toString()));
            textView.setSelection(0, textView.getText().length());
            textView.setCursorVisible(true);

            // the setOnFocusChangeListener prevent keyboard to automatically show
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(v, 0);
          }
        }
      });
    }

    Button btnPlay = m_root.findViewById(R.id.btnMetronomePlay);
    if (btnPlay != null)
    {
      btnPlay.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (MetronomeCore.getInstance().getIsPlaying())
          {
            // MetronomeCore.getInstance().stop();
            getActivity().stopService(new Intent(getActivity(), MetronomePlaybackService.class));
          }
          else
          {
            // MetronomeCore.getInstance().play();
            getActivity().startService(new Intent(getActivity(), MetronomePlaybackService.class));
          }
        }
      });
    }

    SeekBar skbTempo = m_root.findViewById(R.id.skbMetronomeTempo);
    if (skbTempo != null)
    {
      skbTempo.setMax (MetronomeCore.BPM_MAX - MetronomeCore.BPM_MIN);
      skbTempo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            setTempo(progress + MetronomeCore.BPM_MIN);
          }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar)
        { }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar)
        { }
      });
    }

    m_metronomeView.setMetronomeViewTouchListener(new MetronomeView.MetronomeViewTouchListener()
    {
      @Override
      public void onTouchArea(int num)
      {
        // Center
        if (num == 100)
        {
          MetronomeCore.getInstance().tap();
        }
        else
        {
          int[] beatConfig = MetronomeCore.getInstance().getBeatsConfig();
          if (num < beatConfig.length)
          {
            beatConfig[num] = beatConfig[num] - 1;
            if (beatConfig[num] < MetronomeCore.BEATCONFIG_OFF)
              beatConfig[num] = MetronomeCore.BEATCONFIG_ACCENT;
            MetronomeCore.getInstance().setBeatsConfig(beatConfig);
          }
        }
      }
    });

    // Update value
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
    setColorMain(sharedPreferences.getInt(SettingsCore.SETTING_COLOR, Color.DKGRAY));
    prefFromBeatSig(sharedPreferences.getString(SettingsCore.SETTING_METRONOME_BEATSIG, "4_3333"));
    setPlay(MetronomeCore.getInstance().getIsPlaying());
    setTempo(sharedPreferences.getInt(SettingsCore.SETTING_METRONOME_BPM, 60));
    if (MetronomeCore.getInstance().getIsPlaying())
    {
      int tickMax = MetronomeCore.getInstance().getDivision();
      int currentTick = MetronomeCore.getInstance().getCurrentTick();

      m_metronomeView.restoreAnimatedSavedData();
      m_metronomeView.setAnimatedAngle(-180f * (tickMax - currentTick - 1) / tickMax, MetronomeCore.getInstance().getPeriodMs());
      m_metronomeView.setBeatSelected(currentTick);
    }

    return m_root;
  }

  public void setPlay(boolean isPlaying)
  {
    TextView btnPlay = m_root.findViewById(R.id.btnMetronomePlay);
    if (btnPlay != null)
    {
      if (isPlaying)
      {
        btnPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
      }
      else
      {
        btnPlay.setBackgroundResource(android.R.drawable.ic_media_play);
        setTick(-1);
      }
    }
  }

  @SuppressLint("SetTextI18n")
  public void setTempo(int bpm)
  {
    if (bpm < MetronomeCore.BPM_MIN)
    {
      bpm = MetronomeCore.BPM_MIN;
    }
    else if (bpm > MetronomeCore.BPM_MAX)
    {
      bpm = MetronomeCore.BPM_MAX;
    }

    MetronomeCore.getInstance().setTempoBpm(bpm);
    SettingsCore.updateSettingFromUi(getContext(), SettingsCore.SETTING_METRONOME_BPM, bpm);

    TextView texTempo = m_root.findViewById(R.id.btnMetronomeTempo);
    if (texTempo != null)
    {
      texTempo.setText ("" + bpm + " bpm");
    }
    SeekBar skbTempo = m_root.findViewById(R.id.skbMetronomeTempo);
    if (skbTempo != null)
    {
      skbTempo.setProgress(bpm - MetronomeCore.BPM_MIN);
    }

    TextView txtTempoName = m_root.findViewById(R.id.txtMetronomeTempoName);
    if (txtTempoName != null)
    {
      if (bpm < 24)
        txtTempoName.setText("Larghissimo");
      else if (bpm < 40)
        txtTempoName.setText("Adagissimo");
      else if (bpm < 50)
        txtTempoName.setText("Largo");
      else if (bpm < 52)
        txtTempoName.setText("Lento");
      else if (bpm < 56)
        txtTempoName.setText("Larghetto");
      else if (bpm < 66)
        txtTempoName.setText("Adagio");
      else if (bpm < 70)
        txtTempoName.setText("Adagietto");
      else if (bpm < 78)
        txtTempoName.setText("Andante");
      else if (bpm < 84)
        txtTempoName.setText("Andantino");
      else if (bpm < 86)
        txtTempoName.setText("Marcia moderato");
      else if (bpm < 98)
        txtTempoName.setText("Moderato");
      else if (bpm < 110)
        txtTempoName.setText("Allegretto");
      else if (bpm < 133)
        txtTempoName.setText("Allegro");
      else if (bpm < 140)
        txtTempoName.setText("Vivace");
      else if (bpm < 150)
        txtTempoName.setText("Vivacissimo");
      else if (bpm < 168)
        txtTempoName.setText("Allegrissimo");
      else if (bpm < 178)
        txtTempoName.setText("Presto");
      else if (bpm < 200)
        txtTempoName.setText("Prestissimo");
      else if (bpm < 250)
        txtTempoName.setText("Rapido");
      else
        txtTempoName.setText("Veloce");
    }
  }

  public void setColorMain(int color)
  {
    if (m_metronomeView != null)
    {
      m_metronomeView.setColorMain(color);
    }
  }

  public void setTick(int tickNb)
  {
    final int tickMax = MetronomeCore.getInstance().getDivision();
    final long periodMs = MetronomeCore.getInstance().getPeriodMs();
    // Occurs if tickMax change
    if (tickNb > tickMax) tickNb = tickMax - 1;

    if (tickNb >= 0)
    {
      if (tickNb <= 0 && (m_metronomeView.getBeatSelected() > 0 || tickMax <= 1))
      {
        if (MetronomeCore.getInstance().getIsBidirectionalNeedle())
        {
          m_metronomeView.setIsReverse(!m_metronomeView.getIsReverse());
        }
        else
        {
          // skip reverse animated
          m_metronomeView.setAnimatedAngle(-180.0f, 0);
        }
      }
      m_metronomeView.setAnimatedAngle(-180.0f * (tickMax - tickNb - 1) / tickMax, periodMs);
    }
    else
    {
      m_metronomeView.setIsReverse(false);
      m_metronomeView.setAnimatedAngle(-180, 0);
    }
    m_metronomeView.setBeatSelected(tickNb);
  }

  private void prefFromBeatSig(String beatStr)
  {
    int len = beatStr.length();
    if (len > MetronomeCore.MAX_BEATCONFIG) len = MetronomeCore.MAX_BEATCONFIG;
    int[] beatConfig = new int[len];
    int subdiv = 0;
    int subdivMax = 0;
    for (int i = 0; i < len; i++)
    {
      beatConfig[i] = beatStr.charAt(i) - '0';
      if (beatConfig[i] == MetronomeCore.BEATCONFIG_SUBDIV) subdiv++;
      else subdiv = 0;
      if (subdiv > subdivMax)
        subdivMax = subdiv;
    }

    MetronomeCore.getInstance().setBeatsConfig(beatConfig);
    if (m_metronomeView != null)
    {
      m_metronomeView.setBeatConfig(beatConfig);
    }
    m_timeSignatureDialog.setDivision(len);
    m_timeSignatureDialog.setSubDivision(subdiv);
  }

  private String beatSigToPref(int[] beatConfig)
  {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < beatConfig.length; i++)
    {
      sb.append(beatConfig[i]);
    }
    return sb.toString();
  }
}