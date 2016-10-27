package com.android.datetimepicker.time;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.TransportMediator;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.RecyclerView.ItemAnimator;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import com.android.datetimepicker.C0206R;
import com.android.datetimepicker.HapticFeedbackController;
import com.android.datetimepicker.Utils;

public class RadialPickerLayout extends FrameLayout implements OnTouchListener {
    private static final int AM = 0;
    private static final int AMPM_INDEX = 2;
    private static final int ENABLE_PICKER_INDEX = 3;
    private static final int HOUR_INDEX = 0;
    private static final int HOUR_VALUE_TO_DEGREES_STEP_SIZE = 30;
    private static final int MINUTE_INDEX = 1;
    private static final int MINUTE_VALUE_TO_DEGREES_STEP_SIZE = 6;
    private static final int PM = 1;
    private static final String TAG = "RadialPickerLayout";
    private static final int VISIBLE_DEGREES_STEP_SIZE = 30;
    private final int TAP_TIMEOUT;
    private final int TOUCH_SLOP;
    private AccessibilityManager mAccessibilityManager;
    private AmPmCirclesView mAmPmCirclesView;
    private CircleView mCircleView;
    private int mCurrentHoursOfDay;
    private int mCurrentItemShowing;
    private int mCurrentMinutes;
    private boolean mDoingMove;
    private boolean mDoingTouch;
    private int mDownDegrees;
    private float mDownX;
    private float mDownY;
    private View mGrayBox;
    private Handler mHandler;
    private HapticFeedbackController mHapticFeedbackController;
    private boolean mHideAmPm;
    private RadialSelectorView mHourRadialSelectorView;
    private RadialTextsView mHourRadialTextsView;
    private boolean mInputEnabled;
    private boolean mIs24HourMode;
    private int mIsTouchingAmOrPm;
    private int mLastValueSelected;
    private OnValueSelectedListener mListener;
    private int mMaxHour;
    private int mMaxMinute;
    private int mMinHour;
    private int mMinMinute;
    private RadialSelectorView mMinuteRadialSelectorView;
    private RadialTextsView mMinuteRadialTextsView;
    private int[] mSnapPrefer30sMap;
    private boolean mTimeInitialized;
    private AnimatorSet mTransition;

    /* renamed from: com.android.datetimepicker.time.RadialPickerLayout.1 */
    class C02101 implements Runnable {
        C02101() {
        }

        public void run() {
            RadialPickerLayout.this.mAmPmCirclesView.setAmOrPmPressed(RadialPickerLayout.this.mIsTouchingAmOrPm);
            RadialPickerLayout.this.mAmPmCirclesView.invalidate();
        }
    }

    /* renamed from: com.android.datetimepicker.time.RadialPickerLayout.2 */
    class C02112 implements Runnable {
        final /* synthetic */ Boolean[] val$isInnerCircle;

        C02112(Boolean[] boolArr) {
            this.val$isInnerCircle = boolArr;
        }

        public void run() {
            RadialPickerLayout.this.mDoingMove = true;
            int value = RadialPickerLayout.this.reselectSelector(RadialPickerLayout.this.mDownDegrees, this.val$isInnerCircle[RadialPickerLayout.HOUR_INDEX].booleanValue(), false, true);
            RadialPickerLayout.this.mLastValueSelected = value;
            RadialPickerLayout.this.mListener.onValueSelected(RadialPickerLayout.this.getCurrentItemShowing(), value, false);
        }
    }

    public interface OnValueSelectedListener {
        void onValueSelected(int i, int i2, boolean z);
    }

    public RadialPickerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIsTouchingAmOrPm = -1;
        this.mHandler = new Handler();
        setOnTouchListener(this);
        this.TOUCH_SLOP = ViewConfiguration.get(context).getScaledTouchSlop();
        this.TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
        this.mDoingMove = false;
        this.mCircleView = new CircleView(context);
        addView(this.mCircleView);
        this.mAmPmCirclesView = new AmPmCirclesView(context);
        addView(this.mAmPmCirclesView);
        this.mHourRadialTextsView = new RadialTextsView(context);
        addView(this.mHourRadialTextsView);
        this.mMinuteRadialTextsView = new RadialTextsView(context);
        addView(this.mMinuteRadialTextsView);
        this.mHourRadialSelectorView = new RadialSelectorView(context);
        addView(this.mHourRadialSelectorView);
        this.mMinuteRadialSelectorView = new RadialSelectorView(context);
        addView(this.mMinuteRadialSelectorView);
        preparePrefer30sMap();
        this.mLastValueSelected = -1;
        this.mInputEnabled = true;
        this.mGrayBox = new View(context);
        this.mGrayBox.setLayoutParams(new LayoutParams(-1, -1));
        this.mGrayBox.setBackgroundColor(getResources().getColor(C0206R.color.transparent_black));
        this.mGrayBox.setVisibility(4);
        addView(this.mGrayBox);
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        this.mTimeInitialized = false;
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int minDimension = Math.min(measuredWidth, measuredHeight);
        super.onMeasure(MeasureSpec.makeMeasureSpec(minDimension, widthMode), MeasureSpec.makeMeasureSpec(minDimension, heightMode));
    }

    public void setOnValueSelectedListener(OnValueSelectedListener listener) {
        this.mListener = listener;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void initialize(android.content.Context r29, com.android.datetimepicker.HapticFeedbackController r30, int r31, int r32, boolean r33, int r34, int r35, int r36, int r37) {
        /*
        r28 = this;
        r0 = r28;
        r2 = r0.mTimeInitialized;
        if (r2 == 0) goto L_0x000e;
    L_0x0006:
        r2 = "RadialPickerLayout";
        r5 = "Time has already been initialized.";
        android.util.Log.e(r2, r5);
    L_0x000d:
        return;
    L_0x000e:
        r0 = r34;
        r1 = r28;
        r1.mMinHour = r0;
        r0 = r35;
        r1 = r28;
        r1.mMaxHour = r0;
        r0 = r36;
        r1 = r28;
        r1.mMinMinute = r0;
        r0 = r37;
        r1 = r28;
        r1.mMaxMinute = r0;
        r0 = r30;
        r1 = r28;
        r1.mHapticFeedbackController = r0;
        r0 = r33;
        r1 = r28;
        r1.mIs24HourMode = r0;
        r0 = r28;
        r2 = r0.mAccessibilityManager;
        r2 = r2.isTouchExplorationEnabled();
        if (r2 == 0) goto L_0x00ea;
    L_0x003c:
        r2 = 1;
    L_0x003d:
        r0 = r28;
        r0.mHideAmPm = r2;
        r0 = r28;
        r2 = r0.mCircleView;
        r0 = r28;
        r5 = r0.mHideAmPm;
        r0 = r29;
        r2.initialize(r0, r5);
        r0 = r28;
        r2 = r0.mCircleView;
        r2.invalidate();
        r0 = r28;
        r2 = r0.mHideAmPm;
        if (r2 != 0) goto L_0x0072;
    L_0x005b:
        r0 = r28;
        r5 = r0.mAmPmCirclesView;
        r2 = 12;
        r0 = r31;
        if (r0 >= r2) goto L_0x00f0;
    L_0x0065:
        r2 = 0;
    L_0x0066:
        r0 = r29;
        r5.initialize(r0, r2);
        r0 = r28;
        r2 = r0.mAmPmCirclesView;
        r2.invalidate();
    L_0x0072:
        r3 = r29.getResources();
        r2 = 12;
        r0 = new int[r2];
        r22 = r0;
        r22 = {12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        r2 = 12;
        r0 = new int[r2];
        r23 = r0;
        r23 = {0, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
        r2 = 12;
        r0 = new int[r2];
        r26 = r0;
        r26 = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55};
        r2 = 12;
        r4 = new java.lang.String[r2];
        r2 = 12;
        r0 = new java.lang.String[r2];
        r25 = r0;
        r2 = 12;
        r0 = new java.lang.String[r2];
        r27 = r0;
        r24 = 0;
    L_0x00a3:
        r2 = 12;
        r0 = r24;
        if (r0 >= r2) goto L_0x0106;
    L_0x00a9:
        if (r33 == 0) goto L_0x00f3;
    L_0x00ab:
        r2 = "%02d";
        r5 = 1;
        r5 = new java.lang.Object[r5];
        r6 = 0;
        r7 = r23[r24];
        r7 = java.lang.Integer.valueOf(r7);
        r5[r6] = r7;
        r2 = java.lang.String.format(r2, r5);
    L_0x00bd:
        r4[r24] = r2;
        r2 = "%d";
        r5 = 1;
        r5 = new java.lang.Object[r5];
        r6 = 0;
        r7 = r22[r24];
        r7 = java.lang.Integer.valueOf(r7);
        r5[r6] = r7;
        r2 = java.lang.String.format(r2, r5);
        r25[r24] = r2;
        r2 = "%02d";
        r5 = 1;
        r5 = new java.lang.Object[r5];
        r6 = 0;
        r7 = r26[r24];
        r7 = java.lang.Integer.valueOf(r7);
        r5[r6] = r7;
        r2 = java.lang.String.format(r2, r5);
        r27[r24] = r2;
        r24 = r24 + 1;
        goto L_0x00a3;
    L_0x00ea:
        r0 = r28;
        r2 = r0.mIs24HourMode;
        goto L_0x003d;
    L_0x00f0:
        r2 = 1;
        goto L_0x0066;
    L_0x00f3:
        r2 = "%d";
        r5 = 1;
        r5 = new java.lang.Object[r5];
        r6 = 0;
        r7 = r22[r24];
        r7 = java.lang.Integer.valueOf(r7);
        r5[r6] = r7;
        r2 = java.lang.String.format(r2, r5);
        goto L_0x00bd;
    L_0x0106:
        r0 = r28;
        r2 = r0.mHourRadialTextsView;
        if (r33 == 0) goto L_0x01aa;
    L_0x010c:
        r5 = r25;
    L_0x010e:
        r0 = r28;
        r6 = r0.mHideAmPm;
        r7 = 1;
        r0 = r28;
        r8 = r0.mMinHour;
        r0 = r28;
        r9 = r0.mMaxHour;
        r0 = r28;
        r10 = r0.mMinMinute;
        r0 = r28;
        r11 = r0.mMaxMinute;
        r2.initialize(r3, r4, r5, r6, r7, r8, r9, r10, r11);
        r0 = r28;
        r2 = r0.mHourRadialTextsView;
        r2.invalidate();
        r0 = r28;
        r5 = r0.mMinuteRadialTextsView;
        r8 = 0;
        r0 = r28;
        r9 = r0.mHideAmPm;
        r10 = 0;
        r6 = r3;
        r7 = r27;
        r11 = r34;
        r12 = r35;
        r13 = r36;
        r14 = r37;
        r5.initialize(r6, r7, r8, r9, r10, r11, r12, r13, r14);
        r0 = r28;
        r2 = r0.mMinuteRadialTextsView;
        r2.invalidate();
        r2 = 0;
        r0 = r28;
        r1 = r31;
        r0.setValueForItem(r2, r1);
        r2 = 1;
        r0 = r28;
        r1 = r32;
        r0.setValueForItem(r2, r1);
        r2 = r31 % 12;
        r10 = r2 * 30;
        r0 = r28;
        r5 = r0.mHourRadialSelectorView;
        r0 = r28;
        r7 = r0.mHideAmPm;
        r9 = 1;
        r0 = r28;
        r1 = r31;
        r11 = r0.isHourInnerCircle(r1);
        r0 = r28;
        r12 = r0.mMinHour;
        r0 = r28;
        r13 = r0.mMaxHour;
        r0 = r28;
        r14 = r0.mMinMinute;
        r0 = r28;
        r15 = r0.mMaxMinute;
        r6 = r29;
        r8 = r33;
        r5.initialize(r6, r7, r8, r9, r10, r11, r12, r13, r14, r15);
        r16 = r32 * 6;
        r0 = r28;
        r11 = r0.mMinuteRadialSelectorView;
        r0 = r28;
        r13 = r0.mHideAmPm;
        r14 = 0;
        r15 = 0;
        r17 = 0;
        r12 = r29;
        r18 = r34;
        r19 = r35;
        r20 = r36;
        r21 = r37;
        r11.initialize(r12, r13, r14, r15, r16, r17, r18, r19, r20, r21);
        r2 = 1;
        r0 = r28;
        r0.mTimeInitialized = r2;
        goto L_0x000d;
    L_0x01aa:
        r5 = 0;
        goto L_0x010e;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.datetimepicker.time.RadialPickerLayout.initialize(android.content.Context, com.android.datetimepicker.HapticFeedbackController, int, int, boolean, int, int, int, int):void");
    }

    void setTheme(Context context, boolean themeDark) {
        this.mCircleView.setTheme(context, themeDark);
        this.mAmPmCirclesView.setTheme(context, themeDark);
        this.mHourRadialTextsView.setTheme(context, themeDark);
        this.mMinuteRadialTextsView.setTheme(context, themeDark);
        this.mHourRadialSelectorView.setTheme(context, themeDark);
        this.mMinuteRadialSelectorView.setTheme(context, themeDark);
    }

    public void setTime(int hours, int minutes) {
        setItem(HOUR_INDEX, hours);
        setItem(PM, minutes);
    }

    private void setItem(int index, int value) {
        if (index == 0) {
            setValueForItem(HOUR_INDEX, value);
            this.mHourRadialSelectorView.setSelection((value % 12) * VISIBLE_DEGREES_STEP_SIZE, isHourInnerCircle(value), false);
            this.mHourRadialSelectorView.invalidate();
        } else if (index == PM) {
            setValueForItem(PM, value);
            this.mMinuteRadialSelectorView.setSelection(value * MINUTE_VALUE_TO_DEGREES_STEP_SIZE, false, false);
            this.mMinuteRadialSelectorView.invalidate();
        }
    }

    private boolean isHourInnerCircle(int hourOfDay) {
        return this.mIs24HourMode && hourOfDay <= 12 && hourOfDay != 0;
    }

    public int getHours() {
        return this.mCurrentHoursOfDay;
    }

    public int getMinutes() {
        return this.mCurrentMinutes;
    }

    private int getCurrentlyShowingValue() {
        int currentIndex = getCurrentItemShowing();
        if (currentIndex == 0) {
            return this.mCurrentHoursOfDay;
        }
        if (currentIndex == PM) {
            return this.mCurrentMinutes;
        }
        return -1;
    }

    public int getIsCurrentlyAmOrPm() {
        if (this.mCurrentHoursOfDay < 12) {
            return HOUR_INDEX;
        }
        if (this.mCurrentHoursOfDay < 24) {
            return PM;
        }
        return -1;
    }

    private void setValueForItem(int index, int value) {
        if (index == 0) {
            if (this.mMinHour <= value && value <= this.mMaxHour) {
                this.mCurrentHoursOfDay = value;
                this.mMinuteRadialSelectorView.setSelectedHour(this.mCurrentHoursOfDay);
                this.mMinuteRadialSelectorView.invalidate();
                this.mMinuteRadialTextsView.setSelectedHour(this.mCurrentHoursOfDay);
                this.mMinuteRadialTextsView.invalidate();
            }
        } else if (index == PM) {
            boolean checkedMinMinute = true;
            boolean checkedMaxMinute = true;
            if (this.mCurrentHoursOfDay == this.mMinHour) {
                checkedMinMinute = value >= this.mMinMinute;
            }
            if (this.mCurrentHoursOfDay == this.mMaxHour) {
                if (value <= this.mMaxMinute) {
                    checkedMaxMinute = true;
                } else {
                    checkedMaxMinute = false;
                }
            }
            if (checkedMinMinute && checkedMaxMinute) {
                this.mCurrentMinutes = value;
            }
        } else if (index != AMPM_INDEX) {
        } else {
            if (value == 0) {
                this.mCurrentHoursOfDay %= 12;
            } else if (value == PM) {
                this.mCurrentHoursOfDay = (this.mCurrentHoursOfDay % 12) + 12;
            }
        }
    }

    public void setAmOrPm(int amOrPm) {
        this.mAmPmCirclesView.setAmOrPm(amOrPm);
        this.mAmPmCirclesView.invalidate();
        setValueForItem(AMPM_INDEX, amOrPm);
    }

    private void preparePrefer30sMap() {
        this.mSnapPrefer30sMap = new int[361];
        int snappedOutputDegrees = HOUR_INDEX;
        int count = PM;
        int expectedCount = 8;
        for (int degrees = HOUR_INDEX; degrees < 361; degrees += PM) {
            this.mSnapPrefer30sMap[degrees] = snappedOutputDegrees;
            if (count == expectedCount) {
                snappedOutputDegrees += MINUTE_VALUE_TO_DEGREES_STEP_SIZE;
                if (snappedOutputDegrees == 360) {
                    expectedCount = 7;
                } else if (snappedOutputDegrees % VISIBLE_DEGREES_STEP_SIZE == 0) {
                    expectedCount = 14;
                } else {
                    expectedCount = 4;
                }
                count = PM;
            } else {
                count += PM;
            }
        }
    }

    private int snapPrefer30s(int degrees) {
        if (this.mSnapPrefer30sMap == null) {
            return -1;
        }
        return this.mSnapPrefer30sMap[degrees];
    }

    private static int snapOnly30s(int degrees, int forceHigherOrLower) {
        int floor = (degrees / VISIBLE_DEGREES_STEP_SIZE) * VISIBLE_DEGREES_STEP_SIZE;
        int ceiling = floor + VISIBLE_DEGREES_STEP_SIZE;
        if (forceHigherOrLower == PM) {
            return ceiling;
        }
        if (forceHigherOrLower == -1) {
            if (degrees == floor) {
                floor -= VISIBLE_DEGREES_STEP_SIZE;
            }
            return floor;
        } else if (degrees - floor < ceiling - degrees) {
            return floor;
        } else {
            return ceiling;
        }
    }

    private int reselectSelector(int degrees, boolean isInnerCircle, boolean forceToVisibleValue, boolean forceDrawDot) {
        if (degrees == -1) {
            return -1;
        }
        boolean allowFineGrained;
        RadialSelectorView radialSelectorView;
        int stepSize;
        int currentShowing = getCurrentItemShowing();
        if (forceToVisibleValue || currentShowing != PM) {
            allowFineGrained = false;
        } else {
            allowFineGrained = true;
        }
        if (allowFineGrained) {
            degrees = snapPrefer30s(degrees);
        } else {
            degrees = snapOnly30s(degrees, HOUR_INDEX);
        }
        if (currentShowing == 0) {
            radialSelectorView = this.mHourRadialSelectorView;
            stepSize = VISIBLE_DEGREES_STEP_SIZE;
        } else {
            radialSelectorView = this.mMinuteRadialSelectorView;
            stepSize = MINUTE_VALUE_TO_DEGREES_STEP_SIZE;
        }
        radialSelectorView.setSelection(degrees, isInnerCircle, forceDrawDot);
        radialSelectorView.invalidate();
        if (currentShowing == 0) {
            if (this.mIs24HourMode) {
                if (degrees == 0 && isInnerCircle) {
                    degrees = 360;
                } else if (degrees == 360 && !isInnerCircle) {
                    degrees = HOUR_INDEX;
                }
            } else if (degrees == 0) {
                degrees = 360;
            }
        } else if (degrees == 360 && currentShowing == PM) {
            degrees = HOUR_INDEX;
        }
        int value = degrees / stepSize;
        if (currentShowing != 0 || !this.mIs24HourMode || isInnerCircle || degrees == 0) {
            return value;
        }
        return value + 12;
    }

    private int getDegreesFromCoords(float pointX, float pointY, boolean forceLegal, Boolean[] isInnerCircle) {
        int currentItem = getCurrentItemShowing();
        if (currentItem == 0) {
            return this.mHourRadialSelectorView.getDegreesFromCoords(pointX, pointY, forceLegal, isInnerCircle);
        }
        if (currentItem == PM) {
            return this.mMinuteRadialSelectorView.getDegreesFromCoords(pointX, pointY, forceLegal, isInnerCircle);
        }
        return -1;
    }

    public int getCurrentItemShowing() {
        if (this.mCurrentItemShowing == 0 || this.mCurrentItemShowing == PM) {
            return this.mCurrentItemShowing;
        }
        Log.e(TAG, "Current item showing was unfortunately set to " + this.mCurrentItemShowing);
        return -1;
    }

    public void setCurrentItemShowing(int index, boolean animate) {
        int minuteAlpha = Utils.FULL_ALPHA;
        if (index == 0 || index == PM) {
            int lastIndex = getCurrentItemShowing();
            this.mCurrentItemShowing = index;
            if (!animate || index == lastIndex) {
                int hourAlpha;
                if (index == 0) {
                    hourAlpha = Utils.FULL_ALPHA;
                } else {
                    hourAlpha = HOUR_INDEX;
                }
                if (index != PM) {
                    minuteAlpha = HOUR_INDEX;
                }
                this.mHourRadialTextsView.setAlpha((float) hourAlpha);
                this.mHourRadialSelectorView.setAlpha((float) hourAlpha);
                this.mMinuteRadialTextsView.setAlpha((float) minuteAlpha);
                this.mMinuteRadialSelectorView.setAlpha((float) minuteAlpha);
                return;
            }
            ObjectAnimator[] anims = new ObjectAnimator[4];
            if (index == PM) {
                anims[HOUR_INDEX] = this.mHourRadialTextsView.getDisappearAnimator();
                anims[PM] = this.mHourRadialSelectorView.getDisappearAnimator();
                anims[AMPM_INDEX] = this.mMinuteRadialTextsView.getReappearAnimator();
                anims[ENABLE_PICKER_INDEX] = this.mMinuteRadialSelectorView.getReappearAnimator();
            } else if (index == 0) {
                anims[HOUR_INDEX] = this.mHourRadialTextsView.getReappearAnimator();
                anims[PM] = this.mHourRadialSelectorView.getReappearAnimator();
                anims[AMPM_INDEX] = this.mMinuteRadialTextsView.getDisappearAnimator();
                anims[ENABLE_PICKER_INDEX] = this.mMinuteRadialSelectorView.getDisappearAnimator();
            }
            if (this.mTransition != null && this.mTransition.isRunning()) {
                this.mTransition.end();
            }
            this.mTransition = new AnimatorSet();
            this.mTransition.playTogether(anims);
            this.mTransition.start();
            return;
        }
        Log.e(TAG, "TimePicker does not support view at index " + index);
    }

    public boolean onTouch(View v, MotionEvent event) {
        float eventX = event.getX();
        float eventY = event.getY();
        Boolean[] isInnerCircle = new Boolean[PM];
        isInnerCircle[HOUR_INDEX] = Boolean.valueOf(false);
        int degrees;
        int value;
        switch (event.getAction()) {
            case HOUR_INDEX /*0*/:
                if (!this.mInputEnabled) {
                    return true;
                }
                this.mDownX = eventX;
                this.mDownY = eventY;
                this.mLastValueSelected = -1;
                this.mDoingMove = false;
                this.mDoingTouch = true;
                if (this.mHideAmPm) {
                    this.mIsTouchingAmOrPm = -1;
                } else {
                    this.mIsTouchingAmOrPm = this.mAmPmCirclesView.getIsTouchingAmOrPm(eventX, eventY);
                }
                if (this.mIsTouchingAmOrPm == 0 || this.mIsTouchingAmOrPm == PM) {
                    this.mHapticFeedbackController.tryVibrate();
                    this.mDownDegrees = -1;
                    this.mHandler.postDelayed(new C02101(), (long) this.TAP_TIMEOUT);
                } else {
                    this.mDownDegrees = getDegreesFromCoords(eventX, eventY, this.mAccessibilityManager.isTouchExplorationEnabled(), isInnerCircle);
                    if (this.mDownDegrees != -1) {
                        this.mHapticFeedbackController.tryVibrate();
                        this.mHandler.postDelayed(new C02112(isInnerCircle), (long) this.TAP_TIMEOUT);
                    }
                }
                return true;
            case PM /*1*/:
                if (this.mInputEnabled) {
                    this.mHandler.removeCallbacksAndMessages(null);
                    this.mDoingTouch = false;
                    if (this.mIsTouchingAmOrPm == 0 || this.mIsTouchingAmOrPm == PM) {
                        int isTouchingAmOrPm = this.mAmPmCirclesView.getIsTouchingAmOrPm(eventX, eventY);
                        this.mAmPmCirclesView.setAmOrPmPressed(-1);
                        this.mAmPmCirclesView.invalidate();
                        if (isTouchingAmOrPm == this.mIsTouchingAmOrPm) {
                            this.mAmPmCirclesView.setAmOrPm(isTouchingAmOrPm);
                            if (getIsCurrentlyAmOrPm() != isTouchingAmOrPm) {
                                this.mListener.onValueSelected(AMPM_INDEX, this.mIsTouchingAmOrPm, false);
                                setValueForItem(AMPM_INDEX, isTouchingAmOrPm);
                            }
                        }
                        this.mIsTouchingAmOrPm = -1;
                        break;
                    }
                    if (this.mDownDegrees != -1) {
                        degrees = getDegreesFromCoords(eventX, eventY, this.mDoingMove, isInnerCircle);
                        if (degrees != -1) {
                            value = reselectSelector(degrees, isInnerCircle[HOUR_INDEX].booleanValue(), !this.mDoingMove, false);
                            if (getCurrentItemShowing() == 0 && !this.mIs24HourMode) {
                                int amOrPm = getIsCurrentlyAmOrPm();
                                if (amOrPm == 0 && value == 12) {
                                    value = HOUR_INDEX;
                                } else if (amOrPm == PM && value != 12) {
                                    value += 12;
                                }
                            }
                            setValueForItem(getCurrentItemShowing(), value);
                            this.mListener.onValueSelected(getCurrentItemShowing(), value, true);
                        }
                    }
                    this.mDoingMove = false;
                    return true;
                }
                Log.d(TAG, "Input was disabled, but received ACTION_UP.");
                this.mListener.onValueSelected(ENABLE_PICKER_INDEX, PM, false);
                return true;
                break;
            case AMPM_INDEX /*2*/:
                if (this.mInputEnabled) {
                    float dY = Math.abs(eventY - this.mDownY);
                    float dX = Math.abs(eventX - this.mDownX);
                    if (this.mDoingMove || dX > ((float) this.TOUCH_SLOP) || dY > ((float) this.TOUCH_SLOP)) {
                        if (this.mIsTouchingAmOrPm == 0 || this.mIsTouchingAmOrPm == PM) {
                            this.mHandler.removeCallbacksAndMessages(null);
                            if (this.mAmPmCirclesView.getIsTouchingAmOrPm(eventX, eventY) != this.mIsTouchingAmOrPm) {
                                this.mAmPmCirclesView.setAmOrPmPressed(-1);
                                this.mAmPmCirclesView.invalidate();
                                this.mIsTouchingAmOrPm = -1;
                                break;
                            }
                        } else if (this.mDownDegrees != -1) {
                            this.mDoingMove = true;
                            this.mHandler.removeCallbacksAndMessages(null);
                            degrees = getDegreesFromCoords(eventX, eventY, true, isInnerCircle);
                            if (degrees != -1) {
                                value = reselectSelector(degrees, isInnerCircle[HOUR_INDEX].booleanValue(), false, true);
                                if (value != this.mLastValueSelected) {
                                    this.mHapticFeedbackController.tryVibrate();
                                    this.mLastValueSelected = value;
                                    setValueForItem(getCurrentItemShowing(), value);
                                    this.mListener.onValueSelected(getCurrentItemShowing(), value, false);
                                }
                            }
                            return true;
                        }
                    }
                }
                Log.e(TAG, "Input was disabled, but received ACTION_MOVE.");
                return true;
                break;
        }
        return false;
    }

    public boolean trySettingInputEnabled(boolean inputEnabled) {
        int i = HOUR_INDEX;
        if (this.mDoingTouch && !inputEnabled) {
            return false;
        }
        this.mInputEnabled = inputEnabled;
        View view = this.mGrayBox;
        if (inputEnabled) {
            i = 4;
        }
        view.setVisibility(i);
        return true;
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.addAction(ItemAnimator.FLAG_APPEARED_IN_PRE_LAYOUT);
        info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != 32) {
            return super.dispatchPopulateAccessibilityEvent(event);
        }
        event.getText().clear();
        Time time = new Time();
        time.hour = getHours();
        time.minute = getMinutes();
        long millis = time.normalize(true);
        int flags = PM;
        if (this.mIs24HourMode) {
            flags = PM | TransportMediator.FLAG_KEY_MEDIA_NEXT;
        }
        event.getText().add(DateUtils.formatDateTime(getContext(), millis, flags));
        return true;
    }

    @SuppressLint({"NewApi"})
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (super.performAccessibilityAction(action, arguments)) {
            return true;
        }
        int changeMultiplier = HOUR_INDEX;
        if (action == ItemAnimator.FLAG_APPEARED_IN_PRE_LAYOUT) {
            changeMultiplier = PM;
        } else if (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) {
            changeMultiplier = -1;
        }
        if (changeMultiplier == 0) {
            return false;
        }
        int maxValue;
        int value = getCurrentlyShowingValue();
        int stepSize = HOUR_INDEX;
        int currentItemShowing = getCurrentItemShowing();
        if (currentItemShowing == 0) {
            stepSize = VISIBLE_DEGREES_STEP_SIZE;
            value %= 12;
        } else if (currentItemShowing == PM) {
            stepSize = MINUTE_VALUE_TO_DEGREES_STEP_SIZE;
        }
        value = snapOnly30s(value * stepSize, changeMultiplier) / stepSize;
        int minValue = HOUR_INDEX;
        if (currentItemShowing != 0) {
            maxValue = 55;
        } else if (this.mIs24HourMode) {
            maxValue = 23;
        } else {
            maxValue = 12;
            minValue = PM;
        }
        if (value > maxValue) {
            value = minValue;
        } else if (value < minValue) {
            value = maxValue;
        }
        setItem(currentItemShowing, value);
        this.mListener.onValueSelected(currentItemShowing, value, false);
        return true;
    }
}
