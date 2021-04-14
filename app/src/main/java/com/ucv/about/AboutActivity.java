package com.ucv.about;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.ucv.utils.ThemeManager;

import com.ucv.R;

import java.util.Objects;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(this.getClass().getSimpleName(),"I'm in onCreate() of AboutActivity");
        setupTheme();
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new AboutOptionFragment()).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(this.getClass().getSimpleName(),"I'm in onOptionsItemSelected() of AboutActivity");
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
