package bg.nijel.aGrep;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import bg.nijel.aGrep.utils.RootCommands;
import me.grantland.widget.AutofitHelper;

public class DialogChooseDirectory implements OnClickListener, OnItemClickListener {
    AlertDialog m_alertDialog;
    Context m_context;
    File m_currentDir;
    List<File> m_entries = new ArrayList<>();
    ListView m_list;
    Result m_result = null;
    private Prefs mPrefs;
    private TextView title;
    public static String mBrowsingFolder;


    public class DirAdapter extends ArrayAdapter<File> {
        public DirAdapter(int resid) {
            super(DialogChooseDirectory.this.m_context, resid, DialogChooseDirectory.this.m_entries);
        }

        @SuppressWarnings("deprecation")
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textview = (TextView) super.getView(position, convertView, parent);
            if (DialogChooseDirectory.this.m_entries.get(position) != null) {
                textview.setText(DialogChooseDirectory.this.m_entries.get(position).getName());
                if (!DialogChooseDirectory.this.m_entries.get(position).getName().equals("..")) {
                    textview.setCompoundDrawablesWithIntrinsicBounds(DialogChooseDirectory.this.m_context.getResources().getDrawable(R.drawable.folder), null, null, null);
                }
                else {
                    textview.setCompoundDrawablesWithIntrinsicBounds(DialogChooseDirectory.this.m_context.getResources().getDrawable(R.drawable.folder_up), null, null, null);
                }
            }
            return textview;
        }
    }

    public interface Result {
        void onChooseDirectory(String str);
    }

    private void listDirs() {
        this.m_entries.clear();
        RootShell.log(RootShell.debugTag, " list canRead:" + this.m_currentDir.canRead(), RootShell.LogLevel.ERROR, null);
        if (this.m_currentDir.canRead()) {
            File[] files = this.m_currentDir.listFiles();
            if (this.m_currentDir.getParent() != null) {
                this.m_entries.add(new File(".."));
            }
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        this.m_entries.add(file);
                    }
                }
            }
        }else if (mPrefs.mIsRootAccess) {
            if (RootShell.isAccessGiven()) {
                this.m_entries = RootCommands.listFolders(this.m_currentDir.getAbsolutePath());
            }else {
                Toast.makeText(this.m_context, this.m_context.getString(R.string.label_no_root_access), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this.m_context, this.m_currentDir.getAbsolutePath() + ": " + this.m_context.getString(R.string.label_unable_access), Toast.LENGTH_SHORT).show();
        }
        Collections.sort(this.m_entries, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
            }
        });
    }

    @SuppressWarnings("deprecation")
    public DialogChooseDirectory(Context ctx, Result res, String startDir) {
        this.m_context = ctx;
        this.m_result = res;
        this.mPrefs = Prefs.loadPrefs(this.m_context);
        this.mBrowsingFolder = startDir;
        if (startDir != null) {
            this.m_currentDir = new File(startDir);
        } else {
            this.m_currentDir = Environment.getExternalStorageDirectory();
        }
        listDirs();
        DirAdapter adapter = new DirAdapter(R.layout.listitem_row_textview);
        final Builder builder = new Builder(ctx);
      //  builder.setTitle(DialogChooseDirectory.this.m_currentDir.getAbsolutePath());
       // builder.setIcon(R.drawable.folder_add);
        builder.setAdapter(adapter, this);
        builder.setPositiveButton("SELECT", new OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (DialogChooseDirectory.this.m_result != null) {
                    DialogChooseDirectory.this.m_result.onChooseDirectory(DialogChooseDirectory.this.m_currentDir.getAbsolutePath());
                }
                dialog.dismiss();
                mBrowsingFolder = null;
            }
        });

        builder.setNeutralButton("SDCARD", new OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                new DialogChooseDirectory(DialogChooseDirectory.this.m_context, DialogChooseDirectory.this.m_result, Environment.getExternalStorageDirectory().getAbsolutePath());
                dialog.dismiss();
                mBrowsingFolder = null;
            }
        });

        builder.setNegativeButton("CANCEL", new OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                mBrowsingFolder = null;
            }
        });

        this.title = new TextView(ctx);
        this.title.setGravity(Gravity.CENTER_VERTICAL);
        this.title.setPadding(50, 0, 50, 0);
        this.title.setMinLines(2);
        this.title.setMaxLines(5);
        this.title.setText(DialogChooseDirectory.this.m_currentDir.getAbsolutePath());
        if (Build.VERSION.SDK_INT < 23) {
            this.title.setTextAppearance(ctx, android.R.style.TextAppearance_Material_Title);
        } else {
            this.title.setTextAppearance(android.R.style.TextAppearance_Material_Title);
        }
        this.title.setCompoundDrawables(scaleDrawable(DialogChooseDirectory.this.m_context.getResources().getDrawable(R.drawable.folder_add, null), 90, 90), null, null, null);
        AutofitHelper.create(this.title);
        builder.setCustomTitle(this.title);
        m_alertDialog = builder.create();
        this.m_list = m_alertDialog.getListView();
        this.m_list.setOnItemClickListener(this);
        m_alertDialog.show();
    }

    static public Drawable scaleDrawable(Drawable drawable, int width, int height) {
        int wi = drawable.getIntrinsicWidth();
        int hi = drawable.getIntrinsicHeight();
        int dimDiff = Math.abs(wi - width) - Math.abs(hi - height);
        float scale = (dimDiff > 0) ? width/(float)wi : height/(float)hi;
        Rect bounds = new Rect(0, 0, (int)(scale*wi), (int)(scale*hi));
        drawable.setBounds(bounds);
        return drawable;
    }

    public void onItemClick(AdapterView<?> adapterView, View list, int pos, long id) {
        if (pos >= 0 && pos < this.m_entries.size()) {
            if (this.m_entries.get(pos).getName().equals("..")) {
                RootShell.log(RootShell.debugTag, "PARENT:" + this.m_currentDir.getParentFile().getAbsolutePath(), RootShell.LogLevel.WARN, null);
                this.m_currentDir = this.m_currentDir.getParentFile();
            } else {
                RootShell.log(RootShell.debugTag, "CLICKED" + this.m_entries.get(pos).getAbsolutePath() + "->" + this.m_entries.get(pos).canRead(), RootShell.LogLevel.ERROR, null);
                this.m_currentDir = this.m_entries.get(pos);
            }
            listDirs();
            this.title.setText(DialogChooseDirectory.this.m_currentDir.getAbsolutePath());
            mBrowsingFolder = DialogChooseDirectory.this.m_currentDir.getAbsolutePath();/////////////////////////////////////
            //this.m_alertDialog.setTitle(DialogChooseDirectory.this.m_currentDir.getAbsolutePath());
            this.m_list.setAdapter(new DirAdapter(R.layout.listitem_row_textview));
        }
    }

    public void onClick(DialogInterface dialog, int which) {
    }

}
