package bg.nijel.aGrep;

import android.app.ActionBar;
import android.content.Intent;
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

import bg.nijel.aGrep.utils.FriendlyEditTextPreference;

public class OptionActivity extends PreferenceActivity {

    private PreferenceScreen mPs = null;
    private Prefs  mPrefs;

    final private static int REQUEST_CODE_HIGHLIGHT = 0x1000;
    final private static int REQUEST_CODE_BACKGROUND = 0x1001;
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
                Preference pref;
                if (key.equals(Prefs.KEY_HIGHLIGHTFG)) {
                    pref = findPreference(key);
                    if (pref != null) {
                        pref.setSummary(setHighlightPreferenceSummary(REQUEST_CODE_HIGHLIGHT));
                    }
                }
                if (key.equals(Prefs.KEY_HIGHLIGHTBG)) {
                    pref = findPreference(key);
                    if (pref != null) {
                        pref.setSummary(setHighlightPreferenceSummary(REQUEST_CODE_BACKGROUND));
                    }
                }
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

        createHighlightPreference(R.string.label_highlight_fg, REQUEST_CODE_HIGHLIGHT);
        createHighlightPreference(R.string.label_highlight_bg, REQUEST_CODE_BACKGROUND);

        /*{
            final AmbilWarnaPreference pr = new AmbilWarnaPreference(this, null);
            pr.setKey("some_color");
            pr.setTitle("Some color");
            pr.setSummary("some color");
            pr.setDefaultValue(0xff009688);
            pr.alphaEnabled(true);
            mPs.addPreference(pr);
        }*/

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

    private void createHighlightPreference( final int resid , final int reqCode )
    {
        final Preference pr = new Preference(this);
        if(reqCode == REQUEST_CODE_HIGHLIGHT){
            pr.setKey(Prefs.KEY_HIGHLIGHTFG);
        }
        if(reqCode == REQUEST_CODE_BACKGROUND){
            pr.setKey(Prefs.KEY_HIGHLIGHTBG);
        }
        pr.setTitle(resid);
        pr.setSummary(setHighlightPreferenceSummary(reqCode));

        pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Intent intent = new Intent( OptionActivity.this , ColorPickerActivity.class );
                intent.putExtra(ColorPickerActivity.EXTRA_TITLE, getString(resid));
                startActivityForResult(intent, reqCode);
                return true;
            }
        });

        mPs.addPreference(pr);
    }

    private String setHighlightPreferenceSummary (int requestCode){
        String summary = "";
        switch (requestCode){
            case REQUEST_CODE_HIGHLIGHT:
                summary = "Set found text foreground color. Current: " + String.format("#%06X", 0xFFFFFF & mPrefs.mHighlightFg);
                break;
            case REQUEST_CODE_BACKGROUND:
                summary = "Set found text background color. Current: " + String.format("#%06X", 0xFFFFFF & mPrefs.mHighlightBg);
                break;
        }
        return summary;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ( resultCode == RESULT_OK ){
            int color = data.getIntExtra(ColorPickerActivity.EXTRA_COLOR, 0x009688);
            if ( requestCode == REQUEST_CODE_HIGHLIGHT ){
                mPrefs.mHighlightFg = color;
            }else if ( requestCode == REQUEST_CODE_BACKGROUND ){
                mPrefs.mHighlightBg = color;
            }
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            sp.edit()
            .putInt(Prefs.KEY_HIGHLIGHTFG, mPrefs.mHighlightFg)
            .putInt(Prefs.KEY_HIGHLIGHTBG, mPrefs.mHighlightBg)
            .apply();
        }
        onResume();
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
