package com.snowmobile.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "TasksDB.db";
    private static final int DB_VERSION = 3;
    public static final String TASKS_COLUMN_DATE = "taskdate";
    public static final String TASKS_COLUMN_ID = "id";
    public static final String TASKS_COLUMN_NAME = "title";
    public static final String TASKS_TABLE_NAME = "tasks";
    private static final long errorValue = 1000;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table tasks ( id integer primary key autoincrement, title text, taskdate int  ) ");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS tasks");
        onCreate(db);
    }

    public List<TaskItem> getAllTasks() {
        List<TaskItem> array_list = new ArrayList();
        SQLiteDatabase db = getReadableDatabase();
        Cursor res = db.rawQuery("select * from tasks", null);
        res.moveToFirst();
        while (!res.isAfterLast()) {
            Calendar item_date = Calendar.getInstance();
            item_date.setTime(new Date(((long) res.getInt(res.getColumnIndex(TASKS_COLUMN_DATE))) * errorValue));
            item_date.set(item_date.get(1), item_date.get(2), item_date.get(5), 23, 59, 59);
            array_list.add(new TaskItem(res.getString(res.getColumnIndex(TASKS_COLUMN_NAME)), item_date));
            res.moveToNext();
        }
        res.close();
        db.close();
        return array_list;
    }

    public void insertTask(List<TaskItem> data) {
        SQLiteDatabase db = getWritableDatabase();
        onUpgrade(db, 0, 0);
        ContentValues contentValues = new ContentValues();
        for (TaskItem item : data) {
            contentValues.put(TASKS_COLUMN_NAME, item.getTitle());
            contentValues.put(TASKS_COLUMN_DATE, Long.valueOf(item.getDate().getTime().getTime() / errorValue));
            db.insert(TASKS_TABLE_NAME, null, contentValues);
        }
        db.close();
    }

    public int findTodayTasksNumber(List<TaskItem> data) {
        int taskNumber = 0;
        Calendar thisDate = Calendar.getInstance();
        for (TaskItem item : data) {
            Calendar taskDate = item.getDate();
            if (thisDate.get(1) == taskDate.get(1) && thisDate.get(2) == taskDate.get(2) && thisDate.get(5) == taskDate.get(5)) {
                taskNumber++;
            }
        }
        return taskNumber;
    }
}
