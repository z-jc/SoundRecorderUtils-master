package com.sid.soundrecorderutils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TakePhotoActivity extends Activity {
    RelativeLayout relativeLayout = null;
    private static final String PATH_IMAGES = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator + "easy_check/";
    private Camera camera = null;
    private TakePhotoActivity.CameraView cv = null;
    List<String> dList = new ArrayList<>();

    /**
     * 拍完照从回调中获取照片
     */
    private Camera.PictureCallback picture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            relativeLayout.removeAllViews();
            cv = new CameraView(TakePhotoActivity.this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT,
                    LinearLayout.LayoutParams.FILL_PARENT);
            relativeLayout.addView(cv, params);
            dList.add(PATH_IMAGES + "IMG_" + (dList.size() + 1) + ".JPG");
            Log.e("TAG", "" + dList.size());
            if (dList.size() == 1) {
                saveFile(data, dList.get(0));
            }
            if (dList.size() == 2) {
                saveFile(data, dList.get(1));
            }
            if (dList.size() == 3) {
                saveFile(data, dList.get(2));
            }
        }
    };

    /**
     * 保存照片
     */
    public void saveFile(byte[] data, String path) {
        FileOutputStream outputStream = null;
        try {
            File file = new File(PATH_IMAGES);
            if (!file.exists()) {
                file.mkdirs();
            }
            Log.e("TAG", path);
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            outputStream = new FileOutputStream(path);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            bufferedOutputStream.write(baos.toByteArray(), 0, baos.toByteArray().length);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
                Log.e("TAG", "saveFile...");
                Toast.makeText(TakePhotoActivity.this, dList.size() + "张", Toast.LENGTH_SHORT).show();
                if (dList.size() == 1) {
                    Intent intent = new Intent();
                    intent.putStringArrayListExtra("list", (ArrayList<String>) dList);
                    setResult(101, intent);
                    TakePhotoActivity.this.finish();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_takephoto);
        if (dList.size() > 0) {
            dList.clear();
        }
        relativeLayout = (RelativeLayout) findViewById(R.id.cameraView);
        relativeLayout.removeAllViews();
        cv = new CameraView(TakePhotoActivity.this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.FILL_PARENT);
        relativeLayout.addView(cv, params);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 5; i++) {
                    if (dList.size() < 1) {
                        takePicture();
                    }
                    if (dList.size() > 1) {
                        break;
                    }
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 主要的surfaceView，负责展示预览图片，camera的开关
     */
    class CameraView extends SurfaceView {
        private SurfaceHolder holder = null;

        public CameraView(Context context) {
            super(context);
            holder = this.getHolder();
            holder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceChanged(SurfaceHolder holder, int format,
                                           int width, int height) {
                    Log.e("TAG", "surfaceChanged...");
                    int PreviewWidth = 0;
                    int PreviewHeight = 0;
                    WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);//获取窗口的管理器
                    Display display = wm.getDefaultDisplay();//获得窗口里面的屏幕
                    Camera.Parameters parameters = camera.getParameters();
                    // 选择合适的预览尺寸
                    List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
                    // 如果sizeList只有一个我们也没有必要做什么了，因为就他一个别无选择
                    if (sizeList.size() > 1) {
                        Iterator<Camera.Size> itor = sizeList.iterator();
                        while (itor.hasNext()) {
                            Camera.Size cur = itor.next();
                            if (cur.width >= PreviewWidth
                                    && cur.height >= PreviewHeight) {
                                PreviewWidth = cur.width;
                                PreviewHeight = cur.height;
                                break;
                            }
                        }
                    }

                    parameters.setPreviewSize(display.getWidth(), display.getHeight()); //获得摄像区域的大小
                    parameters.setPictureFormat(PixelFormat.JPEG);
                    parameters.setPictureSize(display.getWidth(), display.getHeight());//设置拍出来的屏幕大小
                    camera.setParameters(parameters);//把上面的设置 赋给摄像头
                    camera.startPreview();
                    camera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success) {
                                Log.e("TAG", "对焦成功...");
                                camera.cancelAutoFocus();
                            }
                        }
                    });
                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    Log.e("TAG", "surfaceCreated...");
                    camera = Camera.open(0);
                    try {
                        camera.setDisplayOrientation(90);
                        camera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        camera.release();
                        camera = null;
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    //顾名思义可以看懂
                    Log.e("TAG", "surfaceDestroyed...");
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
            });
        }
    }

    private void takePicture() {
        if (camera != null) {
            camera.takePicture(null, null, picture);
        } else {
            Log.e("TAG", "camera=null");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }
}