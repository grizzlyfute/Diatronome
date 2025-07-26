package org.kalinisa.diatronome.Fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import org.kalinisa.diatronome.Cores.PlayNoteCore;
import org.kalinisa.diatronome.Cores.PlayNoteWave;
import org.kalinisa.diatronome.Cores.SettingsCore;
import org.kalinisa.diatronome.Cores.UiCore;
import org.kalinisa.diatronome.R;
import org.kalinisa.diatronome.databinding.FragmentPlaynoteBinding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*
 */
public class PlaynoteFragment extends Fragment
{
  private FragmentPlaynoteBinding m_binding;
  private int m_colorMiddle;
  private int[] m_octave;
  final private int[] m_pianoId;

  public PlaynoteFragment()
  {
    super();
    m_pianoId = new int[2];
    m_pianoId[0] = R.id.pianoA;
    m_pianoId[1] = R.id.pianoB;
    m_octave = new int[m_pianoId.length];
    for (int i = 0; i < m_octave.length; i++)
    {
      m_octave[i] = i + 3;
    }
    m_colorMiddle = Color.GRAY;
  }

  @SuppressLint({"SetTextI18n", "DefaultLocale"})
  private void updatePlayingNoteText ()
  {
    Collection<PlayNoteWave.PlayNote> playNoteList = PlayNoteCore.getInstance().getPlayingNoteList();
    Collection<View> pianoKeys;
    TextView freqTxt;
    StringBuilder text;
    View pianoView;
    int countForThisPianoView;

    if (getContext() == null) return;

    for (int pianoInd = 0; pianoInd < m_pianoId.length; pianoInd++)
    {
      pianoView = m_binding.getRoot().findViewById(m_pianoId[pianoInd]);
      text = new StringBuilder();

      countForThisPianoView = 0;
      for (PlayNoteWave.PlayNote playNote : playNoteList)
      {
        if (m_octave[pianoInd] == playNote.getOctave())
        {
          countForThisPianoView++;
        }
      }

      if (countForThisPianoView <= 0)
      {
        text.append("--");
      }

      for (PlayNoteWave.PlayNote playNote : playNoteList)
      {
        if (m_octave[pianoInd] == playNote.getOctave())
        {
          if (countForThisPianoView == 1)
          {
            text.append (UiCore.getInstance().getNoteName(getContext().getResources(), playNote.getOctave(), playNote.getNote()));
            text.append (" // ");
            text.append (String.format("%.1f", PlayNoteCore.getInstance().getFrequency(playNote.getOctave(), playNote.getNote())));
            text.append (" Hz");
          }
          else
          {
            if (text.length() > 0) text.append(" - ");
            text.append(UiCore.getInstance().getNoteName(getContext().getResources(), playNote.getOctave(), playNote.getNote()));
          }
        }
      }

      freqTxt = pianoView.findViewById(R.id.playTxtCurrentFreq);
      freqTxt.setText(text.toString()) ;
    }
  }

  private void startPlaying(PlayNoteWave.PlayNote playNote)
  {
    Collection<View> listPianoKey = getPianoKeysByNote(playNote);
    for (View pianoTouch : listPianoKey)
    {
      check(pianoTouch, true);
    }
    PlayNoteCore.getInstance().startPlaying(playNote);
    updatePlayingNoteText();
  }

  private void stopPlaying(PlayNoteWave.PlayNote playNote)
  {
    Collection<View> listPianoKey = getPianoKeysByNote(playNote);
    for (View pianoTouch : listPianoKey)
    {
      check(pianoTouch, false);
    }
    PlayNoteCore.getInstance().stopPlaying(playNote);
    updatePlayingNoteText();
  }

  private void stopAllPlaying()
  {
    for (PlayNoteWave.PlayNote playNote : PlayNoteCore.getInstance().getPlayingNoteList())
    {
      stopPlaying(playNote);
    }
  }

  private Collection<View> getPianoKeysByNote(PlayNoteWave.PlayNote playNote)
  {
    View root = m_binding.getRoot();
    View pianoView = null;
    ViewGroup group = null;
    List<View> result = new ArrayList<View>();
    for (int pianoInd = 0; pianoInd < m_pianoId.length; pianoInd++)
    {
      if (m_octave[pianoInd] == playNote.getOctave())
      {
        pianoView = root.findViewById(m_pianoId[pianoInd]);
        group = pianoView.findViewById(R.id.playLayPiano);
        for (int i = 0; i < group.getChildCount(); i++)
        {
          if (!(group.getChildAt(i) instanceof Button)) continue;
          Button widget = (Button)group.getChildAt(i);
          Object tag = widget.getTag(R.id.playNoteTag);
          if (tag instanceof String)
          {
            int note = Integer.parseInt(tag.toString());
            if (note == playNote.getNote())
            {
              result.add (widget);
              break;
            }
          }
        }
      }
    }
    return result;
  }

  private int getPianoIndOfView(View v)
  {
    int octaveIndex = -1;
    View pianoView = null;
    View candidateView = null;
    View root = m_binding.getRoot();

    for (int pianoInd = 0;
         pianoInd < m_pianoId.length && octaveIndex < 0;
         pianoInd++)
    {
      pianoView = root.findViewById(m_pianoId[pianoInd]);
      candidateView = pianoView.findViewById(v.getId());
      if (candidateView == v)
      {
        octaveIndex = pianoInd;
        break;
      }
    }

    return octaveIndex;
  }

  private final View.OnTouchListener m_onPianoClick = new View.OnTouchListener()
  {
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
      int pianoMode = PlayNoteCore.getInstance().getPianoMode();
      int pianoInd = getPianoIndOfView(v);
      boolean ret = false;
      boolean isPlaying = false;
      Object tag = v.getTag(R.id.playNoteTag);
      if (tag instanceof String && pianoInd >= 0)
      {
        PlayNoteWave.PlayNote newNote = new PlayNoteWave.PlayNote(m_octave[pianoInd], Integer.parseInt(tag.toString()));
        // if note is note continuous, it will stop naturally playing and the button never go relaxed
        if (!PlayNoteCore.getInstance().isNoteContinuous())
        {
          // To be removed, do auto uncheck or force user to disable key
          if (pianoMode == PlayNoteCore.PLAY_MODE_STICKY)
            pianoMode = PlayNoteCore.PLAY_MODE_VOLATILE;
        }

        switch(event.getAction())
        {
          case MotionEvent.ACTION_DOWN:
            isPlaying = PlayNoteCore.getInstance().isPlaying(newNote);
            // If note is note continuous, the button still enforced. Stop any way
            stopPlaying(newNote);
            if (!isPlaying)
            {
              startPlaying(newNote);
            }
            ret = true;
            break;

          case MotionEvent.ACTION_UP:
            if (pianoMode == PlayNoteCore.PLAY_MODE_STICKY)
            {
              // Do nothing
            }
            else if (pianoMode == PlayNoteCore.PLAY_MODE_VOLATILE)
            {
              stopPlaying(newNote);
            }
            ret = true;
            break;

          default:
            ret = false;
            break;
        }
      }
      return ret;
    }
  };

  private final View.OnTouchListener m_onOctaveClick = new View.OnTouchListener()
  {
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
      int pianoMode = PlayNoteCore.getInstance().getPianoMode();
      int pianoInd = getPianoIndOfView(v);
      boolean ret = false;
      boolean isPlaying = false;
      Object tag = v.getTag(R.id.playOctaveTag);

      if (tag instanceof String && pianoInd >= 0)
      {
        int octave = Integer.parseInt(tag.toString());
        switch(event.getAction())
        {
          case MotionEvent.ACTION_DOWN:
            if (octave != m_octave[pianoInd])
            {
              m_octave[pianoInd] = octave;
              Collection<PlayNoteWave.PlayNote> currentNoteList = PlayNoteCore.getInstance().getPlayingNoteList();
              stopAllPlaying();
              for (PlayNoteWave.PlayNote playNote : currentNoteList)
              {
                for (int i = 0; i < m_octave.length; i++)
                {
                  if (m_octave[i] != octave && m_octave[i] == playNote.getOctave() &&
                    pianoMode == PlayNoteCore.PLAY_MODE_STICKY)
                  {
                    startPlaying(new PlayNoteWave.PlayNote(m_octave[i], playNote.getNote()));
                  }
                  else if (m_octave[i] == octave)
                  {
                    startPlaying(new PlayNoteWave.PlayNote(octave, playNote.getNote()));
                  }
                }
              }
            }
            checkInGroup(v, true);
            ret = true;
            break;

          case MotionEvent.ACTION_UP:
            ret = true;
            break;

          default:
            ret = false;
            break;
        }
      }
      return ret;
    }
  };

  private void pianoAddClick(int pianoId, int viewId)
  {
    View pianoView = m_binding.getRoot().findViewById(pianoId);
    if (pianoView != null)
    {
      View v = pianoView.findViewById(viewId);
      if (v != null)
      {
        v.setOnTouchListener(m_onPianoClick);
      }
    }
  }

  private void octaveAddClick(int pianoId, int viewId)
  {
    View pianoView = m_binding.getRoot().findViewById(pianoId);
    if (pianoView != null)
    {
      View v = pianoView.findViewById(viewId);
      if (v != null)
      {
        v.setOnTouchListener(m_onOctaveClick);
      }
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
      check (widget, false);
    }

    if (isChecked)
    {
      check (view, true);
    }
  }

  private void check(View view, boolean isChecked)
  {
    if (m_binding == null) return;

    Drawable d = view.getBackground();
    if (isChecked)
    {
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
    }
    else
    {
      if (d != null)
      {
        d.clearColorFilter();
        view.setBackground(d);
      }
      else
      {
        view.setBackgroundColor(0x00000000);
      }
    }
    view.invalidate();
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

    int pianoId = 0;
    View pianoView = null;
    for (int index = 0; index < m_pianoId.length; index++)
    {
      pianoId = m_pianoId[index];

      pianoAddClick(pianoId, R.id.playBtn0);
      pianoAddClick(pianoId, R.id.playBtn1);
      pianoAddClick(pianoId, R.id.playBtn2);
      pianoAddClick(pianoId, R.id.playBtn3);
      pianoAddClick(pianoId, R.id.playBtn4);
      pianoAddClick(pianoId, R.id.playBtn5);
      pianoAddClick(pianoId, R.id.playBtn6);
      pianoAddClick(pianoId, R.id.playBtn7);
      pianoAddClick(pianoId, R.id.playBtn8);
      pianoAddClick(pianoId, R.id.playBtn9);
      pianoAddClick(pianoId, R.id.playBtn10);
      pianoAddClick(pianoId, R.id.playBtn11);

      octaveAddClick(pianoId, R.id.playBtnOctave0);
      octaveAddClick(pianoId, R.id.playBtnOctave1);
      octaveAddClick(pianoId, R.id.playBtnOctave2);
      octaveAddClick(pianoId, R.id.playBtnOctave3);
      octaveAddClick(pianoId, R.id.playBtnOctave4);
      octaveAddClick(pianoId, R.id.playBtnOctave5);
      octaveAddClick(pianoId, R.id.playBtnOctave6);
      octaveAddClick(pianoId, R.id.playBtnOctave7);
      octaveAddClick(pianoId, R.id.playBtnOctave8);

      m_octave[index] = index + 3;

      pianoView = m_binding.getRoot().findViewById(pianoId);
      if (pianoView != null)
      {
        ViewGroup group = pianoView.findViewById(R.id.playOctaves);
        for (int j = 0; j < group.getChildCount(); j++)
        {
          if (!(group.getChildAt(j) instanceof Button)) continue;
          Button widget = (Button)group.getChildAt(j);
          Object tag = widget.getTag(R.id.playOctaveTag);
          if (tag instanceof String &&  ("" + m_octave[index]).equals((String)tag))
          {
            checkInGroup(widget, true);
          }
        }
      }
    }

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

  private void updateSingleNote(int pianoId, int viewId)
  {
    View pianoView = m_binding.getRoot().findViewById(pianoId);
    TextView v = null;
    if (pianoView != null)
    {
      v = pianoView.findViewById(viewId);
    }
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
    int pianoId = 0;
    for (int i = 0; i < m_pianoId.length; i++)
    {
      pianoId = m_pianoId[i];
      updateSingleNote(pianoId, R.id.playBtn0);
      updateSingleNote(pianoId, R.id.playBtn1);
      updateSingleNote(pianoId, R.id.playBtn2);
      updateSingleNote(pianoId, R.id.playBtn3);
      updateSingleNote(pianoId, R.id.playBtn4);
      updateSingleNote(pianoId, R.id.playBtn5);
      updateSingleNote(pianoId, R.id.playBtn6);
      updateSingleNote(pianoId, R.id.playBtn7);
      updateSingleNote(pianoId, R.id.playBtn8);
      updateSingleNote(pianoId, R.id.playBtn9);
      updateSingleNote(pianoId, R.id.playBtn10);
      updateSingleNote(pianoId, R.id.playBtn11);
    }
  }
}
