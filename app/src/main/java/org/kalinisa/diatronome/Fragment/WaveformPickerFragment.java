package org.kalinisa.diatronome.Fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceDialogFragmentCompat;

import org.kalinisa.diatronome.Cores.PlayNoteCore;
import org.kalinisa.diatronome.Cores.PlayNoteWave;
import org.kalinisa.diatronome.R;
import org.kalinisa.diatronome.Ui.WaveformPickerPreference;

import java.util.Arrays;
import java.util.List;

public class WaveformPickerFragment
  extends PreferenceDialogFragmentCompat
{
  private final String SAVE_STATE_VALUE = WaveformPickerFragment.class + ".value";
  public String m_selectedWaveFromStr;
  private String m_oldWaveformStr = null;
  private CharSequence[] m_waveFormIds = null;
  PlayNoteCore.IAudioAutoStopPlayingListener m_audioAutoStopPlayingListener = null;

  public WaveformPickerFragment()
  {
    // Require public nop arg constructor
  }

  public WaveformPickerFragment(Context context)
  { }

  private WaveformPickerPreference getWaveFormPickerPreference()
  {
    return (WaveformPickerPreference)this.getPreference();
  }

  @NonNull
  public static WaveformPickerFragment newInstance(@NonNull String key, Context context)
  {
    final WaveformPickerFragment fragment = new WaveformPickerFragment(context);
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
      // if it is first run after installation - get the default value
      m_selectedWaveFromStr = getWaveFormPickerPreference().getValue();
    }
    else
    {
      // if not - there is a saved value
      m_selectedWaveFromStr = savedInstanceState.getString(SAVE_STATE_VALUE);
    }
    m_oldWaveformStr = m_selectedWaveFromStr;
  }

  // save the value
  @Override
  public void onSaveInstanceState(@NonNull Bundle outState)
  {
    super.onSaveInstanceState(outState);
    outState.putString(SAVE_STATE_VALUE, m_selectedWaveFromStr);
  }

  @Override
  protected void onBindDialogView(@NonNull View root)
  {
    super.onBindDialogView(root);

    // Resources resources = getResources();

    m_selectedWaveFromStr = getWaveFormPickerPreference().getValue();
    m_oldWaveformStr = m_selectedWaveFromStr;
    PlayNoteCore.getInstance().setWaveForm(m_selectedWaveFromStr);

    // Fill instrument list
    ListView listView = (ListView) root.findViewById(R.id.lstInstruments);
    if (listView != null)
    {
      CharSequence[] waveformName = getWaveFormPickerPreference().getEntries();
      m_waveFormIds = getWaveFormPickerPreference().getEntryValues();
      List<CharSequence> listInstrumentsName = Arrays.asList(waveformName);
      // Caution, listView recycle the view. do not trust arrayAdapter.getView(i, null, listView)
      ArrayAdapter<CharSequence> arrayAdapter = new ArrayAdapter<CharSequence>(requireContext(), R.layout.uc_listviewtext, R.id.txtListView, listInstrumentsName);
      listView.setAdapter(arrayAdapter);
      listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
      {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
          if (position < 0 || position >= m_waveFormIds.length || view == null) return;
          m_selectedWaveFromStr = m_waveFormIds[position].toString();
          PlayNoteCore.getInstance().setWaveForm(m_selectedWaveFromStr);
        }
      });

      for (int i = 0; i < m_waveFormIds.length; i++)
      {
        if (m_waveFormIds[i].equals(m_oldWaveformStr))
        {
          // Set selection (no effect with choice_mode_none)
          listView.setItemChecked(i, true);

          // Scroll to i
          if (i > 3)
            listView.setSelection(i - 3);
          else
            listView.setSelection(0);
          break;
        }
      }
    }

    Button btnWaveformPlay = (Button) root.findViewById(R.id.btnWaveformPlay);
    if (btnWaveformPlay != null)
    {
      btnWaveformPlay.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (!PlayNoteCore.getInstance().getPlayingNoteList().isEmpty())
          {
            PlayNoteCore.getInstance().stopAllPlaying();
            btnWaveformPlay.setBackgroundResource(android.R.drawable.ic_media_play);
          }
          else
          {
            int octave = (int)(Math.random() * 2) + 3;
            int note = (int)(Math.random() * 12);
            PlayNoteCore.getInstance().startPlaying(new PlayNoteWave.PlayNote(octave, note));
            btnWaveformPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
          }
        }
      });

      m_audioAutoStopPlayingListener = new PlayNoteCore.IAudioAutoStopPlayingListener()
      {
        @Override
        public void onStopPlaying(PlayNoteWave.PlayNote note)
        {
          // isVisible return always false
          if (!(isAdded() && !isDetached() && !isHidden())) return;
          if (PlayNoteCore.getInstance().getPlayingNoteList().isEmpty())
          {
            Activity activity = getActivity();
            if (activity != null)
            {
              activity.runOnUiThread(() -> btnWaveformPlay.setBackgroundResource(android.R.drawable.ic_media_play));
            }
          }
        }
      };
      PlayNoteCore.getInstance().addOnAudioStopListener(m_audioAutoStopPlayingListener);
    }
  }

  // What to do when the dialog is closed
  @Override
  public void onDialogClosed(boolean positiveResult)
  {
    PlayNoteCore.getInstance().removeOnAudioStopListener(m_audioAutoStopPlayingListener);
    PlayNoteCore.getInstance().stopAllPlaying();
    PlayNoteCore.getInstance().setWaveForm(m_oldWaveformStr);
    m_audioAutoStopPlayingListener = null;

    // m_thisView.clearFocus();
    if (positiveResult)
    {
      getWaveFormPickerPreference().setValue(m_selectedWaveFromStr);
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