package com.android.datetimepicker.date;

import android.content.Context;
import android.graphics.Canvas;

public class SimpleMonthView extends MonthView {
    public SimpleMonthView(Context context) {
        super(context);
    }

    public void drawMonthDay(Canvas canvas, int year, int month, int day, int x, int y, int startX, int stopX, int startY, int stopY) {
        if (this.mSelectedDay == day) {
            canvas.drawCircle((float) x, (float) (y - (MINI_DAY_NUMBER_TEXT_SIZE / 3)), (float) DAY_SELECTED_CIRCLE_SIZE, this.mSelectedCirclePaint);
        }
        if (isOutOfRange(year, month, day)) {
            this.mMonthNumPaint.setColor(this.mDisabledDayTextColor);
        } else if (this.mHasToday && this.mToday == day) {
            this.mMonthNumPaint.setColor(this.mTodayNumberColor);
        } else {
            this.mMonthNumPaint.setColor(this.mDayTextColor);
        }
        canvas.drawText(String.format("%d", new Object[]{Integer.valueOf(day)}), (float) x, (float) y, this.mMonthNumPaint);
    }
}
