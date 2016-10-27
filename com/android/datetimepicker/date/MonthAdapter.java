package com.android.datetimepicker.date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.Time;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;
import com.android.datetimepicker.date.MonthView.OnDayClickListener;
import java.util.Calendar;
import java.util.HashMap;

public abstract class MonthAdapter extends BaseAdapter implements OnDayClickListener {
    protected static final int MONTHS_IN_YEAR = 12;
    private static final String TAG = "SimpleMonthAdapter";
    protected static int WEEK_7_OVERHANG_HEIGHT;
    private final Context mContext;
    protected final DatePickerController mController;
    private CalendarDay mSelectedDay;

    public static class CalendarDay {
        private Calendar calendar;
        int day;
        int month;
        private Time time;
        int year;

        public CalendarDay() {
            setTime(System.currentTimeMillis());
        }

        public CalendarDay(long timeInMillis) {
            setTime(timeInMillis);
        }

        public CalendarDay(Calendar calendar) {
            this.year = calendar.get(1);
            this.month = calendar.get(2);
            this.day = calendar.get(5);
        }

        public CalendarDay(int year, int month, int day) {
            setDay(year, month, day);
        }

        public void set(CalendarDay date) {
            this.year = date.year;
            this.month = date.month;
            this.day = date.day;
        }

        public void setDay(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        public synchronized void setJulianDay(int julianDay) {
            if (this.time == null) {
                this.time = new Time();
            }
            this.time.setJulianDay(julianDay);
            setTime(this.time.toMillis(false));
        }

        private void setTime(long timeInMillis) {
            if (this.calendar == null) {
                this.calendar = Calendar.getInstance();
            }
            this.calendar.setTimeInMillis(timeInMillis);
            this.month = this.calendar.get(2);
            this.year = this.calendar.get(1);
            this.day = this.calendar.get(5);
        }

        public int getYear() {
            return this.year;
        }

        public int getMonth() {
            return this.month;
        }

        public int getDay() {
            return this.day;
        }
    }

    public abstract MonthView createMonthView(Context context);

    static {
        WEEK_7_OVERHANG_HEIGHT = 7;
    }

    public MonthAdapter(Context context, DatePickerController controller) {
        this.mContext = context;
        this.mController = controller;
        init();
        setSelectedDay(this.mController.getSelectedDay());
    }

    public void setSelectedDay(CalendarDay day) {
        this.mSelectedDay = day;
        notifyDataSetChanged();
    }

    public CalendarDay getSelectedDay() {
        return this.mSelectedDay;
    }

    protected void init() {
        this.mSelectedDay = new CalendarDay(System.currentTimeMillis());
    }

    public int getCount() {
        return ((this.mController.getMaxYear() - this.mController.getMinYear()) + 1) * MONTHS_IN_YEAR;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public boolean hasStableIds() {
        return true;
    }

    @SuppressLint({"NewApi"})
    public View getView(int position, View convertView, ViewGroup parent) {
        MonthView v;
        HashMap<String, Integer> hashMap = null;
        if (convertView != null) {
            v = (MonthView) convertView;
            hashMap = (HashMap) v.getTag();
        } else {
            v = createMonthView(this.mContext);
            v.setLayoutParams(new LayoutParams(-1, -1));
            v.setClickable(true);
            v.setOnDayClickListener(this);
        }
        if (hashMap == null) {
            hashMap = new HashMap();
        }
        hashMap.clear();
        int month = position % MONTHS_IN_YEAR;
        int year = (position / MONTHS_IN_YEAR) + this.mController.getMinYear();
        int selectedDay = -1;
        if (isSelectedDayInMonth(year, month)) {
            selectedDay = this.mSelectedDay.day;
        }
        v.reuse();
        hashMap.put(MonthView.VIEW_PARAMS_SELECTED_DAY, Integer.valueOf(selectedDay));
        hashMap.put(MonthView.VIEW_PARAMS_YEAR, Integer.valueOf(year));
        hashMap.put(MonthView.VIEW_PARAMS_MONTH, Integer.valueOf(month));
        hashMap.put(MonthView.VIEW_PARAMS_WEEK_START, Integer.valueOf(this.mController.getFirstDayOfWeek()));
        v.setMonthParams(hashMap);
        v.invalidate();
        return v;
    }

    private boolean isSelectedDayInMonth(int year, int month) {
        return this.mSelectedDay.year == year && this.mSelectedDay.month == month;
    }

    public void onDayClick(MonthView view, CalendarDay day) {
        if (day != null) {
            onDayTapped(day);
        }
    }

    protected void onDayTapped(CalendarDay day) {
        this.mController.tryVibrate();
        this.mController.onDayOfMonthSelected(day.year, day.month, day.day);
        setSelectedDay(day);
    }
}
