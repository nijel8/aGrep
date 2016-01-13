package bg.nijel.aGrep.utils;

/**
 * Created by Nick on 1/11/2016.
 */

import android.content.Context;
import android.graphics.PointF;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;

public class SnappingLinearLayoutManager extends LinearLayoutManager {

    private static final float MILLISECONDS_PER_INCH = 70f;//speed
    private Context mContext;

    public SnappingLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        mContext = context;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
                                       int position) {
        RecyclerView.SmoothScroller smoothScroller = new TopSnappedSmoothScroller(recyclerView.getContext()){
            //This controls the direction in which smoothScroll looks for your view
            //What is PointF? A class that just holds two float coordinates.
            //Accepts a (x , y)
            //for y: use -1 for up direction, 1 for down direction.
            //for x (did not test): use -1 for left direction, 1 for right
            //direction.
            //We let our custom LinearLayoutManager calculate PointF for us
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
               return SnappingLinearLayoutManager.this
                       .computeScrollVectorForPosition(targetPosition);
            }

            //This returns the milliseconds it takes to scroll one pixel.
            @Override
            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
            }
        };
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }


    private class TopSnappedSmoothScroller extends LinearSmoothScroller {
        public TopSnappedSmoothScroller(Context context) {
            super(context);

        }

        @Override
        public PointF computeScrollVectorForPosition(int targetPosition) {
            return SnappingLinearLayoutManager.this
                    .computeScrollVectorForPosition(targetPosition);
        }

        @Override
        protected int getVerticalSnapPreference() {
            return SNAP_TO_START;
        }
    }
}
