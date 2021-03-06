package org.tasks.preferences;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;

import com.todoroo.astrid.reminders.ReminderPreferences;

import org.tasks.R;
import org.tasks.injection.InjectingPreferenceActivity;

import javax.inject.Inject;

public class BasicPreferences extends InjectingPreferenceActivity {

    private static final String EXTRA_RESULT = "extra_result";
    private static final int RC_PREFS = 10001;

    private Bundle result;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        result = savedInstanceState == null ? new Bundle() : savedInstanceState.getBundle(EXTRA_RESULT);

        addPreferencesFromResource(R.xml.preferences);
        if (!getResources().getBoolean(R.bool.sync_enabled)) {
            getPreferenceScreen().removePreference(findPreference(getString(R.string.synchronization)));
        }
        findPreference(getString(R.string.EPr_appearance_header)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(BasicPreferences.this, AppearancePreferences.class), RC_PREFS);
                return true;
            }
        });
        findPreference(getString(R.string.notifications)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(BasicPreferences.this, ReminderPreferences.class), RC_PREFS);
                return true;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(EXTRA_RESULT, result);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_PREFS) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                result.putAll(data.getExtras());
                setResult(Activity.RESULT_OK, new Intent() {{
                    putExtras(result);
                }});
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
