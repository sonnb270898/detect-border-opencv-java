package com.boxes.uvc.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.boxes.ui.camera.R;
import com.serenegiant.common.BaseActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class BluetoothLeActivity extends BaseActivity {
    private static final String TAG = BluetoothLeActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic gattCharacteristic2;
    private BluetoothAdapter mBluetoothAdapter;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private static final int REQUEST_ENABLE_BT = 1;
    private String mDeviceName;
    private String mDeviceAddress;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            BluetoothLeActivity.this.mBluetoothLeService = ((BluetoothLeService.LocalBinder)service).getService();
            if (!BluetoothLeActivity.this.mBluetoothLeService.initialize()) {
                Log.e(BluetoothLeActivity.TAG, "Unable to initialize Bluetooth");
                BluetoothLeActivity.this.finish();
            }

            BluetoothLeActivity.this.mBluetoothLeService.connect(BluetoothLeActivity.this.mDeviceAddress);
            BluetoothLeActivity.this.getBluetoothLeService(BluetoothLeActivity.this.mBluetoothLeService);
        }

        public void onServiceDisconnected(ComponentName componentName) {
            BluetoothLeActivity.this.mBluetoothLeService = null;
        }
    };
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.example.bluetooth.le.ACTION_GATT_CONNECTED".equals(action)) {
                BluetoothLeActivity.this.mConnected = true;
                BluetoothLeActivity.this.updateConnectionState(BluetoothLeActivity.this.getString(R.string.connected), BluetoothLeActivity.this.mConnected);
                BluetoothLeActivity.this.invalidateOptionsMenu();
            } else if ("com.example.bluetooth.le.ACTION_GATT_DISCONNECTED".equals(action)) {
                BluetoothLeActivity.this.mConnected = false;
                BluetoothLeActivity.this.updateConnectionState(BluetoothLeActivity.this.getString(R.string.disconnected), BluetoothLeActivity.this.mConnected);
                BluetoothLeActivity.this.invalidateOptionsMenu();
                BluetoothLeActivity.this.mGattServicesList.setAdapter((SimpleExpandableListAdapter)null);
            } else if ("com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED".equals(action)) {
                BluetoothLeActivity.this.displayGattServices(BluetoothLeActivity.this.mBluetoothLeService.getSupportedGattServices());
            } else if ("com.example.bluetooth.le.ACTION_DATA_AVAILABLE".equals(action)) {
                BluetoothLeActivity.this.receiveData(intent.getStringExtra("com.example.bluetooth.le.EXTRA_DATA"));
            }

        }
    };
    private final ExpandableListView.OnChildClickListener servicesListClickListner = new ExpandableListView.OnChildClickListener() {
        @TargetApi(18)
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            if (BluetoothLeActivity.this.mGattCharacteristics != null) {
                BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic)((ArrayList) BluetoothLeActivity.this.mGattCharacteristics.get(groupPosition)).get(childPosition);
                int charaProp = characteristic.getProperties();
                if ((charaProp | 2) > 0) {
                    if (BluetoothLeActivity.this.mNotifyCharacteristic != null) {
                        BluetoothLeActivity.this.mBluetoothLeService.setCharacteristicNotification(BluetoothLeActivity.this.mNotifyCharacteristic, false);
                        BluetoothLeActivity.this.mNotifyCharacteristic = null;
                    }

                    BluetoothLeActivity.this.mBluetoothLeService.readCharacteristic(characteristic);
                }

                if ((charaProp | 16) > 0) {
                    BluetoothLeActivity.this.mNotifyCharacteristic = characteristic;
                    BluetoothLeActivity.this.mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                }

                return true;
            } else {
                return false;
            }
        }
    };

    public BluetoothLeActivity() {
    }

    @RequiresApi(
            api = 18
    )
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(128, 128);
        if (!this.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            this.finish();
        }

        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_COARSE_LOCATION") != 0) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, 10);
        }

        BluetoothManager bluetoothManager = (BluetoothManager)this.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = bluetoothManager.getAdapter();
        if (this.mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            this.finish();
        } else {
            Intent intent;
            if (!this.mBluetoothAdapter.isEnabled() && !this.mBluetoothAdapter.isEnabled()) {
                intent = new Intent("android.bluetooth.adapter.action.REQUEST_ENABLE");
                this.startActivityForResult(intent, 1);
            }

            intent = this.getIntent();
            this.mDeviceName = intent.getStringExtra("DEVICE_NAME");
            this.mDeviceAddress = intent.getStringExtra("DEVICE_ADDRESS");
            if (!this.isValidMac(this.mDeviceAddress)) {
                Toast.makeText(this, "建立连接的MAC地址不规范,请重试", Toast.LENGTH_SHORT).show();
                this.finish();
            } else {
                Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                this.bindService(gattServiceIntent, this.mServiceConnection, BIND_AUTO_CREATE);
                Log.e(TAG, "mDeviceName:" + this.mDeviceName + "  Mac:" + this.mDeviceAddress);
                this.getConnectionInfo(this.mDeviceName, this.mDeviceAddress);
                this.mGattServicesList = new ExpandableListView(this);
                this.mGattServicesList.setOnChildClickListener(this.servicesListClickListner);
                this.registerReceiver(this.mGattUpdateReceiver, makeGattUpdateIntentFilter());
                if (this.mBluetoothLeService != null) {
                    boolean result = this.mBluetoothLeService.connect(this.mDeviceAddress);
                    Log.d(TAG, "Connect request result=" + result);
                }

            }
        }
    }

    private void receiveData(String data) {
        Log.e(TAG, "receiveData=" + data);

        try {
            if (data != null) {
                String unit = MyUtils.getWeightUnit(data);
                String strWeight = data.substring(6, 14).replace(" ", "");
                if (data.indexOf("OL") != -1) {
                    this.getData(strWeight, "OL", "超重", unit);
                } else if (data.indexOf("ST") != -1) {
                    this.getData(strWeight, "ST", "稳定", unit);
                } else if (data.indexOf("UN") != -1) {
                    this.getData(strWeight, "UN", "普通", unit);
                }
            }
        } catch (Exception var4) {
            var4.printStackTrace();
            this.getData("", "err", "解析失败", "err");
        }

    }

    public abstract void getData(String var1, String var2, String var3, String var4);

    private void updateConnectionState(final String connStateInfo, final boolean isConnected) {
        this.runOnUiThread(new Runnable() {
            public void run() {
                BluetoothLeActivity.this.getConnectionState(connStateInfo, isConnected);
            }
        });
    }

    public abstract void getConnectionState(String var1, boolean var2);

    public abstract void getConnectionInfo(String var1, String var2);

    public abstract void getBluetoothLeService(BluetoothLeService var1);

    @TargetApi(18)
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        Log.e(TAG, "displayGattServices");
        if (gattServices != null) {
            String uuid = null;
            String unknownServiceString = this.getResources().getString(R.string.unknown_service);
            String unknownCharaString = this.getResources().getString(R.string.unknown_characteristic);
            ArrayList<HashMap<String, String>> gattServiceData = new ArrayList();
            ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList();
            this.mGattCharacteristics = new ArrayList();
            Iterator var7 = gattServices.iterator();

            while(var7.hasNext()) {
                BluetoothGattService gattService = (BluetoothGattService)var7.next();
                HashMap<String, String> currentServiceData = new HashMap();
                uuid = gattService.getUuid().toString();
                currentServiceData.put("NAME", SampleGattAttributes.lookup(uuid, unknownServiceString));
                currentServiceData.put("UUID", uuid);
                gattServiceData.add(currentServiceData);
                ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList();
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas = new ArrayList();
                Iterator var13 = gattCharacteristics.iterator();

                while(var13.hasNext()) {
                    BluetoothGattCharacteristic gattCharacteristic = (BluetoothGattCharacteristic)var13.next();
                    if (gattCharacteristic.getUuid().toString().equals("0000ffe1-0000-1000-8000-00805f9b34fb")) {
                        this.gattCharacteristic2 = gattCharacteristic;
                    }

                    charas.add(gattCharacteristic);
                    HashMap<String, String> currentCharaData = new HashMap();
                    uuid = gattCharacteristic.getUuid().toString();
                    currentCharaData.put("NAME", SampleGattAttributes.lookup(uuid, unknownCharaString));
                    currentCharaData.put("UUID", uuid);
                    gattCharacteristicGroupData.add(currentCharaData);
                }

                this.mGattCharacteristics.add(charas);
                gattCharacteristicData.add(gattCharacteristicGroupData);
            }

            SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(this, gattServiceData, 17367047, new String[]{"NAME", "UUID"}, new int[]{16908308, 16908309}, gattCharacteristicData, 17367047, new String[]{"NAME", "UUID"}, new int[]{16908308, 16908309});
            this.mGattServicesList.setAdapter(gattServiceAdapter);
            Log.e(TAG, "mGattServicesList.setAdapter");

            try {
                this.mGattServicesList.setVisibility(View.GONE);
                if (this.mGattCharacteristics != null) {
                    BluetoothGattCharacteristic characteristic = this.gattCharacteristic2;
                    int charaProp = characteristic.getProperties();
                    if ((charaProp | 2) > 0) {
                        if (this.mNotifyCharacteristic != null) {
                            this.mBluetoothLeService.setCharacteristicNotification(this.mNotifyCharacteristic, false);
                            this.mNotifyCharacteristic = null;
                        }

                        this.mBluetoothLeService.readCharacteristic(characteristic);
                    }

                    if ((charaProp | 16) > 0) {
                        this.mNotifyCharacteristic = characteristic;
                        Log.e(TAG, "characteristic=" + characteristic);
                        this.mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                    }
                }
            } catch (Exception var16) {
                var16.printStackTrace();
            }

        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.bluetooth.le.ACTION_GATT_CONNECTED");
        intentFilter.addAction("com.example.bluetooth.le.ACTION_GATT_DISCONNECTED");
        intentFilter.addAction("com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED");
        intentFilter.addAction("com.example.bluetooth.le.ACTION_DATA_AVAILABLE");
        return intentFilter;
    }

    protected void onDestroy() {
        super.onDestroy();

        try {
            this.unbindService(this.mServiceConnection);
            this.mBluetoothLeService = null;
        } catch (Exception var2) {
            var2.printStackTrace();
        }

        this.unregisterReceiver(this.mGattUpdateReceiver);
    }

    private boolean isValidMac(String macStr) {
        if (macStr != null && !macStr.equals("")) {
            String macAddressRule = "([A-Fa-f0-9]{2}[-,:]){5}[A-Fa-f0-9]{2}";
            if (macStr.matches(macAddressRule)) {
                Log.i(TAG, "it is a valid MAC address");
                return true;
            } else {
                Log.e(TAG, "it is not a valid MAC address!!!");
                return false;
            }
        } else {
            return false;
        }
    }
}
