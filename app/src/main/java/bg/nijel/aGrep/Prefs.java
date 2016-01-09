package bg.nijel.aGrep;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.stericson.RootShell.RootShell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class Prefs
{
    public static final String KEY_DEBUG = "debug";
    public static final String KEY_DEBUG_TAG = "debugtag";
    public static final String KEY_ROOT_ACCESS = "rootaccess";
    public static final String KEY_MATCH_WHOLE = "MatchWhole";
    public static final String KEY_MATCH_CASE = "MatchCase";
    public static final String KEY_REGULAR_EXPRESSION = "RegularExpression";
    public static final String KEY_TARGET_EXTENSIONS_OLD = "TargetExtensions";
    public static final String KEY_TARGET_DIRECTORIES_OLD = "TargetDirectories";
    public static final String KEY_TARGET_EXTENSIONS_NEW = "TargetExtensionsNew";
    public static final String KEY_TARGET_DIRECTORIES_NEW = "TargetDirectoriesNew";
    public static final String KEY_FONTSIZE = "FontSize";
    public static final String KEY_HIGHLIGHTFG = "HighlightFg";
    public static final String KEY_HIGHLIGHTBG = "HighlightBg";
    public static final String KEY_ADD_LINENUMBER = "AddLineNumber";
    public static final String KEY_LAST_SELECTED_DIRECTORY = "LastSelectedDirectory";

    private static final String PREF_RECENT= "recent";

    boolean mDebug = false;
    String mDebugTag = "aGrepD";

    String mLastSelectedDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
    boolean mIsRootAccess = false;
    boolean mMatchWhole = true;
    boolean mRegularExrpression = false;
    boolean mMatchCase = false;
    int mFontSize = 16;
    public int mHighlightBg = 0xFF009688;
    public int mHighlightFg = 0xFFFFFFFF;
    boolean addLineNumber=false;
    ArrayList<CheckedString> mDirList = new ArrayList<>();
    ArrayList<CheckedString> mExtList = new ArrayList<>();

    static public Prefs loadPrefs(Context ctx)
    {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);

        Prefs prefs = new Prefs();

        // target directory
        String dirs = sp.getString(KEY_TARGET_DIRECTORIES_NEW,"" );
        prefs.mDirList	= new ArrayList<>();
        if ( dirs.length()>0 ){
            RootShell.log(RootShell.debugTag,"new dirs----" + dirs);
            String[] dirsarr = dirs.split("\\|");
            int size = dirsarr.length;
            for( int i=0;i<size;i+=2 ){
                boolean c = dirsarr[i].equals("true");
                String s = dirsarr[i+1];
                prefs.mDirList.add(new CheckedString(c,s));
            }
        }else{
            dirs = sp.getString(KEY_TARGET_DIRECTORIES_OLD,"" );
            RootShell.log(RootShell.debugTag,"old dirs----" + dirs);
            if ( dirs.length()>0 ){
                String[] dirsarr = dirs.split("\\|");
                for (String aDirsarr : dirsarr) {
                    prefs.mDirList.add(new CheckedString(aDirsarr));
                }
            }
        }

        // target extensions
        String exts = sp.getString(KEY_TARGET_EXTENSIONS_NEW,"" );
        prefs.mExtList	= new ArrayList<>();
        if ( exts.length()>0 ){
            String[] arr = exts.split("\\|");
            int size = arr.length;
            for( int i=0;i<size;i+=2 ){
                boolean c = arr[i].equals("true");
                String s = arr[i+1];
                prefs.mExtList.add(new CheckedString(c,s));
            }
        }else{
            exts = sp.getString(KEY_TARGET_EXTENSIONS_OLD,"*" /* strings -> label_any_extension */ );
            if ( exts.length()>0 ){
                String[] arr = exts.split("\\|");
                for (String anArr : arr) {
                    if (anArr.equals("*")){
                        prefs.mExtList.add(0, new CheckedString(anArr));
                    }else {
                        prefs.mExtList.add(new CheckedString(anArr));
                    }
                }
            }
        }

        prefs.mDebug = sp.getBoolean(KEY_DEBUG, false);
        prefs.mDebugTag = sp.getString(KEY_DEBUG_TAG, "aGrepD");
        prefs.mLastSelectedDirectory = sp.getString(KEY_LAST_SELECTED_DIRECTORY, Environment.getExternalStorageDirectory().getAbsolutePath());
        prefs.mRegularExrpression = sp.getBoolean(KEY_REGULAR_EXPRESSION, false);
        prefs.mMatchCase = sp.getBoolean(KEY_MATCH_CASE, false);
        prefs.mMatchWhole = sp.getBoolean(KEY_MATCH_WHOLE, true );

        prefs.mFontSize = Integer.parseInt(sp.getString(KEY_FONTSIZE, "-1"));
        prefs.mHighlightFg = sp.getInt(KEY_HIGHLIGHTFG, 0xFFFFFFFF);
        prefs.mHighlightBg = sp.getInt(KEY_HIGHLIGHTBG, 0xFF009688);

        prefs.addLineNumber = sp.getBoolean(KEY_ADD_LINENUMBER, false);
        prefs.mIsRootAccess = sp.getBoolean("rootaccess", false);

        RootShell.debugMode = prefs.mDebug;
        RootShell.debugTag = prefs.mDebugTag;

        RootShell.log(RootShell.debugTag, "Prefs:Loaded...");

        return prefs;
    }

    public void savePrefs(Context context)
    {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        Editor editor = sp.edit();

        // target directory
        StringBuilder dirs = new StringBuilder();
        for( CheckedString t : mDirList ){
            dirs.append(t.checked);
            dirs.append('|');
            dirs.append(t.string);
            dirs.append('|');
        }
        if ( dirs.length() > 0 ){
            dirs.deleteCharAt(dirs.length()-1);
        }

        // target extensions
        StringBuilder exts = new StringBuilder();
        for( CheckedString t : mExtList ){
            exts.append(t.checked);
            exts.append('|');
            exts.append(t.string);
            exts.append('|');
        }
        if ( exts.length() > 0 ){
            exts.deleteCharAt(exts.length()-1);
        }

        editor.putString(KEY_LAST_SELECTED_DIRECTORY, mLastSelectedDirectory );
        editor.putString(KEY_TARGET_DIRECTORIES_NEW, dirs.toString() );
        editor.putString(KEY_TARGET_EXTENSIONS_NEW, exts.toString() );
        editor.remove(KEY_TARGET_DIRECTORIES_OLD);
        editor.remove(KEY_TARGET_EXTENSIONS_OLD);
        editor.putString(KEY_DEBUG_TAG, mDebugTag);
        editor.putBoolean(KEY_MATCH_WHOLE, mMatchWhole);
        editor.putBoolean(KEY_REGULAR_EXPRESSION, mRegularExrpression );
        editor.putBoolean(KEY_MATCH_CASE, mMatchCase );

        editor.apply();

        RootShell.log(RootShell.debugTag, "Prefs:Saved...");

    }

    public void addRecent(Context context , String searchWord)
    {
        // 書き出し
        final SharedPreferences sp = context.getSharedPreferences(PREF_RECENT, Context.MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putLong(searchWord, System.currentTimeMillis());
        editor.apply();
    }

    public static void removeRecentItem(Context context , String searchWord){
        final SharedPreferences sp = context.getSharedPreferences(PREF_RECENT, Context.MODE_PRIVATE);
        sp.edit().remove(searchWord).apply();
    }

    public List<String> getRecent(Context context)
    {
        // ロード
        final SharedPreferences sp = context.getSharedPreferences(PREF_RECENT, Context.MODE_PRIVATE);
        Map<String,?> all = sp.getAll();

        // ソート
        List<Entry<String,?>> entries = new ArrayList<Entry<String,?>>(all.entrySet());
        Collections.sort(entries, new Comparator<Entry<String,?>>(){
            public int compare(Entry<String,?> e1, Entry<String,?> e2){
                return ((Long)e2.getValue()).compareTo((Long)e1.getValue());
            }
        });
        // 取り出し
        ArrayList<String> result = new ArrayList<>();
        for (Entry<String,?> entry : entries) {
            result.add(entry.getKey());
        }

        // 30個目以降は削除
        final int MAX = 30;
        final int size = result.size();
        if ( size > MAX ){
            Editor editor = sp.edit();
            for( int i=size-1 ; i>=MAX ; i-- ){
                editor.remove(result.get(i));
                result.remove(i);
            }
            editor.apply();
        }
        return result;
    }
}