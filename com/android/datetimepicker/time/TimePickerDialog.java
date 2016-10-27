package com.android.datetimepicker.time;

import android.animation.ObjectAnimator;
import android.app.DialogFragment;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.datetimepicker.C0206R;
import com.android.datetimepicker.HapticFeedbackController;
import com.android.datetimepicker.Utils;
import com.android.datetimepicker.date.DayPickerView;
import com.android.datetimepicker.time.RadialPickerLayout.OnValueSelectedListener;
import com.snowmobile.tasks.C0220R;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class TimePickerDialog extends DialogFragment implements OnValueSelectedListener {
    public static final int AM = 0;
    public static final int AMPM_INDEX = 2;
    public static final int ENABLE_PICKER_INDEX = 3;
    public static final int HOUR_INDEX = 0;
    private static final String KEY_CURRENT_ITEM_SHOWING = "current_item_showing";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_HOUR_OF_DAY = "hour_of_day";
    private static final String KEY_IN_KB_MODE = "in_kb_mode";
    private static final String KEY_IS_24_HOUR_VIEW = "is_24_hour_view";
    private static final String KEY_MINUTE = "minute";
    private static final String KEY_TYPED_TIMES = "typed_times";
    public static final int MINUTE_INDEX = 1;
    public static final int PM = 1;
    private static final int PULSE_ANIMATOR_DELAY = 300;
    private static final String TAG = "TimePickerDialog";
    private boolean mAllowAutoAdvance;
    private int mAmKeyCode;
    private View mAmPmHitspace;
    private TextView mAmPmTextView;
    private String mAmText;
    private OnTimeSetListener mCallback;
    private String mDeletedKeyFormat;
    private TextView mDoneButton;
    private String mDoublePlaceholderText;
    private HapticFeedbackController mHapticFeedbackController;
    private String mHourPickerDescription;
    private TextView mHourSpaceView;
    private TextView mHourView;
    private boolean mInKbMode;
    private int mInitialHourOfDay;
    private int mInitialMinute;
    private boolean mIs24HourMode;
    private Node mLegalTimesTree;
    private int mMaxHour;
    private int mMaxMinute;
    private int mMinHour;
    private int mMinMinute;
    private String mMinutePickerDescription;
    private TextView mMinuteSpaceView;
    private TextView mMinuteView;
    private char mPlaceholderText;
    private int mPmKeyCode;
    private String mPmText;
    private String mSelectHours;
    private String mSelectMinutes;
    private int mSelectedColor;
    private boolean mThemeDark;
    private RadialPickerLayout mTimePicker;
    private ArrayList<Integer> mTypedTimes;
    private int mUnselectedColor;

    /* renamed from: com.android.datetimepicker.time.TimePickerDialog.1 */
    class C02141 implements OnClickListener {
        C02141() {
        }

        public void onClick(View v) {
            TimePickerDialog.this.setCurrentItemShowing(TimePickerDialog.HOUR_INDEX, true, false, true);
            TimePickerDialog.this.tryVibrate();
        }
    }

    /* renamed from: com.android.datetimepicker.time.TimePickerDialog.2 */
    class C02152 implements OnClickListener {
        C02152() {
        }

        public void onClick(View v) {
            TimePickerDialog.this.setCurrentItemShowing(TimePickerDialog.PM, true, false, true);
            TimePickerDialog.this.tryVibrate();
        }
    }

    /* renamed from: com.android.datetimepicker.time.TimePickerDialog.3 */
    class C02163 implements OnClickListener {
        C02163() {
        }

        public void onClick(View v) {
            if (TimePickerDialog.this.mInKbMode && TimePickerDialog.this.isTypedTimeFullyLegal()) {
                TimePickerDialog.this.finishKbMode(false);
            } else {
                TimePickerDialog.this.tryVibrate();
            }
            if (TimePickerDialog.this.mCallback != null) {
                TimePickerDialog.this.mCallback.onTimeSet(TimePickerDialog.this.mTimePicker, TimePickerDialog.this.mTimePicker.getHours(), TimePickerDialog.this.mTimePicker.getMinutes());
            }
            TimePickerDialog.this.dismiss();
        }
    }

    /* renamed from: com.android.datetimepicker.time.TimePickerDialog.4 */
    class C02174 implements OnClickListener {
        C02174() {
        }

        public void onClick(View v) {
            TimePickerDialog.this.tryVibrate();
            int amOrPm = TimePickerDialog.this.mTimePicker.getIsCurrentlyAmOrPm();
            if (amOrPm == 0) {
                amOrPm = TimePickerDialog.PM;
            } else if (amOrPm == TimePickerDialog.PM) {
                amOrPm = TimePickerDialog.HOUR_INDEX;
            }
            TimePickerDialog.this.updateAmPmDisplay(amOrPm);
            TimePickerDialog.this.mTimePicker.setAmOrPm(amOrPm);
        }
    }

    private class KeyboardListener implements OnKeyListener {
        private KeyboardListener() {
        }

        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == TimePickerDialog.PM) {
                return TimePickerDialog.this.processKeyUp(keyCode);
            }
            return false;
        }
    }

    private class Node {
        private ArrayList<Node> mChildren;
        private int[] mLegalKeys;

        public Node(int... legalKeys) {
            this.mLegalKeys = legalKeys;
            this.mChildren = new ArrayList();
        }

        public void addChild(Node child) {
            this.mChildren.add(child);
        }

        public boolean containsKey(int key) {
            for (int i = TimePickerDialog.HOUR_INDEX; i < this.mLegalKeys.length; i += TimePickerDialog.PM) {
                if (this.mLegalKeys[i] == key) {
                    return true;
                }
            }
            return false;
        }

        public Node canReach(int key) {
            if (this.mChildren == null) {
                return null;
            }
            Iterator i$ = this.mChildren.iterator();
            while (i$.hasNext()) {
                Node child = (Node) i$.next();
                if (child.containsKey(key)) {
                    return child;
                }
            }
            return null;
        }
    }

    public interface OnTimeSetListener {
        void onTimeSet(RadialPickerLayout radialPickerLayout, int i, int i2);
    }

    public TimePickerDialog() {
        this.mMinHour = HOUR_INDEX;
        this.mMinMinute = HOUR_INDEX;
        this.mMaxHour = 23;
        this.mMaxMinute = 59;
    }

    public TimePickerDialog(Context context, int theme, OnTimeSetListener callback, int hourOfDay, int minute, boolean is24HourMode) {
        this.mMinHour = HOUR_INDEX;
        this.mMinMinute = HOUR_INDEX;
        this.mMaxHour = 23;
        this.mMaxMinute = 59;
    }

    public static TimePickerDialog newInstance(OnTimeSetListener callback, int hourOfDay, int minute, boolean is24HourMode) {
        TimePickerDialog ret = new TimePickerDialog();
        ret.initialize(callback, hourOfDay, minute, is24HourMode);
        return ret;
    }

    public void initialize(OnTimeSetListener callback, int hourOfDay, int minute, boolean is24HourMode) {
        this.mCallback = callback;
        this.mInitialHourOfDay = hourOfDay;
        this.mInitialMinute = minute;
        this.mIs24HourMode = is24HourMode;
        this.mInKbMode = false;
        this.mThemeDark = false;
    }

    public void setThemeDark(boolean dark) {
        this.mThemeDark = dark;
    }

    public boolean isThemeDark() {
        return this.mThemeDark;
    }

    public void setOnTimeSetListener(OnTimeSetListener callback) {
        this.mCallback = callback;
    }

    public void setStartTime(int hourOfDay, int minute) {
        this.mInitialHourOfDay = hourOfDay;
        this.mInitialMinute = minute;
        this.mInKbMode = false;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_HOUR_OF_DAY) && savedInstanceState.containsKey(KEY_MINUTE) && savedInstanceState.containsKey(KEY_IS_24_HOUR_VIEW)) {
            this.mInitialHourOfDay = savedInstanceState.getInt(KEY_HOUR_OF_DAY);
            this.mInitialMinute = savedInstanceState.getInt(KEY_MINUTE);
            this.mIs24HourMode = savedInstanceState.getBoolean(KEY_IS_24_HOUR_VIEW);
            this.mInKbMode = savedInstanceState.getBoolean(KEY_IN_KB_MODE);
            this.mThemeDark = savedInstanceState.getBoolean(KEY_DARK_THEME);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int i;
        int i2;
        getDialog().getWindow().requestFeature(PM);
        View view = inflater.inflate(C0206R.layout.time_picker_dialog, null);
        TimePickerDialog timePickerDialog = this;
        KeyboardListener keyboardListener = new KeyboardListener();
        view.findViewById(C0206R.id.time_picker_dialog).setOnKeyListener(keyboardListener);
        Resources res = getResources();
        this.mHourPickerDescription = res.getString(C0206R.string.hour_picker_description);
        this.mSelectHours = res.getString(C0206R.string.select_hours);
        this.mMinutePickerDescription = res.getString(C0206R.string.minute_picker_description);
        this.mSelectMinutes = res.getString(C0206R.string.select_minutes);
        this.mSelectedColor = res.getColor(this.mThemeDark ? C0206R.color.red : C0206R.color.blue);
        this.mUnselectedColor = res.getColor(this.mThemeDark ? 17170443 : C0206R.color.numbers_text_color);
        this.mHourView = (TextView) view.findViewById(C0206R.id.hours);
        this.mHourView.setOnKeyListener(keyboardListener);
        this.mHourSpaceView = (TextView) view.findViewById(C0206R.id.hour_space);
        this.mMinuteSpaceView = (TextView) view.findViewById(C0206R.id.minutes_space);
        this.mMinuteView = (TextView) view.findViewById(C0206R.id.minutes);
        this.mMinuteView.setOnKeyListener(keyboardListener);
        this.mAmPmTextView = (TextView) view.findViewById(C0206R.id.ampm_label);
        this.mAmPmTextView.setOnKeyListener(keyboardListener);
        String[] amPmTexts = new DateFormatSymbols().getAmPmStrings();
        this.mAmText = amPmTexts[HOUR_INDEX];
        this.mPmText = amPmTexts[PM];
        this.mHapticFeedbackController = new HapticFeedbackController(getActivity());
        this.mTimePicker = (RadialPickerLayout) view.findViewById(C0206R.id.time_picker);
        this.mTimePicker.setOnValueSelectedListener(this);
        this.mTimePicker.setOnKeyListener(keyboardListener);
        this.mTimePicker.initialize(getActivity(), this.mHapticFeedbackController, this.mInitialHourOfDay, this.mInitialMinute, this.mIs24HourMode, this.mMinHour, this.mMaxHour, this.mMinMinute, this.mMaxMinute);
        int currentItemShowing = HOUR_INDEX;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_CURRENT_ITEM_SHOWING)) {
                currentItemShowing = savedInstanceState.getInt(KEY_CURRENT_ITEM_SHOWING);
            }
        }
        setCurrentItemShowing(currentItemShowing, false, true, true);
        this.mTimePicker.invalidate();
        this.mHourView.setOnClickListener(new C02141());
        this.mMinuteView.setOnClickListener(new C02152());
        this.mDoneButton = (TextView) view.findViewById(C0206R.id.done_button);
        this.mDoneButton.setOnClickListener(new C02163());
        this.mDoneButton.setOnKeyListener(keyboardListener);
        this.mAmPmHitspace = view.findViewById(C0206R.id.ampm_hitspace);
        if (this.mIs24HourMode) {
            this.mAmPmTextView.setVisibility(8);
            LayoutParams layoutParams = new RelativeLayout.LayoutParams(-2, -2);
            layoutParams.addRule(13);
            ((TextView) view.findViewById(C0206R.id.separator)).setLayoutParams(layoutParams);
        } else {
            this.mAmPmTextView.setVisibility(HOUR_INDEX);
            updateAmPmDisplay(this.mInitialHourOfDay < 12 ? HOUR_INDEX : PM);
            this.mAmPmHitspace.setOnClickListener(new C02174());
        }
        this.mAllowAutoAdvance = true;
        setHour(this.mInitialHourOfDay, true);
        setMinute(this.mInitialMinute);
        this.mDoublePlaceholderText = res.getString(C0206R.string.time_placeholder);
        this.mDeletedKeyFormat = res.getString(C0206R.string.deleted_key);
        this.mPlaceholderText = this.mDoublePlaceholderText.charAt(HOUR_INDEX);
        this.mPmKeyCode = -1;
        this.mAmKeyCode = -1;
        generateLegalTimesTree();
        if (this.mInKbMode) {
            this.mTypedTimes = savedInstanceState.getIntegerArrayList(KEY_TYPED_TIMES);
            tryStartingKbMode(-1);
            this.mHourView.invalidate();
        } else if (this.mTypedTimes == null) {
            this.mTypedTimes = new ArrayList();
        }
        this.mTimePicker.setTheme(getActivity().getApplicationContext(), this.mThemeDark);
        int white = res.getColor(17170443);
        int circleBackground = res.getColor(C0206R.color.circle_background);
        int line = res.getColor(C0206R.color.line_background);
        int timeDisplay = res.getColor(C0206R.color.numbers_text_color);
        ColorStateList doneTextColor = res.getColorStateList(C0206R.color.done_text_color);
        int doneBackground = C0206R.drawable.done_background_color;
        int darkGray = res.getColor(C0206R.color.dark_gray);
        int lightGray = res.getColor(C0206R.color.light_gray);
        int darkLine = res.getColor(C0206R.color.line_dark);
        ColorStateList darkDoneTextColor = res.getColorStateList(C0206R.color.done_text_color_dark);
        int darkDoneBackground = C0206R.drawable.done_background_color_dark;
        View findViewById = view.findViewById(C0206R.id.time_display_background);
        if (this.mThemeDark) {
            i = darkGray;
        } else {
            i = white;
        }
        findViewById.setBackgroundColor(i);
        View findViewById2 = view.findViewById(C0206R.id.time_display);
        if (!this.mThemeDark) {
            darkGray = white;
        }
        findViewById2.setBackgroundColor(darkGray);
        TextView textView = (TextView) view.findViewById(C0206R.id.separator);
        if (this.mThemeDark) {
            i2 = white;
        } else {
            i2 = timeDisplay;
        }
        textView.setTextColor(i2);
        textView = (TextView) view.findViewById(C0206R.id.ampm_label);
        if (!this.mThemeDark) {
            white = timeDisplay;
        }
        textView.setTextColor(white);
        findViewById2 = view.findViewById(C0206R.id.line);
        if (!this.mThemeDark) {
            darkLine = line;
        }
        findViewById2.setBackgroundColor(darkLine);
        textView = this.mDoneButton;
        if (!this.mThemeDark) {
            darkDoneTextColor = doneTextColor;
        }
        textView.setTextColor(darkDoneTextColor);
        RadialPickerLayout radialPickerLayout = this.mTimePicker;
        if (!this.mThemeDark) {
            lightGray = circleBackground;
        }
        radialPickerLayout.setBackgroundColor(lightGray);
        textView = this.mDoneButton;
        if (!this.mThemeDark) {
            darkDoneBackground = doneBackground;
        }
        textView.setBackgroundResource(darkDoneBackground);
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

    public void tryVibrate() {
        this.mHapticFeedbackController.tryVibrate();
    }

    private void updateAmPmDisplay(int amOrPm) {
        if (amOrPm == 0) {
            this.mAmPmTextView.setText(this.mAmText);
            Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mAmText);
            this.mAmPmHitspace.setContentDescription(this.mAmText);
        } else if (amOrPm == PM) {
            this.mAmPmTextView.setText(this.mPmText);
            Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mPmText);
            this.mAmPmHitspace.setContentDescription(this.mPmText);
        } else {
            this.mAmPmTextView.setText(this.mDoublePlaceholderText);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if (this.mTimePicker != null) {
            outState.putInt(KEY_HOUR_OF_DAY, this.mTimePicker.getHours());
            outState.putInt(KEY_MINUTE, this.mTimePicker.getMinutes());
            outState.putBoolean(KEY_IS_24_HOUR_VIEW, this.mIs24HourMode);
            outState.putInt(KEY_CURRENT_ITEM_SHOWING, this.mTimePicker.getCurrentItemShowing());
            outState.putBoolean(KEY_IN_KB_MODE, this.mInKbMode);
            if (this.mInKbMode) {
                outState.putIntegerArrayList(KEY_TYPED_TIMES, this.mTypedTimes);
            }
            outState.putBoolean(KEY_DARK_THEME, this.mThemeDark);
        }
    }

    public void onValueSelected(int pickerIndex, int newValue, boolean autoAdvance) {
        if (pickerIndex == 0) {
            if (valueRespectsHoursConstraint(newValue)) {
                setHour(newValue, false);
                Object[] objArr = new Object[PM];
                objArr[HOUR_INDEX] = Integer.valueOf(newValue);
                String announcement = String.format("%d", objArr);
                if (this.mAllowAutoAdvance && autoAdvance) {
                    setCurrentItemShowing(PM, true, true, false);
                    announcement = announcement + ". " + this.mSelectMinutes;
                } else {
                    this.mTimePicker.setContentDescription(this.mHourPickerDescription + ": " + newValue);
                }
                Utils.tryAccessibilityAnnounce(this.mTimePicker, announcement);
            }
        } else if (pickerIndex == PM) {
            if (valueRespectsMinutesConstraint(newValue)) {
                setMinute(newValue);
                this.mTimePicker.setContentDescription(this.mMinutePickerDescription + ": " + newValue);
            }
        } else if (pickerIndex == AMPM_INDEX) {
            updateAmPmDisplay(newValue);
        } else if (pickerIndex == ENABLE_PICKER_INDEX) {
            if (!isTypedTimeFullyLegal()) {
                this.mTypedTimes.clear();
            }
            finishKbMode(true);
        }
    }

    private void setHour(int value, boolean announce) {
        String format;
        if (this.mIs24HourMode) {
            format = "%02d";
        } else {
            format = "%d";
            value %= 12;
            if (value == 0) {
                value = 12;
            }
        }
        Object[] objArr = new Object[PM];
        objArr[HOUR_INDEX] = Integer.valueOf(value);
        CharSequence text = String.format(format, objArr);
        this.mHourView.setText(text);
        this.mHourSpaceView.setText(text);
        if (announce) {
            Utils.tryAccessibilityAnnounce(this.mTimePicker, text);
        }
    }

    private void setMinute(int value) {
        if (value == 60) {
            value = HOUR_INDEX;
        }
        Object[] objArr = new Object[PM];
        objArr[HOUR_INDEX] = Integer.valueOf(value);
        CharSequence text = String.format(Locale.getDefault(), "%02d", objArr);
        Utils.tryAccessibilityAnnounce(this.mTimePicker, text);
        this.mMinuteView.setText(text);
        this.mMinuteSpaceView.setText(text);
    }

    private void setCurrentItemShowing(int index, boolean animateCircle, boolean delayLabelAnimate, boolean announce) {
        TextView labelToAnimate;
        this.mTimePicker.setCurrentItemShowing(index, animateCircle);
        if (index == 0) {
            int hours = this.mTimePicker.getHours();
            if (!this.mIs24HourMode) {
                hours %= 12;
            }
            this.mTimePicker.setContentDescription(this.mHourPickerDescription + ": " + hours);
            if (announce) {
                Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mSelectHours);
            }
            labelToAnimate = this.mHourView;
        } else {
            this.mTimePicker.setContentDescription(this.mMinutePickerDescription + ": " + this.mTimePicker.getMinutes());
            if (announce) {
                Utils.tryAccessibilityAnnounce(this.mTimePicker, this.mSelectMinutes);
            }
            labelToAnimate = this.mMinuteView;
        }
        int hourColor = index == 0 ? this.mSelectedColor : this.mUnselectedColor;
        int minuteColor = index == PM ? this.mSelectedColor : this.mUnselectedColor;
        this.mHourView.setTextColor(hourColor);
        this.mMinuteView.setTextColor(minuteColor);
        ObjectAnimator pulseAnimator = Utils.getPulseAnimator(labelToAnimate, 0.85f, 1.1f);
        if (delayLabelAnimate) {
            pulseAnimator.setStartDelay(300);
        }
        pulseAnimator.start();
    }

    private boolean processKeyUp(int keyCode) {
        if (keyCode == 111 || keyCode == 4) {
            dismiss();
            return true;
        }
        if (keyCode == 61) {
            if (this.mInKbMode) {
                if (!isTypedTimeFullyLegal()) {
                    return true;
                }
                finishKbMode(true);
                return true;
            }
        } else if (keyCode == 66) {
            if (this.mInKbMode) {
                if (!isTypedTimeFullyLegal()) {
                    return true;
                }
                finishKbMode(false);
            }
            if (this.mCallback != null) {
                this.mCallback.onTimeSet(this.mTimePicker, this.mTimePicker.getHours(), this.mTimePicker.getMinutes());
            }
            dismiss();
            return true;
        } else if (keyCode == 67) {
            if (this.mInKbMode && !this.mTypedTimes.isEmpty()) {
                String deletedKeyStr;
                int deleted = deleteLastTypedKey();
                if (deleted == getAmOrPmKeyCode(HOUR_INDEX)) {
                    deletedKeyStr = this.mAmText;
                } else if (deleted == getAmOrPmKeyCode(PM)) {
                    deletedKeyStr = this.mPmText;
                } else {
                    Object[] objArr = new Object[PM];
                    objArr[HOUR_INDEX] = Integer.valueOf(getValFromKeyCode(deleted));
                    deletedKeyStr = String.format("%d", objArr);
                }
                View view = this.mTimePicker;
                String str = this.mDeletedKeyFormat;
                Object[] objArr2 = new Object[PM];
                objArr2[HOUR_INDEX] = deletedKeyStr;
                Utils.tryAccessibilityAnnounce(view, String.format(str, objArr2));
                updateDisplay(true);
            }
        } else if (keyCode == 7 || keyCode == 8 || keyCode == 9 || keyCode == 10 || keyCode == 11 || keyCode == 12 || keyCode == 13 || keyCode == 14 || keyCode == 15 || keyCode == 16 || (!this.mIs24HourMode && (keyCode == getAmOrPmKeyCode(HOUR_INDEX) || keyCode == getAmOrPmKeyCode(PM)))) {
            if (this.mInKbMode) {
                if (!addKeyIfLegal(keyCode)) {
                    return true;
                }
                updateDisplay(false);
                return true;
            } else if (this.mTimePicker == null) {
                Log.e(TAG, "Unable to initiate keyboard mode, TimePicker was null.");
                return true;
            } else {
                this.mTypedTimes.clear();
                tryStartingKbMode(keyCode);
                return true;
            }
        }
        return false;
    }

    private void tryStartingKbMode(int keyCode) {
        if (!this.mTimePicker.trySettingInputEnabled(false)) {
            return;
        }
        if (keyCode == -1 || addKeyIfLegal(keyCode)) {
            this.mInKbMode = true;
            this.mDoneButton.setEnabled(false);
            updateDisplay(false);
        }
    }

    private boolean addKeyIfLegal(int keyCode) {
        if (this.mIs24HourMode && this.mTypedTimes.size() == 4) {
            return false;
        }
        if (!this.mIs24HourMode && isTypedTimeFullyLegal()) {
            return false;
        }
        this.mTypedTimes.add(Integer.valueOf(keyCode));
        if (isTypedTimeLegalSoFar()) {
            int val = getValFromKeyCode(keyCode);
            View view = this.mTimePicker;
            Object[] objArr = new Object[PM];
            objArr[HOUR_INDEX] = Integer.valueOf(val);
            Utils.tryAccessibilityAnnounce(view, String.format("%d", objArr));
            if (isTypedTimeFullyLegal()) {
                if (!this.mIs24HourMode && this.mTypedTimes.size() <= ENABLE_PICKER_INDEX) {
                    this.mTypedTimes.add(this.mTypedTimes.size() - 1, Integer.valueOf(7));
                    this.mTypedTimes.add(this.mTypedTimes.size() - 1, Integer.valueOf(7));
                }
                this.mDoneButton.setEnabled(true);
            }
            return true;
        }
        deleteLastTypedKey();
        return false;
    }

    private boolean isTypedTimeLegalSoFar() {
        Node node = this.mLegalTimesTree;
        Iterator i$ = this.mTypedTimes.iterator();
        while (i$.hasNext()) {
            node = node.canReach(((Integer) i$.next()).intValue());
            if (node == null) {
                return false;
            }
        }
        return true;
    }

    private boolean isTypedTimeFullyLegal() {
        boolean z = false;
        if (this.mIs24HourMode) {
            int[] values = getEnteredTime(null);
            if (values[HOUR_INDEX] < 0 || values[PM] < 0 || values[PM] >= 60) {
                return false;
            }
            return true;
        }
        if (this.mTypedTimes.contains(Integer.valueOf(getAmOrPmKeyCode(HOUR_INDEX))) || this.mTypedTimes.contains(Integer.valueOf(getAmOrPmKeyCode(PM)))) {
            z = PM;
        }
        return z;
    }

    private int deleteLastTypedKey() {
        int deleted = ((Integer) this.mTypedTimes.remove(this.mTypedTimes.size() - 1)).intValue();
        if (!isTypedTimeFullyLegal()) {
            this.mDoneButton.setEnabled(false);
        }
        return deleted;
    }

    private void finishKbMode(boolean updateDisplays) {
        this.mInKbMode = false;
        if (!this.mTypedTimes.isEmpty()) {
            int[] values = getEnteredTime(null);
            this.mTimePicker.setTime(values[HOUR_INDEX], values[PM]);
            if (!this.mIs24HourMode) {
                this.mTimePicker.setAmOrPm(values[AMPM_INDEX]);
            }
            this.mTypedTimes.clear();
        }
        if (updateDisplays) {
            updateDisplay(false);
            this.mTimePicker.trySettingInputEnabled(true);
        }
    }

    private void updateDisplay(boolean allowEmptyDisplay) {
        if (allowEmptyDisplay || !this.mTypedTimes.isEmpty()) {
            String hourStr;
            Object[] objArr;
            String minuteStr;
            Boolean[] enteredZeros = new Boolean[AMPM_INDEX];
            enteredZeros[HOUR_INDEX] = Boolean.valueOf(false);
            enteredZeros[PM] = Boolean.valueOf(false);
            int[] values = getEnteredTime(enteredZeros);
            String hourFormat = enteredZeros[HOUR_INDEX].booleanValue() ? "%02d" : "%2d";
            String minuteFormat = enteredZeros[PM].booleanValue() ? "%02d" : "%2d";
            if (values[HOUR_INDEX] == -1) {
                hourStr = this.mDoublePlaceholderText;
            } else {
                objArr = new Object[PM];
                objArr[HOUR_INDEX] = Integer.valueOf(values[HOUR_INDEX]);
                hourStr = String.format(hourFormat, objArr).replace(' ', this.mPlaceholderText);
            }
            if (values[PM] == -1) {
                minuteStr = this.mDoublePlaceholderText;
            } else {
                objArr = new Object[PM];
                objArr[HOUR_INDEX] = Integer.valueOf(values[PM]);
                minuteStr = String.format(minuteFormat, objArr).replace(' ', this.mPlaceholderText);
            }
            this.mHourView.setText(hourStr);
            this.mHourSpaceView.setText(hourStr);
            this.mHourView.setTextColor(this.mUnselectedColor);
            this.mMinuteView.setText(minuteStr);
            this.mMinuteSpaceView.setText(minuteStr);
            this.mMinuteView.setTextColor(this.mUnselectedColor);
            if (!this.mIs24HourMode) {
                updateAmPmDisplay(values[AMPM_INDEX]);
                return;
            }
            return;
        }
        int hour = this.mTimePicker.getHours();
        int minute = this.mTimePicker.getMinutes();
        setHour(hour, true);
        setMinute(minute);
        if (!this.mIs24HourMode) {
            updateAmPmDisplay(hour < 12 ? HOUR_INDEX : PM);
        }
        setCurrentItemShowing(this.mTimePicker.getCurrentItemShowing(), true, true, true);
        this.mDoneButton.setEnabled(true);
    }

    private static int getValFromKeyCode(int keyCode) {
        switch (keyCode) {
            case DayPickerView.DAYS_PER_WEEK /*7*/:
                return HOUR_INDEX;
            case ItemTouchHelper.RIGHT /*8*/:
                return PM;
            case C0220R.styleable.Toolbar_popupTheme /*9*/:
                return AMPM_INDEX;
            case C0220R.styleable.Toolbar_titleTextAppearance /*10*/:
                return ENABLE_PICKER_INDEX;
            case C0220R.styleable.Toolbar_subtitleTextAppearance /*11*/:
                return 4;
            case C0220R.styleable.Toolbar_titleMargins /*12*/:
                return 5;
            case C0220R.styleable.Toolbar_titleMarginStart /*13*/:
                return 6;
            case C0220R.styleable.Toolbar_titleMarginEnd /*14*/:
                return 7;
            case C0220R.styleable.Toolbar_titleMarginTop /*15*/:
                return 8;
            case ItemTouchHelper.START /*16*/:
                return 9;
            default:
                return -1;
        }
    }

    private int[] getEnteredTime(Boolean[] enteredZeros) {
        int amOrPm = -1;
        int startIndex = PM;
        if (!this.mIs24HourMode && isTypedTimeFullyLegal()) {
            int keyCode = ((Integer) this.mTypedTimes.get(this.mTypedTimes.size() - 1)).intValue();
            if (keyCode == getAmOrPmKeyCode(HOUR_INDEX)) {
                amOrPm = HOUR_INDEX;
            } else if (keyCode == getAmOrPmKeyCode(PM)) {
                amOrPm = PM;
            }
            startIndex = AMPM_INDEX;
        }
        int minute = -1;
        int hour = -1;
        for (int i = startIndex; i <= this.mTypedTimes.size(); i += PM) {
            int val = getValFromKeyCode(((Integer) this.mTypedTimes.get(this.mTypedTimes.size() - i)).intValue());
            if (i == startIndex) {
                minute = val;
            } else if (i == startIndex + PM) {
                minute += val * 10;
                if (enteredZeros != null && val == 0) {
                    enteredZeros[PM] = Boolean.valueOf(true);
                }
            } else if (i == startIndex + AMPM_INDEX) {
                hour = val;
            } else if (i == startIndex + ENABLE_PICKER_INDEX) {
                hour += val * 10;
                if (enteredZeros != null && val == 0) {
                    enteredZeros[HOUR_INDEX] = Boolean.valueOf(true);
                }
            }
        }
        int[] ret = new int[ENABLE_PICKER_INDEX];
        ret[HOUR_INDEX] = hour;
        ret[PM] = minute;
        ret[AMPM_INDEX] = amOrPm;
        return ret;
    }

    private int getAmOrPmKeyCode(int amOrPm) {
        if (this.mAmKeyCode == -1 || this.mPmKeyCode == -1) {
            KeyCharacterMap kcm = KeyCharacterMap.load(-1);
            int i = HOUR_INDEX;
            while (i < Math.max(this.mAmText.length(), this.mPmText.length())) {
                char amChar = this.mAmText.toLowerCase(Locale.getDefault()).charAt(i);
                char pmChar = this.mPmText.toLowerCase(Locale.getDefault()).charAt(i);
                if (amChar != pmChar) {
                    char[] cArr = new char[AMPM_INDEX];
                    cArr[HOUR_INDEX] = amChar;
                    cArr[PM] = pmChar;
                    KeyEvent[] events = kcm.getEvents(cArr);
                    if (events == null || events.length != 4) {
                        Log.e(TAG, "Unable to find keycodes for AM and PM.");
                    } else {
                        this.mAmKeyCode = events[HOUR_INDEX].getKeyCode();
                        this.mPmKeyCode = events[AMPM_INDEX].getKeyCode();
                    }
                } else {
                    i += PM;
                }
            }
        }
        if (amOrPm == 0) {
            return this.mAmKeyCode;
        }
        if (amOrPm == PM) {
            return this.mPmKeyCode;
        }
        return -1;
    }

    private void generateLegalTimesTree() {
        this.mLegalTimesTree = new Node(new int[HOUR_INDEX]);
        if (this.mIs24HourMode) {
            Node node = new Node(7, 8, 9, 10, 11, 12);
            node = new Node(7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
            node.addChild(node);
            int[] iArr = new int[AMPM_INDEX];
            iArr[HOUR_INDEX] = 7;
            iArr[PM] = 8;
            Node firstDigit = new Node(iArr);
            this.mLegalTimesTree.addChild(firstDigit);
            node = new Node(7, 8, 9, 10, 11, 12);
            firstDigit.addChild(node);
            node.addChild(node);
            node.addChild(new Node(13, 14, 15, 16));
            node = new Node(13, 14, 15, 16);
            firstDigit.addChild(node);
            node.addChild(node);
            iArr = new int[PM];
            iArr[HOUR_INDEX] = 9;
            firstDigit = new Node(iArr);
            this.mLegalTimesTree.addChild(firstDigit);
            node = new Node(7, 8, 9, 10);
            firstDigit.addChild(node);
            node.addChild(node);
            iArr = new int[AMPM_INDEX];
            iArr[HOUR_INDEX] = 11;
            iArr[PM] = 12;
            node = new Node(iArr);
            firstDigit.addChild(node);
            node.addChild(node);
            firstDigit = new Node(10, 11, 12, 13, 14, 15, 16);
            this.mLegalTimesTree.addChild(firstDigit);
            firstDigit.addChild(node);
            return;
        }
        iArr = new int[AMPM_INDEX];
        iArr[HOUR_INDEX] = getAmOrPmKeyCode(HOUR_INDEX);
        iArr[PM] = getAmOrPmKeyCode(PM);
        Node ampm = new Node(iArr);
        iArr = new int[PM];
        iArr[HOUR_INDEX] = 8;
        firstDigit = new Node(iArr);
        this.mLegalTimesTree.addChild(firstDigit);
        firstDigit.addChild(ampm);
        iArr = new int[ENABLE_PICKER_INDEX];
        iArr[HOUR_INDEX] = 7;
        iArr[PM] = 8;
        iArr[AMPM_INDEX] = 9;
        node = new Node(iArr);
        firstDigit.addChild(node);
        node.addChild(ampm);
        node = new Node(7, 8, 9, 10, 11, 12);
        node.addChild(node);
        node.addChild(ampm);
        Node fourthDigit = new Node(7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        node.addChild(fourthDigit);
        fourthDigit.addChild(ampm);
        node = new Node(13, 14, 15, 16);
        node.addChild(node);
        node.addChild(ampm);
        iArr = new int[ENABLE_PICKER_INDEX];
        iArr[HOUR_INDEX] = 10;
        iArr[PM] = 11;
        iArr[AMPM_INDEX] = 12;
        node = new Node(iArr);
        firstDigit.addChild(node);
        node = new Node(7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        node.addChild(node);
        node.addChild(ampm);
        firstDigit = new Node(9, 10, 11, 12, 13, 14, 15, 16);
        this.mLegalTimesTree.addChild(firstDigit);
        firstDigit.addChild(ampm);
        node = new Node(7, 8, 9, 10, 11, 12);
        firstDigit.addChild(node);
        node = new Node(7, 8, 9, 10, 11, 12, 13, 14, 15, 16);
        node.addChild(node);
        node.addChild(ampm);
    }

    public void setMinTime(int minHour, int minMinute) {
        this.mMinHour = minHour;
        this.mMinMinute = minMinute;
    }

    public void setMaxTime(int maxHour, int maxMinute) {
        this.mMaxHour = maxHour;
        this.mMaxMinute = maxMinute;
    }

    private boolean valueRespectsHoursConstraint(int value) {
        return this.mMinHour <= value && this.mMaxHour >= value;
    }

    private boolean valueRespectsMinutesConstraint(int value) {
        int hour = this.mTimePicker.getHours();
        boolean checkedMinMinute = true;
        boolean checkedMaxMinute = true;
        if (hour == this.mMinHour) {
            checkedMinMinute = value >= this.mMinMinute;
        }
        if (hour == this.mMaxHour) {
            if (value <= this.mMaxMinute) {
                checkedMaxMinute = true;
            } else {
                checkedMaxMinute = false;
            }
        }
        if (checkedMinMinute && checkedMaxMinute) {
            return true;
        }
        return false;
    }
}
