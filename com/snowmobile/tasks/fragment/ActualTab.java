package com.snowmobile.tasks.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.snowmobile.tasks.C0220R;
import com.snowmobile.tasks.DBHelper;
import com.snowmobile.tasks.TaskItem;
import com.snowmobile.tasks.adapter.TabPagerFragmentAdapter;
import com.snowmobile.tasks.adapter.TaskAdapter;
import com.snowmobile.tasks.filter.AbstractFilterList;
import java.util.List;

public class ActualTab extends AbstractTab {
    private static final int LAYOUT = 2130968619;
    private TaskAdapter adapter;
    private List<TaskItem> data;
    private DBHelper dbHelper;
    private AbstractFilterList filter;
    private TabPagerFragmentAdapter holder;

    public static ActualTab getInstance(Context context, String title, List<TaskItem> dataLink, TabPagerFragmentAdapter holder, AbstractFilterList filter) {
        Bundle args = new Bundle();
        ActualTab fragment = new ActualTab();
        fragment.setArguments(args);
        fragment.setContext(context);
        fragment.setHolder(holder);
        fragment.setTitle(title);
        fragment.setData(dataLink);
        fragment.setFilter(filter);
        fragment.setDbHelper(context);
        return fragment;
    }

    @Nullable
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.view = inflater.inflate(LAYOUT, container, false);
        RecyclerView recView = (RecyclerView) this.view.findViewById(C0220R.id.recyclerView);
        registerForContextMenu(recView);
        recView.setHasFixedSize(true);
        recView.setLayoutManager(new LinearLayoutManager(this.context));
        this.adapter = new TaskAdapter(this.filter.filter(this.data), this.holder);
        recView.setAdapter(this.adapter);
        return this.view;
    }

    public void notifyDataChanges() {
        this.data.size();
        this.adapter.setData(this.filter.filter(this.data));
        this.adapter.notifyDataSetChanged();
    }

    private void setContext(Context context) {
        this.context = context;
    }

    private void setData(List<TaskItem> data) {
        this.data = data;
    }

    private void setFilter(AbstractFilterList filter) {
        this.filter = filter;
    }

    private void setHolder(TabPagerFragmentAdapter holder) {
        this.holder = holder;
    }

    private void setDbHelper(Context context) {
        this.dbHelper = new DBHelper(context);
    }

    private List<TaskItem> fillData() {
        return this.dbHelper.getAllTasks();
    }
}
