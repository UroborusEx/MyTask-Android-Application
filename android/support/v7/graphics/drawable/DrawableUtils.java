package android.support.v7.graphics.drawable;

import android.graphics.PorterDuff.Mode;
import android.os.Build.VERSION;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.support.v7.widget.helper.ItemTouchHelper;
import com.android.datetimepicker.time.TimePickerDialog;
import com.snowmobile.tasks.C0220R;

public class DrawableUtils {
    public static Mode parseTintMode(int value, Mode defaultMode) {
        switch (value) {
            case TimePickerDialog.ENABLE_PICKER_INDEX /*3*/:
                return Mode.SRC_OVER;
            case WearableExtender.SIZE_FULL_SCREEN /*5*/:
                return Mode.SRC_IN;
            case C0220R.styleable.Toolbar_popupTheme /*9*/:
                return Mode.SRC_ATOP;
            case C0220R.styleable.Toolbar_titleMarginEnd /*14*/:
                return Mode.MULTIPLY;
            case C0220R.styleable.Toolbar_titleMarginTop /*15*/:
                return Mode.SCREEN;
            case ItemTouchHelper.START /*16*/:
                if (VERSION.SDK_INT >= 11) {
                    return Mode.valueOf("ADD");
                }
                return defaultMode;
            default:
                return defaultMode;
        }
    }
}
