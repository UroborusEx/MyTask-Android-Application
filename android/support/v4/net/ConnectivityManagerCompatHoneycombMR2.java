package android.support.v4.net;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.support.v7.widget.helper.ItemTouchHelper;
import com.android.datetimepicker.date.DayPickerView;
import com.android.datetimepicker.time.TimePickerDialog;
import com.snowmobile.tasks.C0220R;

class ConnectivityManagerCompatHoneycombMR2 {
    ConnectivityManagerCompatHoneycombMR2() {
    }

    public static boolean isActiveNetworkMetered(ConnectivityManager cm) {
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) {
            return true;
        }
        switch (info.getType()) {
            case TimePickerDialog.HOUR_INDEX /*0*/:
            case TimePickerDialog.AMPM_INDEX /*2*/:
            case TimePickerDialog.ENABLE_PICKER_INDEX /*3*/:
            case ItemTouchHelper.LEFT /*4*/:
            case WearableExtender.SIZE_FULL_SCREEN /*5*/:
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT /*6*/:
                return true;
            case TimePickerDialog.PM /*1*/:
            case DayPickerView.DAYS_PER_WEEK /*7*/:
            case C0220R.styleable.Toolbar_popupTheme /*9*/:
                return false;
            default:
                return true;
        }
    }
}
