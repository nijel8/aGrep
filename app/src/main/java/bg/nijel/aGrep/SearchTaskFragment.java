package bg.nijel.aGrep;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import com.stericson.RootShell.RootShell;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bg.nijel.aGrep.utils.EncodingHelper;
import bg.nijel.aGrep.utils.RootCommands;

/**
 * Created by Nick on 1/13/2016.
 */
public class SearchTaskFragment extends Fragment {

    public static final String TAG_SEARCH_FRAGMENT = "search_fragment";

    SearchStatusCallback mSearchStatusCallback;
    SearchTask mSearchTask;
    public static boolean isSearchExecuting = false;
    public static ArrayList<GrepView.Data> mData;
    public static ArrayList<String> mTargetFiles = new ArrayList<>();
    private static ArrayList<CheckedString> mDirList = new ArrayList<>();
    private static ArrayList<CheckedString> mExtList = new ArrayList<>();
    public static String mSearchingFile = "";
    public static int mFoundcount = 0;
    public static int mFileCount = 0;
    public static Pattern mPattern;
    public static int mHits = 0;
    private static Timer mTimer;

    public interface SearchStatusCallback {
        void onPreExecute();

        void onProgressUpdate(int progress);

        void onPostExecute();

        void onCancelled();
    }

    /**
     * Called when a fragment is first attached to its activity.
     * onCreate(Bundle) will be called after this.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSearchStatusCallback = (SearchStatusCallback) context;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mSearchStatusCallback = (SearchStatusCallback) activity;
        // mContext = activity;
    }

    /**
     * Called to do initial creation of a fragment.
     * This is called after onAttach(Activity) and before onCreateView(LayoutInflater, ViewGroup, Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    /**
     * Called when the fragment is no longer attached to its activity. This is called after onDestroy().
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mSearchStatusCallback = null;
    }

    public void startSearchTask(Context context) {
            if (mSearchTask == null && !isSearchExecuting) {
                mData = new ArrayList<>();
                mDirList = Prefs.loadPrefs(context).mDirList;
                mExtList = Prefs.loadPrefs(context).mExtList;
                mSearchTask = new SearchTask();
                mSearchTask.execute();
                isSearchExecuting = true;
            }
    }

    public void cancelSearchTask() {
            if (mSearchTask != null) {
                RootCommands.cancelAllRootCommands();
                mSearchTask.cancel(true);
                isSearchExecuting = false;
            }
    }

    public void updateSearchStatus(boolean isSearching) {
        isSearchExecuting = isSearching;
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
    public void onDestroy(){
        super.onDestroy();
        Runtime.getRuntime().gc();
    }

    private class SearchTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            mData.clear();
            mTargetFiles.clear();
            mSearchingFile = "";
            mFoundcount = 0;
            mFileCount = 0;
            mHits = 0;
            if (mSearchStatusCallback != null) {
                mSearchStatusCallback.onPreExecute();
            }
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SearchTaskActivity.mProgressFilteredCount.post(new Runnable() {
                        public void run() {
                            SearchTaskActivity.mProgressFilteredCount.setText(String.valueOf(mTargetFiles.size()));
                        }
                    });
                }
            }, 0, 1);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // RootShell.log("search_task mDirList: " + mDirList.size());
            ArrayList<String> allFiles = new ArrayList<>();
            for (CheckedString dir : mDirList) {
                if (!isSearchExecuting) {
                    return null;
                }
                if (dir.checked) {
                    allFiles.addAll(RootCommands.listFiles(dir.string));
                }
            }
            // RootShell.log("search_task allFiles: " + allFiles.size());
            filterMatchingFiles(allFiles);
            if (mTargetFiles != null && mTargetFiles.size() > 0 && isSearchExecuting) {
                // RootShell.log("search_task mTaegetFiles: " + mTargetFiles.size());
                mTimer.cancel();
                mTimer.purge();
                mTimer = null;
                grepFile();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (mSearchStatusCallback != null) {
                mSearchStatusCallback.onProgressUpdate(progress[0]);
            }
        }


        @Override
        protected void onPostExecute(Void result) {
            if (mSearchStatusCallback != null)
                mSearchStatusCallback.onPostExecute();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (mSearchStatusCallback != null) {
                mSearchStatusCallback.onCancelled();
            }
            mSearchTask = null;
        }

        private boolean filterMatchingFiles(final ArrayList<String> files) {
            if (!isSearchExecuting) {
                return false;
            }
            mTargetFiles.clear();
            filesloop:
            for (final String file : files) {
                if (!isSearchExecuting) {
                    return false;
                }
                for (CheckedString ext : mExtList) {
                    if (!isSearchExecuting) {
                        return false;
                    }
                    if (ext.checked) {
                        if (ext.string.equals("*")) {
                            mTargetFiles.add(file);
                            continue filesloop;
                        }
                        if (ext.string.equals("*no_ext") && !file.substring(file.lastIndexOf('/') + 1, file.length()).contains(".")) {
                            mTargetFiles.add(file);
                            continue filesloop;
                        }
                        if (file.toLowerCase().endsWith("." + ext.string.toLowerCase())) {
                            mTargetFiles.add(file);
                            continue filesloop;
                        }
                        if (ext.string.equals(".") && file.endsWith(".")) {
                            mTargetFiles.add(file);
                            continue filesloop;
                        }
                    }
                }
            }
            return true;
        }

        boolean grepFile() {
            if (!isSearchExecuting) {
                return false;
            }
            for (String path : mTargetFiles) {
                if (!isSearchExecuting) {
                    return false;
                }
                mSearchingFile = path.substring(path.lastIndexOf('/') + 1, path.length());
                publishProgress(mFileCount++);
                int firstmatch = 0;
                File file = new File(path);
                EncodingHelper helper;
                RootShell.log("file exists & canRead: " + file.getPath() + "->" + (file.exists() && file.canRead()));
                if (file.exists() && file.canRead()) {
                    helper = new EncodingHelper(file);
                }else {
                    String hex = RootCommands.fileAsHex(path);
                    if (hex != null) {
                        helper = new EncodingHelper(hex);
                    }else {
                        continue;
                    }
                }
                BufferedReader reader;
                try {
                    reader = helper.getReader();
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
                if (reader == null) {
                    continue;
                }
                String text;
                int line = 0;
                boolean found = false;
                Matcher m = null;
                ArrayList<GrepView.Data> data = null;
                try {
                    while ((text = reader.readLine()) != null) {
                        line++;
                        if (m == null) {
                            m = mPattern.matcher(text);
                        } else {
                            m.reset(text);
                        }
                        if (m.find()) {
                            found = true;
                            firstmatch++;
                            if (firstmatch == 1) {
                                mFoundcount++;
                            }
                            synchronized (mData) {
                                mHits++;
                                if (data == null) {
                                    data = new ArrayList<>();
                                }
                                data.add(new GrepView.Data(file, line, text));

                                if (mHits < 10) {
                                    updateData(data.toArray(new GrepView.Data[0]));
                                    data = null;
                                }
                            }
                            if (!isSearchExecuting) {
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                helper.reset();
                if (data != null) {
                    updateData(data.toArray(new GrepView.Data[0]));
                }
                if (!found) {
                    if (mFileCount % 10 == 0) {//every 10 files is true
                        updateData((GrepView.Data[]) null);
                    }
                }
            }
            return true;
        }

        private void updateData(GrepView.Data... dataset) {
            if (!isSearchExecuting) {
                return;
            }
            if (dataset != null) {
                synchronized (mData) {
                    Collections.addAll(mData, dataset);
                }
            }
        }
    }
}