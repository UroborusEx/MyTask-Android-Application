package com.android.datetimepicker.date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.widget.RecyclerView.ItemAnimator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AbsListView;
import android.widget.AbsListView.LayoutParams;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import com.android.datetimepicker.Utils;
import com.android.datetimepicker.date.DatePickerDialog.OnDateChangedListener;
import com.android.datetimepicker.date.MonthAdapter.CalendarDay;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public abstract class DayPickerView extends ListView implements OnScrollListener, OnDateChangedListener {
    public static final int DAYS_PER_WEEK = 7;
    protected static final int GOTO_SCROLL_DURATION = 250;
    public static int LIST_TOP_OFFSET = 0;
    protected static final int SCROLL_CHANGE_DELAY = 40;
    protected static final int SCROLL_HYST_WEEKS = 2;
    private static final String TAG = "MonthFragment";
    private static SimpleDateFormat YEAR_FORMAT;
    protected MonthAdapter mAdapter;
    protected Context mContext;
    private DatePickerController mController;
    protected int mCurrentMonthDisplayed;
    protected int mCurrentScrollState;
    protected int mDaysPerWeek;
    protected int mFirstDayOfWeek;
    protected float mFriction;
    protected Handler mHandler;
    protected int mNumWeeks;
    private boolean mPerformingScroll;
    protected CharSequence mPrevMonthName;
    protected long mPreviousScrollPosition;
    protected int mPreviousScrollState;
    protected ScrollStateRunnable mScrollStateChangedRunnable;
    protected CalendarDay mSelectedDay;
    protected boolean mShowWeekNumber;
    protected CalendarDay mTempDay;

    /* renamed from: com.android.datetimepicker.date.DayPickerView.1 */
    class C02081 implements Runnable {
        final /* synthetic */ int val$position;

        C02081(int i) {
            this.val$position = i;
        }

        public void run() {
            DayPickerView.this.setSelection(this.val$position);
        }
    }

    protected class ScrollStateRunnable implements Runnable {
        private int mNewState;

        protected ScrollStateRunnable() {
        }

        public void doScrollStateChange(AbsListView view, int scrollState) {
            DayPickerView.this.mHandler.removeCallbacks(this);
            this.mNewState = scrollState;
            DayPickerView.this.mHandler.postDelayed(this, 40);
        }

        public void run() {
            boolean scroll = true;
            DayPickerView.this.mCurrentScrollState = this.mNewState;
            if (Log.isLoggable(DayPickerView.TAG, 3)) {
                Log.d(DayPickerView.TAG, "new scroll state: " + this.mNewState + " old state: " + DayPickerView.this.mPreviousScrollState);
            }
            if (this.mNewState != 0 || DayPickerView.this.mPreviousScrollState == 0 || DayPickerView.this.mPreviousScrollState == 1) {
                DayPickerView.this.mPreviousScrollState = this.mNewState;
                return;
            }
            DayPickerView.this.mPreviousScrollState = this.mNewState;
            int i = 0;
            View child = DayPickerView.this.getChildAt(0);
            while (child != null && child.getBottom() <= 0) {
                i++;
                child = DayPickerView.this.getChildAt(i);
            }
            if (child != null) {
                int firstPosition = DayPickerView.this.getFirstVisiblePosition();
                int lastPosition = DayPickerView.this.getLastVisiblePosition();
                if (firstPosition == 0 || lastPosition == DayPickerView.this.getCount() - 1) {
                    scroll = false;
                }
                int top = child.getTop();
                int bottom = child.getBottom();
                int midpoint = DayPickerView.this.getHeight() / DayPickerView.SCROLL_HYST_WEEKS;
                if (scroll && top < DayPickerView.LIST_TOP_OFFSET) {
                    if (bottom > midpoint) {
                        DayPickerView.this.smoothScrollBy(top, DayPickerView.GOTO_SCROLL_DURATION);
                    } else {
                        DayPickerView.this.smoothScrollBy(bottom, DayPickerView.GOTO_SCROLL_DURATION);
                    }
                }
            }
        }
    }

    public abstract MonthAdapter createMonthAdapter(Context context, DatePickerController datePickerController);

    static {
        LIST_TOP_OFFSET = -1;
        YEAR_FORMAT = new SimpleDateFormat("yyyy", Locale.getDefault());
    }

    public DayPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mNumWeeks = 6;
        this.mShowWeekNumber = false;
        this.mDaysPerWeek = DAYS_PER_WEEK;
        this.mFriction = 1.0f;
        this.mSelectedDay = new CalendarDay();
        this.mTempDay = new CalendarDay();
        this.mPreviousScrollState = 0;
        this.mCurrentScrollState = 0;
        this.mScrollStateChangedRunnable = new ScrollStateRunnable();
        init(context);
    }

    public DayPickerView(Context context, DatePickerController controller) {
        super(context);
        this.mNumWeeks = 6;
        this.mShowWeekNumber = false;
        this.mDaysPerWeek = DAYS_PER_WEEK;
        this.mFriction = 1.0f;
        this.mSelectedDay = new CalendarDay();
        this.mTempDay = new CalendarDay();
        this.mPreviousScrollState = 0;
        this.mCurrentScrollState = 0;
        this.mScrollStateChangedRunnable = new ScrollStateRunnable();
        init(context);
        setController(controller);
    }

    public void setController(DatePickerController controller) {
        this.mController = controller;
        this.mController.registerOnDateChangedListener(this);
        refreshAdapter();
        onDateChanged();
    }

    public void init(Context context) {
        this.mHandler = new Handler();
        setLayoutParams(new LayoutParams(-1, -1));
        setDrawSelectorOnTop(false);
        this.mContext = context;
        setUpListView();
    }

    public void onChange() {
        refreshAdapter();
    }

    protected void refreshAdapter() {
        if (this.mAdapter == null) {
            this.mAdapter = createMonthAdapter(getContext(), this.mController);
        } else {
            this.mAdapter.setSelectedDay(this.mSelectedDay);
        }
        setAdapter(this.mAdapter);
    }

    protected void setUpListView() {
        setCacheColorHint(0);
        setDivider(null);
        setItemsCanFocus(true);
        setFastScrollEnabled(false);
        setVerticalScrollBarEnabled(false);
        setOnScrollListener(this);
        setFadingEdgeLength(0);
        setFriction(ViewConfiguration.getScrollFriction() * this.mFriction);
    }

    public boolean goTo(CalendarDay day, boolean animate, boolean setSelected, boolean forceScroll) {
        int selectedPosition;
        if (setSelected) {
            this.mSelectedDay.set(day);
        }
        this.mTempDay.set(day);
        int position = ((day.year - this.mController.getMinYear()) * 12) + day.month;
        int i = 0;
        while (true) {
            int i2 = i + 1;
            View child = getChildAt(i);
            if (child != null) {
                int top = child.getTop();
                if (Log.isLoggable(TAG, 3)) {
                    Log.d(TAG, "child at " + (i2 - 1) + " has top " + top);
                }
                if (top >= 0) {
                    break;
                }
                i = i2;
            } else {
                break;
            }
        }
        if (child != null) {
            selectedPosition = getPositionForView(child);
        } else {
            selectedPosition = 0;
        }
        if (setSelected) {
            this.mAdapter.setSelectedDay(this.mSelectedDay);
        }
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "GoTo position " + position);
        }
        if (position != selectedPosition || forceScroll) {
            setMonthDisplayed(this.mTempDay);
            this.mPreviousScrollState = SCROLL_HYST_WEEKS;
            if (animate) {
                smoothScrollToPositionFromTop(position, LIST_TOP_OFFSET, GOTO_SCROLL_DURATION);
                return true;
            }
            postSetSelection(position);
        } else if (setSelected) {
            setMonthDisplayed(this.mSelectedDay);
        }
        return false;
    }

    public void postSetSelection(int position) {
        clearFocus();
        post(new C02081(position));
        onScrollStateChanged(this, 0);
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        MonthView child = (MonthView) view.getChildAt(0);
        if (child != null) {
            this.mPreviousScrollPosition = (long) ((view.getFirstVisiblePosition() * child.getHeight()) - child.getBottom());
            this.mPreviousScrollState = this.mCurrentScrollState;
        }
    }

    protected void setMonthDisplayed(CalendarDay date) {
        this.mCurrentMonthDisplayed = date.month;
        invalidateViews();
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        this.mScrollStateChangedRunnable.doScrollStateChange(view, scrollState);
    }

    public int getMostVisiblePosition() {
        int firstPosition = getFirstVisiblePosition();
        int height = getHeight();
        int maxDisplayedHeight = 0;
        int mostVisibleIndex = 0;
        int i = 0;
        int bottom = 0;
        while (bottom < height) {
            View child = getChildAt(i);
            if (child == null) {
                break;
            }
            bottom = child.getBottom();
            int displayedHeight = Math.min(bottom, height) - Math.max(0, child.getTop());
            if (displayedHeight > maxDisplayedHeight) {
                mostVisibleIndex = i;
                maxDisplayedHeight = displayedHeight;
            }
            i++;
        }
        return firstPosition + mostVisibleIndex;
    }

    public void onDateChanged() {
        goTo(this.mController.getSelectedDay(), false, true, true);
    }

    private CalendarDay findAccessibilityFocus() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child instanceof MonthView) {
                CalendarDay focus = ((MonthView) child).getAccessibilityFocus();
                if (focus != null) {
                    if (VERSION.SDK_INT != 17) {
                        return focus;
                    }
                    ((MonthView) child).clearAccessibilityFocus();
                    return focus;
                }
            }
        }
        return null;
    }

    private boolean restoreAccessibilityFocus(CalendarDay day) {
        if (day == null) {
            return false;
        }
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if ((child instanceof MonthView) && ((MonthView) child).restoreAccessibilityFocus(day)) {
                return true;
            }
        }
        return false;
    }

    protected void layoutChildren() {
        CalendarDay focusedDay = findAccessibilityFocus();
        super.layoutChildren();
        if (this.mPerformingScroll) {
            this.mPerformingScroll = false;
        } else {
            restoreAccessibilityFocus(focusedDay);
        }
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setItemCount(-1);
    }

    private static String getMonthAndYearString(CalendarDay day) {
        Calendar cal = Calendar.getInstance();
        cal.set(day.year, day.month, day.day);
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(cal.getDisplayName(SCROLL_HYST_WEEKS, SCROLL_HYST_WEEKS, Locale.getDefault()));
        sbuf.append(" ");
        sbuf.append(YEAR_FORMAT.format(cal.getTime()));
        return sbuf.toString();
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.addAction(ItemAnimator.FLAG_APPEARED_IN_PRE_LAYOUT);
        info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
    }

    @SuppressLint({"NewApi"})
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action != ItemAnimator.FLAG_APPEARED_IN_PRE_LAYOUT && action != AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) {
            return super.performAccessibilityAction(action, arguments);
        }
        int firstVisiblePosition = getFirstVisiblePosition();
        CalendarDay day = new CalendarDay((firstVisiblePosition / 12) + this.mController.getMinYear(), firstVisiblePosition % 12, 1);
        if (action == ItemAnimator.FLAG_APPEARED_IN_PRE_LAYOUT) {
            day.month++;
            if (day.month == 12) {
                day.month = 0;
                day.year++;
            }
        } else if (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) {
            View firstVisibleView = getChildAt(0);
            if (firstVisibleView != null && firstVisibleView.getTop() >= -1) {
                day.month--;
                if (day.month == -1) {
                    day.month = 11;
                    day.year--;
                }
            }
        }
        Utils.tryAccessibilityAnnounce(this, getMonthAndYearString(day));
        goTo(day, true, false, true);
        this.mPerformingScroll = true;
        return true;
    }
}
