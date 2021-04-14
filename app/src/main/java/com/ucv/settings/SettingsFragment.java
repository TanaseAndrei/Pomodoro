package com.ucv.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ucv.R;

public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(this.getClass().getSimpleName(), "Inside onCreate() in SettingsFragment");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        initSummary(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        Log.d(this.getClass().getSimpleName(), "Inside onResume() in SettingsFragment");
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        Log.d(this.getClass().getSimpleName(), "Inside onPause() in SettingsFragment");
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Log.d(this.getClass().getSimpleName(), "Inside onSharedPreferenceChanged() in SettingsFragment");
        updatePrefSummary(findPreference(key));
    }

    private void initSummary(Preference p) {
        Log.d(this.getClass().getSimpleName(), "Inside initSummary() in SettingsFragment");
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {
        Log.d(this.getClass().getSimpleName(), "Inside updatePrefSummary() in SettingsFragment");
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            modifySummary(p, String.valueOf(listPref.getEntry()));
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            modifySummary(p, editTextPref.getText());
        }
        if (p instanceof MultiSelectListPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
            modifySummary(p, editTextPref.getText());
        }
        if (p instanceof NumberPickerDialogPreference) {
            NumberPickerDialogPreference minutePickerPref = (NumberPickerDialogPreference) p;
            modifySummary(p, String.valueOf(minutePickerPref.getValue()));
        }
    }

    private void modifySummary(Preference preference, String value){
        Log.d(this.getClass().getSimpleName(), "Inside modifySummary() in SettingsFragment");
        String summary = String.valueOf(preference.getSummary());
        summary = summary.replaceFirst("\\d{1,4}", value);
        preference.setSummary(summary);
    }
}