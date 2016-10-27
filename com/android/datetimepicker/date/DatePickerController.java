package com.android.datetimepicker.date;

import com.android.datetimepicker.date.DatePickerDialog.OnDateChangedListener;
import com.android.datetimepicker.date.MonthAdapter.CalendarDay;
import java.util.Calendar;

public interface DatePickerController {
    int getFirstDayOfWeek();

    Calendar getMaxDate();

    int getMaxYear();

    Calendar getMinDate();

    int getMinYear();

    CalendarDay getSelectedDay();

    void onDayOfMonthSelected(int i, int i2, int i3);

    void onYearSelected(int i);

    void registerOnDateChangedListener(OnDateChangedListener onDateChangedListener);

    void tryVibrate();

    void unregisterOnDateChangedListener(OnDateChangedListener onDateChangedListener);
}
