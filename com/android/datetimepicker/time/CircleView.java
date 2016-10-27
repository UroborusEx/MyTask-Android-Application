package com.android.datetimepicker.time;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import com.android.datetimepicker.C0206R;

public class CircleView extends View {
    private static final String TAG = "CircleView";
    private float mAmPmCircleRadiusMultiplier;
    private int mCircleColor;
    private int mCircleRadius;
    private float mCircleRadiusMultiplier;
    private int mDotColor;
    private boolean mDrawValuesReady;
    private boolean mIs24HourMode;
    private boolean mIsInitialized;
    private final Paint mPaint;
    private int mXCenter;
    private int mYCenter;

    public CircleView(Context context) {
        super(context);
        this.mPaint = new Paint();
        Resources res = context.getResources();
        this.mCircleColor = res.getColor(17170443);
        this.mDotColor = res.getColor(C0206R.color.numbers_text_color);
        this.mPaint.setAntiAlias(true);
        this.mIsInitialized = false;
    }

    public void initialize(Context context, boolean is24HourMode) {
        if (this.mIsInitialized) {
            Log.e(TAG, "CircleView may only be initialized once.");
            return;
        }
        Resources res = context.getResources();
        this.mIs24HourMode = is24HourMode;
        if (is24HourMode) {
            this.mCircleRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.circle_radius_multiplier_24HourMode));
        } else {
            this.mCircleRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.circle_radius_multiplier));
            this.mAmPmCircleRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.ampm_circle_radius_multiplier));
        }
        this.mIsInitialized = true;
    }

    void setTheme(Context context, boolean dark) {
        Resources res = context.getResources();
        if (dark) {
            this.mCircleColor = res.getColor(C0206R.color.dark_gray);
            this.mDotColor = res.getColor(C0206R.color.light_gray);
            return;
        }
        this.mCircleColor = res.getColor(17170443);
        this.mDotColor = res.getColor(C0206R.color.numbers_text_color);
    }

    public void onDraw(Canvas canvas) {
        if (getWidth() != 0 && this.mIsInitialized) {
            if (!this.mDrawValuesReady) {
                this.mXCenter = getWidth() / 2;
                this.mYCenter = getHeight() / 2;
                this.mCircleRadius = (int) (((float) Math.min(this.mXCenter, this.mYCenter)) * this.mCircleRadiusMultiplier);
                if (!this.mIs24HourMode) {
                    this.mYCenter -= ((int) (((float) this.mCircleRadius) * this.mAmPmCircleRadiusMultiplier)) / 2;
                }
                this.mDrawValuesReady = true;
            }
            this.mPaint.setColor(this.mCircleColor);
            canvas.drawCircle((float) this.mXCenter, (float) this.mYCenter, (float) this.mCircleRadius, this.mPaint);
            this.mPaint.setColor(this.mDotColor);
            canvas.drawCircle((float) this.mXCenter, (float) this.mYCenter, 2.0f, this.mPaint);
        }
    }
}
