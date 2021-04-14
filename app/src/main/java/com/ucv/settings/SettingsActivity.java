package com.ucv.settings;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.ucv.utils.ThemeManager;

import com.ucv.R;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(this.getClass().getSimpleName(), "Inside onCreate() in SettingsActivity");
        setupTheme();
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Fragment existingFragment = getFragmentManager().findFragmentById(android.R.id.content);
        if (existingFragment == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content,
                    new SettingsFragment()).commit();
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(this.getClass().getSimpleName(), "Inside onBackPressed() in SettingsActivity");
        // Turn off activity close animation
        super.onBackPressed();
        overridePendingTransition(0,0);
    }

    @Override
    protected void onResume() {
        Log.d(this.getClass().getSimpleName(), "Inside onResume() in SettingsActivity");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(this.getClass().getSimpleName(), "Inside onPause() in SettingsActivity");
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(this.getClass().getSimpleName(), "Inside onOptionsItemSelected() in SettingsActivity");
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupTheme() {
        new ThemeManager(this)
                .setLightTheme(R.style.AppThemeLight)
                .applyTheme();
    }

}
