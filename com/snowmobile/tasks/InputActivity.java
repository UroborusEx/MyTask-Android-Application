package com.snowmobile.tasks;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.Toolbar.OnMenuItemClickListener;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.EditText;
import java.util.Calendar;

public class InputActivity extends AppCompatActivity {
    private static final int DEFAULT_LAYOUT = 2130968601;
    private Calendar calendar;
    private EditText editText;
    private Toolbar toolbar;

    /* renamed from: com.snowmobile.tasks.InputActivity.1 */
    class C03021 implements OnMenuItemClickListener {

        /* renamed from: com.snowmobile.tasks.InputActivity.1.1 */
        class C02181 implements OnDateSetListener {
            C02181() {
            }

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Intent intent = InputActivity.this.getIntent();
                intent.putExtra(InputActivity.this.getString(C0220R.string.inflate_key_text), InputActivity.this.editText.getText().toString());
                intent.putExtra(InputActivity.this.getString(C0220R.string.inflate_key_year), year);
                intent.putExtra(InputActivity.this.getString(C0220R.string.inflate_key_month), monthOfYear);
                intent.putExtra(InputActivity.this.getString(C0220R.string.inflate_key_day), dayOfMonth);
                InputActivity.this.setResult(-1, intent);
                InputActivity.this.finish();
            }
        }

        C03021() {
        }

        public boolean onMenuItemClick(MenuItem menuItem) {
            InputActivity.this.calendar = Calendar.getInstance();
            new DatePickerDialog(InputActivity.this, new C02181(), InputActivity.this.calendar.get(1), InputActivity.this.calendar.get(2), InputActivity.this.calendar.get(5)).show();
            return true;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        setTheme(C0220R.style.AppDefault);
        super.onCreate(savedInstanceState);
        setContentView((int) DEFAULT_LAYOUT);
        this.editText = (EditText) findViewById(C0220R.id.edit_text);
        this.toolbar = (Toolbar) findViewById(C0220R.id.toolbar);
        this.toolbar.setTitle((int) C0220R.string.app_name);
        this.toolbar.setOnMenuItemClickListener(new C03021());
        this.toolbar.inflateMenu(C0220R.menu.input_menu);
    }
}
