package com.boxes.service;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.boxes.callback.IDataCallback;

import java.lang.ref.WeakReference;

public class MyHandler extends Handler {
    private final WeakReference<Activity> mActivity;
    private IDataCallback callback;

    public MyHandler(Activity activity, IDataCallback callback) {
        mActivity = new WeakReference<>(activity);
        this.callback = callback;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case UsbService.MESSAGE_FROM_SERIAL_PORT:
                String data = (String) msg.obj;
                callback.receiveData(data);
                break;
            case UsbService.CTS_CHANGE:
                Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                break;
            case UsbService.DSR_CHANGE:
                Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                break;
        }
    }
}
