package org.kalinisa.diatronome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import org.kalinisa.diatronome.Cores.SettingsCore;
import org.kalinisa.diatronome.Cores.UiCore;
import org.kalinisa.diatronome.Fragment.SettingsFragment;

import java.util.Map;

public class SettingsActivity extends AppCompatActivity
{
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    // Apply theme (before setContentView)
    int primaryColor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).
      getInt(SettingsCore.SETTING_COLOR, 0);
    int themeId = UiCore.getThemeIdFromColor(getApplicationContext(), primaryColor);
    getTheme().applyStyle(themeId, true);
    // setTheme(R.style.AppTheme_Custom_xx);

    setContentView(R.layout.activity_settings);

    getSupportFragmentManager()
      .beginTransaction()
      // or android.R.id.Content to have the view root element ?
      .replace(R.id.settings_container, new SettingsFragment())
      //.replace(android.R.id.content, new SettingsFragment ())
      .commit();

    Toolbar toolbar = findViewById(R.id.settings_toolbar);
    if (toolbar != null)
    {
      setSupportActionBar(toolbar);
    }

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null)
    {
      actionBar.setTitle(" " + getString(R.string.action_settings));
      actionBar.setLogo(R.drawable.menu_settings);
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setDisplayShowHomeEnabled(true);
      actionBar.setBackgroundDrawable(new ColorDrawable(primaryColor));
    }
  }

  public static void applyAllSettings(Context context)
  {
    SettingsFragment settingsFragment = new SettingsFragment();

    // Read all
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    Map<String, ?> allPreferences = sharedPreferences.getAll();

    // The entry.value is the preference value
    for (Map.Entry<String, ?> entry : allPreferences.entrySet())
    {
      // Update default value
      settingsFragment.onSharedPreferenceChanged(sharedPreferences, entry.getKey());
    }
  }

  // Button on toolbar
  public boolean onOptionsItemSelected(@NonNull MenuItem item)
  {
    //noinspection SwitchStatementWithTooFewBranches
    switch (item.getItemId())
    {
      case android.R.id.home:
        this.finish();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
