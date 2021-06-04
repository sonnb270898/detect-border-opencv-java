package com.boxes;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


import com.boxes.service.UsbReceiver;
import com.boxes.service.UsbService;

import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.posprinterface.UiExecute;
import net.posprinter.service.PosprinterService;
import net.posprinter.utils.PosPrinterDev;

import java.util.Set;

public class ApplicationController extends Application {

    private static volatile ApplicationController mInstance;
    public static ApplicationController getInstance() {
        return mInstance;
    }

    public static ApplicationController get() {
        return mInstance;
    }


    private static void setInstance(ApplicationController value) {
        mInstance = value;
    }

    // Connect USB
    public boolean connected = false;
    private UsbReceiver mUsbReceiver;
    public UsbService usbService;
    //IMyBinder interface，All methods that can be invoked to connect and send data are encapsulated within this interface
    // connect USB xprinter
    public static IMyBinder binder;
    public static PosPrinterDev.PortType portType;

    ServiceConnection conn= new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder= (IMyBinder) iBinder;
            connectXprinter();
            Log.e("tienld","ServiceConnection true");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("tienld","ServiceConnection fail");
        }
    };

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            connected = true;
            usbService = ((UsbService.UsbBinder) arg1).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
            connected = false;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        setInstance(this);
        registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
    }


    private void connectXprinter(){
        if (PosPrinterDev.GetUsbPathNames(this) !=null && !PosPrinterDev.GetUsbPathNames(this).isEmpty()) {
            String printer = PosPrinterDev.GetUsbPathNames(this).get(0);
            binder.connectUsbPort(getApplicationContext(), printer , new UiExecute() {
                @Override
                public void onsucess() {
                    setPortType();
                }

                @Override
                public void onfailed() {
                    Log.e("tienld","connectXprinter" + " faild");
                }
            });
        }

    }


    public void send(String str) {
        if (!connected) {
            Toast.makeText(this, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] data = str.getBytes();
            usbService.write(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }


    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setPortType(){
       portType = PosPrinterDev.PortType.USB;
    }

    private final ActivityLifecycleCallbacks mActivityLifecycleCallbacks = new ActivityLifecycleCallbacks() {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

        }

        @Override
        public void onActivityStarted(Activity activity) {
            mUsbReceiver = new UsbReceiver();
            //bind service，get ImyBinder object
            Intent intent=new Intent(activity, PosprinterService.class);
            bindService(intent, conn, BIND_AUTO_CREATE);
        }

        @Override
        public void onActivityResumed(Activity activity) {
            setFilters();
            startService(UsbService.class, usbConnection, null);
        }


        @Override
        public void onActivityPaused(Activity activity) {
            unregisterReceiver(mUsbReceiver);
            unbindService(usbConnection);
        }

        @Override
        public void onActivityStopped(Activity activity) {

        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    };

}
