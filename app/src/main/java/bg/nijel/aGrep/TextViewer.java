package bg.nijel.aGrep;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import bg.nijel.aGrep.utils.EncodingHelper;
import bg.nijel.aGrep.utils.RootCommands;

public class TextViewer extends Activity implements OnItemLongClickListener, OnItemClickListener {
    public static final String EXTRA_LINE = "line";
    public static final String EXTRA_PATH = "path";

    private TextLoadTask mTask;
    private int mLine;
    private Prefs mPrefs;
    private String mPath;
    private TextPreview mTextPreview;
    ArrayList<CharSequence> mFileContent = new ArrayList<>();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.textviewer);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mPrefs = Prefs.loadPrefs(getApplicationContext());
        mTextPreview = (TextPreview) findViewById(R.id.TextPreview);
        mTextPreview.setOnItemLongClickListener(this);
        mTextPreview.setOnItemClickListener(this);
        Intent it = getIntent();
        if (it != null) {
            Bundle extra = it.getExtras();
            if (extra != null) {
                mPath = extra.getString(EXTRA_PATH);
                mLine = extra.getInt(EXTRA_LINE);
                setTitle(mPath + " - aGrep");
                mTask = new TextLoadTask();
                mTask.execute(mPath);
            }
        }
    }

    class TextLoadTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            File file = new File(params[0]);
            EncodingHelper helper;
            if (file.exists() && file.canRead()) {
                helper = new EncodingHelper(file);
            } else {
                String hex = RootCommands.fileAsHex(params[0]);
                if (hex != null) {
                    helper = new EncodingHelper(hex);
                } else {
                    return false;
                }
            }
            BufferedReader reader;
            try {
                reader = helper.getReader();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if (reader == null) {
                return false;
            }
            String text;
            try {
                while ((text = reader.readLine()) != null) {
                    mFileContent.add(text);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            helper.reset();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (result) {
                TextPreview.Adapter adapter = new TextPreview.Adapter(getApplicationContext(), R.layout.textpreview_row, R.id.TextPreview, mFileContent);
                mFileContent = null;
                adapter.setFormat(SearchTaskFragment.mPattern, mPrefs.mHighlightFg, mPrefs.mHighlightBg, mPrefs.mFontSize);
                mTextPreview.setAdapter(adapter);
                final int height = mTextPreview.getHeight();
                mTextPreview.setSelectionFromTop(mLine - 1, height / 4);
                mTextPreview = null;
                mTask = null;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;

    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.menu_viewer) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            if (mPrefs.addLineNumber) {
                TextPreview textPreview = (TextPreview) findViewById(R.id.TextPreview);
                intent.setDataAndType(Uri.parse("file://" + mPath + "?line=" + textPreview.getFirstVisiblePosition()), "text/plain");
            } else {
                intent.setDataAndType(Uri.parse("file://" + mPath), "text/plain");
            }
            startActivity(intent);
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        if (mPrefs.addLineNumber) {
            intent.setDataAndType(Uri.parse("file://" + mPath + "?line=" + (1 + position)), "text/plain");
        } else {
            intent.setDataAndType(Uri.parse("file://" + mPath), "text/plain");
        }
        startActivity(intent);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        TextView tv = (TextView) arg1;
        ClipData clip = ClipData.newPlainText("aGrep Text Viewer", tv.getText());
        cm.setPrimaryClip(clip);
        Toast.makeText(this, R.string.label_copied, Toast.LENGTH_LONG).show();
    }

}
