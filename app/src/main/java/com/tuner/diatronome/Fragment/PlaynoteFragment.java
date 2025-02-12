package com.tuner.diatronome.Fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.tuner.diatronome.Cores.PlayNoteCore;
import com.tuner.diatronome.Cores.SettingsCore;
import com.tuner.diatronome.Cores.UiCore;
import com.tuner.diatronome.R;
import com.tuner.diatronome.databinding.FragmentPlaynoteBinding;

/*
 */
public class PlaynoteFragment extends Fragment
{
  private FragmentPlaynoteBinding m_binding;
  private int m_colorMiddle;
  private int m_octave;
  private int m_note = 0;

  public PlaynoteFragment()
  {
    super();
    m_octave = 4;
    m_note = 9; // A
    m_colorMiddle = Color.GRAY;
  }

  @SuppressLint({"SetTextI18n", "DefaultLocale"})
  private boolean togglePlaying()
  {
    TextView freqTxt = m_binding.getRoot().findViewById(R.id.playTxtCurrentFreq);
    boolean isPlaying = PlayNoteCore.getInstance().isPlaying(m_octave, m_note);
    if (!isPlaying)
    {
      freqTxt.setText(
        UiCore.getInstance().getNoteName(getContext().getResources(), m_octave, m_note) +
          " // " +
          String.format("%.1f", PlayNoteCore.getInstance().getFrequency(m_octave, m_note)) + " Hz");
      PlayNoteCore.getInstance().startPlaying(m_octave, m_note);
    }
    else
    {
      freqTxt.setText ("--");
      PlayNoteCore.getInstance().stopPlaying();
    }
    return !isPlaying;
  }

  private final View.OnClickListener m_onPianoClick = new View.OnClickListener()
  {
    @Override
    public void onClick(View v)
    {
      Object tag = v.getTag(R.id.playNoteTag);
      if (tag instanceof String)
      {
        m_note = Integer.parseInt(tag.toString());
        boolean isPlaying = togglePlaying();
        checkInGroup (v, isPlaying);
      }
    }
  };

  private final View.OnClickListener m_onOctaveClick = new View.OnClickListener()
  {
    @Override
    public void onClick(View v)
    {
      Object tag = v.getTag(R.id.playOctaveTag);
      if (tag instanceof String)
      {
        boolean isPlaying = PlayNoteCore.getInstance().isPlaying(m_octave, m_note);
        int octave = Integer.parseInt(tag.toString());
        checkInGroup (v, true);
        //noinspection StatementWithEmptyBody
        if (isPlaying && octave != m_octave)
        {
          m_octave = octave;
          // Change the current octave
          isPlaying = togglePlaying();
        }
        else
        {
          // Same octave. Do nothing
        }
      }
    }
  };

  private void pianoAddClick(int viewId)
  {
    View v = m_binding.getRoot().findViewById(viewId);
    if (v != null)
    {
      v.setOnClickListener(m_onPianoClick);
    }
  }

  private void octaveAddClick(int viewId)
  {
    View v = m_binding.getRoot().findViewById(viewId);
    if (v != null)
    {
      v.setOnClickListener(m_onOctaveClick);
    }
  }

  private void checkInGroup(View view, boolean isChecked)
  {
    if (m_binding == null) return;
    if (!(view.getParent() instanceof ViewGroup)) return;

    ViewGroup group = (ViewGroup)view.getParent();
    // Uncheck all
    for (int i = 0; i < group.getChildCount(); i++)
    {
      if (!(group.getChildAt(i) instanceof Button)) continue;
      Button widget = (Button)group.getChildAt(i);
      Drawable d = widget.getBackground();
      if (d != null)
      {
        d.clearColorFilter();
        widget.setBackground(d);
      }
      else
      {
        widget.setBackgroundColor(0x00000000);
      }
      widget.invalidate();
    }

    // Check the concerned
    if (isChecked)
    {
      Drawable d = view.getBackground();
      if (d != null)
      {
        ColorFilter cf = new PorterDuffColorFilter(m_colorMiddle, PorterDuff.Mode.SRC);
        d.setColorFilter(cf);
        view.setBackground(d);
      }
      else
      {
        view.setBackgroundColor(m_colorMiddle);
      }
      view.invalidate();
    }
  }

  @Override
  public View onCreateView(
    LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState
  )
  {
    m_binding = FragmentPlaynoteBinding.inflate(inflater, container, false);

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
    m_colorMiddle = sharedPreferences.getInt(SettingsCore.SETTING_COLOR, Color.GRAY);

    pianoAddClick(R.id.playBtn0);
    pianoAddClick(R.id.playBtn1);
    pianoAddClick(R.id.playBtn2);
    pianoAddClick(R.id.playBtn3);
    pianoAddClick(R.id.playBtn4);
    pianoAddClick(R.id.playBtn5);
    pianoAddClick(R.id.playBtn6);
    pianoAddClick(R.id.playBtn7);
    pianoAddClick(R.id.playBtn8);
    pianoAddClick(R.id.playBtn9);
    pianoAddClick(R.id.playBtn10);
    pianoAddClick(R.id.playBtn11);

    octaveAddClick(R.id.playBtnOctave0);
    octaveAddClick(R.id.playBtnOctave1);
    octaveAddClick(R.id.playBtnOctave2);
    octaveAddClick(R.id.playBtnOctave3);
    octaveAddClick(R.id.playBtnOctave4);
    octaveAddClick(R.id.playBtnOctave5);
    octaveAddClick(R.id.playBtnOctave6);
    octaveAddClick(R.id.playBtnOctave7);
    octaveAddClick(R.id.playBtnOctave8);

    m_octave = 4;
    checkInGroup(m_binding.getRoot().findViewById(R.id.playBtnOctave4), true);

    updateNoteName();

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

  private void updateSingleNote(int viewId)
  {
    TextView v = m_binding.getRoot().findViewById(viewId);
    if (v != null)
    {
      Object tag = v.getTag(R.id.playNoteTag);
      if (tag instanceof String)
      {
        int note = Integer.parseInt(tag.toString());
        // Do note passe m_octave to not overload
        v.setText(UiCore.getInstance().getNoteName(getResources(), -1, note));
      }
    }
  }

  public void updateNoteName()
  {
    updateSingleNote(R.id.playBtn0);
    updateSingleNote(R.id.playBtn1);
    updateSingleNote(R.id.playBtn2);
    updateSingleNote(R.id.playBtn3);
    updateSingleNote(R.id.playBtn4);
    updateSingleNote(R.id.playBtn5);
    updateSingleNote(R.id.playBtn6);
    updateSingleNote(R.id.playBtn7);
    updateSingleNote(R.id.playBtn8);
    updateSingleNote(R.id.playBtn9);
    updateSingleNote(R.id.playBtn10);
    updateSingleNote(R.id.playBtn11);
  }
}