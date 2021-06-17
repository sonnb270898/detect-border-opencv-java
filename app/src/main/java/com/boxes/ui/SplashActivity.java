package com.boxes.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

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
    int[] arr = {
            R.drawable.i011,
            R.drawable.i025,
            R.drawable.i037,
            R.drawable.i056,
            R.drawable.i064,
            R.drawable.i074,
            R.drawable.i089,
            R.drawable.i093
    };

    int s = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView imageView = findViewById(R.id.imgTest);


        EditText editText = findViewById(R.id.edtTest);
         findViewById(R.id.btnTest).setOnClickListener(v -> {
             if (editText.getText().toString().isEmpty()){
                 Toast.makeText(this, "FAILLLLLLLLLLL", Toast.LENGTH_SHORT).show();
                 return;
             }
             int s = Integer.parseInt(editText.getText().toString());
             if (s>arr.length){
                 editText.setText("");
                 Toast.makeText(this, "FAILLLLLLLLLLL", Toast.LENGTH_SHORT).show();
                 return;
             }
             Bitmap icon = BitmapFactory.decodeResource(getResources(), arr[s]);
             Bitmap bitmap = ConvertFile.main(icon);
             imageView.setImageBitmap(bitmap);
             editText.setText("");
         });



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