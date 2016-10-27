package com.android.datetimepicker.time;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import com.android.datetimepicker.C0206R;
import com.android.datetimepicker.Utils;
import java.text.DateFormatSymbols;

public class AmPmCirclesView extends View {
    private static final int AM = 0;
    private static final int PM = 1;
    private static final int SELECTED_ALPHA = 51;
    private static final int SELECTED_ALPHA_THEME_DARK = 102;
    private static final String TAG = "AmPmCirclesView";
    private int mAmOrPm;
    private int mAmOrPmPressed;
    private int mAmPmCircleRadius;
    private float mAmPmCircleRadiusMultiplier;
    private int mAmPmTextColor;
    private int mAmPmYCenter;
    private String mAmText;
    private int mAmXCenter;
    private float mCircleRadiusMultiplier;
    private boolean mDrawValuesReady;
    private boolean mIsInitialized;
    private final Paint mPaint;
    private String mPmText;
    private int mPmXCenter;
    private int mSelectedAlpha;
    private int mSelectedColor;
    private int mUnselectedColor;

    public AmPmCirclesView(Context context) {
        super(context);
        this.mPaint = new Paint();
        this.mIsInitialized = false;
    }

    public void initialize(Context context, int amOrPm) {
        if (this.mIsInitialized) {
            Log.e(TAG, "AmPmCirclesView may only be initialized once.");
            return;
        }
        Resources res = context.getResources();
        this.mUnselectedColor = res.getColor(17170443);
        this.mSelectedColor = res.getColor(C0206R.color.blue);
        this.mAmPmTextColor = res.getColor(C0206R.color.ampm_text_color);
        this.mSelectedAlpha = SELECTED_ALPHA;
        this.mPaint.setTypeface(Typeface.create(res.getString(C0206R.string.sans_serif), AM));
        this.mPaint.setAntiAlias(true);
        this.mPaint.setTextAlign(Align.CENTER);
        this.mCircleRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.circle_radius_multiplier));
        this.mAmPmCircleRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.ampm_circle_radius_multiplier));
        String[] amPmTexts = new DateFormatSymbols().getAmPmStrings();
        this.mAmText = amPmTexts[AM];
        this.mPmText = amPmTexts[PM];
        setAmOrPm(amOrPm);
        this.mAmOrPmPressed = -1;
        this.mIsInitialized = true;
    }

    void setTheme(Context context, boolean themeDark) {
        Resources res = context.getResources();
        if (themeDark) {
            this.mUnselectedColor = res.getColor(C0206R.color.dark_gray);
            this.mSelectedColor = res.getColor(C0206R.color.red);
            this.mAmPmTextColor = res.getColor(17170443);
            this.mSelectedAlpha = SELECTED_ALPHA_THEME_DARK;
            return;
        }
        this.mUnselectedColor = res.getColor(17170443);
        this.mSelectedColor = res.getColor(C0206R.color.blue);
        this.mAmPmTextColor = res.getColor(C0206R.color.ampm_text_color);
        this.mSelectedAlpha = SELECTED_ALPHA;
    }

    public void setAmOrPm(int amOrPm) {
        this.mAmOrPm = amOrPm;
    }

    public void setAmOrPmPressed(int amOrPmPressed) {
        this.mAmOrPmPressed = amOrPmPressed;
    }

    public int getIsTouchingAmOrPm(float xCoord, float yCoord) {
        if (!this.mDrawValuesReady) {
            return -1;
        }
        int squaredYDistance = (int) ((yCoord - ((float) this.mAmPmYCenter)) * (yCoord - ((float) this.mAmPmYCenter)));
        if (((int) Math.sqrt((double) (((xCoord - ((float) this.mAmXCenter)) * (xCoord - ((float) this.mAmXCenter))) + ((float) squaredYDistance)))) <= this.mAmPmCircleRadius) {
            return AM;
        }
        if (((int) Math.sqrt((double) (((xCoord - ((float) this.mPmXCenter)) * (xCoord - ((float) this.mPmXCenter))) + ((float) squaredYDistance)))) <= this.mAmPmCircleRadius) {
            return PM;
        }
        return -1;
    }

    public void onDraw(Canvas canvas) {
        if (getWidth() != 0 && this.mIsInitialized) {
            if (!this.mDrawValuesReady) {
                int layoutXCenter = getWidth() / 2;
                int layoutYCenter = getHeight() / 2;
                int circleRadius = (int) (((float) Math.min(layoutXCenter, layoutYCenter)) * this.mCircleRadiusMultiplier);
                this.mAmPmCircleRadius = (int) (((float) circleRadius) * this.mAmPmCircleRadiusMultiplier);
                this.mPaint.setTextSize((float) ((this.mAmPmCircleRadius * 3) / 4));
                this.mAmPmYCenter = (layoutYCenter - (this.mAmPmCircleRadius / 2)) + circleRadius;
                this.mAmXCenter = (layoutXCenter - circleRadius) + this.mAmPmCircleRadius;
                this.mPmXCenter = (layoutXCenter + circleRadius) - this.mAmPmCircleRadius;
                this.mDrawValuesReady = true;
            }
            int amColor = this.mUnselectedColor;
            int amAlpha = Utils.FULL_ALPHA;
            int pmColor = this.mUnselectedColor;
            int pmAlpha = Utils.FULL_ALPHA;
            if (this.mAmOrPm == 0) {
                amColor = this.mSelectedColor;
                amAlpha = this.mSelectedAlpha;
            } else if (this.mAmOrPm == PM) {
                pmColor = this.mSelectedColor;
                pmAlpha = this.mSelectedAlpha;
            }
            if (this.mAmOrPmPressed == 0) {
                amColor = this.mSelectedColor;
                amAlpha = this.mSelectedAlpha;
            } else if (this.mAmOrPmPressed == PM) {
                pmColor = this.mSelectedColor;
                pmAlpha = this.mSelectedAlpha;
            }
            this.mPaint.setColor(amColor);
            this.mPaint.setAlpha(amAlpha);
            canvas.drawCircle((float) this.mAmXCenter, (float) this.mAmPmYCenter, (float) this.mAmPmCircleRadius, this.mPaint);
            this.mPaint.setColor(pmColor);
            this.mPaint.setAlpha(pmAlpha);
            canvas.drawCircle((float) this.mPmXCenter, (float) this.mAmPmYCenter, (float) this.mAmPmCircleRadius, this.mPaint);
            this.mPaint.setColor(this.mAmPmTextColor);
            int textYCenter = this.mAmPmYCenter - (((int) (this.mPaint.descent() + this.mPaint.ascent())) / 2);
            canvas.drawText(this.mAmText, (float) this.mAmXCenter, (float) textYCenter, this.mPaint);
            canvas.drawText(this.mPmText, (float) this.mPmXCenter, (float) textYCenter, this.mPaint);
        }
    }
}
