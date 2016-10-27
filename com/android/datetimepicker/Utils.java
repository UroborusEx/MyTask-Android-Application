package com.android.datetimepicker;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.os.Build.VERSION;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import com.android.datetimepicker.date.DayPickerView;
import com.android.datetimepicker.time.TimePickerDialog;
import com.snowmobile.tasks.C0220R;

public class Utils {
    public static final int FULL_ALPHA = 255;
    public static final int MONDAY_BEFORE_JULIAN_EPOCH = 2440585;
    public static final int PULSE_ANIMATOR_DURATION = 544;
    public static final int SELECTED_ALPHA = 51;
    public static final int SELECTED_ALPHA_THEME_DARK = 102;
    static final String SHARED_PREFS_NAME = "com.android.calendar_preferences";

    public static boolean isJellybeanOrLater() {
        return VERSION.SDK_INT >= 16;
    }

    @SuppressLint({"NewApi"})
    public static void tryAccessibilityAnnounce(View view, CharSequence text) {
        if (isJellybeanOrLater() && view != null && text != null) {
            view.announceForAccessibility(text);
        }
    }

    public static int getDaysInMonth(int month, int year) {
        switch (month) {
            case TimePickerDialog.HOUR_INDEX /*0*/:
            case TimePickerDialog.AMPM_INDEX /*2*/:
            case ItemTouchHelper.LEFT /*4*/:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT /*6*/:
            case DayPickerView.DAYS_PER_WEEK /*7*/:
            case C0220R.styleable.Toolbar_popupTheme /*9*/:
            case C0220R.styleable.Toolbar_subtitleTextAppearance /*11*/:
                return 31;
            case TimePickerDialog.PM /*1*/:
                return year % 4 == 0 ? 29 : 28;
            case TimePickerDialog.ENABLE_PICKER_INDEX /*3*/:
            case WearableExtender.SIZE_FULL_SCREEN /*5*/:
            case ItemTouchHelper.RIGHT /*8*/:
            case C0220R.styleable.Toolbar_titleTextAppearance /*10*/:
                return 30;
            default:
                throw new IllegalArgumentException("Invalid Month");
        }
    }

    public static int getJulianMondayFromWeeksSinceEpoch(int week) {
        return MONDAY_BEFORE_JULIAN_EPOCH + (week * 7);
    }

    public static int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
        int diff = 4 - firstDayOfWeek;
        if (diff < 0) {
            diff += 7;
        }
        return (julianDay - (2440588 - diff)) / 7;
    }

    public static ObjectAnimator getPulseAnimator(View labelToAnimate, float decreaseRatio, float increaseRatio) {
        Keyframe k0 = Keyframe.ofFloat(0.0f, 1.0f);
        Keyframe k1 = Keyframe.ofFloat(0.275f, decreaseRatio);
        Keyframe k2 = Keyframe.ofFloat(0.69f, increaseRatio);
        Keyframe k3 = Keyframe.ofFloat(1.0f, 1.0f);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofKeyframe("scaleX", new Keyframe[]{k0, k1, k2, k3});
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofKeyframe("scaleY", new Keyframe[]{k0, k1, k2, k3});
        ObjectAnimator pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(labelToAnimate, new PropertyValuesHolder[]{scaleX, scaleY});
        pulseAnimator.setDuration(544);
        return pulseAnimator;
    }
}
