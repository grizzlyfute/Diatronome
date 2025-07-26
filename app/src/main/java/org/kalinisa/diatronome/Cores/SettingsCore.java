package org.kalinisa.diatronome.Cores;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class SettingsCore extends BaseCore
{
  public static final String SETTING_THEME = "setting_theme";
  public static final String SETTING_SCREEN_ON = "setting_screen_on";
  public static final String SETTING_COLOR = "setting_color";
  public static final String SETTING_HOME_SCREEN = "setting_home_screen";

  public static final String SETTING_NOTE_NAME = "setting_note_name";
  public static final String SETTING_USE_SHARP_FLAT = "setting_use_sharp_flat";
  public static final String SETTING_PITCH_REF = "setting_pitch_ref";
  public static final String SETTING_WAVEFORM = "setting_waveform";
  public static final String SETTING_PIANOTOUCH = "setting_pianotouch";
  public static final String SETTING_TRANSPOSITION = "setting_transposition";
  public static final String SETTING_TEMPERAMENT = "setting_temperament";

  public static final String SETTING_METRONOME_PITCH_MAIN = "setting_metronome_pitch_main";
  public static final String SETTING_METRONOME_PITCH_ACCENT = "setting_metronome_pitch_accent";
  public static final String SETTING_METRONOME_PITCH_SUBDIVISION = "setting_metronome_pitch_subdivision";
  public static final String SETTING_METRONOME_BIDIRECTIONNAL = "setting_metronome_bidirectionalneedle";
  public static final String SETTING_METRONOME_BPM = "setting_metronome_bpm";

  public static final String SETTING_SOUNDANALYZE_THRESHOLD = "setting_soundanalyze_threshold";
  public static final String SETTING_SOUNDANALYZE_ALGORITHM = "setting_soundanalyze_algorithm";

  public static final String SETTING_METRONOME_BEATSIG = "setting_metronome_beatsig";
  public static final String SETTING_VERSION = "setting_version";

  private static SettingsCore s_instance = null;
  private SettingsCore()
  { }

  public static SettingsCore getInstance()
  {
    if (s_instance == null)
    {
      s_instance = new SettingsCore();
    }
    return s_instance;
  }

  static public void analyzePreference(SharedPreferences sharedPreferences, String key)
  {
    String strValue;
    int intValue;
    float floatValue;
    boolean boolValue;

    try
    {
      switch (key)
      {
        case SettingsCore.SETTING_THEME:
          strValue = sharedPreferences.getString(key, "");
          UiCore.getInstance().setThemeNightMode(strValue);
          break;

        case SettingsCore.SETTING_SCREEN_ON:
          boolValue = sharedPreferences.getBoolean(key, false);
          UiCore.getInstance().setKeepScreenOn(boolValue);
          break;

        case SettingsCore.SETTING_COLOR:
          intValue = sharedPreferences.getInt(key, 0);
          UiCore.getInstance().setPrimaryColor(intValue);
          break;

        case SettingsCore.SETTING_HOME_SCREEN:
          strValue = sharedPreferences.getString(key, "");
          UiCore.getInstance().setHomeScreen(strValue);
          break;

        case SettingsCore.SETTING_NOTE_NAME:
          strValue = sharedPreferences.getString(key, "");
          UiCore.getInstance().setNoteName(Integer.parseInt(strValue));
          break;

        case SettingsCore.SETTING_USE_SHARP_FLAT:
          boolValue = sharedPreferences.getBoolean(key, false);
          UiCore.getInstance().setUseFlatSharp(boolValue);
          PlayNoteCore.getInstance().setUseFlatSharp(boolValue);
          break;

        case SettingsCore.SETTING_PITCH_REF:
          intValue = sharedPreferences.getInt(key, 0);
          PlayNoteCore.getInstance().setRefPitch(intValue);
          MetronomeCore.getInstance().setRefPitch(intValue);
          break;

        case SettingsCore.SETTING_TRANSPOSITION:
          intValue = Integer.parseInt(sharedPreferences.getString(key, "0"));
          UiCore.getInstance().setTransposition(intValue);
          break;

        case SettingsCore.SETTING_TEMPERAMENT:
          intValue = Integer.parseInt(sharedPreferences.getString(key, "0"));
          PlayNoteCore.getInstance().setTemperament(intValue);
          break;

        case SettingsCore.SETTING_WAVEFORM:
          strValue = sharedPreferences.getString(key, "");
          PlayNoteCore.getInstance().setWaveForm(strValue);
          MetronomeCore.getInstance().setWaveForm(strValue); // After PlayNoteCore
          break;

        case SettingsCore.SETTING_PIANOTOUCH:
          intValue = Integer.parseInt(sharedPreferences.getString(key, "1"));
          PlayNoteCore.getInstance().setPianoMode(intValue);
          break;

        case SettingsCore.SETTING_METRONOME_PITCH_MAIN:
          intValue = sharedPreferences.getInt(key, 0);
          MetronomeCore.getInstance().setPitchMainSkb(intValue);
          break;

        case SettingsCore.SETTING_METRONOME_PITCH_ACCENT:
          intValue = sharedPreferences.getInt(key, 0);
          MetronomeCore.getInstance().setPitchAccentSkb(intValue);
          break;

        case SettingsCore.SETTING_METRONOME_PITCH_SUBDIVISION:
          intValue = sharedPreferences.getInt(key, 0);
          MetronomeCore.getInstance().setPitchSubdivisionSkb(intValue);
          break;

        case SettingsCore.SETTING_METRONOME_BIDIRECTIONNAL:
          boolValue = sharedPreferences.getBoolean(key, false);
          MetronomeCore.getInstance().setIsBidirectionalNeedle(boolValue);
          break;

        case SettingsCore.SETTING_METRONOME_BPM:
          intValue = sharedPreferences.getInt(key, 0);
          // Not monitored
          break;

        case SettingsCore.SETTING_METRONOME_BEATSIG:
          strValue = sharedPreferences.getString(key, "");
          // Not monitored
          break;

        case SettingsCore.SETTING_SOUNDANALYZE_THRESHOLD:
          intValue = sharedPreferences.getInt(key, 0);
          SoundAnalyzeCore.getInstance().setThreshold(intValue);
          break;

        case SettingsCore.SETTING_SOUNDANALYZE_ALGORITHM:
          strValue = sharedPreferences.getString(key, "0");
          SoundAnalyzeCore.getInstance().setAlgo(Integer.parseInt(strValue));
          break;

        case SettingsCore.SETTING_VERSION:
          // Not monitored - for future use/Schema update
          break;

        default:
          android.util.Log.w(SettingsCore.getInstance().getClass().getName(), "Unknown preference \"" + key + "\"");
          break;
      }
    }
    catch (NumberFormatException e)
    {
      // Just ignore this option
      android.util.Log.w(SettingsCore.getInstance().getClass().getName(), "Invalid preference \"" + key + "\"");
    }
  }

  public static void updateSettingFromUi(Context context, @NonNull String key, Object newValue)
  {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    if (newValue instanceof Integer)
    {
      editor.putInt(key, ((Integer)newValue).intValue());
    }
    else if (newValue instanceof String)
    {
      editor.putString(key, (String)newValue);
    }
    else if (newValue instanceof Float)
    {
      editor.putFloat(key, ((Float)newValue).floatValue());
    }
    else if (newValue instanceof Boolean)
    {
      editor.putBoolean(key, ((Boolean)newValue).booleanValue());
    }
    editor.apply();
    // editor.commit();

    // Force preference changed event. The setting fragment is not loaded.
    analyzePreference(sharedPreferences, key);
  }
}
