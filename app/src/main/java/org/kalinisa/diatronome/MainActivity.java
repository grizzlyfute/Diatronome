package org.kalinisa.diatronome;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.text.HtmlCompat;
import androidx.preference.PreferenceManager;

import org.kalinisa.diatronome.Cores.MetronomeCore;
import org.kalinisa.diatronome.Cores.MetronomePlaybackService;
import org.kalinisa.diatronome.Cores.PlayNoteCore;
import org.kalinisa.diatronome.Cores.SettingsCore;
import org.kalinisa.diatronome.Cores.UiCore;
import org.kalinisa.diatronome.Fragment.MetronomeFragment;
import org.kalinisa.diatronome.Fragment.PlaynoteFragment;
import org.kalinisa.diatronome.Fragment.TunerFragment;
import org.kalinisa.diatronome.Ui.MetronomeView;
import org.kalinisa.diatronome.Ui.NeedleView;
import org.kalinisa.diatronome.Cores.SoundAnalyzeCore;

public class MainActivity extends AppCompatActivity
{
  private Menu m_menu;
  private int m_primaryColor = 0;
  private boolean m_permissionToRecordAccepted = false;
  // Have to be static to be robust to recreate
  private static int m_currentItem = 0;
  private int m_homeLayout = 0;
  private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

  private final String FRAGMENT_TAG_TUNER = "fragment_tag_tuner";
  private final String FRAGMENT_TAG_PLAYNOTE = "fragment_tag_playnote";
  private final String FRAGMENT_TAG_METRONOME = "fragment_tag_metronome";

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    this.setupUiCore();
    this.setupTunerCore();
    this.setupMetonomeCore();

    // Prepare settings
    PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
    SharedPreferences sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    m_primaryColor = sharedPreferences.getInt(SettingsCore.SETTING_COLOR, 0);
    m_homeLayout = UiCore.getHomeLayoutFormName(sharedPreferences.getString(SettingsCore.SETTING_HOME_SCREEN, ""));
    SettingsActivity.applyAllSettings(this.getApplicationContext());

    // Apply theme (before setContentView and load settings (change color call finish()).
    int themeId = UiCore.getThemeIdFromColor(getApplicationContext(), m_primaryColor);
    getTheme().applyStyle(themeId, true);

    setContentView(R.layout.activity_main);

    OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
      @Override
      public void handleOnBackPressed()
      {
        if (m_currentItem != m_homeLayout)
          navigateTo(m_homeLayout);
        else
          finish();
      }
    };
    getOnBackPressedDispatcher().addCallback(this, callback);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null)
    {
      actionBar.setTitle("");

      actionBar.setHomeButtonEnabled(false);
      actionBar.setDisplayHomeAsUpEnabled(false);
      actionBar.setDisplayShowHomeEnabled(false);
    }

    // Set volume control to media instead of ringtone
    setVolumeControlStream(AudioManager.STREAM_MUSIC);

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
    {
      ActivityCompat.requestPermissions(this,
      new String[]{ android.Manifest.permission.RECORD_AUDIO },
        REQUEST_RECORD_AUDIO_PERMISSION);
    }
    else
    {
      m_permissionToRecordAccepted = true;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
  {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    //noinspection SwitchStatementWithTooFewBranches
    switch (requestCode)
    {
      case REQUEST_RECORD_AUDIO_PERMISSION:
        m_permissionToRecordAccepted = (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
        if (!m_permissionToRecordAccepted)
        {
          android.util.Log.w(getString(R.string.app_name), "Permission to record audio not granted");
        }
        break;
      default:
        break;
    }
  }

  @Override
  public void onResume()
  {
    super.onResume();
    // cause IllegalStateException. navigateTo(m_currentItem); Trust onPostResume
  }

  @Override
  public void onPause()
  {
    PlayNoteCore.getInstance().stopPlaying();
    SoundAnalyzeCore.getInstance().stopFromUi();
    // Keep metronome running in background
    // MetronomeCore.getInstance().stop();
    super.onPause();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    m_menu = menu;
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);

    navigateTo(0);
    return true;
  }

  public void onPostResume()
  {
    super.onPostResume();
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    if (MetronomeCore.getInstance().getIsPlaying())
    {
      navigateTo(R.layout.fragment_metronome);
    }
    else
    {
      navigateTo(0);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState)
  {
    // https://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-wit
    // exception "IllegalStateException: Can not perform this action after onSaveInstanceState
    // may occurs when activity goes in background

    // Put something in out state to preserve onSaveInstance
    outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
    super.onSaveInstanceState(outState);
    // Can be avoided by using commitAllowingStateLoss
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item)
  {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    boolean isHandled = false;

    switch (id)
    {
      case R.id.action_tuner:
        navigateTo(R.layout.fragment_tuner);
        isHandled = true;
        break;

      case R.id.action_playnote:
        navigateTo(R.layout.fragment_playnote);
        isHandled = true;
        break;

      case R.id.action_metronome:
        navigateTo(R.layout.fragment_metronome);
        isHandled = true;
        break;

      case R.id.action_settings:
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        isHandled = true;
        break;

      case R.id.action_about:
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setIcon(R.drawable.app_icon);
        // Use BuildConfig.VERSION_CODE for code. See build.gradle
        alertDialogBuilder.setTitle(getResources().getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME);
        String msg = getString(R.string.about_content, getString(R.string.about_license), getString(R.string.about_source), getString(R.string.about_support));
        alertDialogBuilder.setMessage(HtmlCompat.fromHtml(msg , HtmlCompat.FROM_HTML_MODE_LEGACY));
        alertDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            dialog.cancel();
          }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        // Make link clickable (to call after show());
        ((TextView)alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        isHandled = true;
        break;

      default:
        break;
    }

    if (!isHandled)
    {
      isHandled = super.onOptionsItemSelected(item);
    }

    return isHandled;
  }

  private void setupUiCore()
  {
    Handler themeHandler = new Handler(this.getMainLooper())
    {
      @Override
      public void handleMessage(@NonNull Message msg)
      {
        if (msg.what == UiCore.HANDLER_MSG_CHANGE_COLOR)
        {
          updateColor(msg.arg1);
        }
        else if (msg.what == UiCore.HANDLER_MSG_KEEP_SCREEN_ON)
        {
          if (msg.arg1 != 0)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else if (msg.what == UiCore.HANDLER_MSG_NOTE_STRING_CHANGE)
        {
          notifyNoteStringChanged();
        }
        else if (msg.what == UiCore.HANDLER_MSG_HOME_SCREEN_CHANGE)
        {
          m_homeLayout = msg.arg1;
        }
      }
    };
    UiCore.getInstance().setHandler(themeHandler);
  }

  private void setupTunerCore()
  {
    Handler needleInfoHandler = new Handler(this.getMainLooper())
    {
      @Override
      public void handleMessage(@NonNull Message msg)
      {
        if (msg.what == SoundAnalyzeCore.HANDLER_MSG_UPDATE_NEEDLE)
        {
          updateNeedleView((SoundAnalyzeCore.NeedleParameters) msg.obj);
        }
      }
    };
    SoundAnalyzeCore.getInstance().setHandler(needleInfoHandler);
  }

  private void setupMetonomeCore()
  {
    Handler metronomeHandler = new Handler(this.getMainLooper())
    {
      @Override
      public void handleMessage(@NonNull Message msg)
      {
        if (msg.what == MetronomeCore.HANDLER_MSG_TICK)
        {
          updateMetronomeTick (msg.arg1);
        }
        else if (msg.what == MetronomeCore.HANDLER_MSG_PLAY)
        {
          updateMetronomePlay (msg.arg1 != 0);
        }
        else if (msg.what == MetronomeCore.HANDLER_MSG_TEMPO)
        {
          updateMetronomeNewTempo(msg.arg1);
        }
      }
    };
    MetronomeCore.getInstance().setHandler(metronomeHandler);
  }

  private void navigateTo(int idLayout)
  {
    // Recover from memory
    if (idLayout == 0) idLayout = m_currentItem;
    else if (m_currentItem == idLayout) return;
    if (m_menu == null) return;

    // Restart core if necessary
    // Metronome may run according to the use.
    PlayNoteCore.getInstance().stopPlaying();
    SoundAnalyzeCore.getInstance().stopFromUi();
    if (idLayout == R.layout.fragment_tuner)
    {
      if (m_permissionToRecordAccepted)
      {
        SoundAnalyzeCore.getInstance().startFromUi();
      }
    }
    else if (idLayout == R.layout.fragment_playnote)
    {
      //MetronomeCore.getInstance().stop();
      stopService(new Intent(this, MetronomePlaybackService.class));
    }

    if (idLayout == R.layout.fragment_tuner)
    {
      getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.layoutMainContent, new TunerFragment(), FRAGMENT_TAG_TUNER)
        // .addToBackStack(null)
        .commitAllowingStateLoss();
    }
    else if (idLayout == R.layout.fragment_playnote)
    {
      getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.layoutMainContent, new PlaynoteFragment(), FRAGMENT_TAG_PLAYNOTE)
        // .addToBackStack(null)
        .commitAllowingStateLoss();
    }
    else if (idLayout == R.layout.fragment_metronome)
    {
      getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.layoutMainContent, new MetronomeFragment(), FRAGMENT_TAG_METRONOME)
        // .addToBackStack(null)
        .commitAllowingStateLoss();
    }
    else
    {
      if (m_homeLayout != 0)
      {
        navigateTo(m_homeLayout);
      }
      return;
    }
    m_currentItem = idLayout;

    if (m_menu != null)
    {
      MenuItem itemTuner = m_menu.findItem(R.id.action_tuner);
      MenuItem itemMetronome = m_menu.findItem(R.id.action_metronome);
      MenuItem itemPlaynote = m_menu.findItem(R.id.action_playnote);
      if (itemTuner != null)
      {
        itemTuner.setVisible(idLayout != R.layout.fragment_tuner);
      }
      if (itemPlaynote != null)
      {
        itemPlaynote.setVisible(idLayout != R.layout.fragment_playnote);
      }
      if (itemMetronome != null)
      {
        itemMetronome.setVisible(idLayout != R.layout.fragment_metronome);
      }
    }
  }

  // or can use thisActivity.runOnUiThread from the this
  // run() { someWork(); this.runOnUiThread( updateUi()); }
  // Caution: the view can be not found on change (change orientation, ...)
  @SuppressLint("SetTextI18n")
  private void updateNeedleView(SoundAnalyzeCore.NeedleParameters parameters)
  {
     NeedleView needleView = findViewById(R.id.viewNeedle);
    // Can be temporary null on rotate...
    if (needleView != null)
    {
      needleView.updateAccuracy(parameters.accuracy);
    }

    // Change the labels
    TextView txtMeasure = findViewById(R.id.btnMeasure);
    if (txtMeasure != null)
    {
      String percentile = "" + (parameters.accuracy < 0 ? "" : "+") + Math.round(parameters.accuracy * 50) + " c";
      @SuppressLint("DefaultLocale") String frequency = String.format("%,.1f", parameters.frequency) + " Hz";
      // @SuppressLint("DefaultLocale") String decibel = String.format("%,.1f", parameters.intensity) + " %";
      String decibel = ""; // not pertinent for now
      txtMeasure.setText(percentile + "\n" + frequency + "\n" + decibel);
    }

    TextView txtNote = findViewById(R.id.btnNote);
    if (txtNote != null)
    {
      if (parameters.note >= 0 && parameters.octave >= 0)
      {
        txtNote.setText(UiCore.getInstance().getNoteName(getBaseContext().getResources(), parameters.octave, parameters.note));
      }
      else
      {
        txtNote.setText("-");
      }
    }
  }

  public void updateColor (int color)
  {
    // Avoid onCreate recursive loop.
    if (m_primaryColor == color) return;
    // Recreate the activity to apply theme (before the setContentView, done only one create)
    finish();
    Intent intent = new Intent(MainActivity.this, MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    startActivity(intent);

    NeedleView needleView = findViewById(R.id.viewNeedle);
    // Can be temporary null on rotate...
    if (needleView != null)
    {
      needleView.setColorMain(color);
    }

    MetronomeView metronomeView = findViewById(R.id.viewMetronome);
    if (metronomeView != null)
    {
      metronomeView.setColorMain(color);
    }
  }

  private void notifyNoteStringChanged()
  {
    PlaynoteFragment fragment = (PlaynoteFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_PLAYNOTE);
    if (fragment != null)
    {
      fragment.updateNoteName();
    }
  }

  private void updateMetronomeTick(int tickNb)
  {
    MetronomeFragment fragment = (MetronomeFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_METRONOME);
    if (fragment != null)
    {
      fragment.setTick(tickNb);
    }
  }

  private void updateMetronomePlay(boolean isPlaying)
  {
    MetronomeFragment fragment = (MetronomeFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_METRONOME);
    if (fragment != null)
    {
      fragment.setPlay(isPlaying);
    }
  }

  private void updateMetronomeNewTempo(int bpm)
  {
    MetronomeFragment fragment = (MetronomeFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_METRONOME);
    if (fragment != null)
    {
      fragment.setTempo(bpm);
    }
  }
}
