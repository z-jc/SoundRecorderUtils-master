package com.sid.soundrecorderutils.view;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sid.soundrecorderutils.R;

public class SelectActivity extends BaseActivity implements View.OnClickListener {
    private Button mBtn1;
    private Button mBtn2;
    private TextView mTitle;
    private EditText mEdNumber;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);
        initView();
    }

    private void initView() {

        mBtn1 = (Button) findViewById(R.id.btn_1);
        mBtn2 = (Button) findViewById(R.id.btn_2);

        mBtn1.setOnClickListener(this);
        mBtn2.setOnClickListener(this);
        mTitle = (TextView) findViewById(R.id.title);
        mTitle.setText("采集模式选择");
        mEdNumber = (EditText) findViewById(R.id.ed_number);
        mEdNumber.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_1:
                submit();
                break;
            case R.id.btn_2:
                startAct(2,"2222");
                break;
        }
    }

    private void startAct(int arr,String serialNumber) {
        startActivity(MainActivity.newIntent(SelectActivity.this, arr,serialNumber));
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void submit() {
        String number = mEdNumber.getText().toString().trim();
        if (TextUtils.isEmpty(number)) {
            showToast("选择模式一请填写影厅编号");
            return;
        }
        startAct(1,number);
    }
}