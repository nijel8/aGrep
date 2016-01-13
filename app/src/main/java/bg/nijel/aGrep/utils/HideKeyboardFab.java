package bg.nijel.aGrep.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by Nick on 1/12/2016.
 */
public class HideKeyboardFab extends com.github.clans.fab.FloatingActionMenu{


    public HideKeyboardFab(Context context) {
        super(context);
    }

    public HideKeyboardFab(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HideKeyboardFab(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void open(final boolean animate) {
        super.open(animate);
        if (!isOpened()) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        }
    }

}
