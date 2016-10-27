package com.android.datetimepicker.date;

import android.animation.ObjectAnimator;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.datetimepicker.C0206R;
import com.android.datetimepicker.HapticFeedbackController;
import com.android.datetimepicker.Utils;
import com.android.datetimepicker.date.MonthAdapter.CalendarDay;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

public class DatePickerDialog extends DialogFragment implements OnClickListener, DatePickerController {
    private static final int ANIMATION_DELAY = 500;
    private static final int ANIMATION_DURATION = 300;
    private static SimpleDateFormat DAY_FORMAT = null;
    private static final int DEFAULT_END_YEAR = 2100;
    private static final int DEFAULT_START_YEAR = 1900;
    private static final String KEY_CURRENT_VIEW = "current_view";
    private static final String KEY_LIST_POSITION = "list_position";
    private static final String KEY_LIST_POSITION_OFFSET = "list_position_offset";
    private static final String KEY_MAX_DATE = "max_date";
    private static final String KEY_MIN_DATE = "min_date";
    private static final String KEY_SELECTED_DAY = "day";
    private static final String KEY_SELECTED_MONTH = "month";
    private static final String KEY_SELECTED_YEAR = "year";
    private static final String KEY_WEEK_START = "week_start";
    private static final String KEY_YEAR_END = "year_end";
    private static final String KEY_YEAR_START = "year_start";
    private static final int MONTH_AND_DAY_VIEW = 0;
    private static final String TAG = "DatePickerDialog";
    private static final int UNINITIALIZED = -1;
    private static SimpleDateFormat YEAR_FORMAT = null;
    private static final int YEAR_VIEW = 1;
    private AccessibleDateAnimator mAnimator;
    private final Calendar mCalendar;
    private OnDateSetListener mCallBack;
    private int mCurrentView;
    private TextView mDayOfWeekView;
    private String mDayPickerDescription;
    private DayPickerView mDayPickerView;
    private boolean mDelayAnimation;
    private Button mDoneButton;
    private HapticFeedbackController mHapticFeedbackController;
    private HashSet<OnDateChangedListener> mListeners;
    private Calendar mMaxDate;
    private int mMaxYear;
    private Calendar mMinDate;
    private int mMinYear;
    private LinearLayout mMonthAndDayView;
    private String mSelectDay;
    private String mSelectYear;
    private TextView mSelectedDayTextView;
    private TextView mSelectedMonthTextView;
    private int mWeekStart;
    private String mYearPickerDescription;
    private YearPickerView mYearPickerView;
    private TextView mYearView;

    /* renamed from: com.android.datetimepicker.date.DatePickerDialog.1 */
    class C02071 implements OnClickListener {
        C02071() {
        }

        public void onClick(View v) {
            DatePickerDialog.this.tryVibrate();
            if (DatePickerDialog.this.mCallBack != null) {
                DatePickerDialog.this.mCallBack.onDateSet(DatePickerDialog.this, DatePickerDialog.this.mCalendar.get(DatePickerDialog.YEAR_VIEW), DatePickerDialog.this.mCalendar.get(2), DatePickerDialog.this.mCalendar.get(5));
            }
            DatePickerDialog.this.dismiss();
        }
    }

    public interface OnDateChangedListener {
        void onDateChanged();
    }

    public interface OnDateSetListener {
        void onDateSet(DatePickerDialog datePickerDialog, int i, int i2, int i3);
    }

    static {
        YEAR_FORMAT = new SimpleDateFormat("yyyy", Locale.getDefault());
        DAY_FORMAT = new SimpleDateFormat("dd", Locale.getDefault());
    }

    public DatePickerDialog() {
        this.mCalendar = Calendar.getInstance();
        this.mListeners = new HashSet();
        this.mCurrentView = UNINITIALIZED;
        this.mWeekStart = this.mCalendar.getFirstDayOfWeek();
        this.mMinYear = DEFAULT_START_YEAR;
        this.mMaxYear = DEFAULT_END_YEAR;
        this.mDelayAnimation = true;
    }

    public static DatePickerDialog newInstance(OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth) {
        DatePickerDialog ret = new DatePickerDialog();
        ret.initialize(callBack, year, monthOfYear, dayOfMonth);
        return ret;
    }

    public void initialize(OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth) {
        this.mCallBack = callBack;
        this.mCalendar.set(YEAR_VIEW, year);
        this.mCalendar.set(2, monthOfYear);
        this.mCalendar.set(5, dayOfMonth);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getWindow().setSoftInputMode(3);
        if (savedInstanceState != null) {
            this.mCalendar.set(YEAR_VIEW, savedInstanceState.getInt(KEY_SELECTED_YEAR));
            this.mCalendar.set(2, savedInstanceState.getInt(KEY_SELECTED_MONTH));
            this.mCalendar.set(5, savedInstanceState.getInt(KEY_SELECTED_DAY));
            setMinDate((Calendar) savedInstanceState.getSerializable(KEY_MIN_DATE));
            setMaxDate((Calendar) savedInstanceState.getSerializable(KEY_MAX_DATE));
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_YEAR, this.mCalendar.get(YEAR_VIEW));
        outState.putInt(KEY_SELECTED_MONTH, this.mCalendar.get(2));
        outState.putInt(KEY_SELECTED_DAY, this.mCalendar.get(5));
        outState.putInt(KEY_WEEK_START, this.mWeekStart);
        outState.putInt(KEY_YEAR_START, this.mMinYear);
        outState.putInt(KEY_YEAR_END, this.mMaxYear);
        outState.putInt(KEY_CURRENT_VIEW, this.mCurrentView);
        int listPosition = UNINITIALIZED;
        if (this.mCurrentView == 0) {
            listPosition = this.mDayPickerView.getMostVisiblePosition();
        } else if (this.mCurrentView == YEAR_VIEW) {
            listPosition = this.mYearPickerView.getFirstVisiblePosition();
            outState.putInt(KEY_LIST_POSITION_OFFSET, this.mYearPickerView.getFirstPositionOffset());
        }
        outState.putInt(KEY_LIST_POSITION, listPosition);
        outState.putSerializable(KEY_MIN_DATE, this.mMinDate);
        outState.putSerializable(KEY_MAX_DATE, this.mMaxDate);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        getDialog().getWindow().requestFeature(YEAR_VIEW);
        View view = inflater.inflate(C0206R.layout.date_picker_dialog, null);
        this.mDayOfWeekView = (TextView) view.findViewById(C0206R.id.date_picker_header);
        this.mMonthAndDayView = (LinearLayout) view.findViewById(C0206R.id.date_picker_month_and_day);
        this.mMonthAndDayView.setOnClickListener(this);
        this.mSelectedMonthTextView = (TextView) view.findViewById(C0206R.id.date_picker_month);
        this.mSelectedDayTextView = (TextView) view.findViewById(C0206R.id.date_picker_day);
        this.mYearView = (TextView) view.findViewById(C0206R.id.date_picker_year);
        this.mYearView.setOnClickListener(this);
        int listPosition = UNINITIALIZED;
        int listPositionOffset = MONTH_AND_DAY_VIEW;
        int currentView = MONTH_AND_DAY_VIEW;
        if (savedInstanceState != null) {
            this.mWeekStart = savedInstanceState.getInt(KEY_WEEK_START);
            this.mMinYear = savedInstanceState.getInt(KEY_YEAR_START);
            this.mMaxYear = savedInstanceState.getInt(KEY_YEAR_END);
            currentView = savedInstanceState.getInt(KEY_CURRENT_VIEW);
            listPosition = savedInstanceState.getInt(KEY_LIST_POSITION);
            listPositionOffset = savedInstanceState.getInt(KEY_LIST_POSITION_OFFSET);
        }
        Context activity = getActivity();
        this.mDayPickerView = new SimpleDayPickerView(activity, (DatePickerController) this);
        this.mYearPickerView = new YearPickerView(activity, this);
        Resources res = getResources();
        this.mDayPickerDescription = res.getString(C0206R.string.day_picker_description);
        this.mSelectDay = res.getString(C0206R.string.select_day);
        this.mYearPickerDescription = res.getString(C0206R.string.year_picker_description);
        this.mSelectYear = res.getString(C0206R.string.select_year);
        this.mAnimator = (AccessibleDateAnimator) view.findViewById(C0206R.id.animator);
        this.mAnimator.addView(this.mDayPickerView);
        this.mAnimator.addView(this.mYearPickerView);
        this.mAnimator.setDateMillis(this.mCalendar.getTimeInMillis());
        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(300);
        this.mAnimator.setInAnimation(animation);
        Animation animation2 = new AlphaAnimation(1.0f, 0.0f);
        animation2.setDuration(300);
        this.mAnimator.setOutAnimation(animation2);
        this.mDoneButton = (Button) view.findViewById(C0206R.id.done);
        this.mDoneButton.setOnClickListener(new C02071());
        updateDisplay(false);
        setCurrentView(currentView);
        if (listPosition != UNINITIALIZED) {
            if (currentView == 0) {
                this.mDayPickerView.postSetSelection(listPosition);
            } else if (currentView == YEAR_VIEW) {
                this.mYearPickerView.postSetSelectionFromTop(listPosition, listPositionOffset);
            }
        }
        this.mHapticFeedbackController = new HapticFeedbackController(activity);
        return view;
    }

    public void onResume() {
        super.onResume();
        this.mHapticFeedbackController.start();
    }

    public void onPause() {
        super.onPause();
        this.mHapticFeedbackController.stop();
    }

    private void setCurrentView(int viewIndex) {
        long millis = this.mCalendar.getTimeInMillis();
        ObjectAnimator pulseAnimator;
        switch (viewIndex) {
            case MONTH_AND_DAY_VIEW /*0*/:
                pulseAnimator = Utils.getPulseAnimator(this.mMonthAndDayView, 0.9f, 1.05f);
                if (this.mDelayAnimation) {
                    pulseAnimator.setStartDelay(500);
                    this.mDelayAnimation = false;
                }
                this.mDayPickerView.onDateChanged();
                if (this.mCurrentView != viewIndex) {
                    this.mMonthAndDayView.setSelected(true);
                    this.mYearView.setSelected(false);
                    this.mAnimator.setDisplayedChild(MONTH_AND_DAY_VIEW);
                    this.mCurrentView = viewIndex;
                }
                pulseAnimator.start();
                this.mAnimator.setContentDescription(this.mDayPickerDescription + ": " + DateUtils.formatDateTime(getActivity(), millis, 16));
                Utils.tryAccessibilityAnnounce(this.mAnimator, this.mSelectDay);
            case YEAR_VIEW /*1*/:
                pulseAnimator = Utils.getPulseAnimator(this.mYearView, 0.85f, 1.1f);
                if (this.mDelayAnimation) {
                    pulseAnimator.setStartDelay(500);
                    this.mDelayAnimation = false;
                }
                this.mYearPickerView.onDateChanged();
                if (this.mCurrentView != viewIndex) {
                    this.mMonthAndDayView.setSelected(false);
                    this.mYearView.setSelected(true);
                    this.mAnimator.setDisplayedChild(YEAR_VIEW);
                    this.mCurrentView = viewIndex;
                }
                pulseAnimator.start();
                this.mAnimator.setContentDescription(this.mYearPickerDescription + ": " + YEAR_FORMAT.format(Long.valueOf(millis)));
                Utils.tryAccessibilityAnnounce(this.mAnimator, this.mSelectYear);
            default:
        }
    }

    private void updateDisplay(boolean announce) {
        if (this.mDayOfWeekView != null) {
            this.mDayOfWeekView.setText(this.mCalendar.getDisplayName(7, 2, Locale.getDefault()).toUpperCase(Locale.getDefault()));
        }
        this.mSelectedMonthTextView.setText(this.mCalendar.getDisplayName(2, YEAR_VIEW, Locale.getDefault()).toUpperCase(Locale.getDefault()));
        this.mSelectedDayTextView.setText(DAY_FORMAT.format(this.mCalendar.getTime()));
        this.mYearView.setText(YEAR_FORMAT.format(this.mCalendar.getTime()));
        long millis = this.mCalendar.getTimeInMillis();
        this.mAnimator.setDateMillis(millis);
        this.mMonthAndDayView.setContentDescription(DateUtils.formatDateTime(getActivity(), millis, 24));
        if (announce) {
            Utils.tryAccessibilityAnnounce(this.mAnimator, DateUtils.formatDateTime(getActivity(), millis, 20));
        }
    }

    public void setFirstDayOfWeek(int startOfWeek) {
        if (startOfWeek < YEAR_VIEW || startOfWeek > 7) {
            throw new IllegalArgumentException("Value must be between Calendar.SUNDAY and Calendar.SATURDAY");
        }
        this.mWeekStart = startOfWeek;
        if (this.mDayPickerView != null) {
            this.mDayPickerView.onChange();
        }
    }

    public void setYearRange(int startYear, int endYear) {
        if (endYear <= startYear) {
            throw new IllegalArgumentException("Year end must be larger than year start");
        }
        this.mMinYear = startYear;
        this.mMaxYear = endYear;
        if (this.mDayPickerView != null) {
            this.mDayPickerView.onChange();
        }
    }

    public void setMinDate(Calendar calendar) {
        this.mMinDate = calendar;
        if (this.mDayPickerView != null) {
            this.mDayPickerView.onChange();
        }
    }

    public Calendar getMinDate() {
        return this.mMinDate;
    }

    public void setMaxDate(Calendar calendar) {
        this.mMaxDate = calendar;
        if (this.mDayPickerView != null) {
            this.mDayPickerView.onChange();
        }
    }

    public Calendar getMaxDate() {
        return this.mMaxDate;
    }

    public void setOnDateSetListener(OnDateSetListener listener) {
        this.mCallBack = listener;
    }

    private void adjustDayInMonthIfNeeded(int month, int year) {
        int day = this.mCalendar.get(5);
        int daysInMonth = Utils.getDaysInMonth(month, year);
        if (day > daysInMonth) {
            this.mCalendar.set(5, daysInMonth);
        }
    }

    public void onClick(View v) {
        tryVibrate();
        if (v.getId() == C0206R.id.date_picker_year) {
            setCurrentView(YEAR_VIEW);
        } else if (v.getId() == C0206R.id.date_picker_month_and_day) {
            setCurrentView(MONTH_AND_DAY_VIEW);
        }
    }

    public void onYearSelected(int year) {
        adjustDayInMonthIfNeeded(this.mCalendar.get(2), year);
        this.mCalendar.set(YEAR_VIEW, year);
        updatePickers();
        setCurrentView(MONTH_AND_DAY_VIEW);
        updateDisplay(true);
    }

    public void onDayOfMonthSelected(int year, int month, int day) {
        this.mCalendar.set(YEAR_VIEW, year);
        this.mCalendar.set(2, month);
        this.mCalendar.set(5, day);
        updatePickers();
        updateDisplay(true);
    }

    private void updatePickers() {
        Iterator<OnDateChangedListener> iterator = this.mListeners.iterator();
        while (iterator.hasNext()) {
            ((OnDateChangedListener) iterator.next()).onDateChanged();
        }
    }

    public CalendarDay getSelectedDay() {
        return new CalendarDay(this.mCalendar);
    }

    public int getMinYear() {
        return this.mMinYear;
    }

    public int getMaxYear() {
        return this.mMaxYear;
    }

    public int getFirstDayOfWeek() {
        return this.mWeekStart;
    }

    public void registerOnDateChangedListener(OnDateChangedListener listener) {
        this.mListeners.add(listener);
    }

    public void unregisterOnDateChangedListener(OnDateChangedListener listener) {
        this.mListeners.remove(listener);
    }

    public void tryVibrate() {
        this.mHapticFeedbackController.tryVibrate();
    }
}
