package com.android.datetimepicker.time;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import com.android.datetimepicker.C0206R;

public class RadialTextsView extends View {
    private static final String TAG = "RadialTextsView";
    private float mAmPmCircleRadiusMultiplier;
    private float mAnimationRadiusMultiplier;
    private float mCircleRadius;
    private float mCircleRadiusMultiplier;
    protected int mDisabledTextColor;
    ObjectAnimator mDisappearAnimator;
    private boolean mDrawValuesReady;
    private boolean mHasInnerCircle;
    private float mInnerNumbersRadiusMultiplier;
    private float[] mInnerTextGridHeights;
    private float[] mInnerTextGridWidths;
    private float mInnerTextSize;
    private float mInnerTextSizeMultiplier;
    private String[] mInnerTexts;
    private InvalidateUpdateListener mInvalidateUpdateListener;
    private boolean mIs24HourMode;
    private boolean mIsInitialized;
    private int mMaxHour;
    private int mMaxMinute;
    private int mMinHour;
    private int mMinMinute;
    private float mNumbersRadiusMultiplier;
    private final Paint mPaint;
    ObjectAnimator mReappearAnimator;
    private int mSelectedHour;
    protected int mTextColorNormal;
    private float[] mTextGridHeights;
    private boolean mTextGridValuesDirty;
    private float[] mTextGridWidths;
    private float mTextSize;
    private float mTextSizeMultiplier;
    private String[] mTexts;
    private float mTransitionEndRadiusMultiplier;
    private float mTransitionMidRadiusMultiplier;
    private Typeface mTypefaceLight;
    private Typeface mTypefaceRegular;
    private int mXCenter;
    private int mYCenter;

    private class InvalidateUpdateListener implements AnimatorUpdateListener {
        private InvalidateUpdateListener() {
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            RadialTextsView.this.invalidate();
        }
    }

    public RadialTextsView(Context context) {
        super(context);
        this.mPaint = new Paint();
        this.mIsInitialized = false;
    }

    public void initialize(Resources res, String[] texts, String[] innerTexts, boolean is24HourMode, boolean disappearsOut, int minHour, int maxHour, int minMinute, int maxMinute) {
        if (this.mIsInitialized) {
            Log.e(TAG, "This RadialTextsView may only be initialized once.");
            return;
        }
        this.mDisabledTextColor = res.getColor(C0206R.color.date_picker_text_disabled);
        this.mTextColorNormal = res.getColor(C0206R.color.date_picker_text_normal);
        this.mMinHour = minHour;
        this.mMaxHour = maxHour;
        this.mMinMinute = minMinute;
        this.mMaxMinute = maxMinute;
        this.mPaint.setColor(res.getColor(C0206R.color.numbers_text_color));
        this.mTypefaceLight = Typeface.create(res.getString(C0206R.string.radial_numbers_typeface), 0);
        this.mTypefaceRegular = Typeface.create(res.getString(C0206R.string.sans_serif), 0);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setTextAlign(Align.CENTER);
        this.mTexts = texts;
        this.mInnerTexts = innerTexts;
        this.mIs24HourMode = is24HourMode;
        this.mHasInnerCircle = innerTexts != null;
        if (is24HourMode) {
            this.mCircleRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.circle_radius_multiplier_24HourMode));
        } else {
            this.mCircleRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.circle_radius_multiplier));
            this.mAmPmCircleRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.ampm_circle_radius_multiplier));
        }
        this.mTextGridHeights = new float[7];
        this.mTextGridWidths = new float[7];
        if (this.mHasInnerCircle) {
            this.mNumbersRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.numbers_radius_multiplier_outer));
            this.mTextSizeMultiplier = Float.parseFloat(res.getString(C0206R.string.text_size_multiplier_outer));
            this.mInnerNumbersRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.numbers_radius_multiplier_inner));
            this.mInnerTextSizeMultiplier = Float.parseFloat(res.getString(C0206R.string.text_size_multiplier_inner));
            this.mInnerTextGridHeights = new float[7];
            this.mInnerTextGridWidths = new float[7];
        } else {
            this.mNumbersRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.numbers_radius_multiplier_normal));
            this.mTextSizeMultiplier = Float.parseFloat(res.getString(C0206R.string.text_size_multiplier_normal));
        }
        this.mAnimationRadiusMultiplier = 1.0f;
        this.mTransitionMidRadiusMultiplier = (((float) (disappearsOut ? -1 : 1)) * 0.05f) + 1.0f;
        this.mTransitionEndRadiusMultiplier = (((float) (disappearsOut ? 1 : -1)) * 0.3f) + 1.0f;
        this.mInvalidateUpdateListener = new InvalidateUpdateListener();
        this.mTextGridValuesDirty = true;
        this.mIsInitialized = true;
    }

    void setTheme(Context context, boolean themeDark) {
        int textColor;
        Resources res = context.getResources();
        if (themeDark) {
            textColor = res.getColor(17170443);
        } else {
            textColor = res.getColor(C0206R.color.numbers_text_color);
        }
        this.mPaint.setColor(textColor);
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setAnimationRadiusMultiplier(float animationRadiusMultiplier) {
        this.mAnimationRadiusMultiplier = animationRadiusMultiplier;
        this.mTextGridValuesDirty = true;
    }

    public void onDraw(Canvas canvas) {
        if (getWidth() != 0 && this.mIsInitialized) {
            if (!this.mDrawValuesReady) {
                this.mXCenter = getWidth() / 2;
                this.mYCenter = getHeight() / 2;
                this.mCircleRadius = ((float) Math.min(this.mXCenter, this.mYCenter)) * this.mCircleRadiusMultiplier;
                if (!this.mIs24HourMode) {
                    this.mYCenter = (int) (((float) this.mYCenter) - ((this.mCircleRadius * this.mAmPmCircleRadiusMultiplier) / 2.0f));
                }
                this.mTextSize = this.mCircleRadius * this.mTextSizeMultiplier;
                if (this.mHasInnerCircle) {
                    this.mInnerTextSize = this.mCircleRadius * this.mInnerTextSizeMultiplier;
                }
                renderAnimations();
                this.mTextGridValuesDirty = true;
                this.mDrawValuesReady = true;
            }
            if (this.mTextGridValuesDirty) {
                calculateGridSizes((this.mCircleRadius * this.mNumbersRadiusMultiplier) * this.mAnimationRadiusMultiplier, (float) this.mXCenter, (float) this.mYCenter, this.mTextSize, this.mTextGridHeights, this.mTextGridWidths);
                if (this.mHasInnerCircle) {
                    calculateGridSizes((this.mCircleRadius * this.mInnerNumbersRadiusMultiplier) * this.mAnimationRadiusMultiplier, (float) this.mXCenter, (float) this.mYCenter, this.mInnerTextSize, this.mInnerTextGridHeights, this.mInnerTextGridWidths);
                }
                this.mTextGridValuesDirty = false;
            }
            drawTexts(canvas, this.mTextSize, this.mTypefaceLight, this.mTexts, this.mTextGridWidths, this.mTextGridHeights);
            if (this.mHasInnerCircle) {
                drawTexts(canvas, this.mInnerTextSize, this.mTypefaceRegular, this.mInnerTexts, this.mInnerTextGridWidths, this.mInnerTextGridHeights);
            }
        }
    }

    private void calculateGridSizes(float numbersRadius, float xCenter, float yCenter, float textSize, float[] textGridHeights, float[] textGridWidths) {
        float offset1 = numbersRadius;
        float offset2 = (((float) Math.sqrt(3.0d)) * numbersRadius) / 2.0f;
        float offset3 = numbersRadius / 2.0f;
        this.mPaint.setTextSize(textSize);
        yCenter -= (this.mPaint.descent() + this.mPaint.ascent()) / 2.0f;
        textGridHeights[0] = yCenter - offset1;
        textGridWidths[0] = xCenter - offset1;
        textGridHeights[1] = yCenter - offset2;
        textGridWidths[1] = xCenter - offset2;
        textGridHeights[2] = yCenter - offset3;
        textGridWidths[2] = xCenter - offset3;
        textGridHeights[3] = yCenter;
        textGridWidths[3] = xCenter;
        textGridHeights[4] = yCenter + offset3;
        textGridWidths[4] = xCenter + offset3;
        textGridHeights[5] = yCenter + offset2;
        textGridWidths[5] = xCenter + offset2;
        textGridHeights[6] = yCenter + offset1;
        textGridWidths[6] = xCenter + offset1;
    }

    private void drawTexts(Canvas canvas, float textSize, Typeface typeface, String[] texts, float[] textGridWidths, float[] textGridHeights) {
        this.mPaint.setTextSize(textSize);
        this.mPaint.setTypeface(typeface);
        drawText(canvas, texts[0], textGridWidths[3], textGridHeights[0]);
        drawText(canvas, texts[1], textGridWidths[4], textGridHeights[1]);
        drawText(canvas, texts[2], textGridWidths[5], textGridHeights[2]);
        drawText(canvas, texts[3], textGridWidths[6], textGridHeights[3]);
        drawText(canvas, texts[4], textGridWidths[5], textGridHeights[4]);
        drawText(canvas, texts[5], textGridWidths[4], textGridHeights[5]);
        drawText(canvas, texts[6], textGridWidths[3], textGridHeights[6]);
        drawText(canvas, texts[7], textGridWidths[2], textGridHeights[5]);
        drawText(canvas, texts[8], textGridWidths[1], textGridHeights[4]);
        drawText(canvas, texts[9], textGridWidths[0], textGridHeights[3]);
        drawText(canvas, texts[10], textGridWidths[1], textGridHeights[2]);
        drawText(canvas, texts[11], textGridWidths[2], textGridHeights[1]);
    }

    private void drawText(Canvas canvas, String text, float width, float height) {
        setPaintColorEnabledOrDisabled(text);
        canvas.drawText(text, width, height, this.mPaint);
    }

    public void setSelectedHour(int selectedHour) {
        this.mSelectedHour = selectedHour;
    }

    private void renderAnimations() {
        Keyframe kf0 = Keyframe.ofFloat(0.0f, 1.0f);
        Keyframe kf1 = Keyframe.ofFloat(0.2f, this.mTransitionMidRadiusMultiplier);
        Keyframe kf2 = Keyframe.ofFloat(1.0f, this.mTransitionEndRadiusMultiplier);
        PropertyValuesHolder radiusDisappear = PropertyValuesHolder.ofKeyframe("animationRadiusMultiplier", new Keyframe[]{kf0, kf1, kf2});
        kf0 = Keyframe.ofFloat(0.0f, 1.0f);
        kf1 = Keyframe.ofFloat(1.0f, 0.0f);
        PropertyValuesHolder fadeOut = PropertyValuesHolder.ofKeyframe("alpha", new Keyframe[]{kf0, kf1});
        long j = (long) 500;
        this.mDisappearAnimator = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{radiusDisappear, fadeOut}).setDuration(r0);
        this.mDisappearAnimator.addUpdateListener(this.mInvalidateUpdateListener);
        int totalDuration = (int) (((float) 500) * (1.0f + 0.25f));
        float f = (float) totalDuration;
        float delayPoint = (((float) 500) * 0.25f) / r0;
        float midwayPoint = 1.0f - ((1.0f - delayPoint) * 0.2f);
        kf0 = Keyframe.ofFloat(0.0f, this.mTransitionEndRadiusMultiplier);
        kf1 = Keyframe.ofFloat(delayPoint, this.mTransitionEndRadiusMultiplier);
        kf2 = Keyframe.ofFloat(midwayPoint, this.mTransitionMidRadiusMultiplier);
        Keyframe kf3 = Keyframe.ofFloat(1.0f, 1.0f);
        PropertyValuesHolder radiusReappear = PropertyValuesHolder.ofKeyframe("animationRadiusMultiplier", new Keyframe[]{kf0, kf1, kf2, kf3});
        kf0 = Keyframe.ofFloat(0.0f, 0.0f);
        kf1 = Keyframe.ofFloat(delayPoint, 0.0f);
        kf2 = Keyframe.ofFloat(1.0f, 1.0f);
        PropertyValuesHolder fadeIn = PropertyValuesHolder.ofKeyframe("alpha", new Keyframe[]{kf0, kf1, kf2});
        j = (long) totalDuration;
        this.mReappearAnimator = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{radiusReappear, fadeIn}).setDuration(r0);
        this.mReappearAnimator.addUpdateListener(this.mInvalidateUpdateListener);
    }

    public ObjectAnimator getDisappearAnimator() {
        if (this.mIsInitialized && this.mDrawValuesReady && this.mDisappearAnimator != null) {
            return this.mDisappearAnimator;
        }
        Log.e(TAG, "RadialTextView was not ready for animation.");
        return null;
    }

    public ObjectAnimator getReappearAnimator() {
        if (this.mIsInitialized && this.mDrawValuesReady && this.mReappearAnimator != null) {
            return this.mReappearAnimator;
        }
        Log.e(TAG, "RadialTextView was not ready for animation.");
        return null;
    }

    private void setPaintColorEnabledOrDisabled(String text) {
        if (!this.mHasInnerCircle) {
            boolean checkedMinMinute = true;
            boolean checkedMaxMinute = true;
            if (this.mSelectedHour == this.mMinHour) {
                checkedMinMinute = Integer.parseInt(text) >= this.mMinMinute;
            }
            if (this.mSelectedHour == this.mMaxHour) {
                if (Integer.parseInt(text) <= this.mMaxMinute) {
                    checkedMaxMinute = true;
                } else {
                    checkedMaxMinute = false;
                }
            }
            if (checkedMinMinute && checkedMaxMinute) {
                this.mPaint.setColor(this.mTextColorNormal);
            } else {
                this.mPaint.setColor(this.mDisabledTextColor);
            }
        } else if (Integer.parseInt(text) < this.mMinHour || Integer.parseInt(text) > this.mMaxHour) {
            this.mPaint.setColor(this.mDisabledTextColor);
        } else {
            this.mPaint.setColor(this.mTextColorNormal);
        }
    }
}
