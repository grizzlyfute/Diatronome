package org.kalinisa.diatronome.Cores;

import android.content.Context;
import android.content.res.Resources;
import android.text.Spanned;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.text.HtmlCompat;

import org.kalinisa.diatronome.R;

public class UiCore extends BaseCore
{
  public static final int HANDLER_MSG_CHANGE_COLOR = 1;
  public static final int HANDLER_MSG_KEEP_SCREEN_ON = 2;
  public static final int HANDLER_MSG_NOTE_STRING_CHANGE = 3;
  public static final int HANDLER_MSG_HOME_SCREEN_CHANGE = 4;

  private static UiCore s_instance;
  private int m_transposition = 0;
  private boolean m_useSharp = false;
  private int m_noteName = 0;

  private UiCore()
  {
    super();
  }

  public static UiCore getInstance()
  {
    if (s_instance == null)
    {
      s_instance = new UiCore();
    }
    return s_instance;
  }

  public void setThemeNightMode(String themeStrSetting)
  {
    // Do not set the view here by getView().findViewById. DO it in, concerned activity
    // Handle here the theme even not committed
    if ("MODE_NIGHT_YES".equals (themeStrSetting))
    {
      AppCompatDelegate.setDefaultNightMode (AppCompatDelegate.MODE_NIGHT_YES);
    }
    else if ("MODE_NIGHT_NO".equals (themeStrSetting))
    {
      AppCompatDelegate.setDefaultNightMode (AppCompatDelegate.MODE_NIGHT_NO);
    }
    else
    {
      // AppCompatDelegate.setDefaultNightMode (AppCompatDelegate.MODE_NIGHT_UNSPECIFIED);
      AppCompatDelegate.setDefaultNightMode (AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
    // Activity.setTheme() ?
  }

  // Use
  // colorAttr = context.getResources().getIdentifier("colorAccent", "attr", context.getPackageName());
  // context.getTheme().resolveAttribute(appCompatAttribute, outValue, true);
  // for the reverse operation...
  public static int getThemeIdFromColor(Context context, int color)
  {
    Resources resources = context.getResources();
    int themeId;

    if (color == ResourcesCompat.getColor(resources, R.color.color_primary_00, null))
      themeId = R.style.AppTheme_Custom_00;
    else if (color == ResourcesCompat.getColor(resources, R.color.color_primary_01, null))
      themeId = R.style.AppTheme_Custom_01;
    else if (color == ResourcesCompat.getColor(resources, R.color.color_primary_02, null))
      themeId = R.style.AppTheme_Custom_02;
    else if (color == ResourcesCompat.getColor(resources, R.color.color_primary_03, null))
      themeId = R.style.AppTheme_Custom_03;
    else if (color == ResourcesCompat.getColor(resources, R.color.color_primary_04, null))
      themeId = R.style.AppTheme_Custom_04;
    else if (color == ResourcesCompat.getColor(resources, R.color.color_primary_05, null))
      themeId = R.style.AppTheme_Custom_05;
    else if (color == ResourcesCompat.getColor(resources, R.color.color_primary_06, null))
      themeId = R.style.AppTheme_Custom_06;
    else if (color == ResourcesCompat.getColor(resources, R.color.color_primary_07, null))
      themeId = R.style.AppTheme_Custom_07;
    else if (color == ResourcesCompat.getColor(resources, R.color.color_primary_08, null))
      themeId = R.style.AppTheme_Custom_08;
    else
      themeId = R.style.AppTheme_Custom_00;

    return themeId;
  }

  public void setPrimaryColor(int color)
  {
    sendMessage(HANDLER_MSG_CHANGE_COLOR, color, 0);
  }

  public static int getHomeLayoutFormName(String screenName)
  {
    int homeLayout = 0;
    if ("HOME_SCREEN_TUNER".equals(screenName))
    {
      homeLayout = R.layout.fragment_tuner;
    }
    else if ("HOME_SCREEN_METRONOME".equals(screenName))
    {
      homeLayout = R.layout.fragment_metronome;
    }
    else if ("HOME_SCREEN_PLAYNOTE".equals(screenName))
    {
      homeLayout = R.layout.fragment_playnote;
    }
    else
    {
      homeLayout = R.layout.fragment_tuner;
    }
    return homeLayout;
  }

  public void setHomeScreen(String screenName)
  {
    int homeLayout = getHomeLayoutFormName(screenName);
    sendMessage(HANDLER_MSG_HOME_SCREEN_CHANGE, homeLayout, 0);
  }

  public void setKeepScreenOn(boolean isScreenOn)
  {
    sendMessage(HANDLER_MSG_KEEP_SCREEN_ON, isScreenOn ? 1 : 0, 0);
  }

  public void setNoteName(int noteName)
  {
    m_noteName = noteName;
    sendMessage(HANDLER_MSG_NOTE_STRING_CHANGE, noteName, 0);
  }

  public void setUseFlatSharp(boolean useSharp)
  {
    m_useSharp = useSharp;
    sendMessage(HANDLER_MSG_NOTE_STRING_CHANGE, useSharp ? 1 : 0, 0);
  }

  public int getTransposition() { return m_transposition; }
  public void setTransposition(int transposition)
  {
    m_transposition = transposition;
    sendMessage(HANDLER_MSG_NOTE_STRING_CHANGE, transposition, 0);
  }

  public Spanned getNoteName(Resources r, int octave, int note)
  {
    if (note < 0 || note >= 12)
      return HtmlCompat.fromHtml("--", HtmlCompat.FROM_HTML_MODE_LEGACY);
    note = (note - m_transposition + 12) % 12;

    boolean isFlatSharp =
     (note == 1 || note == 3 ||
      note == 6 || note == 8 || note == 10);

    if (isFlatSharp)
    {
      if (m_useSharp) note -= 1;
      else note += 1;
    }
    int index = note / 2;
    if (note >= 5) index += 1;

    StringBuilder sb = new StringBuilder();
    switch (m_noteName)
    {
      case 1:
        sb.append(r.getStringArray(R.array.array_notes_name_solfege)[index]);
        break;

      case 2:
        sb.append(r.getStringArray(R.array.array_notes_name_letters)[index]);
        break;

      default:
        sb.append(r.getStringArray(R.array.array_notes_name_letters)[index]);
        break;
    }

    if (isFlatSharp)
    {
      if (m_useSharp)
      {
        sb.append("&#x266F;");
      }
      else
      {
        sb.append("&#x266D;");
      }
    }

    if (octave > 0)
    {
      sb.append("<small><small>");
      sb.append(octave);
      sb.append("</small></small>");
    }

    return HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY);
  }
}
