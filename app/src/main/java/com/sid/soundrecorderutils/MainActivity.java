package com.sid.soundrecorderutils;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button mBtnPress, mBtnPlay, mBtnTakePhoto;
    private TextView textView;
    private String filePath = null;
    private AudioRecoderUtils recoderUtils = null;
    private RecordPlayer recordPlayer = null;
    private boolean isReCord = false;//记录当前是否在录音
    private boolean isPlayer = false;
    private String[] STRINGS = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.LOCATION_HARDWARE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WAKE_LOCK};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * 动态申请读写权限
         * */
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(getBaseContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        STRINGS, 1);
            }
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initView();
        recoderUtils = new AudioRecoderUtils();
        mBtnPress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isReCord) {
                    recoderUtils.stopRecord();
                    mBtnPress.setText("按住说话");
                    isReCord = false;
                } else {
                    mBtnPress.setText("再按一次保存");
                    recoderUtils.startRecord();
                    isReCord = true;
                }
            }
        });
        //录音回调
        recoderUtils.setOnAudioStatusUpdateListener(new AudioRecoderUtils.OnAudioStatusUpdateListener() {
            //录音中....db为声音分贝，time为录音时长
            @Override
            public void onUpdate(double db, long time) {
                Log.e("TAG", "当前时间:" + time / 1000);
                int m = 00;
                int s = 00;
                if (time / 1000 >= 60) {
                    m = (int) (time / 1000 / 60);
                    s = (int) ((time / 1000) & 60);
                } else if (time / 1000 < 60) {
                    m = 00;
                    s = (int) (time / 1000);
                }
                if (m < 10 && s < 10) {
                    textView.setText("0" + m + ":0" + s + "\n正在录音");
                }
                if (m < 10 && s >= 10) {
                    textView.setText("0" + m + ":" + s + "\n正在录音");
                }
                if (m >= 10 && s < 10) {
                    textView.setText(m + ":0" + s + "\n正在录音");
                }
            }

            //录音结束，filePath为保存路径
            @Override
            public void onStop(String filepath) {
                filePath = filepath;
                textView.setText("录音结束,保存的路径为:" + filepath);
                Log.e("TAG", "录音结束,保存的路径为:" + filePath);
            }
        });

        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filePath == null) {
                    Toast.makeText(MainActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isReCord) {
                    Toast.makeText(MainActivity.this, "正在录音 请稍等...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isPlayer) {
                    Toast.makeText(MainActivity.this, "正在播放录音 请稍等...", Toast.LENGTH_SHORT).show();
                    return;
                }
                File file = new File(filePath);
                recordPlayer = new RecordPlayer(MainActivity.this, handler);
                recordPlayer.playRecordFile(file);
                isPlayer = true;
            }
        });

        mBtnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTake();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 101:
                List<String> list = data.getStringArrayListExtra("list");
                for (String path : list) {
                    Log.e("TAG", "照片路径:" + path);
                }
                break;
        }
    }

    public void startTake() {
        startActivityForResult(new Intent(MainActivity.this, TakePhotoActivity.class), 222);
    }

    /**
     * 初始化UI
     */
    private void initView() {
        mBtnPress = (Button) findViewById(R.id.btn_press);
        mBtnPlay = (Button) findViewById(R.id.btn_play);
        mBtnTakePhoto = (Button) findViewById(R.id.btn_take_photo);
        textView = (TextView) findViewById(R.id.tv_clock);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String data = msg.obj.toString();
            switch (msg.what) {
                case 200:
                    Log.e("TAG", "录音时长:" + data + "计算后的时间:" + Long.valueOf(data).longValue() / 1000 * 1000);
                    new CountDownTimer(Long.valueOf(data).longValue() / 1000 * 1000 + 1000, 1000) {
                        @Override
                        public void onTick(final long time) {
                            Log.e("TAG", "当前时间:" + time / 1000);
                            int m = 00;
                            int s = 00;
                            if (time / 1000 >= 60) {
                                m = (int) (time / 1000 / 60);
                                s = (int) ((time / 1000) & 60);
                            } else if (time / 1000 < 60) {
                                m = 00;
                                s = (int) (time / 1000);
                            }
                            final int finalS = s;
                            final int finalM = m;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (finalM < 10 && finalS < 10) {
                                        textView.setText("0" + finalM + ":0" + finalS + "\n正在播放");
                                    }
                                    if (finalM < 10 && finalS >= 10) {
                                        textView.setText("0" + finalM + ":" + finalS + "\n正在播放");
                                    }
                                    if (finalM >= 10 && finalS < 10) {
                                        textView.setText("开始录音:" + finalM + ":0" + finalS + "\n正在播放");
                                    }
                                }
                            });
                        }

                        @Override
                        public void onFinish() {
                            Log.e("TAG", "onFinish...");
                            textView.setText("长按下方按钮\n开始录音");
                        }
                    }.start();
                    break;

                case 400:
                    isPlayer = false;
                    break;
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        recoderUtils.cancelRecord();
        recordPlayer.stopPalyer();
    }
}