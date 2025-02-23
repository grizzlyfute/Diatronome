package org.kalinisa.diatronome.Ui;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;

import org.kalinisa.diatronome.R;

public class ColorChooserPreference
  extends DialogPreference
{
  final int DEF_COLOR = 0xFF808080;
  int m_value;
  int m_defaultValue;
  View m_thumbnailView;

  @SuppressWarnings("unused")
  public ColorChooserPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAtt, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr, defStyleAttr);
    init(context, attrs);
  }

  @SuppressWarnings("unused")
  public ColorChooserPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  @SuppressWarnings("unused")
  public ColorChooserPreference(@NonNull Context context, @Nullable AttributeSet attrs)
  {
    super(context, attrs);
    init(context, attrs);
  }

  @SuppressWarnings("unused")
  public ColorChooserPreference(@NonNull Context context)
  {
    super(context);
    init(context, null);
  }

  @SuppressWarnings("CommentedOutCode")
  private void init(@NonNull Context context, @Nullable AttributeSet attrs)
  {
    // override the widget layout
    setWidgetLayoutResource(R.layout.pref_color);
    // override the all view
    // setLayoutResource(R.layout.pref_float_seekbar);

    setDialogLayoutResource(R.layout.pref_color_dialog);
    // setDialogIcon(iconId);
    // setDialogTitle("Title");
    // setDialogMessage("Message");
    // setNegativeButtonText ("Cancel");
    // setPositiveButtonText("Ok");

    Dialog dialog = new Dialog(context);
    dialog.setContentView(R.layout.pref_color_dialog);

    TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ColorPreference);
    @SuppressWarnings("unused") int dummy = ta.getInt(R.styleable.ColorPreference_color_dummy, DEF_COLOR);
    ta.recycle();

    m_defaultValue = DEF_COLOR;
    m_value = m_defaultValue;
    m_thumbnailView = null;
  }

  public int getValue()
  {
    // return getPersistedInt(m_defaultValue);
    return m_value;
  }

  public void setValue(int value)
  {
    m_value = value;
    persistInt(value);
    notifyChanged();
  }

  // **** Override from Preferences

  // Called when a Preference is being inflated and the default value attribute needs to be read. Since different
  // Preference types have different value types, the subclass should get and return the default value which will be
  // its value type.
  @Override
  protected @Nullable Object onGetDefaultValue(@NonNull TypedArray ta, int i)
  {
    m_defaultValue = ta.getInt(i, DEF_COLOR);
    return m_defaultValue;
  }

  // Implement this to set the initial value of the Preference.
  @Override
  protected void onSetInitialValue(@Nullable Object defaultValue)
  {
    if (defaultValue instanceof Integer)
    {
      m_value = getPersistedInt((Integer)defaultValue);
    }
    else
    {
      m_value = getPersistedInt(m_defaultValue);
    }
    // Force to store the value, so will be included on load default value
    if (getPreferenceManager() != null && getPreferenceManager().getSharedPreferences() != null &&
        !getPreferenceManager().getSharedPreferences().getAll().containsKey (getKey()))
    {
      setValue (m_value);
    }
  }

  // call order: onGetDefaultValue -> onSetInitialValue -> onBindViewHolder
  @Override
  public void onBindViewHolder(@NonNull PreferenceViewHolder holder)
  {
    super.onBindViewHolder(holder);

    // holder.itemView.setClickable(false);
    m_thumbnailView = holder.findViewById(R.id.pref_color_chooser_view);

    if (m_thumbnailView != null)
    {
      m_thumbnailView.setBackgroundColor(m_value);
    }
  }
}
