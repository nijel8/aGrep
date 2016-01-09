package bg.nijel.aGrep.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import net.margaritov.preference.colorpicker.AlphaPatternDrawable;

/**
 * Created by Nick on 1/9/2016.
 * Because of the added complexity to handle
 * two colors at once I had to clone
 * ColorPickerPanelView.class so both panels
 * draws independently. Otherwise couldn't preserve
 * mOldColor color state when system calls global
 * invalidate() on the view
 */
public class ColorPickerPanelViewOld extends View{

    /**
     * The width in pixels of the border
     * surrounding the color panel.
     */
   // private final static float BORDER_WIDTH_PX = 0f;//was 1

    private float mDensity = 1f;

   // private int mBorderColor = 0xff6E6E6E;
    public static int mColor = 0xff000000;

  //  private Paint mBorderPaint;
    private Paint mColorPaint;

    private RectF mDrawingRect;
    private RectF mColorRect;

    private AlphaPatternDrawable mAlphaPattern;

    private Paint mTextPaint;//my code
    public static int mColorText = 0xffffffff; //my code

    public ColorPickerPanelViewOld(Context context) {
        this(context, null);
    }

    public ColorPickerPanelViewOld(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPickerPanelViewOld(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
       // mBorderPaint = new Paint();
        mColorPaint = new Paint();
        mTextPaint = new Paint();//my code
        mDensity = getContext().getResources().getDisplayMetrics().density;
    }


    @Override
    protected void onDraw(Canvas canvas) {

        final RectF rect = mColorRect;

        /*if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.setColor(mBorderColor);
            canvas.drawRect(mDrawingRect, mBorderPaint);
        }*/

        if (mAlphaPattern != null) {
            mAlphaPattern.draw(canvas);
        }

        mColorPaint.setColor(mColor);

        canvas.drawRect(rect, mColorPaint);

        drawTextOver(canvas);//my code
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mDrawingRect = new RectF();
        mDrawingRect.left = getPaddingLeft();
        mDrawingRect.right = w - getPaddingRight();
        mDrawingRect.top = getPaddingTop();
        mDrawingRect.bottom = h - getPaddingBottom();

        setUpColorRect();

    }

    private void setUpColorRect() {
        final RectF dRect = mDrawingRect;

        float left = dRect.left;// + BORDER_WIDTH_PX;
        float top = dRect.top;// + BORDER_WIDTH_PX;
        float bottom = dRect.bottom;// - BORDER_WIDTH_PX;
        float right = dRect.right;// - BORDER_WIDTH_PX;

        mColorRect = new RectF(left, top, right, bottom);

        mAlphaPattern = new AlphaPatternDrawable((int) (5 * mDensity));

        mAlphaPattern.setBounds(
                Math.round(mColorRect.left),
                Math.round(mColorRect.top),
                Math.round(mColorRect.right),
                Math.round(mColorRect.bottom)
        );

    }

    /**
     * Set the color that should be shown by this view.
     *
     * @param color
     */
    /*public void setColor(int color) {
        if (ColorPickerPanelView.mIsTextColor){// my code
            mColorText = color;// my code
        }else {// my code
            mColor = color;
        }// my code
        invalidate();
    }*/

    /**
     * Get the color currently show by this view.
     *
     * @return
     */
    /*public int getColor() {
        //my code
        if (ColorPickerPanelView.mIsTextColor){
            return mColorText;
        }
        //end my code
        return mColor;
    }*/

    /**
     * Set the color of the border surrounding the panel.
     *
     * @param color
     */
    /*public void setBorderColor(int color) {
        mBorderColor = color;
        invalidate();
    }*/

    /**
     * Get the color of the border surrounding the panel.
     */
    /*public int getBorderColor() {
        return mBorderColor;
    }*/

    //my code
    private void drawTextOver(Canvas canvas) {
        final String text = "Found text";
        Paint.Align align = mTextPaint.getTextAlign();
        mTextPaint.setColor(mColorText);
        final int maxWidth = Math.round(mColorRect.width() - (int) (10 * mDensity));
        final int size = determineMaxTextSize(text, maxWidth);
        mTextPaint.setTextSize(size);
        final float x;
        final float y;
        //x
        if (align == Paint.Align.LEFT) {
            x = mColorRect.centerX() - mTextPaint.measureText(text) / 2;
        } else if (align == Paint.Align.CENTER) {
            x = mColorRect.centerX();
        } else {
            x = mColorRect.centerX() + mTextPaint.measureText(text) / 2;
        }
        //y
        Paint.FontMetrics metrics = mTextPaint.getFontMetrics();
        final float acent = Math.abs(metrics.ascent);
        final float descent = Math.abs(metrics.descent);
        y = mColorRect.centerY() + (acent - descent) / 2f;
        canvas.drawText(text, x, y, mTextPaint);
    }

    /**
     * Retrieve the maximum text size to fit in a given width.
     *
     * @param str      (String): Text to check for size.
     * @param maxWidth (float): Maximum allowed width.
     * @return (int): The desired text size.
     */
    private int determineMaxTextSize(final String str, final float maxWidth) {
        int size = 0;
        Paint paint = new Paint();

        do {
            paint.setTextSize(++size);
        } while (paint.measureText(str) < maxWidth);

        return size;
    }
    //end my code


}
