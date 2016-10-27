package com.snowmobile.tasks;

import android.support.annotation.NonNull;
import java.util.Calendar;

public class TaskItem implements Comparable<TaskItem> {
    private static int id_pool;
    private Calendar date;
    private int id;
    private String title;

    static {
        id_pool = 1;
    }

    public TaskItem(String title, Calendar date) {
        this.title = title;
        this.date = date;
        this.id = id_pool;
        id_pool++;
    }

    public Calendar getDate() {
        return this.date;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getId() {
        return this.id;
    }

    public int compareTo(@NonNull TaskItem taskForCompare) {
        if (this.date.before(taskForCompare.date)) {
            return -1;
        }
        return 1;
    }
}
