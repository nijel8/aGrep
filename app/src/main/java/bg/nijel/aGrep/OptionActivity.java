package bg.nijel.aGrep;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import bg.nijel.aGrep.utils.FriendlyEditTextPreference;

public class OptionActivity extends PreferenceActivity {

    private PreferenceScreen mPs = null;
    private Prefs  mPrefs;

    private FriendlyEditTextPreference mDpref;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        mPrefs = Prefs.loadPrefs(this);

        PreferenceManager mPm = getPreferenceManager();
        mPs = mPm.createPreferenceScreen(this);
        setPreferenceScreen(mPs);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        sp.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                /*Preference pref;
                if (key.equals(Prefs.KEY_HIGHLIGHTFG)) {
                    pref = findPreference(key);
                    if (pref != null) {
                        pref.setSummary(setHighlightPreferenceSummary(REQUEST_CODE_HIGHLIGHT));
                    }
                }*/
                if (key.equals(Prefs.KEY_DEBUG)) {
                    CheckBoxPreference debug = (CheckBoxPreference) findPreference(key);
                    if (debug.isChecked()) {
                        addDebugTagPref();
                        mPs.addPreference(mDpref);
                    } else {
                        mPs.removePreference(mDpref);
                    }
                }
            }
        });

        {
            // Add Text Size
            final ListPreference pr = new ListPreference(this);
            pr.setKey(Prefs.KEY_FONTSIZE);
            pr.setSummary("Current font size: %s");
            pr.setTitle(R.string.label_font_size);
            pr.setEntries(new String[]{"10", "14", "16", "18", "20", "24", "30", "36",});
            pr.setEntryValues(new String[]{"10", "14", "16", "18", "20", "24", "30", "36",});
            mPs.addPreference(pr);
        }

        {
            //Add text highlight foreground color
            final ColorPickerPreference pr = new ColorPickerPreference(this);
            pr.setKey(Prefs.KEY_HIGHLIGHTFG);
            pr.setTitle(R.string.label_highlight_fg);
            pr.setSummary("Set text foreground color.");
            pr.setDefaultValue(0xFFFFFFFF);
            pr.setAlphaSliderEnabled(true);
            pr.setHexValueEnabled(true);
            mPs.addPreference(pr);
        }

        {
            //Add text highlight background color
            final ColorPickerPreference pr = new ColorPickerPreference(this);
            pr.setKey(Prefs.KEY_HIGHLIGHTBG);
            pr.setTitle(R.string.label_highlight_bg);
            pr.setSummary("Set text background color.");
            pr.setDefaultValue(0xFF009688);
            pr.setAlphaSliderEnabled(true);
            pr.setHexValueEnabled(true);
            mPs.addPreference(pr);
        }

        {
            // Add Linenumber
            final CheckBoxPreference pr = new CheckBoxPreference(this);
            pr.setKey(Prefs.KEY_ADD_LINENUMBER);
            pr.setSummary(R.string.summary_add_linenumber);
            pr.setTitle(R.string.label_add_linenumber);
            mPs.addPreference(pr);
        }

        {
            // Add RootAccess
            final CheckBoxPreference pr = new CheckBoxPreference(this);
            pr.setKey(Prefs.KEY_ROOT_ACCESS);
            pr.setSummary(R.string.summary_root_access);
            pr.setTitle(R.string.label_root_access);
            pr.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue.toString().equals("true") && !RootShell.isAccessGiven()) {
                        Toast.makeText(OptionActivity.this, getString(R.string.label_no_root_access), Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
            mPs.addPreference(pr);
        }

        {
            // Add Debug
            final CheckBoxPreference pr = new CheckBoxPreference(this);
            pr.setKey(Prefs.KEY_DEBUG);
            pr.setSummary(R.string.summary_debug);
            pr.setTitle(R.string.label_debug);
            mPs.addPreference(pr);
        }

        {
            addDebugTagPref();
            if (mPrefs.mDebug) {
                mPs.addPreference(mDpref);
            } else {
                mPs.removePreference(mDpref);
            }
        }

        {
            String namever = "";
            try {
                namever = getString(R.string.version, getPackageManager()
                        .getPackageInfo(getPackageName(), 0).versionName);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            getActionBar().setTitle(namever + " - Settings");

        }
    }

    private void addDebugTagPref(){
        mDpref = new FriendlyEditTextPreference(this);
        mDpref.setKey(Prefs.KEY_DEBUG_TAG);
        mDpref.setSummary(R.string.summary_debug_tag);
        mDpref.setTitle(R.string.label_debug_tag);
        mDpref.getEditText().setHint(R.string.summary_debug_hint);
        mDpref.setDialogTitle(R.string.label_debug_tag);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
    }
}
