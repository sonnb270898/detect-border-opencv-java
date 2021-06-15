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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;

import com.boxes.service.UsbReceiver;
import com.boxes.service.UsbService;
import com.boxes.ui.MainActivity;
import com.boxes.ui.R;

import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.posprinterface.UiExecute;
import net.posprinter.service.PosprinterService;
import net.posprinter.utils.PosPrinterDev;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;
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

    View dialogView3;
    private TextView tv_usb;
    private List<String> usbList,usblist;
    private ListView lv1,lv2,lv_usb;
    private ArrayAdapter<String> adapter;
    // Connect USB
    public boolean connected = false;
    public boolean connectedXprinter = false;
    private UsbReceiver mUsbReceiver;
    public UsbService usbService;
    //IMyBinder interface，All methods that can be invoked to connect and send data are encapsulated within this interface
    // connect USB xprinter
    public static IMyBinder binder;
    public static PosPrinterDev.PortType portType;

    ServiceConnection conn= new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e("tienld", "conn: ");
            binder= (IMyBinder) iBinder;
            connectXprinter();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("tienld", "dis conn: ");
        }
    };

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            Log.e("tienld", "onServiceConnected: ");
            //bind service，get ImyBinder object
            Intent intent=new Intent(getApplicationContext(), PosprinterService.class);
            bindService(intent, conn, BIND_AUTO_CREATE);
            connected = true;
            usbService = ((UsbService.UsbBinder) arg1).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.e("tienld", "onServiceDisconnected: ");
            usbService = null;
            connected = false;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (!OpenCVLoader.initDebug()) {
            Log.e("tienld", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
        } else {
            Log.d("tienld", "OpenCV library found inside package. Using it!");
        }
        setInstance(this);
        registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
    }


    private void connectXprinter(){
        if (PosPrinterDev.GetUsbPathNames(this) !=null && !PosPrinterDev.GetUsbPathNames(this).isEmpty()) {
            String printer = PosPrinterDev.GetUsbPathNames(this).get(0);
            binder.connectUsbPort(getApplicationContext(), printer , new UiExecute() {
                @Override
                public void onsucess() {
                    connectedXprinter = true;
                    setPortType();
                }

                @Override
                public void onfailed() {
                    // Lần đâu connect máy in fail sẽ call laị để kết nối máy in
                    connectXprinter();
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
            if (activity instanceof MainActivity){
                mUsbReceiver = new UsbReceiver();
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
            if (activity instanceof MainActivity){
                setFilters();
                startService(UsbService.class, usbConnection, null);
            }
        }


        @Override
        public void onActivityPaused(Activity activity) {
            if (activity instanceof MainActivity){
                unregisterReceiver(mUsbReceiver);
                unbindService(usbConnection);
            }
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

    public void setUSB(){
        LayoutInflater inflater=LayoutInflater.from(this);
        dialogView3=inflater.inflate(R.layout.usb_link,null);
        tv_usb= (TextView) dialogView3.findViewById(R.id.textView1);
        lv_usb= (ListView) dialogView3.findViewById(R.id.listView1);


        usbList= PosPrinterDev.GetUsbPathNames(this);
        if (usbList==null){
            usbList=new ArrayList<>();
        }
        usblist=usbList;
        tv_usb.setText(getString(R.string.usb_pre_con)+usbList.size());
        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,usbList);
        lv_usb.setAdapter(adapter);


        AlertDialog dialog=new AlertDialog.Builder(this).setView(dialogView3).create();
        dialog.show();

        setUsbLisener(dialog);

    }

    String usbDev="";
    public void setUsbLisener(final AlertDialog dialog) {
        lv_usb.setOnItemClickListener((adapterView, view, i, l) -> {
            usbDev=usbList.get(i);
            dialog.cancel();
            Log.e("usbDev: ",usbDev);
        });
    }

}
