package com.snowmobile.tasks.adapter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import com.snowmobile.tasks.C0220R;
import com.snowmobile.tasks.DBHelper;
import com.snowmobile.tasks.TaskItem;
import com.snowmobile.tasks.broadcastReceiver.NotificationBroadcastReceiver;
import com.snowmobile.tasks.filter.AbstractFilterList;
import com.snowmobile.tasks.fragment.AbstractTab;
import com.snowmobile.tasks.fragment.ActualTab;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TabPagerFragmentAdapter extends FragmentPagerAdapter {
    private Context context;
    private List<TaskItem> data;
    private DBHelper dbHelper;
    private Map<Integer, AbstractTab> tabs;

    /* renamed from: com.snowmobile.tasks.adapter.TabPagerFragmentAdapter.1 */
    class C03041 extends AbstractFilterList {
        C03041() {
        }

        public List<TaskItem> filter(List<TaskItem> data) {
            List<TaskItem> filtredData = new ArrayList();
            Calendar thisDate = Calendar.getInstance();
            for (TaskItem item : data) {
                if (!thisDate.after(item.getDate())) {
                    filtredData.add(item);
                }
            }
            return filtredData;
        }
    }

    /* renamed from: com.snowmobile.tasks.adapter.TabPagerFragmentAdapter.2 */
    class C03052 extends AbstractFilterList {
        C03052() {
        }

        public List<TaskItem> filter(List<TaskItem> data) {
            List<TaskItem> filtredData = new ArrayList();
            Calendar thisDate = Calendar.getInstance();
            for (TaskItem item : data) {
                if (thisDate.after(item.getDate())) {
                    filtredData.add(item);
                }
            }
            return filtredData;
        }
    }

    public TabPagerFragmentAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
        this.dbHelper = new DBHelper(context);
        this.data = fillData();
        this.tabs = new HashMap();
        initTabs();
        startAlarm();
    }

    public CharSequence getPageTitle(int position) {
        return ((AbstractTab) this.tabs.get(Integer.valueOf(position))).getTitle();
    }

    public Fragment getItem(int position) {
        return (Fragment) this.tabs.get(Integer.valueOf(position));
    }

    public int getCount() {
        return this.tabs.size();
    }

    private void initTabs() {
        Resources resources = this.context.getResources();
        this.tabs.put(Integer.valueOf(resources.getInteger(C0220R.integer.actual_tab)), ActualTab.getInstance(this.context, this.context.getString(C0220R.string.actual), this.data, this, new C03041()));
        this.tabs.put(Integer.valueOf(resources.getInteger(C0220R.integer.history_tab)), ActualTab.getInstance(this.context, this.context.getString(C0220R.string.history), this.data, this, new C03052()));
    }

    public void addTask(TaskItem item) {
        this.data.size();
        this.data.add(item);
        this.data.size();
        notificateDataSetChanged();
        startAlarm();
    }

    public void removeTask(TaskItem item) {
        this.data.remove(item);
        notificateDataSetChanged();
        startAlarm();
    }

    private List<TaskItem> fillData() {
        return this.dbHelper.getAllTasks();
    }

    public void saveData() {
        this.dbHelper.insertTask(this.data);
    }

    public void notificateDataSetChanged() {
        for (Entry<Integer, AbstractTab> entry : this.tabs.entrySet()) {
            ((AbstractTab) entry.getValue()).notifyDataChanges();
        }
    }

    public TaskItem findById(int id) {
        for (TaskItem item : this.data) {
            if (id == item.getId()) {
                return item;
            }
        }
        return null;
    }

    public void startAlarm() {
        Intent intent = new Intent(this.context, NotificationBroadcastReceiver.class);
        PendingIntent repeatSender = PendingIntent.getBroadcast(this.context, 0, intent, 134217728);
        PendingIntent updateSender = PendingIntent.getBroadcast(this.context, 2, intent, 134217728);
        AlarmManager alarmManagerm = (AlarmManager) this.context.getSystemService(NotificationCompatApi21.CATEGORY_ALARM);
        alarmManagerm.setRepeating(1, Calendar.getInstance().getTimeInMillis(), 1800000, repeatSender);
        alarmManagerm.set(3, System.currentTimeMillis(), updateSender);
    }

    public Context getContext() {
        return this.context;
    }
}
