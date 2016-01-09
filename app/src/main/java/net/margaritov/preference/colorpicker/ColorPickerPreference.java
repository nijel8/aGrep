/*
 * Copyright (C) 2011 Sergey Margaritov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.margaritov.preference.colorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import bg.nijel.aGrep.Prefs;
import bg.nijel.aGrep.R;
import bg.nijel.aGrep.utils.ColorPickerPanelViewOld;

/**
 * A preference type that allows a user to choose a time
 *
 * @author Sergey Margaritov
 */
public class ColorPickerPreference
        extends
        Preference
        implements
        Preference.OnPreferenceClickListener,
        ColorPickerDialog.OnColorChangedListener {

    View mView;
    ColorPickerDialog mDialog;
    private int mValue = Color.BLACK;
    private float mDensity = 0;
    private boolean mAlphaSliderEnabled = false;
    private boolean mHexValueEnabled = false;

    //my field
    String whichColor = "";

    public ColorPickerPreference(Context context) {
        super(context);
        init(context, null);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    /**Method edited by
     * @author Anna Berkovitch
     * added functionality to accept hex string as defaultValue
     * and to properly persist resources reference string, such as @color/someColor
     * previously persisted 0*/
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        int colorInt;
        String mHexDefaultValue = a.getString(index);
        if (mHexDefaultValue != null && mHexDefaultValue.startsWith("#")) {
            colorInt = convertToColorInt(mHexDefaultValue);
            return colorInt;

        } else {
            return a.getColor(index, Color.BLACK);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        onColorChanged(restoreValue ? getPersistedInt(mValue) : (Integer) defaultValue);
    }

    private void init(Context context, AttributeSet attrs) {
        mDensity = getContext().getResources().getDisplayMetrics().density;
        setOnPreferenceClickListener(this);
        if (attrs != null) {
            mAlphaSliderEnabled = attrs.getAttributeBooleanValue(null, "alphaSlider", false);
            mHexValueEnabled = attrs.getAttributeBooleanValue(null, "hexValue", false);
        }
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mView = view;
        setPreviewColor();
    }

    private void setPreviewColor() {
        if (mView == null) return;
        ImageView iView = new ImageView(getContext());
        LinearLayout widgetFrameView = ((LinearLayout) mView.findViewById(android.R.id.widget_frame));
        if (widgetFrameView == null) return;
        widgetFrameView.setVisibility(View.VISIBLE);
        widgetFrameView.setPadding(
                widgetFrameView.getPaddingLeft(),
                widgetFrameView.getPaddingTop(),
                0,// (int) (mDensity * 8), //original code
                widgetFrameView.getPaddingBottom()
        );
        // remove already create preview image
        int count = widgetFrameView.getChildCount();
        if (count > 0) {
            widgetFrameView.removeViews(0, count);
        }
        widgetFrameView.addView(iView);
        widgetFrameView.setMinimumWidth(0);
       // iView.setBackgroundDrawable(new AlphaPatternDrawable((int) (5 * mDensity)));
        iView.setImageBitmap(getPreviewBitmap());
    }

    private Bitmap getPreviewBitmap() {
        int d = (int) (mDensity * 40); //30dip  //original code was * 31
        int color = mValue;
        Bitmap bmrec = Bitmap.createBitmap(d, d, Config.ARGB_8888);
        //code borrowed from NativeClipboard author... sorry for stealing it :(
        Bitmap bmcir = Bitmap.createBitmap(bmrec.getWidth(), bmrec.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bmcir);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bmrec.getWidth(), bmrec.getHeight());
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(0xFFCCCCCC);//grey circle outline
        canvas.drawCircle(bmrec.getWidth() / 2, bmrec.getHeight() / 2, bmrec.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bmcir, rect, rect, paint);
        Canvas canvas1 = new Canvas(bmcir);
        Paint paint1 = new Paint();
        Rect rect1 = new Rect(0, 0, bmrec.getWidth(), bmrec.getHeight());
        paint1.setAntiAlias(true);
        canvas1.drawARGB(0, 0, 0, 0);
        paint1.setColor(color);
        canvas1.drawCircle(bmrec.getWidth() / 2, bmrec.getHeight() / 2, (bmrec.getWidth() - (int)(mDensity * 4)) / 2, paint1);
        paint1.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas1.drawBitmap(bmcir, rect1, rect1, paint1);
        return bmcir;
        //end
        //original code
        /*int w = bm.getWidth();
        int h = bm.getHeight();
        int c = color;
        for (int i = 0; i < w; i++) {
            for (int j = i; j < h; j++) {
                c = (i <= 1 || j <= 1 || i >= w - 2 || j >= h - 2) ? Color.GRAY : color;
                bm.setPixel(i, j, c);
                if (i != j) {
                    bm.setPixel(j, i, c);
                }
            }
        }
        return bm;*/
    }

    @Override
    public void onColorChanged(int color) {
        if (isPersistent()) {
            persistInt(color);
        }
        mValue = color;
        setPreviewColor();
        try {
            getOnPreferenceChangeListener().onPreferenceChange(this, color);
        } catch (NullPointerException e) {

        }
    }

    public boolean onPreferenceClick(Preference preference) {
        //my code
        if (preference != null) {
            whichColor = preference.getKey();
        }
        //end my code
        showDialog(null);
        return false;
    }

    protected void showDialog(Bundle state) {
        mDialog = new ColorPickerDialog(getContext(), mValue);
        //my code
        Prefs p = Prefs.loadPrefs(getContext());
        ColorPickerPanelView.mColorText = p.mHighlightFg;
        ColorPickerPanelView.mColor = p.mHighlightBg;
        ColorPickerPanelViewOld.mColorText = ColorPickerPanelView.mColorText;
        ColorPickerPanelViewOld.mColor = ColorPickerPanelView.mColor;
        if (whichColor.equals(Prefs.KEY_HIGHLIGHTFG)) {
            ColorPickerPanelView.mIsTextColor = true;
            mDialog.setTitle(R.string.label_highlight_fg);
        } else if (whichColor.equals(Prefs.KEY_HIGHLIGHTBG)) {
            ColorPickerPanelView.mIsTextColor = false;
            mDialog.setTitle(R.string.label_highlight_bg);
        }
        //end my code
        mDialog.setOnColorChangedListener(this);
        if (mAlphaSliderEnabled) {
            mDialog.setAlphaSliderVisible(true);
        }
        if (mHexValueEnabled) {
            mDialog.setHexValueEnabled(true);
        }
        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.show();
    }

    /**
     * Toggle Alpha Slider visibility (by default it's disabled)
     *
     * @param enable
     */
    public void setAlphaSliderEnabled(boolean enable) {
        mAlphaSliderEnabled = enable;
    }

    /**
     * Toggle Hex Value visibility (by default it's disabled)
     *
     * @param enable
     */
    public void setHexValueEnabled(boolean enable) {
        mHexValueEnabled = enable;
    }

    /**
     * For custom purposes. Not used by ColorPickerPreferrence
     *
     * @param color
     * @author Unknown
     */
    public static String convertToARGB(int color) {
        String alpha = Integer.toHexString(Color.alpha(color));
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (alpha.length() == 1) {
            alpha = "0" + alpha;
        }

        if (red.length() == 1) {
            red = "0" + red;
        }

        if (green.length() == 1) {
            green = "0" + green;
        }

        if (blue.length() == 1) {
            blue = "0" + blue;
        }

        return "#" + alpha + red + green + blue;
    }

    /**
     * Method currently used by onGetDefaultValue method to
     * convert hex string provided in android:defaultValue to color integer.
     *
     * @param color
     * @return A string representing the hex value of color,
     * without the alpha value
     * @author Charles Rosaaen
     */
    public static String convertToRGB(int color) {
        String red = Integer.toHexString(Color.red(color));
        String green = Integer.toHexString(Color.green(color));
        String blue = Integer.toHexString(Color.blue(color));

        if (red.length() == 1) {
            red = "0" + red;
        }

        if (green.length() == 1) {
            green = "0" + green;
        }

        if (blue.length() == 1) {
            blue = "0" + blue;
        }

        return "#" + red + green + blue;
    }

    /**
     * For custom purposes. Not used by ColorPickerPreferrence
     *
     * @param argb
     * @throws NumberFormatException
     * @author Unknown
     */
    public static int convertToColorInt(String argb) throws IllegalArgumentException {

        if (!argb.startsWith("#")) {
            argb = "#" + argb;
        }

        return Color.parseColor(argb);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mDialog == null || !mDialog.isShowing()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.dialogBundle = mDialog.onSaveInstanceState();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !(state instanceof SavedState)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        showDialog(myState.dialogBundle);
    }

    private static class SavedState extends BaseSavedState {
        Bundle dialogBundle;

        public SavedState(Parcel source) {
            super(source);
            dialogBundle = source.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeBundle(dialogBundle);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}