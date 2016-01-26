package bg.nijel.aGrep;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;

import java.io.File;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import bg.nijel.aGrep.GrepView.GrepViewCallback;
import bg.nijel.aGrep.SearchTaskFragment.SearchStatusCallback;

/**
 * Created by Nick on 1/13/2016.
 */
public class SearchTaskActivity extends Activity implements SearchStatusCallback, GrepViewCallback {

    private SearchTaskFragment mSearchTaskFragment;
    private static Prefs mPrefs;
    private GrepView mGrepView;
    public static String mQuery;
    RelativeLayout mProgressLayout;
    private static ProgressBar mProgressBar;
    private static RelativeLayout mSearchCountLayoyt;
    private static TextView mProgressMessage;
    public static TextView mProgressFilteredCount;
    private static TextView mHitsCount;
    private static TextView mFilesCount;
    private static TextView mProgressFileCount;
    private static TextView mCurrentFile;


    static final String SERACH_QUERY = "searchQuery";
    static final String PROGRESS_VALUE = "progressValue";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("aGrep - Searching...");
        }
        mPrefs = Prefs.loadPrefs(this);
        setupLayout();
        if (savedInstanceState != null && !SearchTaskFragment.isSearchExecuting) {
            mProgressLayout.setVisibility(View.GONE);
            mQuery = savedInstanceState.getString(SERACH_QUERY);
            setupGrepViewAdapter();
        } else if (savedInstanceState != null && SearchTaskFragment.isSearchExecuting) {
            FragmentManager mMgr = getFragmentManager();
            mSearchTaskFragment = (SearchTaskFragment) mMgr
                    .findFragmentByTag(SearchTaskFragment.TAG_SEARCH_FRAGMENT);
            int progress = savedInstanceState.getInt(PROGRESS_VALUE);
            setupProgerssView(progress);
        } else {
            SearchTaskFragment.mPattern = null;
            Intent it = getIntent();
            if (it != null && Intent.ACTION_SEARCH.equals(it.getAction())) {
                Bundle extras = it.getExtras();
                mQuery = extras.getString(SearchManager.QUERY);
                if (mQuery != null && mQuery.length() > 0) {
                    SearchTaskFragment.mPattern = getPattern(mQuery);
                }
                if (SearchTaskFragment.mPattern != null) {
                    setupProgerssView(0);
                    mPrefs.addRecent(this, mQuery);
                    FragmentManager mMgr = getFragmentManager();
                    mSearchTaskFragment = (SearchTaskFragment) mMgr
                            .findFragmentByTag(SearchTaskFragment.TAG_SEARCH_FRAGMENT);
                    if (mSearchTaskFragment == null) {
                        mSearchTaskFragment = new SearchTaskFragment();
                        mMgr.beginTransaction()
                                .add(mSearchTaskFragment, SearchTaskFragment.TAG_SEARCH_FRAGMENT)
                                .commit();
                        if (mSearchTaskFragment != null && !SearchTaskFragment.isSearchExecuting) {
                            mSearchTaskFragment.startSearchTask(this);
                        }
                    }
                } else {
                    startActivity(new Intent(this, Settings.class));
                    finish();
                }
            }
        }
    }

    public static Pattern getPattern(String query) {

        Context context = new MyApp().getInstance();
        Pattern returnPattern = null;
        String regex;
        String[] subs;
        int i = 0;
        if (mPrefs.mMatchWhole) i |= 100;
        if (mPrefs.mMatchCase) i |= 10;
        if (mPrefs.mRegularExrpression) i |= 1;
        String cond = String.format("%03d", i);
        switch (cond) {
            case "000": //* replace inner spaces with 'or' if any, ignore metacharacterss (literal search)
                subs = Pattern.compile("\\b(\\s+|\\t+)\\b").split(query);
                if (subs.length == 1) {
                    returnPattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE | Pattern.LITERAL);
                    break;
                }
                regex = "(";
                for (int n = 0; n < subs.length; n++) {
                    if (n < (subs.length - 1)) {
                        regex += Pattern.compile(subs[n], Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE | Pattern.LITERAL).pattern() + "|";
                    } else {
                        regex += Pattern.compile(subs[n], Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE | Pattern.LITERAL).pattern() + ")";
                    }
                }
                returnPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
                break;

            case "100": //* ignore metacharacterss (literal search whole expression as is)
                returnPattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE | Pattern.LITERAL);
                break;

            case "010": //* replace inner spaces with 'or' if any, ignore metacharacterss (literal search), case sensitive
                subs = Pattern.compile("\\b(\\s+|\\t+)\\b").split(query);
                if (subs.length == 1) {
                    returnPattern = Pattern.compile(query, Pattern.MULTILINE | Pattern.LITERAL);
                    break;
                }
                regex = "(";
                for (int n = 0; n < subs.length; n++) {
                    if (n < (subs.length - 1)) {
                        regex += Pattern.compile(subs[n], Pattern.MULTILINE | Pattern.LITERAL).pattern() + "|";
                    } else {
                        regex += Pattern.compile(subs[n], Pattern.MULTILINE | Pattern.LITERAL).pattern() + ")";
                    }
                }
                returnPattern = Pattern.compile(regex, Pattern.MULTILINE);
                break;

            case "110": //* ignore metacharacterss (literal search whole expression as is), case sensitive
                returnPattern = Pattern.compile(query, Pattern.MULTILINE | Pattern.LITERAL);
                break;

            case "001": //* regex
                try {
                    returnPattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                } catch (PatternSyntaxException pse) {
                    context.startActivity(new Intent(context, Settings.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    Toast.makeText(new MyApp().getInstance(), "PatternSyntaxException: " + pse.getMessage(), Toast.LENGTH_LONG).show();
                    RootShell.log(RootShell.debugTag, "PatternSyntaxException: " + pse.getMessage());
                    return null;
                }
                break;

            case "011": //* regex, case sensitive
                try {
                    returnPattern = Pattern.compile(query);
                } catch (PatternSyntaxException pse) {
                    context.startActivity(new Intent(context, Settings.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    Toast.makeText(new MyApp().getInstance(), "PatternSyntaxException: " + pse.getMessage(), Toast.LENGTH_LONG).show();
                    RootShell.log(RootShell.debugTag, "PatternSyntaxException: " + pse.getMessage());
                    return null;
                }
                break;
        }
        RootShell.log(RootShell.debugTag, "Pattern:" + returnPattern);
        return returnPattern;
    }

    public static SpannableString highlightKeyword(CharSequence text, Pattern p, int fgcolor, int bgcolor) {
        SpannableString ss = new SpannableString(text);

        int start = 0;
        int end;
        Matcher m = p.matcher(text);
        while (m.find(start)) {
            start = m.start();
            end = m.end();

            BackgroundColorSpan bgspan = new BackgroundColorSpan(bgcolor);
            ss.setSpan(bgspan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ForegroundColorSpan fgspan = new ForegroundColorSpan(fgcolor);
            ss.setSpan(fgspan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            start = end;
        }
        return ss;
    }

    public void onClickBtnCancel(View view) {
        if (mSearchTaskFragment != null) {
            mSearchTaskFragment.cancelSearchTask();
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        onClickBtnCancel(null);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                if (mSearchTaskFragment != null)
                    mSearchTaskFragment.cancelSearchTask();
                finish();
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onGrepItemClicked(int position) {
        GrepView.Data data = (GrepView.Data) mGrepView.getAdapter().getItem(position);

        Intent it = new Intent(this, TextViewer.class);

        it.putExtra(TextViewer.EXTRA_PATH, data.mFile.getAbsolutePath());
        it.putExtra(TextViewer.EXTRA_LINE, data.mLinenumber);

        startActivity(it);
    }

    @Override
    public boolean onGrepItemLongClicked(int position) {

        GrepView.Data data = (GrepView.Data) mGrepView.getAdapter().getItem(position);
        Uri startDir = Uri.fromFile(new File(data.mFile.getParent()));
        Intent intent = new Intent();
        intent.setData(startDir);
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_VIEW);
        startActivity(intent);

        return false;
    }

    // Background task Callbacks
    private void setupLayout() {
        setContentView(R.layout.layout_search);
        mProgressLayout = (RelativeLayout) findViewById(R.id.layout_task);
        mGrepView = (GrepView) findViewById(R.id.search_result);
        mProgressMessage = (TextView) findViewById(R.id.progressMessage);
        mSearchCountLayoyt = (RelativeLayout) findViewById(R.id.layout_files_count);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar_searching);
        mProgressFilteredCount = (TextView) findViewById(R.id.progress_filteredcount);
        mHitsCount = (TextView) findViewById(R.id.hits_count);
        mFilesCount = (TextView) findViewById(R.id.files_count);
        mProgressFileCount = (TextView) findViewById(R.id.progress_filecount);
        mCurrentFile = (TextView) findViewById(R.id.current_file);
        mProgressLayout.setVisibility(View.VISIBLE);
        mSearchCountLayoyt.setVisibility(View.GONE);
        mCurrentFile.setVisibility(View.GONE);
    }

    private void setupProgerssView(int progress) {
        if (progress == 0) {
            mProgressMessage.setText(R.string.count_progress);
            mProgressBar.setIndeterminate(true);
        } else {
            mProgressBar.setIndeterminate(false);
            mProgressBar.setMax(SearchTaskFragment.mTargetFiles.size());
            mProgressBar.setProgress(progress);
            mSearchCountLayoyt.setVisibility(View.VISIBLE);
            mCurrentFile.setVisibility(View.VISIBLE);
            mCurrentFile.setText(SearchTaskFragment.mSearchingFile);
            mProgressFileCount.setText(String.valueOf(progress));
            mProgressMessage.setText(mQuery);
            mProgressMessage.setPaintFlags(mProgressMessage.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            mHitsCount.setText(String.valueOf(String.valueOf(SearchTaskFragment.mHits)));
            mFilesCount.setText(String.valueOf(SearchTaskFragment.mFoundcount));
            mProgressFilteredCount.setText(String.valueOf(SearchTaskFragment.mTargetFiles.size()));
        }
    }

    private void setupGrepViewAdapter() {
        do {
            if (mGrepView != null) {
                GrepView.GrepAdapter mAdapter = new GrepView.GrepAdapter(getApplicationContext(), R.layout.list_row, R.id.search_result, SearchTaskFragment.mData);
                mGrepView.setAdapter(mAdapter);
                mAdapter.setFormat(SearchTaskFragment.mPattern, mPrefs.mHighlightFg, mPrefs.mHighlightBg, mPrefs.mFontSize);
                mGrepView.setCallback(this);
                mAdapter.notifyDataSetChanged();
                mGrepView.setSelection(0);
            }
        } while (mGrepView == null);
        if (getActionBar() != null) {
            getActionBar().setTitle("aGrep - " + SearchTaskFragment.mData.size() + " hits in " + SearchTaskFragment.mFoundcount + " files");
        }
    }

    @Override
    public void onPreExecute() {
        setupProgerssView(0);
    }

    @Override
    public void onProgressUpdate(int progress) {
        setupProgerssView(progress);
    }

    @Override
    public void onPostExecute() {
        if (mSearchTaskFragment != null) {
            mSearchTaskFragment.updateSearchStatus(false);
        }
        if (SearchTaskFragment.mData != null && SearchTaskFragment.mData.size() > 0) {
            mSearchCountLayoyt.setVisibility(View.GONE);
            mCurrentFile.setVisibility(View.GONE);
            mProgressLayout.setVisibility(View.GONE);
            mGrepView.setVisibility(View.VISIBLE);
            Collections.sort(SearchTaskFragment.mData, new GrepView.Data());
            setupGrepViewAdapter();
            Toast.makeText(getApplicationContext(), R.string.grep_finished, Toast.LENGTH_LONG).show();
        } else {
            getFragmentManager().beginTransaction().remove(mSearchTaskFragment).commit();
            finish();
            Toast.makeText(getApplicationContext(), getString(R.string.grep_aborted), Toast.LENGTH_LONG).show();
            startActivity(new Intent(getApplicationContext(), Settings.class));
        }
        //mAdapter = null;
    }

    @Override
    public void onCancelled() {
        getFragmentManager().beginTransaction().remove(mSearchTaskFragment).commit();
        finish();
        Toast.makeText(getApplicationContext(), R.string.grep_canceled, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(getApplicationContext(), Settings.class));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (SearchTaskFragment.isSearchExecuting) {
            savedInstanceState.putInt(PROGRESS_VALUE, mProgressBar.getProgress());
        } else {
            savedInstanceState.putString(SERACH_QUERY, mQuery);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Runtime.getRuntime().gc();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Runtime.getRuntime().gc();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Runtime.getRuntime().gc();
    }
}
