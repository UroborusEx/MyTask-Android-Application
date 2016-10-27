package com.snowmobile.tasks.fragment;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.view.View;

public class AbstractTab extends Fragment {
    protected Context context;
    protected String title;
    protected View view;

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void notifyDataChanges() {
    }
}
