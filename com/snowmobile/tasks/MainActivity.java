package com.snowmobile.tasks;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import com.snowmobile.tasks.adapter.TabPagerFragmentAdapter;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private static final int DEFAULT_LAYOUT = 2130968602;
    private FloatingActionButton newTaskButton;
    private TabPagerFragmentAdapter tabAdapter;
    private TabLayout tabLayout;
    private Toolbar toolbar;
    private ViewPager viewPager;

    /* renamed from: com.snowmobile.tasks.MainActivity.1 */
    class C02191 implements OnClickListener {
        C02191() {
        }

        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, InputActivity.class);
            intent.putExtra(MainActivity.this.getString(C0220R.string.inflate_key_task), 0);
            MainActivity.this.startActivityForResult(intent, 0);
        }
    }

    /* renamed from: com.snowmobile.tasks.MainActivity.2 */
    class C03032 implements OnMenuItemClickListener {
        C03032() {
        }

        public boolean onMenuItemClick(MenuItem menuItem) {
            return false;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        setTheme(C0220R.style.AppDefault);
        super.onCreate(savedInstanceState);
        setContentView((int) DEFAULT_LAYOUT);
        this.newTaskButton = (FloatingActionButton) findViewById(C0220R.id.actionButton);
        this.newTaskButton.setOnClickListener(new C02191());
        initToolbar();
        initTabs();
    }

    private void initToolbar() {
        this.toolbar = (Toolbar) findViewById(C0220R.id.toolbar);
        this.toolbar.setTitle((int) C0220R.string.app_name);
        this.toolbar.setOnMenuItemClickListener(new C03032());
        this.toolbar.inflateMenu(C0220R.menu.menu);
    }

    private void initTabs() {
        this.viewPager = (ViewPager) findViewById(C0220R.id.viewPager);
        this.tabAdapter = new TabPagerFragmentAdapter(this, getSupportFragmentManager());
        this.viewPager.setAdapter(this.tabAdapter);
        this.tabLayout = (TabLayout) findViewById(C0220R.id.tabLayout);
        this.tabLayout.setupWithViewPager(this.viewPager);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == -1) {
            Bundle extras = data.getExtras();
            Calendar taskCalendar = Calendar.getInstance();
            taskCalendar.set(extras.getInt(getString(C0220R.string.inflate_key_year)), extras.getInt(getString(C0220R.string.inflate_key_month)), extras.getInt(getString(C0220R.string.inflate_key_day)), 23, 59, 59);
            if (requestCode == 1) {
                TaskItem changedTaks = this.tabAdapter.findById(extras.getInt(getString(C0220R.string.inflate_key_task)));
                changedTaks.setDate(taskCalendar);
                changedTaks.setTitle(extras.getString(getString(C0220R.string.inflate_key_text)));
                this.tabAdapter.notificateDataSetChanged();
                return;
            }
            this.tabAdapter.addTask(new TaskItem(extras.getString(getString(C0220R.string.inflate_key_text)), taskCalendar));
            this.tabAdapter.notificateDataSetChanged();
        }
    }

    protected void onResume() {
        super.onResume();
    }

    protected void onPause() {
        super.onPause();
        this.tabAdapter.saveData();
    }
}
