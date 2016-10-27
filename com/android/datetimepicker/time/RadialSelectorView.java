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
import android.util.Log;
import android.view.View;
import com.android.datetimepicker.C0206R;

public class RadialSelectorView extends View {
    private static final int FULL_ALPHA = 255;
    private static final int SELECTED_ALPHA = 51;
    private static final int SELECTED_ALPHA_THEME_DARK = 102;
    private static final String TAG = "RadialSelectorView";
    private final float COMPARING_FLOATS_EPSILON;
    private boolean innerRadiusSelection;
    private float mAmPmCircleRadiusMultiplier;
    private float mAnimationRadiusMultiplier;
    private int mCircleRadius;
    private float mCircleRadiusMultiplier;
    private boolean mDrawValuesReady;
    private boolean mForceDrawDot;
    private boolean mHasInnerCircle;
    private float mInnerNumbersRadiusMultiplier;
    private InvalidateUpdateListener mInvalidateUpdateListener;
    private boolean mIs24HourMode;
    private boolean mIsHourSelection;
    private boolean mIsInitialized;
    private int mLineLength;
    private int mMaxHour;
    private int mMaxMinute;
    private int mMinHour;
    private int mMinMinute;
    private float mNumbersRadiusMultiplier;
    private float mOuterNumbersRadiusMultiplier;
    private final Paint mPaint;
    private int mSelectedHour;
    private int mSelectionAlpha;
    private int mSelectionDegrees;
    private double mSelectionRadians;
    private int mSelectionRadius;
    private float mSelectionRadiusMultiplier;
    private float mTransitionEndRadiusMultiplier;
    private float mTransitionMidRadiusMultiplier;
    private int mXCenter;
    private int mYCenter;

    private class InvalidateUpdateListener implements AnimatorUpdateListener {
        private InvalidateUpdateListener() {
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            RadialSelectorView.this.invalidate();
        }
    }

    public RadialSelectorView(Context context) {
        super(context);
        this.mPaint = new Paint();
        this.COMPARING_FLOATS_EPSILON = 0.001f;
        this.mIsInitialized = false;
    }

    public void initialize(Context context, boolean is24HourMode, boolean hasInnerCircle, boolean disappearsOut, int selectionDegrees, boolean isInnerCircle, int minHour, int maxHour, int minMinute, int maxMinute) {
        if (this.mIsInitialized) {
            Log.e(TAG, "This RadialSelectorView may only be initialized once.");
            return;
        }
        this.mMinHour = minHour;
        this.mMaxHour = maxHour;
        if (this.mMaxHour == 0) {
            this.mMaxHour = 23;
        }
        this.mMinMinute = minMinute;
        this.mMaxMinute = maxMinute;
        if (this.mMaxMinute == 0) {
            this.mMaxMinute = 59;
        }
        this.mIsHourSelection = disappearsOut;
        Resources res = context.getResources();
        this.mPaint.setColor(res.getColor(C0206R.color.blue));
        this.mPaint.setAntiAlias(true);
        this.mSelectionAlpha = SELECTED_ALPHA;
        this.mIs24HourMode = is24HourMode;
        if (is24HourMode) {
            this.mCircleRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.circle_radius_multiplier_24HourMode));
        } else {
            this.mCircleRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.circle_radius_multiplier));
            this.mAmPmCircleRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.ampm_circle_radius_multiplier));
        }
        this.mHasInnerCircle = hasInnerCircle;
        if (hasInnerCircle) {
            this.mInnerNumbersRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.numbers_radius_multiplier_inner));
            this.mOuterNumbersRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.numbers_radius_multiplier_outer));
        } else {
            this.mNumbersRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.numbers_radius_multiplier_normal));
        }
        this.mSelectionRadiusMultiplier = Float.parseFloat(res.getString(C0206R.string.selection_radius_multiplier));
        this.mAnimationRadiusMultiplier = 1.0f;
        this.mTransitionMidRadiusMultiplier = (((float) (disappearsOut ? -1 : 1)) * 0.05f) + 1.0f;
        this.mTransitionEndRadiusMultiplier = (((float) (disappearsOut ? 1 : -1)) * 0.3f) + 1.0f;
        this.mInvalidateUpdateListener = new InvalidateUpdateListener();
        setSelection(selectionDegrees, isInnerCircle, false);
        this.mIsInitialized = true;
    }

    void setTheme(Context context, boolean themeDark) {
        int color;
        Resources res = context.getResources();
        if (themeDark) {
            color = res.getColor(C0206R.color.red);
            this.mSelectionAlpha = SELECTED_ALPHA_THEME_DARK;
        } else {
            color = res.getColor(C0206R.color.blue);
            this.mSelectionAlpha = SELECTED_ALPHA;
        }
        this.mPaint.setColor(color);
    }

    public void setSelection(int selectionDegrees, boolean isInnerCircle, boolean forceDrawDot) {
        this.mSelectionDegrees = selectionDegrees;
        this.mSelectionRadians = (((double) selectionDegrees) * 3.141592653589793d) / 180.0d;
        this.mForceDrawDot = forceDrawDot;
        if (!this.mHasInnerCircle) {
            return;
        }
        if (isInnerCircle) {
            this.mNumbersRadiusMultiplier = this.mInnerNumbersRadiusMultiplier;
        } else {
            this.mNumbersRadiusMultiplier = this.mOuterNumbersRadiusMultiplier;
        }
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setAnimationRadiusMultiplier(float animationRadiusMultiplier) {
        this.mAnimationRadiusMultiplier = animationRadiusMultiplier;
    }

    public int getDegreesFromCoords(float pointX, float pointY, boolean forceLegal, Boolean[] isInnerCircle) {
        if (!this.mDrawValuesReady) {
            return -1;
        }
        double hypotenuse = Math.sqrt((double) (((pointY - ((float) this.mYCenter)) * (pointY - ((float) this.mYCenter))) + ((pointX - ((float) this.mXCenter)) * (pointX - ((float) this.mXCenter)))));
        if (this.mHasInnerCircle) {
            if (forceLegal) {
                boolean z;
                if (((int) Math.abs(hypotenuse - ((double) ((int) (((float) this.mCircleRadius) * this.mInnerNumbersRadiusMultiplier))))) <= ((int) Math.abs(hypotenuse - ((double) ((int) (((float) this.mCircleRadius) * this.mOuterNumbersRadiusMultiplier)))))) {
                    z = true;
                } else {
                    z = false;
                }
                isInnerCircle[0] = Boolean.valueOf(z);
            } else {
                int maxAllowedHypotenuseForOuterNumber = ((int) (((float) this.mCircleRadius) * this.mOuterNumbersRadiusMultiplier)) + this.mSelectionRadius;
                int halfwayHypotenusePoint = (int) (((float) this.mCircleRadius) * ((this.mOuterNumbersRadiusMultiplier + this.mInnerNumbersRadiusMultiplier) / 2.0f));
                if (hypotenuse >= ((double) (((int) (((float) this.mCircleRadius) * this.mInnerNumbersRadiusMultiplier)) - this.mSelectionRadius))) {
                    if (hypotenuse <= ((double) halfwayHypotenusePoint)) {
                        isInnerCircle[0] = Boolean.valueOf(true);
                    }
                }
                if (hypotenuse <= ((double) maxAllowedHypotenuseForOuterNumber)) {
                    if (hypotenuse >= ((double) halfwayHypotenusePoint)) {
                        isInnerCircle[0] = Boolean.valueOf(false);
                    }
                }
                return -1;
            }
        } else if (!forceLegal) {
            if (((int) Math.abs(hypotenuse - ((double) this.mLineLength))) > ((int) (((float) this.mCircleRadius) * (1.0f - this.mNumbersRadiusMultiplier)))) {
                return -1;
            }
        }
        int degrees = (int) ((180.0d * Math.asin(((double) Math.abs(pointY - ((float) this.mYCenter))) / hypotenuse)) / 3.141592653589793d);
        boolean rightSide = pointX > ((float) this.mXCenter);
        boolean topSide = pointY < ((float) this.mYCenter);
        if (rightSide && topSide) {
            return 90 - degrees;
        }
        if (rightSide && !topSide) {
            return degrees + 90;
        }
        if (!rightSide && !topSide) {
            return 270 - degrees;
        }
        if (rightSide || !topSide) {
            return degrees;
        }
        return degrees + 270;
    }

    public void onDraw(Canvas canvas) {
        int i = 1;
        if (getWidth() != 0 && this.mIsInitialized) {
            if (!this.mDrawValuesReady) {
                this.mXCenter = getWidth() / 2;
                this.mYCenter = getHeight() / 2;
                this.mCircleRadius = (int) (((float) Math.min(this.mXCenter, this.mYCenter)) * this.mCircleRadiusMultiplier);
                if (!this.mIs24HourMode) {
                    this.mYCenter -= ((int) (((float) this.mCircleRadius) * this.mAmPmCircleRadiusMultiplier)) / 2;
                }
                this.mSelectionRadius = (int) (((float) this.mCircleRadius) * this.mSelectionRadiusMultiplier);
                this.mDrawValuesReady = true;
            }
            this.mLineLength = (int) ((((float) this.mCircleRadius) * this.mNumbersRadiusMultiplier) * this.mAnimationRadiusMultiplier);
            int pointX = this.mXCenter + ((int) (((double) this.mLineLength) * Math.sin(this.mSelectionRadians)));
            int pointY = this.mYCenter - ((int) (((double) this.mLineLength) * Math.cos(this.mSelectionRadians)));
            this.mPaint.setAlpha(this.mSelectionAlpha);
            this.innerRadiusSelection = equals(this.mNumbersRadiusMultiplier, Float.parseFloat(getResources().getString(C0206R.string.numbers_radius_multiplier_inner)));
            if (constraintsAreMet(this.innerRadiusSelection, this.mSelectionDegrees)) {
                canvas.drawCircle((float) pointX, (float) pointY, (float) this.mSelectionRadius, this.mPaint);
            }
            boolean z = this.mForceDrawDot;
            if (this.mSelectionDegrees % 30 == 0) {
                i = 0;
            }
            if ((i | z) != 0) {
                this.mPaint.setAlpha(FULL_ALPHA);
                if (constraintsAreMet(this.innerRadiusSelection, this.mSelectionDegrees)) {
                    canvas.drawCircle((float) pointX, (float) pointY, (float) ((this.mSelectionRadius * 2) / 7), this.mPaint);
                }
            } else {
                int lineLength = this.mLineLength - this.mSelectionRadius;
                pointX = this.mXCenter + ((int) (((double) lineLength) * Math.sin(this.mSelectionRadians)));
                pointY = this.mYCenter - ((int) (((double) lineLength) * Math.cos(this.mSelectionRadians)));
            }
            this.mPaint.setAlpha(FULL_ALPHA);
            this.mPaint.setStrokeWidth(1.0f);
            if (constraintsAreMet(this.innerRadiusSelection, this.mSelectionDegrees)) {
                canvas.drawLine((float) this.mXCenter, (float) this.mYCenter, (float) pointX, (float) pointY, this.mPaint);
            }
        }
    }

    public ObjectAnimator getDisappearAnimator() {
        if (this.mIsInitialized && this.mDrawValuesReady) {
            Keyframe kf0 = Keyframe.ofFloat(0.0f, 1.0f);
            Keyframe kf1 = Keyframe.ofFloat(0.2f, this.mTransitionMidRadiusMultiplier);
            Keyframe kf2 = Keyframe.ofFloat(1.0f, this.mTransitionEndRadiusMultiplier);
            PropertyValuesHolder radiusDisappear = PropertyValuesHolder.ofKeyframe("animationRadiusMultiplier", new Keyframe[]{kf0, kf1, kf2});
            kf0 = Keyframe.ofFloat(0.0f, 1.0f);
            kf1 = Keyframe.ofFloat(1.0f, 0.0f);
            PropertyValuesHolder fadeOut = PropertyValuesHolder.ofKeyframe("alpha", new Keyframe[]{kf0, kf1});
            ObjectAnimator disappearAnimator = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{radiusDisappear, fadeOut}).setDuration((long) 500);
            disappearAnimator.addUpdateListener(this.mInvalidateUpdateListener);
            return disappearAnimator;
        }
        Log.e(TAG, "RadialSelectorView was not ready for animation.");
        return null;
    }

    public ObjectAnimator getReappearAnimator() {
        if (this.mIsInitialized && this.mDrawValuesReady) {
            int totalDuration = (int) (((float) 500) * (1.0f + 0.25f));
            float f = (float) totalDuration;
            float delayPoint = (((float) 500) * 0.25f) / r0;
            float midwayPoint = 1.0f - ((1.0f - delayPoint) * 0.2f);
            Keyframe kf0 = Keyframe.ofFloat(0.0f, this.mTransitionEndRadiusMultiplier);
            Keyframe kf1 = Keyframe.ofFloat(delayPoint, this.mTransitionEndRadiusMultiplier);
            Keyframe kf2 = Keyframe.ofFloat(midwayPoint, this.mTransitionMidRadiusMultiplier);
            Keyframe kf3 = Keyframe.ofFloat(1.0f, 1.0f);
            PropertyValuesHolder radiusReappear = PropertyValuesHolder.ofKeyframe("animationRadiusMultiplier", new Keyframe[]{kf0, kf1, kf2, kf3});
            kf0 = Keyframe.ofFloat(0.0f, 0.0f);
            kf1 = Keyframe.ofFloat(delayPoint, 0.0f);
            kf2 = Keyframe.ofFloat(1.0f, 1.0f);
            PropertyValuesHolder fadeIn = PropertyValuesHolder.ofKeyframe("alpha", new Keyframe[]{kf0, kf1, kf2});
            ObjectAnimator reappearAnimator = ObjectAnimator.ofPropertyValuesHolder(this, new PropertyValuesHolder[]{radiusReappear, fadeIn}).setDuration((long) totalDuration);
            reappearAnimator.addUpdateListener(this.mInvalidateUpdateListener);
            return reappearAnimator;
        }
        Log.e(TAG, "RadialSelectorView was not ready for animation.");
        return null;
    }

    public void setSelectedHour(int selectedHour) {
        this.mSelectedHour = selectedHour;
    }

    private boolean equals(float val1, float val2) {
        return Math.abs(val1 - val2) < 0.001f;
    }

    private boolean minAndMaxHourConstraintIsMet(boolean isInnerRadius, int selectionDegrees) {
        int i;
        float hour_float = ((float) selectionDegrees) / 30.0f;
        int i2 = (int) hour_float;
        if (hour_float - ((float) ((int) hour_float)) < 0.5f) {
            i = 0;
        } else {
            i = 1;
        }
        int hour = i2 + i;
        if (!isInnerRadius) {
            hour += 12;
            if (hour == 24) {
                hour = 0;
            }
        } else if (hour == 0) {
            hour = 12;
        }
        if (hour < this.mMinHour || hour > this.mMaxHour) {
            return false;
        }
        return true;
    }

    private boolean minAndMaxMinutesConstraintIsMet(int selectionDegrees) {
        int selectedMinute = selectionDegrees / 6;
        boolean checkedMinMinute = true;
        boolean checkedMaxMinute = true;
        if (this.mSelectedHour == this.mMinHour) {
            checkedMinMinute = selectedMinute >= this.mMinMinute;
        }
        if (this.mSelectedHour == this.mMaxHour) {
            if (selectedMinute <= this.mMaxMinute) {
                checkedMaxMinute = true;
            } else {
                checkedMaxMinute = false;
            }
        }
        if (checkedMaxMinute && checkedMinMinute) {
            return true;
        }
        return false;
    }

    private boolean constraintsAreMet(boolean isInnerRadius, int selectionDegrees) {
        if (this.mIsHourSelection) {
            return minAndMaxHourConstraintIsMet(isInnerRadius, selectionDegrees);
        }
        return minAndMaxMinutesConstraintIsMet(selectionDegrees);
    }
}
