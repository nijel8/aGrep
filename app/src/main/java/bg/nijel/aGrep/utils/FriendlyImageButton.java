package bg.nijel.aGrep.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * Created by Nick on 1/11/2016.
 */
public class FriendlyImageButton extends ImageButton {

    private int mImageResource = 0;

    public FriendlyImageButton(Context context) {
        super(context, null);
    }

    public FriendlyImageButton(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.imageButtonStyle);
    }

    public FriendlyImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
    }

    public FriendlyImageButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setFocusable(true);
    }

    @Override
    public void setImageResource (int resId) {
        mImageResource = resId;
        super.setImageResource(resId);
    }

    public int getImageResource() {
        return mImageResource;
    }
}
