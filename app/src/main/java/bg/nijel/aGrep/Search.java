package bg.nijel.aGrep;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bg.nijel.aGrep.utils.RootCommands;


@SuppressLint("DefaultLocale")
public class Search extends Activity implements GrepView.Callback {
    private GrepView mGrepView;
    private GrepView.GrepAdapter mAdapter;
    private ArrayList<GrepView.Data> mData;
    private AsyncTask<String, Integer, Boolean> mGetFilesTask;
    private GrepTask mTask;
    private String mQuery;
    private Pattern mPattern;
    private ArrayList<String> mTargetFiles = new ArrayList<>();
    private int mFoundcount;

    private Prefs mPrefs;

    static final String SEARCH_DATA = "searchData";
    static final String SEARCH_PATTERN = "searchPattern";
    static final String SERACH_QUERY = "searchQuery";
    static final String TEXT_IN_FILES_COUNT = "textInFilesCount";
    //static final String FILES_MATCHED = "filesMatched";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mFoundcount = 0;
        mPrefs = Prefs.loadPrefs(this);
        setContentView(R.layout.result);
        mGrepView = (GrepView) findViewById(R.id.DicView01);
        mData = new ArrayList<>();
        if (savedInstanceState != null) {
            mPattern = (Pattern) savedInstanceState.getSerializable(SEARCH_PATTERN);
            mQuery = savedInstanceState.getString(SERACH_QUERY);
            mData = savedInstanceState.getParcelableArrayList(SEARCH_DATA);
            mFoundcount = savedInstanceState.getInt(TEXT_IN_FILES_COUNT);
            mAdapter = new GrepView.GrepAdapter(getApplicationContext(), R.layout.list_row, R.id.DicView01, mData);
            mGrepView.setAdapter(mAdapter);
            mAdapter.setFormat(mPattern, mPrefs.mHighlightFg, mPrefs.mHighlightBg, mPrefs.mFontSize);
            mGrepView.setCallback(this);
        } else {
            if (mPrefs.mDirList.size() == 0) {
                Toast.makeText(getApplicationContext(), R.string.label_no_target_dir, Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, Settings.class));
                finish();
            }
            mAdapter = new GrepView.GrepAdapter(getApplicationContext(), R.layout.list_row, R.id.DicView01, mData);
            mGrepView.setAdapter(mAdapter);
            mGrepView.setCallback(this);
            Intent it = getIntent();
            if (it != null && Intent.ACTION_SEARCH.equals(it.getAction())) {
                Bundle extras = it.getExtras();
                mQuery = extras.getString(SearchManager.QUERY);
                if (mQuery != null && mQuery.length() > 0) {
                    mPrefs.addRecent(this, mQuery);
                    String patternText = mQuery;
                    if (!mPrefs.mMatchWhole) {
                        if (!mPrefs.mRegularExrpression) {
                            patternText = escapeMetaChar(patternText);
                            patternText = convertOrPattern(patternText);
                        }
                    }
                    if (!mPrefs.mMatchCase) {
                        mPattern = Pattern.compile(patternText, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
                    } else {
                        mPattern = Pattern.compile(patternText);
                    }
                    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                        mData.removeAll(mData);
                        mAdapter.setFormat(mPattern, mPrefs.mHighlightFg, mPrefs.mHighlightBg, mPrefs.mFontSize);
                        mGetFilesTask = new getFilesTask();
                        mGetFilesTask.execute("true");// mTask = new GrepTask();// mTask.execute(mQuery);
                    }
                } else {
                    startActivity(new Intent(this, Settings.class));
                    finish();
                }
            }
        }
        getActionBar().setTitle("aGrep - " + mFoundcount + " files");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putParcelableArrayList(SEARCH_DATA, mData);
        savedInstanceState.putSerializable(SEARCH_PATTERN, mPattern);
        savedInstanceState.putString(SERACH_QUERY, mQuery);
        savedInstanceState.putInt(TEXT_IN_FILES_COUNT, mFoundcount);
        // savedInstanceState.putInt(FILES_MATCHED, mTargetFiles.size());
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    static public String escapeMetaChar(String pattern) {
        final String metachar = ".^${}[]*+?|()\\";

        StringBuilder newpat = new StringBuilder();

        int len = pattern.length();

        for (int i = 0; i < len; i++) {
            char c = pattern.charAt(i);
            if (metachar.indexOf(c) >= 0) {
                newpat.append('\\');
            }
            newpat.append(c);
        }
        return newpat.toString();
    }

    static public String convertOrPattern(String pattern) {
        if (pattern.contains(" ")) {
            return "(" + pattern.replace(" ", "|") + ")";
        } else {
            return pattern;
        }
    }

    public class getFilesTask extends AsyncTask<String, Integer, Boolean> {
        private ProgressDialog mProgressDialog;
        private boolean mCancelled;

        protected void onPreExecute() {
            mTargetFiles.clear();
            mCancelled = false;
            mProgressDialog = new ProgressDialog(Search.this);
            mProgressDialog.setTitle(R.string.count_title);
            mProgressDialog.setMessage(getString(R.string.count_progress));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    mCancelled = true;
                    cancel(false);
                }
            });
            mProgressDialog.show();

        }

        @Override
        protected Boolean doInBackground(String... params) {
            RootShell.log(RootShell.debugTag, "mTargetFiles Initial Size: " + mTargetFiles.size(), RootShell.LogLevel.WARN, null);
            for (CheckedString dir : mPrefs.mDirList) {
                if (dir.checked) {
                    mTargetFiles.addAll(RootCommands.listFiles(dir.string));
                }
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            RootShell.log(RootShell.debugTag, "mTargetFiles Size: " + mTargetFiles.size(), RootShell.LogLevel.WARN, null);
            ArrayList<String> temp = new ArrayList<>();
            temp.addAll(mTargetFiles);
            RootShell.log(RootShell.debugTag, "temp Size: " + mTargetFiles.size(), RootShell.LogLevel.WARN, null);
            filterMatchingFiles(temp);
            /*for (String file : mTargetFiles){
                RootShell.log(RootShell.debugTag, "mTargetFiles Filtered File: " + mTargetFiles.get(mTargetFiles.indexOf(file)), RootShell.LogLevel.ERROR, null);
            }*/
            RootShell.log(RootShell.debugTag, "mTargetFiles Filtered Size: " + mTargetFiles.size(), RootShell.LogLevel.WARN, null);
            mProgressDialog.dismiss();
            mProgressDialog = null;
            if (mTargetFiles != null && mTargetFiles.size() > 0 && !mCancelled) {
                mTask = new GrepTask();
                mTask.execute(mQuery);
            } else if(!mCancelled){
                Toast.makeText(getApplicationContext(), getString(R.string.search_aborted), Toast.LENGTH_LONG).show();
              //  startActivity(new Intent(getApplicationContext(), Settings.class));
                finish();
            }
            mGetFilesTask = null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mCancelled = true;
            Toast.makeText(getApplicationContext(), getString(R.string.grep_canceled), Toast.LENGTH_LONG).show();
            startActivity(new Intent(getApplicationContext(), Settings.class));
            onPostExecute(false);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (isCancelled() || mCancelled) {
                return;
            }
        }

        private boolean filterMatchingFiles(ArrayList<String> files) {
            if (isCancelled() || mCancelled) {
                return false;
            }
            mTargetFiles.clear();
            filesloop:
            for (String file : files) {
                if (isCancelled() || mCancelled) {
                    return false;
                }
                for (CheckedString ext : mPrefs.mExtList) {
                    if (ext.checked) {
                        //  RootShell.log(RootShell.debugTag, "path:" + file, RootShell.LogLevel.ERROR, null);
                        if (ext.string.equals("*")) {
                            mTargetFiles.add(file);
                            continue filesloop;
                        }
                        if (ext.string.equals("*no_ext") && !file.substring(file.lastIndexOf('/') + 1, file.length()).contains(".")) {
                            //     RootShell.log(RootShell.debugTag, "*no_ext:" + file, RootShell.LogLevel.WARN, null);
                            mTargetFiles.add(file);
                            continue filesloop;
                        }
                        if (file.toLowerCase().endsWith("." + ext.string.toLowerCase())/* && !ext.string.equals(".")*/) {
                            //   RootShell.log(RootShell.debugTag, "ext name:" + file, RootShell.LogLevel.WARN, null);
                            mTargetFiles.add(file);
                            continue filesloop;
                        }
                        if (ext.string.equals(".") && file.endsWith(".")) {
                            //  RootShell.log(RootShell.debugTag, ". name:" + file, RootShell.LogLevel.WARN, null);
                            mTargetFiles.add(file);
                            continue filesloop;
                        }
                        // RootShell.log(RootShell.debugTag, ". name:" + ext.string.equals(".") + ":" + !file.endsWith("."), RootShell.LogLevel.WARN, null);
                    }
                }
            }
            return true;
        }
    }

    class GrepTask extends AsyncTask<String, GrepView.Data, Boolean> {
        private ProgressDialog mProgressDialog;
        private int mFileCount = 0;
        private boolean mCancelled = false;
        private String mSearchingFile;

        @Override
        protected void onPreExecute() {

            mCancelled = false;
            mProgressDialog = new ProgressDialog(Search.this);
            mProgressDialog.setTitle(R.string.grep_spinner);
            mProgressDialog.setMessage(mQuery);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    mCancelled = true;
                    cancel(false);
                }
            });
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... query) {
            return grepFile();
        }


        @Override
        protected void onPostExecute(Boolean result) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
            if (mData != null && mData.size() > 0) {
                synchronized (mData) {
                    Collections.sort(mData, new GrepView.Data());
                    mAdapter.notifyDataSetChanged();
                }
                mGrepView.setSelection(0);
                Toast.makeText(getApplicationContext(), result ? R.string.grep_finished : R.string.grep_canceled, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.grep_aborted), Toast.LENGTH_LONG).show();
                startActivity(new Intent(getApplicationContext(), Settings.class));
                finish();
            }
            //  mData = null;
            mAdapter = null;
            mTask = null;
            getActionBar().setTitle("aGrep - " + mFoundcount + " files");
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mCancelled = true;
            onPostExecute(false);
        }

        @Override
        protected void onProgressUpdate(GrepView.Data... progress) {
            if (isCancelled() || mCancelled) {
                return;
            }
            mProgressDialog.setMessage(Search.this.getString(R.string.progress, mQuery, mSearchingFile, mFileCount, mTargetFiles.size()));
            if (progress != null) {
                synchronized (mData) {
                    for (GrepView.Data data : progress) {
                        mData.add(data);
                    }
                    mAdapter.notifyDataSetChanged();
                    mGrepView.setSelection(mData.size() - 1);
                }
            }
        }

        boolean grepFile(/* File file  */) {
            if (isCancelled() || mCancelled) {
                return false;
            }
            InputStream is;
            for (String path : mTargetFiles) {
                if (isCancelled() || mCancelled) {
                    return false;
                }
                File file = new File(path);
                try {
                    is = new BufferedInputStream(new FileInputStream(file), 65536);
                    is.mark(65536);

                    String encode = null;
                    try {
                        UniversalDetector detector = new UniversalDetector();
                        try {
                            int nread;
                            byte[] buff = new byte[4096];
                            if ((nread = is.read(buff)) > 0) {
                                detector.handleData(buff, 0, nread);
                            }
                            detector.dataEnd();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            is.close();
                            return true;
                        } catch (IOException e) {
                            e.printStackTrace();
                            is.close();
                            return true;
                        }
                        encode = detector.getCharset();
                        detector.reset();
                        detector.destroy();
                    } catch (UniversalDetector.DetectorException e) {
                    }
                    is.reset();
                    BufferedReader br = null;
                    try {
                        if (encode != null) {
                            br = new BufferedReader(new InputStreamReader(is, encode), 8192);

                        } else {
                            br = new BufferedReader(new InputStreamReader(is), 8192);
                        }
                        String text;
                        int line = 0;
                        boolean found = false;
                        Pattern pattern = mPattern;
                        Matcher m = null;
                        ArrayList<GrepView.Data> data = null;
                        mSearchingFile = path.substring(path.lastIndexOf('/') + 1, path.length());
                        mFileCount++;
                        while ((text = br.readLine()) != null) {
                            line++;
                            if (m == null) {
                                m = pattern.matcher(text);
                            } else {
                                m.reset(text);
                            }
                            if (m.find()) {
                                found = true;

                                synchronized (mData) {
                                    mFoundcount++;
                                    if (data == null) {
                                        data = new ArrayList<GrepView.Data>();
                                    }
                                    data.add(new GrepView.Data(file, line, text));

                                    if (mFoundcount < 10) {
                                        publishProgress(data.toArray(new GrepView.Data[0]));
                                        data = null;
                                    }
                                }
                                if (mCancelled) {
                                    break;
                                }
                            }
                        }
                        br.close();
                        is.close();
                        if (data != null) {
                            publishProgress(data.toArray(new GrepView.Data[0]));
                            data = null;
                        }
                        if (!found) {
                            if (mFileCount % 10 == 0) {
                                publishProgress((GrepView.Data[]) null);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return true;
        }
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

    @Override
    public void onGrepItemClicked(int position) {
        GrepView.Data data = (GrepView.Data) mGrepView.getAdapter().getItem(position);

        Intent it = new Intent(this, TextViewer.class);

        it.putExtra(TextViewer.EXTRA_PATH, data.mFile.getAbsolutePath());
        it.putExtra(TextViewer.EXTRA_QUERY, mQuery);
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


    public void onDestroy() {
        super.onDestroy();
    }

    protected void onStop() {
        super.onStop();
    }
}