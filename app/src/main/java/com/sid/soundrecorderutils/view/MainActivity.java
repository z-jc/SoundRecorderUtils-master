package com.sid.soundrecorderutils.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sid.soundrecorderutils.record.AudioRecoderUtils;
import com.sid.soundrecorderutils.R;
import com.sid.soundrecorderutils.ftp.FtpClient;
import com.sid.soundrecorderutils.util.FileUtil;
import com.sid.soundrecorderutils.util.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private String TAG = "MainActivity";
    private Button mBtnPress, /*mBtnPlay,*/
            mBtnTakePhoto, mBtnUplode;
    private TextView mTv;
    private AudioRecoderUtils mRecoderUtils = null;
    private boolean isReCord = false;       //记录当前是否在录音
    private boolean isPlayer = false;       //记录当前是否在播放
    private String imgPath = "";            //图片文件路径
    private String imgName = "";            //图片文件名
    private String mp3Path = "";            //录音文件路径
    private String mp3Name = "";            //录音文件名
    private int flagPatt = -1;              //标记当前选择的1:模式一还是2:模式二   -1:未选择
    private String flagSerial = "";         //标记当前影厅编号
    private TextView mTitle;

    public static Intent newIntent(Activity activity, int arr, String serial) {
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra("patt", arr);
        intent.putExtra("serial", serial);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        flagPatt = getIntent().getIntExtra("patt", -1);
        showToast("模式" + flagPatt);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initView();
        mRecoderUtils = new AudioRecoderUtils();
        mBtnPress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlayer) {
                    Toast.makeText(MainActivity.this, "正在播放录音 请稍等...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isReCord) {
                    mRecoderUtils.stopRecord();
                    mBtnPress.setText("按住说话");
                    isReCord = false;
                } else {
                    mBtnPress.setText("再按一次保存");
                    mRecoderUtils.startRecord();
                    isReCord = true;
                }
            }
        });

        mRecoderUtils.setOnAudioStatusUpdateListener(new AudioRecoderUtils.OnAudioStatusUpdateListener() {
            //录音中....db为声音分贝，time为录音时长
            @Override
            public void onUpdate(double db, long time) {
                int m = 00;
                int s = 00;
                if (time >= 60000) {
                    m = (int) (time / 60000);
                    s = (int) ((time / 1000) & 60);
                } else if (time < 60000) {
                    m = 00;
                    s = (int) (time / 1000);
                }
                Log.e("TAG", "当前时间:" + m + ":" + s);
                final int finalM = m;
                final int finalS = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTv.setText(finalM + ":" + finalS + "\n正在录音");
                    }
                });
            }

            //录音结束，filePath为保存路径
            @Override
            public void onStop(String filepath) {
                mTv.setText("录音结束,保存的路径为:" + filepath);
                Log.e("TAG", "录音结束,保存的路径为:" + filepath);
                mp3Path = filepath;
                mp3Name = filepath.substring(filepath.length() - 23, filepath.length());
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
        switch (resultCode) {
            case 222:
                imgPath = data.getStringExtra("imgPath");
                imgName = data.getStringExtra("imgName");
                break;
        }
    }

    /**
     * 拍照
     */
    public void startTake() {
        startActivityForResult(new Intent(MainActivity.this, TakePhotoActivity.class), 222);
    }

    /**
     * 初始化UI
     */
    private void initView() {
        mBtnPress = (Button) findViewById(R.id.btn_press);
        //mBtnPlay = (Button) findViewById(R.id.btn_play);
        mBtnTakePhoto = (Button) findViewById(R.id.btn_take_photo);
        mTv = (TextView) findViewById(R.id.tv_clock);
        mBtnUplode = (Button) findViewById(R.id.btn_uplode);
        mBtnUplode.setOnClickListener(this);
        mTitle = (TextView) findViewById(R.id.title);
        mTitle.setText("采集数据");
    }

    @Override
    protected void onResume() {
        super.onResume();
        imgName = "";
        imgPath = "";
        mp3Name = "";
        mp3Path = "";
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isReCord) {
            mRecoderUtils.stopRecord();
            isReCord = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecoderUtils != null) {
            mRecoderUtils.cancelRecord();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_uplode:

                //弹出单选对话框选择
                final String[] arr = {"图片", "音频"};//选择的选项
                final int[] size = {-1};
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("选择上传文件类型")
                        .setSingleChoiceItems(arr, -1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int witch) {
                                size[0] = witch;
                                LogUtils.e("TAG", "当前选择的类型:" + size[0]);
                            }
                        }).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final ArrayList<Integer> integer = new ArrayList<Integer>();
                        Map<String, String> map = new HashMap<>();
                        String[] arrs = null;
                        if (size[0] == 0) {
                            map = FileUtil.getFileName(TakePhotoActivity.PATH_IMAGES);
                            arrs = new String[map.values().size()];
                            arrs = map.values().toArray(arrs);
                            LogUtils.e("TAG", "image arrs:" + Arrays.toString(arrs));
                        } else if (size[0] == 1) {
                            map = FileUtil.getFileName(AudioRecoderUtils.MP3_PATH);
                            arrs = new String[map.values().size()];
                            arrs = map.values().toArray(arrs);
                            LogUtils.e("TAG", "mp3 arrs:" + Arrays.toString(arrs));
                        }
                        final Map<String, String> finalMap = map;
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("选择上传文件")
                                .setMultiChoiceItems(arrs,
                                        new boolean[arrs.length],
                                        new DialogInterface.OnMultiChoiceClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                                                if (b) {
                                                    integer.add(i);
                                                } else {
                                                    integer.remove(i);
                                                }
                                            }
                                        })
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        for (int teger : integer) {
                                            if (finalMap.get(String.valueOf(teger)).endsWith(".mp3")) {
                                                uplode(AudioRecoderUtils.MP3_PATH + finalMap.get(String.valueOf(teger)), finalMap.get(String.valueOf(teger)));
                                            }
                                            if (finalMap.get(String.valueOf(teger)).endsWith(".jpg")) {
                                                uplode(TakePhotoActivity.PATH_IMAGES + finalMap.get(String.valueOf(teger)), finalMap.get(String.valueOf(teger)));
                                            }
                                        }
                                    }
                                }).show();
                    }
                }).show();
                break;
        }
    }

    private void uplode(final String filePath, final String fileName) {
        // 网络操作，但开一个线程进行处理
        new Thread(new Runnable() {
            @Override
            public void run() {
                FtpClient ftpClient = new FtpClient();
                Log.e("TAG", "filePath:" + filePath);
                Log.e("TAG", "fileName:" + fileName);
                final String str = ftpClient.ftpUpload(filePath, fileName);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (str.equals("1")) {
                            Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }
}