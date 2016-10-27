package com.snowmobile.tasks.adapter;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.TextView;
import com.snowmobile.tasks.C0220R;
import com.snowmobile.tasks.InputActivity;
import com.snowmobile.tasks.MainActivity;
import com.snowmobile.tasks.TaskItem;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class TaskAdapter extends Adapter<TaskHolder> {
    private List<TaskItem> data;
    private TabPagerFragmentAdapter holderTabs;

    public static class TaskHolder extends ViewHolder implements OnCreateContextMenuListener, OnMenuItemClickListener {
        private static final int MENU_DELETE = 2131493022;
        private static final int MENU_RENAME = 2131493021;
        private CardView cardView;
        private TaskItem content;
        private TextView date;
        private TabPagerFragmentAdapter holder;
        private TextView title;

        public TaskHolder(View itemView) {
            super(itemView);
            this.cardView = (CardView) itemView.findViewById(C0220R.id.cardView);
            this.title = (TextView) itemView.findViewById(C0220R.id.title);
            this.date = (TextView) itemView.findViewById(C0220R.id.date);
            itemView.setOnCreateContextMenuListener(this);
        }

        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            ((Activity) v.getContext()).getMenuInflater().inflate(C0220R.menu.context_task_menu, menu);
            menu.findItem(MENU_RENAME).setOnMenuItemClickListener(this);
            menu.findItem(MENU_DELETE).setOnMenuItemClickListener(this);
        }

        public boolean onMenuItemClick(MenuItem item) {
            Log.d("1", "onMenuItemClick");
            switch (item.getItemId()) {
                case MENU_RENAME /*2131493021*/:
                    onRenameMenu();
                    break;
                case MENU_DELETE /*2131493022*/:
                    onDeleteMenu();
                    break;
            }
            return true;
        }

        public void setHolder(TabPagerFragmentAdapter holder) {
            this.holder = holder;
        }

        public void setContent(TaskItem content) {
            this.content = content;
        }

        private void onRenameMenu() {
            Intent intent = new Intent(this.holder.getContext(), InputActivity.class);
            intent.putExtra(this.holder.getContext().getString(C0220R.string.inflate_key_task), this.content.getId());
            ((MainActivity) this.holder.getContext()).startActivityForResult(intent, 1);
        }

        private void onDeleteMenu() {
            this.holder.removeTask(this.content);
        }
    }

    public void setData(List<TaskItem> data) {
        this.data = data;
        Collections.sort(data);
    }

    public TaskAdapter(List<TaskItem> data, TabPagerFragmentAdapter holder) {
        setData(data);
        this.holderTabs = holder;
    }

    public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TaskHolder(LayoutInflater.from(parent.getContext()).inflate(C0220R.layout.task_item, parent, false));
    }

    public void onBindViewHolder(TaskHolder holder, int position) {
        TaskItem item = (TaskItem) this.data.get(position);
        holder.setHolder(this.holderTabs);
        holder.setContent(item);
        holder.title.setText(item.getTitle());
        Calendar taskDate = item.getDate();
        holder.date.setText(String.format("%d.%s.%d", new Object[]{Integer.valueOf(taskDate.get(5)), Integer.toString(taskDate.get(2) + 1), Integer.valueOf(taskDate.get(1))}));
    }

    public int getItemCount() {
        return this.data.size();
    }
}
