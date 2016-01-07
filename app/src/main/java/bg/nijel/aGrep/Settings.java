package bg.nijel.aGrep;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import bg.nijel.aGrep.DialogChooseDirectory.Result;
import bg.nijel.aGrep.utils.AutoComleteDropDownAdapter;

public class Settings extends Activity implements Result {

   // final static int REQUEST_CODE_ADDDIC = 0x1001;

    private static Prefs mPrefs;
    private LinearLayout mDirListView;
    private LinearLayout mExtListView;
    private View.OnLongClickListener mDirListener;
    private View.OnLongClickListener mExtListener;
    private CompoundButton.OnCheckedChangeListener mCheckListener;
    private ArrayAdapter<String> mRecentAdapter;
    private static Context mContext;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    private SharedPreferences sharedPrefs;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        mPrefs = Prefs.loadPrefs(mContext);
        setContentView(R.layout.main);

       /* sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                mPrefs = Prefs.loadPrefs(mContext);
                if(key.equals(Prefs.KEY_DEBUG) || key.equals(Prefs.KEY_ROOT_ACCESS)){
                    setActionBarTile();
                }

            }
        };*/
        //sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener);

        mDirListView = (LinearLayout)findViewById(R.id.listdir);
        mExtListView = (LinearLayout)findViewById(R.id.listext);

        mDirListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view)
            {
                final CheckedString strItem = (CheckedString) view.getTag();
                // Show Dialog
                new AlertDialog.Builder(mContext)
                .setTitle(strItem.string)
                .setMessage( getString(R.string.label_remove_item , strItem ) )
                .setPositiveButton(R.string.label_OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mPrefs.mDirList.remove(strItem);
                        refreshDirList();
                        mPrefs.savePrefs(mContext);
                    }
                })
                .setNegativeButton(R.string.label_CANCEL, null )
                .setCancelable(true)
                .show();
                return true;
            }
        };

        mExtListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view)
            {
                final String strText = (String) ((TextView)view).getText();
                final CheckedString strItem = (CheckedString) view.getTag();
                // Show Dialog
                new AlertDialog.Builder(mContext)
                .setTitle(strText)
                .setMessage( getString(R.string.label_remove_item , strText ) )
                .setPositiveButton(R.string.label_OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mPrefs.mExtList.remove(strItem);
                        refreshExtList();
                        mPrefs.savePrefs(mContext);
                    }
                })
                .setNegativeButton(R.string.label_CANCEL, null )
                .setCancelable(true)
                .show();
                return true;
            }
        };
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        mCheckListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final CheckedString strItem = (CheckedString) buttonView.getTag();
                strItem.checked = isChecked;
                if (mPrefs.mExtList.contains(strItem)) {
                    View v;
                    for (int i = 0; i < mExtListView.getChildCount(); i++) {
                        v = mExtListView.getChildAt(i);
                        if (v instanceof CheckBox) {
                            if (strItem.string.equals(getString(R.string.label_any_extension)) && strItem.checked && i > 0) {
                                ((CheckBox) v).setChecked(!strItem.checked);
                            } else if (!strItem.string.equals(getString(R.string.label_any_extension)) && strItem.checked && i == 0 && ((CheckBox) v).isChecked()) {
                                ((CheckBox) v).setChecked(!strItem.checked);
                            }
                        }
                    }
                }
                mPrefs.savePrefs(mContext);

            }
        };
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        refreshDirList();
        refreshExtList();

        Button btnAddDir = (Button) findViewById(R.id.adddir);
        Button btnAddExt = (Button) findViewById(R.id.addext);

        btnAddDir.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v)
            {
                new DialogChooseDirectory(mContext, (Result) mContext, mPrefs.mLastSelectedDirectory);
            }
        });

        btnAddExt.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view)
            {
                // Create EditText
                final EditText edtInput = new EditText(mContext);
                edtInput.setSingleLine();
                // Show Dialog
                AlertDialog.Builder b = new AlertDialog.Builder(mContext);
                        b.setTitle(R.string.label_addext);
                        b.setView(edtInput);
                        b.setIcon(R.drawable.ext_add);
                        b.setPositiveButton(R.string.label_ADD, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                        /* OKボタンをクリックした時の処理 */

                                String ext = edtInput.getText().toString();
                                if (ext.length() > 0) {
                                    //  boolean state = true;
                                    for (CheckedString t : mPrefs.mExtList) {
                                        if (t.string.equalsIgnoreCase(ext)) {
                                            return;
                                        }
                                    }
                                    if (ext.equals("*")) {
                                        for (CheckedString t : mPrefs.mExtList) {
                                            RootShell.log(RootShell.debugTag, t.string, RootShell.LogLevel.ERROR, null);
                                            CheckBox v = (CheckBox) mExtListView.findViewWithTag(t);
                                            if (null != v) {
                                                v.setChecked(false);
                                            }
                                        }
                                        mPrefs.mExtList.add(0, new CheckedString(ext));
                                        RootShell.log(RootShell.debugTag, "ADD", RootShell.LogLevel.ERROR, null);
                                    } else {
                                        CheckBox v = (CheckBox) mExtListView.getChildAt(0);
                                        if (null != v && v.getText().equals("*")) {
                                            v.setChecked(false);
                                        }
                                        mPrefs.mExtList.add(new CheckedString(ext));
                                    }
                                    refreshExtList();
                                    mPrefs.savePrefs(mContext);
                                }
                            }
                        });
                       b.setNeutralButton(R.string.label_no_extension, new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int whichButton) {
                        /* 拡張子無しボタンをクリックした時の処理 */

                               String ext = "*no_ext";
                               // 二重チェック
                               for (CheckedString t : mPrefs.mExtList) {
                                   if (t.string.equalsIgnoreCase(ext)) {
                                       return;
                                   }
                               }
                               CheckBox v = (CheckBox) mExtListView.getChildAt(0);
                               if (null != v && v.getText().equals("*")) {
                                   v.setChecked(false);
                               }
                               mPrefs.mExtList.add(new CheckedString(ext));
                               refreshExtList();
                               mPrefs.savePrefs(mContext);
                           }
                       });
                        b.setNegativeButton(R.string.label_CANCEL, null);
                b.setCancelable(true);
                AlertDialog dialog = b.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.show();
            }
        });


        final CheckBox chkRe = (CheckBox)findViewById(R.id.checkre);
        final CheckBox chkIc = (CheckBox)findViewById(R.id.checkmatchcase);
        final CheckBox chkem = (CheckBox)findViewById(R.id.checkmatchwholeword);

        chkRe.setChecked(mPrefs.mRegularExrpression);
        chkIc.setChecked(mPrefs.mMatchCase);
        chkem.setChecked(mPrefs.mMatchWhole);

        chkRe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPrefs.mRegularExrpression = chkRe.isChecked();
                mPrefs.savePrefs(mContext);
            }
        });
        
        chkem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPrefs.mMatchWhole = chkem.isChecked();
                mPrefs.savePrefs(mContext);
            }
        });
        
        chkIc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPrefs.mMatchCase = chkIc.isChecked();
                mPrefs.savePrefs(mContext);
            }
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        final AutoCompleteTextView edittext = (AutoCompleteTextView) findViewById(R.id.EditText01);
        edittext.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    String text = edittext.getEditableText().toString();
                    Intent it = new Intent(mContext, Search.class);
                    it.setAction(Intent.ACTION_SEARCH);
                    it.putExtra(SearchManager.QUERY, text);
                    startActivity(it);
                    return true;
                }
                return false;
            }
        });
        mRecentAdapter = new AutoComleteDropDownAdapter<>(mContext, R.layout.recents_row, new ArrayList<String>());
        edittext.setAdapter(mRecentAdapter);
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

        ImageButton clrBtn = (ImageButton) findViewById(R.id.ButtonClear);
        clrBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                edittext.setText("");
                edittext.requestFocus();
            }
        });

        ImageButton searchBtn = (ImageButton) findViewById(R.id.ButtonSearch);
        searchBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View view)
            {
                if (isTargetSelected(mPrefs.mExtList) && isTargetSelected(mPrefs.mDirList)) {
                    String text = edittext.getText().toString();
                    if (!text.isEmpty()) {
                        Intent it = new Intent(mContext, Search.class);
                        it.setAction(Intent.ACTION_SEARCH);
                        it.putExtra(SearchManager.QUERY, text);
                        startActivity(it);
                    }else {
                        Toast.makeText(mContext, R.string.search_text_empty, Toast.LENGTH_LONG).show();
                    }
                }else if (!isTargetSelected(mPrefs.mExtList)){
                    Toast.makeText(mContext, R.string.no_target_extension, Toast.LENGTH_LONG).show();
                }else if (!isTargetSelected(mPrefs.mDirList)){
                    Toast.makeText(mContext, R.string.no_target_directory, Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(mContext, R.string.no_target_extension, Toast.LENGTH_LONG).show();
                    Toast.makeText(mContext, R.string.no_target_directory, Toast.LENGTH_LONG).show();
                }
            }
        });
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
        ImageButton historyBtn = (ImageButton) findViewById(R.id.ButtonHistory);
        historyBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (mPrefs.getRecent(mContext).size() > 0){
                    edittext.showDropDown();
                }else {
                    Toast.makeText(mContext, "No recent history...", Toast.LENGTH_LONG).show();
                }
        }
    });
        setActionBarTile();
    }
    
    public void onChooseDirectory(String dir) {
        if (dir != null && dir.length()>0 ) {
                for( CheckedString t : mPrefs.mDirList ){
                    if ( t.string.equalsIgnoreCase(dir)){
                        return;
                    }
                }
                mPrefs.mDirList.add(new CheckedString(dir));
                mPrefs.mLastSelectedDirectory = dir;
                refreshDirList();
                mPrefs.savePrefs(mContext);
            }
    }

    void setListItem( LinearLayout view ,
            ArrayList<CheckedString> list ,
            View.OnLongClickListener logclicklistener ,
                      CompoundButton.OnCheckedChangeListener checkedChangeListener) {
        view.removeAllViews();
        Collections.sort(list, new Comparator<CheckedString>() {
            @Override
            public int compare(CheckedString object1, CheckedString object2) {
                return object1.string.compareToIgnoreCase(object2.string);
            }
        });
        for (CheckedString s : list ){
            CheckBox v = (CheckBox)View.inflate(this, R.layout.list_dir, null);
            if (s.string.equals("*no_ext")){
                v.setText(R.string.label_no_extension);
            }else{
                v.setText(s.string);
            }
            v.setChecked( s.checked );
            v.setTag(s);
            v.setOnLongClickListener(logclicklistener);
            v.setOnCheckedChangeListener(checkedChangeListener);
            view.addView(v);
        }
    }

    private void refreshDirList(){
        setListItem(mDirListView, mPrefs.mDirList, mDirListener, mCheckListener);
    }
    private void refreshExtList() {
        setListItem(mExtListView, mPrefs.mExtList, mExtListener, mCheckListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.optionmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                mPrefs = Prefs.loadPrefs(mContext);
                if(key.equals(Prefs.KEY_DEBUG) || key.equals(Prefs.KEY_ROOT_ACCESS)){
                    setActionBarTile();
                }
            }
        };
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefListener);
        if ( item.getItemId() == R.id.menu_option ){
            Intent intent = new Intent( this ,  OptionActivity.class );
            //1 - request we will act on in onActivityResult
            startActivityForResult(intent, 1);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
      //  if (requestCode == 1) {
            // Make sure the request was successful
        //    if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                // Do something with the contact here (bigger example below)
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        prefListener = null;
        sharedPrefs = null;
        recreate();
        RootShell.log(RootShell.debugTag, "Settings:onActivityResult:" + requestCode + ":" + resultCode + ":" + data);
           // }
      //  }
    }
//**************************************************************************************************************************************************************************************
    @Override
    protected void onResume() {
        super.onResume();
        final List<String> recent = mPrefs.getRecent(mContext);
        mRecentAdapter.clear();
        mRecentAdapter.addAll(recent);
        mRecentAdapter.notifyDataSetChanged();
    }

    protected boolean isTargetSelected(ArrayList<CheckedString> list){
        if (list.isEmpty()){
            return false;
        }
        for( CheckedString s : list ) {
            if (s.checked) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setActionBarTile(){
        String appname = getString(R.string.app_name);
        if (mPrefs.mIsRootAccess && mPrefs.mDebug) appname += "(root, debug)";
        if (mPrefs.mIsRootAccess && !mPrefs.mDebug) appname += "(root)";
        if (!mPrefs.mIsRootAccess && mPrefs.mDebug) appname += "(debug)";
        getActionBar().setTitle(appname);
    }
}
