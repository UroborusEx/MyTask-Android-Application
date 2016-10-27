package com.android.datetimepicker.date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.datetimepicker.C0206R;

public class TextViewWithCircularIndicator extends TextView {
    private static final int SELECTED_CIRCLE_ALPHA = 60;
    private final int mCircleColor;
    Paint mCirclePaint;
    private boolean mDrawCircle;
    private final String mItemIsSelectedText;
    private final int mRadius;

    public TextViewWithCircularIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mCirclePaint = new Paint();
        Resources res = context.getResources();
        this.mCircleColor = res.getColor(C0206R.color.blue);
        this.mRadius = res.getDimensionPixelOffset(C0206R.dimen.month_select_circle_radius);
        this.mItemIsSelectedText = context.getResources().getString(C0206R.string.item_is_selected);
        init();
    }

    private void init() {
        this.mCirclePaint.setFakeBoldText(true);
        this.mCirclePaint.setAntiAlias(true);
        this.mCirclePaint.setColor(this.mCircleColor);
        this.mCirclePaint.setTextAlign(Align.CENTER);
        this.mCirclePaint.setStyle(Style.FILL);
        this.mCirclePaint.setAlpha(SELECTED_CIRCLE_ALPHA);
    }

    public void drawIndicator(boolean drawCircle) {
        this.mDrawCircle = drawCircle;
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mDrawCircle) {
            int width = getWidth();
            int height = getHeight();
            canvas.drawCircle((float) (width / 2), (float) (height / 2), (float) (Math.min(width, height) / 2), this.mCirclePaint);
        }
    }

    public CharSequence getContentDescription() {
        CharSequence itemText = getText();
        if (!this.mDrawCircle) {
            return itemText;
        }
        return String.format(this.mItemIsSelectedText, new Object[]{itemText});
    }
}
