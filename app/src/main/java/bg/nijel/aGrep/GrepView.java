package bg.nijel.aGrep;

import android.content.Context;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Pattern;

public class GrepView extends ListView {

    private GrepViewCallback mCallback;

    interface GrepViewCallback {
        void onGrepItemClicked(int position);
        boolean onGrepItemLongClicked(int position);
    }

    public GrepView(Context context) {
        super(context);
        init();
    }

    public GrepView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public GrepView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setSmoothScrollbarEnabled(true);
        setScrollingCacheEnabled(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setFastScrollEnabled(true);
        setBackgroundColor(Color.WHITE);
        setCacheColorHint(Color.WHITE);
        setDividerHeight(2);
        setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mCallback != null) {
                    mCallback.onGrepItemClicked(position);
                }
            }
        });
        setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return mCallback != null && mCallback.onGrepItemLongClicked(position);
            }
        });
        setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState != SCROLL_STATE_IDLE) {
                    setFastScrollAlwaysVisible(true);
                } else {
                    setFastScrollAlwaysVisible(false);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });
    }

    public void setCallback(GrepViewCallback cb) {
        mCallback = cb;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        requestFocus();
        return super.onTouchEvent(ev);
    }

    static class GrepAdapter extends ArrayAdapter<Data> {

        private Pattern mPattern;
        private int mFgColor;
        private int mBgColor;
        private int mFontSize;

        static class ViewHolder {
            TextView Parent;
            TextView Index;
            TextView kwic;
        }


        public GrepAdapter(Context context, int resource, int textViewResourceId, ArrayList<Data> objects) {
            super(context, resource, textViewResourceId, objects);
          //  mAdapter = this;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflate(getContext(), R.layout.list_row, null);
                holder = new ViewHolder();
                holder.Parent = (TextView) convertView.findViewById(R.id.ListIndexParent);
                holder.Index = (TextView) convertView.findViewById(R.id.ListIndex);
                holder.kwic = (TextView) convertView.findViewById(R.id.ListPhone);
                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.Parent.setTextColor(Color.BLUE);
            holder.Index.setTextColor(mBgColor);
            holder.kwic.setTextColor(Color.BLACK);
            holder.Index.setTextSize(mFontSize - 2);
            holder.Parent.setTextSize(mFontSize - 2);
            holder.kwic.setTextSize(mFontSize);
            Data mData = getItem(position);
            String fparent = mData.mFile.getParent() + File.separator;
            String fname = mData.mFile.getName() + "(" + mData.mLinenumber + "):";
            SpannableString ffound = SearchTaskActivity.highlightKeyword(mData.mText, mPattern, mFgColor, mBgColor);
            holder.Parent.setText(fparent);
            holder.Index.setText(fname);
            holder.kwic.setText(ffound);
            return convertView;
        }

        public void setFormat(Pattern pattern, int fgcolor, int bgcolor, int size) {
            mPattern = pattern;
            mFgColor = fgcolor;
            mBgColor = bgcolor;
            mFontSize = size;

        }
    }

    static class Data implements Comparator<Data>, Parcelable {

        public File mFile;
        public int mLinenumber;
        public CharSequence mText;

        public Data() {
            this(null, 0, null);
        }

        public Data(File file, int linenumber, CharSequence text) {
            mFile = file;
            mLinenumber = linenumber;
            mText = text;
        }

        @Override
        public int compare(Data object1, Data object2) {
            int ret = object1.mFile.getName().compareToIgnoreCase(object2.mFile.getName());
            if (ret == 0) {
                ret = object1.mLinenumber - object2.mLinenumber;
            }
            return ret;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeSerializable(this.mFile);
            dest.writeInt(this.mLinenumber);
            TextUtils.writeToParcel(this.mText, dest, 0);
        }

        protected Data(Parcel in) {
            this.mFile = (File) in.readSerializable();
            this.mLinenumber = in.readInt();
            this.mText = in.readParcelable(CharSequence.class.getClassLoader());
        }

        public static final Parcelable.Creator<Data> CREATOR = new Parcelable.Creator<Data>() {
            public Data createFromParcel(Parcel source) {
                return new Data(source);
            }

            public Data[] newArray(int size) {
                return new Data[size];
            }
        };
    }
}
