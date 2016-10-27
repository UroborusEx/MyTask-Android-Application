package com.android.datetimepicker.date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.media.TransportMediator;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.AccessibilityDelegate;
import android.view.View.MeasureSpec;
import android.view.accessibility.AccessibilityEvent;
import com.android.datetimepicker.C0206R;
import com.android.datetimepicker.Utils;
import com.android.datetimepicker.date.MonthAdapter.CalendarDay;
import java.security.InvalidParameterException;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public abstract class MonthView extends View {
    protected static int DAY_SELECTED_CIRCLE_SIZE = 0;
    protected static int DAY_SEPARATOR_WIDTH = 0;
    protected static final int DEFAULT_FOCUS_MONTH = -1;
    protected static int DEFAULT_HEIGHT = 0;
    protected static final int DEFAULT_NUM_DAYS = 7;
    protected static final int DEFAULT_NUM_ROWS = 6;
    protected static final int DEFAULT_SELECTED_DAY = -1;
    protected static final int DEFAULT_SHOW_WK_NUM = 0;
    protected static final int DEFAULT_WEEK_START = 1;
    protected static final int MAX_NUM_ROWS = 6;
    protected static int MINI_DAY_NUMBER_TEXT_SIZE = 0;
    protected static int MIN_HEIGHT = 0;
    protected static int MONTH_DAY_LABEL_TEXT_SIZE = 0;
    protected static int MONTH_HEADER_SIZE = 0;
    protected static int MONTH_LABEL_TEXT_SIZE = 0;
    private static final int SELECTED_CIRCLE_ALPHA = 60;
    private static final String TAG = "MonthView";
    public static final String VIEW_PARAMS_FOCUS_MONTH = "focus_month";
    public static final String VIEW_PARAMS_HEIGHT = "height";
    public static final String VIEW_PARAMS_MONTH = "month";
    public static final String VIEW_PARAMS_NUM_DAYS = "num_days";
    public static final String VIEW_PARAMS_SELECTED_DAY = "selected_day";
    public static final String VIEW_PARAMS_SHOW_WK_NUM = "show_wk_num";
    public static final String VIEW_PARAMS_WEEK_START = "week_start";
    public static final String VIEW_PARAMS_YEAR = "year";
    protected static float mScale;
    private final Calendar mCalendar;
    protected DatePickerController mController;
    protected final Calendar mDayLabelCalendar;
    private int mDayOfWeekStart;
    private String mDayOfWeekTypeface;
    protected int mDayTextColor;
    protected int mDisabledDayTextColor;
    protected int mEdgePadding;
    protected int mFirstJulianDay;
    protected int mFirstMonth;
    private final Formatter mFormatter;
    protected boolean mHasToday;
    protected int mLastMonth;
    private boolean mLockAccessibilityDelegate;
    protected int mMonth;
    protected Paint mMonthDayLabelPaint;
    protected Paint mMonthNumPaint;
    protected int mMonthTitleBGColor;
    protected Paint mMonthTitleBGPaint;
    protected int mMonthTitleColor;
    protected Paint mMonthTitlePaint;
    private String mMonthTitleTypeface;
    protected int mNumCells;
    protected int mNumDays;
    protected int mNumRows;
    protected OnDayClickListener mOnDayClickListener;
    protected int mRowHeight;
    protected Paint mSelectedCirclePaint;
    protected int mSelectedDay;
    protected int mSelectedLeft;
    protected int mSelectedRight;
    private final StringBuilder mStringBuilder;
    protected int mToday;
    protected int mTodayNumberColor;
    private final MonthViewTouchHelper mTouchHelper;
    protected int mWeekStart;
    protected int mWidth;
    protected int mYear;

    public interface OnDayClickListener {
        void onDayClick(MonthView monthView, CalendarDay calendarDay);
    }

    protected class MonthViewTouchHelper extends ExploreByTouchHelper {
        private static final String DATE_FORMAT = "dd MMMM yyyy";
        private final Calendar mTempCalendar;
        private final Rect mTempRect;

        public MonthViewTouchHelper(View host) {
            super(host);
            this.mTempRect = new Rect();
            this.mTempCalendar = Calendar.getInstance();
        }

        public void setFocusedVirtualView(int virtualViewId) {
            getAccessibilityNodeProvider(MonthView.this).performAction(virtualViewId, 64, null);
        }

        public void clearFocusedVirtualView() {
            int focusedVirtualView = getFocusedVirtualView();
            if (focusedVirtualView != LinearLayoutManager.INVALID_OFFSET) {
                getAccessibilityNodeProvider(MonthView.this).performAction(focusedVirtualView, TransportMediator.FLAG_KEY_MEDIA_NEXT, null);
            }
        }

        protected int getVirtualViewAt(float x, float y) {
            int day = MonthView.this.getDayFromLocation(x, y);
            return day >= 0 ? day : LinearLayoutManager.INVALID_OFFSET;
        }

        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            for (int day = MonthView.DEFAULT_WEEK_START; day <= MonthView.this.mNumCells; day += MonthView.DEFAULT_WEEK_START) {
                virtualViewIds.add(Integer.valueOf(day));
            }
        }

        protected void onPopulateEventForVirtualView(int virtualViewId, AccessibilityEvent event) {
            event.setContentDescription(getItemDescription(virtualViewId));
        }

        protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfoCompat node) {
            getItemBounds(virtualViewId, this.mTempRect);
            node.setContentDescription(getItemDescription(virtualViewId));
            node.setBoundsInParent(this.mTempRect);
            node.addAction(16);
            if (virtualViewId == MonthView.this.mSelectedDay) {
                node.setSelected(true);
            }
        }

        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
            switch (action) {
                case ItemTouchHelper.START /*16*/:
                    MonthView.this.onDayClick(virtualViewId);
                    return true;
                default:
                    return false;
            }
        }

        protected void getItemBounds(int day, Rect rect) {
            int offsetX = MonthView.this.mEdgePadding;
            int offsetY = MonthView.this.getMonthHeaderSize();
            int cellHeight = MonthView.this.mRowHeight;
            int cellWidth = (MonthView.this.mWidth - (MonthView.this.mEdgePadding * 2)) / MonthView.this.mNumDays;
            int index = (day + MonthView.DEFAULT_SELECTED_DAY) + MonthView.this.findDayOffset();
            int x = offsetX + ((index % MonthView.this.mNumDays) * cellWidth);
            int y = offsetY + ((index / MonthView.this.mNumDays) * cellHeight);
            rect.set(x, y, x + cellWidth, y + cellHeight);
        }

        protected CharSequence getItemDescription(int day) {
            this.mTempCalendar.set(MonthView.this.mYear, MonthView.this.mMonth, day);
            CharSequence date = DateFormat.format(DATE_FORMAT, this.mTempCalendar.getTimeInMillis());
            if (day != MonthView.this.mSelectedDay) {
                return date;
            }
            Context context = MonthView.this.getContext();
            int i = C0206R.string.item_is_selected;
            Object[] objArr = new Object[MonthView.DEFAULT_WEEK_START];
            objArr[MonthView.DEFAULT_SHOW_WK_NUM] = date;
            return context.getString(i, objArr);
        }
    }

    public abstract void drawMonthDay(Canvas canvas, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9);

    static {
        DEFAULT_HEIGHT = 32;
        MIN_HEIGHT = 10;
        DAY_SEPARATOR_WIDTH = DEFAULT_WEEK_START;
        mScale = 0.0f;
    }

    public MonthView(Context context) {
        this(context, null);
    }

    public MonthView(Context context, AttributeSet attr) {
        super(context, attr);
        this.mEdgePadding = DEFAULT_SHOW_WK_NUM;
        this.mFirstJulianDay = DEFAULT_SELECTED_DAY;
        this.mFirstMonth = DEFAULT_SELECTED_DAY;
        this.mLastMonth = DEFAULT_SELECTED_DAY;
        this.mRowHeight = DEFAULT_HEIGHT;
        this.mHasToday = false;
        this.mSelectedDay = DEFAULT_SELECTED_DAY;
        this.mToday = DEFAULT_SELECTED_DAY;
        this.mWeekStart = DEFAULT_WEEK_START;
        this.mNumDays = DEFAULT_NUM_DAYS;
        this.mNumCells = this.mNumDays;
        this.mSelectedLeft = DEFAULT_SELECTED_DAY;
        this.mSelectedRight = DEFAULT_SELECTED_DAY;
        this.mNumRows = MAX_NUM_ROWS;
        this.mDayOfWeekStart = DEFAULT_SHOW_WK_NUM;
        Resources res = context.getResources();
        this.mDayLabelCalendar = Calendar.getInstance();
        this.mCalendar = Calendar.getInstance();
        this.mDayOfWeekTypeface = res.getString(C0206R.string.day_of_week_label_typeface);
        this.mMonthTitleTypeface = res.getString(C0206R.string.sans_serif);
        this.mDayTextColor = res.getColor(C0206R.color.date_picker_text_normal);
        this.mTodayNumberColor = res.getColor(C0206R.color.blue);
        this.mDisabledDayTextColor = res.getColor(C0206R.color.date_picker_text_disabled);
        this.mMonthTitleColor = res.getColor(17170443);
        this.mMonthTitleBGColor = res.getColor(C0206R.color.circle_background);
        this.mStringBuilder = new StringBuilder(50);
        this.mFormatter = new Formatter(this.mStringBuilder, Locale.getDefault());
        MINI_DAY_NUMBER_TEXT_SIZE = res.getDimensionPixelSize(C0206R.dimen.day_number_size);
        MONTH_LABEL_TEXT_SIZE = res.getDimensionPixelSize(C0206R.dimen.month_label_size);
        MONTH_DAY_LABEL_TEXT_SIZE = res.getDimensionPixelSize(C0206R.dimen.month_day_label_text_size);
        MONTH_HEADER_SIZE = res.getDimensionPixelOffset(C0206R.dimen.month_list_item_header_height);
        DAY_SELECTED_CIRCLE_SIZE = res.getDimensionPixelSize(C0206R.dimen.day_number_select_circle_radius);
        this.mRowHeight = (res.getDimensionPixelOffset(C0206R.dimen.date_picker_view_animator_height) - getMonthHeaderSize()) / MAX_NUM_ROWS;
        this.mTouchHelper = getMonthViewTouchHelper();
        ViewCompat.setAccessibilityDelegate(this, this.mTouchHelper);
        ViewCompat.setImportantForAccessibility(this, DEFAULT_WEEK_START);
        this.mLockAccessibilityDelegate = true;
        initView();
    }

    public void setDatePickerController(DatePickerController controller) {
        this.mController = controller;
    }

    protected MonthViewTouchHelper getMonthViewTouchHelper() {
        return new MonthViewTouchHelper(this);
    }

    public void setAccessibilityDelegate(AccessibilityDelegate delegate) {
        if (!this.mLockAccessibilityDelegate) {
            super.setAccessibilityDelegate(delegate);
        }
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.mOnDayClickListener = listener;
    }

    public boolean dispatchHoverEvent(MotionEvent event) {
        if (this.mTouchHelper.dispatchHoverEvent(event)) {
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case DEFAULT_WEEK_START /*1*/:
                int day = getDayFromLocation(event.getX(), event.getY());
                if (day >= 0) {
                    onDayClick(day);
                    break;
                }
                break;
        }
        return true;
    }

    protected void initView() {
        this.mMonthTitlePaint = new Paint();
        this.mMonthTitlePaint.setFakeBoldText(true);
        this.mMonthTitlePaint.setAntiAlias(true);
        this.mMonthTitlePaint.setTextSize((float) MONTH_LABEL_TEXT_SIZE);
        this.mMonthTitlePaint.setTypeface(Typeface.create(this.mMonthTitleTypeface, DEFAULT_WEEK_START));
        this.mMonthTitlePaint.setColor(this.mDayTextColor);
        this.mMonthTitlePaint.setTextAlign(Align.CENTER);
        this.mMonthTitlePaint.setStyle(Style.FILL);
        this.mMonthTitleBGPaint = new Paint();
        this.mMonthTitleBGPaint.setFakeBoldText(true);
        this.mMonthTitleBGPaint.setAntiAlias(true);
        this.mMonthTitleBGPaint.setColor(this.mMonthTitleBGColor);
        this.mMonthTitleBGPaint.setTextAlign(Align.CENTER);
        this.mMonthTitleBGPaint.setStyle(Style.FILL);
        this.mSelectedCirclePaint = new Paint();
        this.mSelectedCirclePaint.setFakeBoldText(true);
        this.mSelectedCirclePaint.setAntiAlias(true);
        this.mSelectedCirclePaint.setColor(this.mTodayNumberColor);
        this.mSelectedCirclePaint.setTextAlign(Align.CENTER);
        this.mSelectedCirclePaint.setStyle(Style.FILL);
        this.mSelectedCirclePaint.setAlpha(SELECTED_CIRCLE_ALPHA);
        this.mMonthDayLabelPaint = new Paint();
        this.mMonthDayLabelPaint.setAntiAlias(true);
        this.mMonthDayLabelPaint.setTextSize((float) MONTH_DAY_LABEL_TEXT_SIZE);
        this.mMonthDayLabelPaint.setColor(this.mDayTextColor);
        this.mMonthDayLabelPaint.setTypeface(Typeface.create(this.mDayOfWeekTypeface, DEFAULT_SHOW_WK_NUM));
        this.mMonthDayLabelPaint.setStyle(Style.FILL);
        this.mMonthDayLabelPaint.setTextAlign(Align.CENTER);
        this.mMonthDayLabelPaint.setFakeBoldText(true);
        this.mMonthNumPaint = new Paint();
        this.mMonthNumPaint.setAntiAlias(true);
        this.mMonthNumPaint.setTextSize((float) MINI_DAY_NUMBER_TEXT_SIZE);
        this.mMonthNumPaint.setStyle(Style.FILL);
        this.mMonthNumPaint.setTextAlign(Align.CENTER);
        this.mMonthNumPaint.setFakeBoldText(false);
    }

    protected void onDraw(Canvas canvas) {
        drawMonthTitle(canvas);
        drawMonthDayLabels(canvas);
        drawMonthNums(canvas);
    }

    public void setMonthParams(HashMap<String, Integer> params) {
        if (params.containsKey(VIEW_PARAMS_MONTH) || params.containsKey(VIEW_PARAMS_YEAR)) {
            setTag(params);
            if (params.containsKey(VIEW_PARAMS_HEIGHT)) {
                this.mRowHeight = ((Integer) params.get(VIEW_PARAMS_HEIGHT)).intValue();
                if (this.mRowHeight < MIN_HEIGHT) {
                    this.mRowHeight = MIN_HEIGHT;
                }
            }
            if (params.containsKey(VIEW_PARAMS_SELECTED_DAY)) {
                this.mSelectedDay = ((Integer) params.get(VIEW_PARAMS_SELECTED_DAY)).intValue();
            }
            this.mMonth = ((Integer) params.get(VIEW_PARAMS_MONTH)).intValue();
            this.mYear = ((Integer) params.get(VIEW_PARAMS_YEAR)).intValue();
            Time today = new Time(Time.getCurrentTimezone());
            today.setToNow();
            this.mHasToday = false;
            this.mToday = DEFAULT_SELECTED_DAY;
            this.mCalendar.set(2, this.mMonth);
            this.mCalendar.set(DEFAULT_WEEK_START, this.mYear);
            this.mCalendar.set(5, DEFAULT_WEEK_START);
            this.mDayOfWeekStart = this.mCalendar.get(DEFAULT_NUM_DAYS);
            if (params.containsKey(VIEW_PARAMS_WEEK_START)) {
                this.mWeekStart = ((Integer) params.get(VIEW_PARAMS_WEEK_START)).intValue();
            } else {
                this.mWeekStart = this.mCalendar.getFirstDayOfWeek();
            }
            this.mNumCells = Utils.getDaysInMonth(this.mMonth, this.mYear);
            for (int i = DEFAULT_SHOW_WK_NUM; i < this.mNumCells; i += DEFAULT_WEEK_START) {
                int day = i + DEFAULT_WEEK_START;
                if (sameDay(day, today)) {
                    this.mHasToday = true;
                    this.mToday = day;
                }
            }
            this.mNumRows = calculateNumRows();
            this.mTouchHelper.invalidateRoot();
            return;
        }
        throw new InvalidParameterException("You must specify month and year for this view");
    }

    public void setSelectedDay(int day) {
        this.mSelectedDay = day;
    }

    public void reuse() {
        this.mNumRows = MAX_NUM_ROWS;
        requestLayout();
    }

    private int calculateNumRows() {
        int offset = findDayOffset();
        return ((this.mNumCells + offset) % this.mNumDays > 0 ? DEFAULT_WEEK_START : DEFAULT_SHOW_WK_NUM) + ((this.mNumCells + offset) / this.mNumDays);
    }

    private boolean sameDay(int day, Time today) {
        return this.mYear == today.year && this.mMonth == today.month && day == today.monthDay;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (this.mRowHeight * this.mNumRows) + getMonthHeaderSize());
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mWidth = w;
        this.mTouchHelper.invalidateRoot();
    }

    public int getMonth() {
        return this.mMonth;
    }

    public int getYear() {
        return this.mYear;
    }

    protected int getMonthHeaderSize() {
        return MONTH_HEADER_SIZE;
    }

    private String getMonthAndYearString() {
        this.mStringBuilder.setLength(DEFAULT_SHOW_WK_NUM);
        long millis = this.mCalendar.getTimeInMillis();
        return DateUtils.formatDateRange(getContext(), this.mFormatter, millis, millis, 52, Time.getCurrentTimezone()).toString();
    }

    protected void drawMonthTitle(Canvas canvas) {
        canvas.drawText(getMonthAndYearString(), (float) ((this.mWidth + (this.mEdgePadding * 2)) / 2), (float) (((getMonthHeaderSize() - MONTH_DAY_LABEL_TEXT_SIZE) / 2) + (MONTH_LABEL_TEXT_SIZE / 3)), this.mMonthTitlePaint);
    }

    protected void drawMonthDayLabels(Canvas canvas) {
        int y = getMonthHeaderSize() - (MONTH_DAY_LABEL_TEXT_SIZE / 2);
        int dayWidthHalf = (this.mWidth - (this.mEdgePadding * 2)) / (this.mNumDays * 2);
        for (int i = DEFAULT_SHOW_WK_NUM; i < this.mNumDays; i += DEFAULT_WEEK_START) {
            int x = (((i * 2) + DEFAULT_WEEK_START) * dayWidthHalf) + this.mEdgePadding;
            this.mDayLabelCalendar.set(DEFAULT_NUM_DAYS, (this.mWeekStart + i) % this.mNumDays);
            canvas.drawText(this.mDayLabelCalendar.getDisplayName(DEFAULT_NUM_DAYS, DEFAULT_WEEK_START, Locale.getDefault()).toUpperCase(Locale.getDefault()), (float) x, (float) y, this.mMonthDayLabelPaint);
        }
    }

    protected void drawMonthNums(Canvas canvas) {
        int y = (((this.mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2) - DAY_SEPARATOR_WIDTH) + getMonthHeaderSize();
        float dayWidthHalf = ((float) (this.mWidth - (this.mEdgePadding * 2))) / (((float) this.mNumDays) * 2.0f);
        int j = findDayOffset();
        for (int dayNumber = DEFAULT_WEEK_START; dayNumber <= this.mNumCells; dayNumber += DEFAULT_WEEK_START) {
            int x = (int) ((((float) ((j * 2) + DEFAULT_WEEK_START)) * dayWidthHalf) + ((float) this.mEdgePadding));
            int startY = y - (((this.mRowHeight + MINI_DAY_NUMBER_TEXT_SIZE) / 2) - DAY_SEPARATOR_WIDTH);
            Canvas canvas2 = canvas;
            drawMonthDay(canvas2, this.mYear, this.mMonth, dayNumber, x, y, (int) (((float) x) - dayWidthHalf), (int) (((float) x) + dayWidthHalf), startY, startY + this.mRowHeight);
            j += DEFAULT_WEEK_START;
            if (j == this.mNumDays) {
                j = DEFAULT_SHOW_WK_NUM;
                y += this.mRowHeight;
            }
        }
    }

    protected int findDayOffset() {
        return (this.mDayOfWeekStart < this.mWeekStart ? this.mDayOfWeekStart + this.mNumDays : this.mDayOfWeekStart) - this.mWeekStart;
    }

    public int getDayFromLocation(float x, float y) {
        int day = getInternalDayFromLocation(x, y);
        if (day < DEFAULT_WEEK_START || day > this.mNumCells) {
            return DEFAULT_SELECTED_DAY;
        }
        return day;
    }

    protected int getInternalDayFromLocation(float x, float y) {
        int dayStart = this.mEdgePadding;
        if (x < ((float) dayStart) || x > ((float) (this.mWidth - this.mEdgePadding))) {
            return DEFAULT_SELECTED_DAY;
        }
        return ((((int) (((x - ((float) dayStart)) * ((float) this.mNumDays)) / ((float) ((this.mWidth - dayStart) - this.mEdgePadding)))) - findDayOffset()) + DEFAULT_WEEK_START) + (this.mNumDays * (((int) (y - ((float) getMonthHeaderSize()))) / this.mRowHeight));
    }

    private void onDayClick(int day) {
        if (!isOutOfRange(this.mYear, this.mMonth, day)) {
            if (this.mOnDayClickListener != null) {
                this.mOnDayClickListener.onDayClick(this, new CalendarDay(this.mYear, this.mMonth, day));
            }
            this.mTouchHelper.sendEventForVirtualView(day, DEFAULT_WEEK_START);
        }
    }

    protected boolean isOutOfRange(int year, int month, int day) {
        if (isBeforeMin(year, month, day) || isAfterMax(year, month, day)) {
            return true;
        }
        return false;
    }

    private boolean isBeforeMin(int year, int month, int day) {
        if (this.mController == null) {
            return false;
        }
        Calendar minDate = this.mController.getMinDate();
        if (minDate == null) {
            return false;
        }
        if (year < minDate.get(DEFAULT_WEEK_START)) {
            return true;
        }
        if (year > minDate.get(DEFAULT_WEEK_START)) {
            return false;
        }
        if (month < minDate.get(2)) {
            return true;
        }
        if (month > minDate.get(2) || day >= minDate.get(5)) {
            return false;
        }
        return true;
    }

    private boolean isAfterMax(int year, int month, int day) {
        if (this.mController == null) {
            return false;
        }
        Calendar maxDate = this.mController.getMaxDate();
        if (maxDate == null) {
            return false;
        }
        if (year > maxDate.get(DEFAULT_WEEK_START)) {
            return true;
        }
        if (year < maxDate.get(DEFAULT_WEEK_START)) {
            return false;
        }
        if (month > maxDate.get(2)) {
            return true;
        }
        if (month < maxDate.get(2) || day <= maxDate.get(5)) {
            return false;
        }
        return true;
    }

    public CalendarDay getAccessibilityFocus() {
        int day = this.mTouchHelper.getFocusedVirtualView();
        if (day >= 0) {
            return new CalendarDay(this.mYear, this.mMonth, day);
        }
        return null;
    }

    public void clearAccessibilityFocus() {
        this.mTouchHelper.clearFocusedVirtualView();
    }

    public boolean restoreAccessibilityFocus(CalendarDay day) {
        if (day.year != this.mYear || day.month != this.mMonth || day.day > this.mNumCells) {
            return false;
        }
        this.mTouchHelper.setFocusedVirtualView(day.day);
        return true;
    }
}
