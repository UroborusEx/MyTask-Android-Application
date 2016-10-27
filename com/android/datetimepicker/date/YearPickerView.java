package com.android.datetimepicker.date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.StateListDrawable;
import android.support.v7.widget.RecyclerView.ItemAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.datetimepicker.C0206R;
import com.android.datetimepicker.date.DatePickerDialog.OnDateChangedListener;
import java.util.ArrayList;
import java.util.List;

public class YearPickerView extends ListView implements OnItemClickListener, OnDateChangedListener {
    private static final String TAG = "YearPickerView";
    private YearAdapter mAdapter;
    private int mChildSize;
    private final DatePickerController mController;
    private TextViewWithCircularIndicator mSelectedView;
    private int mViewSize;

    /* renamed from: com.android.datetimepicker.date.YearPickerView.1 */
    class C02091 implements Runnable {
        final /* synthetic */ int val$offset;
        final /* synthetic */ int val$position;

        C02091(int i, int i2) {
            this.val$position = i;
            this.val$offset = i2;
        }

        public void run() {
            YearPickerView.this.setSelectionFromTop(this.val$position, this.val$offset);
            YearPickerView.this.requestLayout();
        }
    }

    private class YearAdapter extends ArrayAdapter<String> {
        public YearAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextViewWithCircularIndicator v = (TextViewWithCircularIndicator) super.getView(position, convertView, parent);
            v.requestLayout();
            boolean selected = YearPickerView.this.mController.getSelectedDay().year == YearPickerView.getYearFromTextView(v);
            v.drawIndicator(selected);
            if (selected) {
                YearPickerView.this.mSelectedView = v;
            }
            return v;
        }
    }

    public YearPickerView(Context context, DatePickerController controller) {
        super(context);
        this.mController = controller;
        this.mController.registerOnDateChangedListener(this);
        setLayoutParams(new LayoutParams(-1, -2));
        Resources res = context.getResources();
        this.mViewSize = res.getDimensionPixelOffset(C0206R.dimen.date_picker_view_animator_height);
        this.mChildSize = res.getDimensionPixelOffset(C0206R.dimen.year_label_height);
        setVerticalFadingEdgeEnabled(false);
        setFadingEdgeLength(this.mChildSize / 3);
        init(context);
        setOnItemClickListener(this);
        setSelector(new StateListDrawable());
        setDividerHeight(0);
        onDateChanged();
    }

    private void init(Context context) {
        ArrayList<String> years = new ArrayList();
        for (int year = this.mController.getMinYear(); year <= this.mController.getMaxYear(); year++) {
            years.add(String.format("%d", new Object[]{Integer.valueOf(year)}));
        }
        this.mAdapter = new YearAdapter(context, C0206R.layout.year_label_text_view, years);
        setAdapter(this.mAdapter);
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        this.mController.tryVibrate();
        TextViewWithCircularIndicator clickedView = (TextViewWithCircularIndicator) view;
        if (clickedView != null) {
            if (clickedView != this.mSelectedView) {
                if (this.mSelectedView != null) {
                    this.mSelectedView.drawIndicator(false);
                    this.mSelectedView.requestLayout();
                }
                clickedView.drawIndicator(true);
                clickedView.requestLayout();
                this.mSelectedView = clickedView;
            }
            this.mController.onYearSelected(getYearFromTextView(clickedView));
            this.mAdapter.notifyDataSetChanged();
        }
    }

    private static int getYearFromTextView(TextView view) {
        return Integer.valueOf(view.getText().toString()).intValue();
    }

    public void postSetSelectionCentered(int position) {
        postSetSelectionFromTop(position, (this.mViewSize / 2) - (this.mChildSize / 2));
    }

    public void postSetSelectionFromTop(int position, int offset) {
        post(new C02091(position, offset));
    }

    public int getFirstPositionOffset() {
        View firstChild = getChildAt(0);
        if (firstChild == null) {
            return 0;
        }
        return firstChild.getTop();
    }

    public void onDateChanged() {
        this.mAdapter.notifyDataSetChanged();
        postSetSelectionCentered(this.mController.getSelectedDay().year - this.mController.getMinYear());
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (event.getEventType() == ItemAnimator.FLAG_APPEARED_IN_PRE_LAYOUT) {
            event.setFromIndex(0);
            event.setToIndex(0);
        }
    }
}
