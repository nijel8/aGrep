package bg.nijel.aGrep.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AutoCompleteTextView;

import com.stericson.RootShell.RootShell;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;



/**
 * Created by Nick on 1/6/2016.
 */
public class LongClickableTextView extends AutoCompleteTextView {

    public LongClickableTextView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
       // RootShell.log(RootShell.debugTag,"1defStyle:"+defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LongClickableTextView, defStyle, 0);
      //  RootShell.log(RootShell.debugTag,"2defStyle:"+defStyle);

        final int N = a.getIndexCount();
        for (int i = 0; i < N; ++i)
        {
            int attr = a.getIndex(i);
            switch (attr)
            {
                case R.styleable.LongClickableTextView_onItemLongClick: {
                    if (context.isRestricted()) {
                        throw new IllegalStateException("The "+getClass().getCanonicalName()+":onItemLongClick attribute cannot "
                                + "be used within a restricted context");
                    }

                    final String handlerName = a.getString(attr);
                    if (handlerName != null) {
                        setOnLongClickListener(new OnLongClickListener() {
                            private Method mHandler;

                            @Override
                            public boolean onLongClick(final View p_v) {
                                boolean result = false;
                                if (mHandler == null) {
                                    try {
                                        mHandler = getContext().getClass().getMethod(handlerName, View.class);
                                    } catch (NoSuchMethodException e) {
                                        int id = getId();
                                        String idText = id == NO_ID ? "" : " with id '"
                                                + getContext().getResources().getResourceEntryName(
                                                id) + "'";
                                        throw new IllegalStateException("Could not find a method " +
                                                handlerName + "(View) in the activity "
                                                + getContext().getClass() + " for onItemLongClick handler"
                                                + " on view " + LongClickableTextView.this.getClass() + idText, e);
                                    }
                                }

                                try {
                                    mHandler.invoke(getContext(), LongClickableTextView.this);
                                    result = true;
                                } catch (IllegalAccessException e) {
                                    throw new IllegalStateException("Could not execute non "
                                            + "public method of the activity", e);
                                } catch (InvocationTargetException e) {
                                    throw new IllegalStateException("Could not execute "
                                            + "method of the activity", e);
                                }
                                return result;
                            }
                        });
                    }
                    break;
                }
                default:
                    break;
            }
        }
        a.recycle();

    }

}
