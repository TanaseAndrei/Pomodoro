package com.ucv.about;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.util.Log;

import com.ucv.BuildConfig;
import com.ucv.R;

public class AboutOptionFragment extends PreferenceFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(this.getClass().getSimpleName(),"I'm in onCreate() of AboutOptionFragment");
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about);
        setVersionNumberAsSummary(findPreference(getString(R.string.about_app_version_key)));
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.d(this.getClass().getSimpleName(),"I'm in onPreferenceTreeClick () of AboutOptionFragment");
        String preferenceKey = preference.getKey();
        if (preferenceKey.equals(getString(R.string.about_source_code_key))){
            showGithub();
            return true;
        } else if (preferenceKey.equals(getString(R.string.about_send_email_key))){
            sendEmail();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void setVersionNumberAsSummary(Preference preference){
        String version = BuildConfig.VERSION_NAME;
        preference.setSummary(version);
    }

    private void showGithub(){
        Intent githubIntent = new Intent("android.intent.action.VIEW");
        githubIntent.setData(Uri.parse(getString(R.string.github_link)));
        startActivity(githubIntent);
    }

    private void sendEmail(){
        Intent emailIntent = new Intent("android.intent.action.SENDTO");
        emailIntent.setData(Uri.parse(getString(R.string.feedback_email)));
        startActivity(emailIntent);
    }
}
