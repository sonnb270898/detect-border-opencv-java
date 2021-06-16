package com.boxes.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.boxes.utils.ConvertFile;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;


public class SplashActivity extends AppCompatActivity {

    private Handler mHandler;
    private final Runnable mRunnable = () -> {
//        startActivity(new Intent(SplashActivity.this,MainActivity.class));
//        finish();
    };

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.e("tienld", "onManagerConnected: " +  status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("tienld", "OpenCV loaded successfully");
                    // Create and set View
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.i011);
        Log.e("tienld", "Trying to load OpenCV library");
        Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.i011);
        ConvertFile.main(icon,bitmap1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(mRunnable,1500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRunnable);
    }
}