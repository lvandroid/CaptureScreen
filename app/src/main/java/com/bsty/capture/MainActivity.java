package com.bsty.capture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private int result = 0;
    private Intent intent = null;
    private int REQUEST_MEDIA_PROJECTION = 1;

    private SimpleDateFormat dateFormat = null;
    private String strDate = null;
    private String pathImage = null;
    private String nameImage = null;

    private MediaProjection mMediaProjection = null;
    private VirtualDisplay mVirtualDisplay = null;

    private Handler mainHandler = new Handler();

    public static int mResultCode = 0;
    public static Intent mResultData = null;
    public static MediaProjectionManager mMediaProjectionManager = null;

    private WindowManager mWindowManager = null;
    private int windowWidth = 0;
    private int windowHeight = 0;
    private ImageReader mImageReader = null;
    private DisplayMetrics metrics = null;
    private int mScreenDensity = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createVirtualEnvironment();
//        findViewById(R.id.btn_permission).setOnClickListener(v -> {
            startIntent();
//        });
        findViewById(R.id.btn_capture).setOnClickListener(v -> {
            Handler handlerVirtual = new Handler();
            handlerVirtual.post(() -> {
                startVirtual();
                Handler captureHandler = new Handler();
                captureHandler.postDelayed(this::startCapture, 100);
            });
        });
    }

    private void createVirtualEnvironment() {
        dateFormat = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
        strDate = dateFormat.format(new java.util.Date());
        pathImage = Environment.getExternalStorageDirectory().getPath() + "/Pictures/";
        nameImage = pathImage + strDate + ".png";

        mMediaProjectionManager = (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowWidth = mWindowManager.getDefaultDisplay().getWidth();
        windowHeight = mWindowManager.getDefaultDisplay().getHeight();

        metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mImageReader = ImageReader.newInstance(windowWidth, windowHeight, 0x1, 2);//ImageFormat.RGB_565

        Log.i(TAG, "prepared the virtual environment");

    }

    public void startVirtual() {
        if (null != mMediaProjection) {
            Log.i(TAG, "want to display virtual");
            virtualDisplay();
        } else {
            Log.i(TAG, "start screen capture intent");
            Log.i(TAG, "want to build mediaprojection and display virtual");
            setUpMediaProjection();
            virtualDisplay();
        }
    }

    public void setUpMediaProjection() {
//        mResultData = ((App) getApplication()).getIntent();
//        mResultCode = ((App) getApplication()).getResult();
//        mMediaProjectionManager = ((App) getApplication()).getMediaProjectionManager();
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
        Log.i(TAG, "mMediaProjection defined");
    }

    private void virtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                windowWidth,
                windowHeight,
                mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
        Log.i(TAG, "virtual displayed");

    }

    private void startCapture() {
        try {
            strDate = dateFormat.format(new Date());
            nameImage = pathImage + strDate + ".png";

            Image image = mImageReader.acquireLatestImage();
            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            int piexlStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - piexlStride * width;
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / piexlStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            image.close();
            Bitmap finalBitmap = bitmap;
            mainHandler.post(() -> {
                ((ImageView) findViewById(R.id.iv_pic)).setImageBitmap(finalBitmap);
            });
            if (bitmap != null) {

                File fileImage = new File(nameImage);
                if (!fileImage.exists()) {
                    fileImage.createNewFile();
                    Log.i(TAG, "image file created");
                }
                FileOutputStream out = new FileOutputStream(fileImage);
                if (null != out) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.flush();
                    out.close();
                    Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri contentUri = Uri.fromFile(fileImage);
                    media.setData(contentUri);
                    this.sendBroadcast(media);
                    Log.i(TAG, "screen image saved");
                }

            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG, "mMediaProjection undefined");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tearDownMediaProjection();
        Log.i(TAG, "application destroy");
    }

    private void startIntent() {
        if (intent != null && result != 0) {
            Log.i(TAG, "user agree the application to capture screen");
            //Service1.mResultCode = resultCode;
            //Service1.mResultData = data;
//            ((App) getApplication()).setResult(result);
//            ((App) getApplication()).setIntent(intent);
            mResultCode = result;
            mResultData = intent;
            //Intent intent = new Intent(getApplicationContext(), Service1.class);
            //startService(intent);
            //Log.i(TAG, "start service Service1");
        } else {
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
            //Service1.mMediaProjectionManager1 = mMediaProjectionManager;
//            ((App) getApplication()).setMediaProjectionManager(mMediaProjectionManager);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            } else if (data != null && resultCode != 0) {
                Log.i(TAG, "user agree the application to capture screen");
                //Service1.mResultCode = resultCode;
                //Service1.mResultData = data;
                result = resultCode;
                intent = data;
                mResultCode = resultCode;
                mResultData = data;
//                ((App) getApplication()).setResult(resultCode);
//                ((App) getApplication()).setIntent(data);
            }
        }
    }
}

