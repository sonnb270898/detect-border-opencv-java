package com.boxes.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.boxes.ui.adapter.DeviceAdapter;
import com.boxes.ui.camera.DetectActivity;
import com.boxes.ui.camera.R;
import com.boxes.uvc.bluetooth.BluetoothLeActivity;
import com.tbruyelle.rxpermissions3.RxPermissions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements DeviceAdapter.IOnClickListener , View.OnClickListener {

    RecyclerView listViewDetected;
    Button buttonSearch,buttonOn,buttonDesc,buttonOff;
    private DeviceAdapter adapter;
    BluetoothAdapter bluetoothAdapter = null;
    List<BluetoothDevice> arrayListBluetoothDevices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        initView();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listViewDetected.setLayoutManager(new LinearLayoutManager(this));
        listViewDetected.setHasFixedSize(true);
        adapter = new DeviceAdapter(this,this);
        adapter.setListDevice(arrayListBluetoothDevices);
        listViewDetected.setAdapter(adapter);
        checkPermission();
    }

    private void checkPermission(){
        RxPermissions permissions = new RxPermissions(this);
        permissions.request(Manifest.permission.ACCESS_COARSE_LOCATION)
                .subscribe(granted -> {
                    if (granted) {
                        startSearching();
                    } else {
                        Toast.makeText(this, "Please permissions ACCESS_COARSE_LOCATION", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initView(){
        listViewDetected = findViewById(R.id.listViewDetected);
        buttonSearch = findViewById(R.id.buttonSearch);
        buttonOn = findViewById(R.id.buttonOn);
        buttonDesc = findViewById(R.id.buttonDesc);
        buttonOff = findViewById(R.id.buttonOff);

        buttonOn.setOnClickListener(this);
        buttonSearch.setOnClickListener(this);
        buttonDesc.setOnClickListener(this);
        buttonOff.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public void listener(BluetoothDevice device) {
        Intent intent = new Intent(this, DetectActivity.class);
        intent.putExtra(BluetoothLeActivity.EXTRAS_DEVICE_NAME,device.getName());
        intent.putExtra(BluetoothLeActivity.EXTRAS_DEVICE_ADDRESS,device.getAddress());
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonOn:
                onBluetooth();
                break;
            case R.id.buttonSearch:
                arrayListBluetoothDevices.clear();
                adapter.notifyDataSetChanged();
                startSearching();
                break;
            case R.id.buttonDesc:
                makeDiscoverable();
                break;
            case R.id.buttonOff:
                offBluetooth();
                break;
            default:
                break;
        }
    }

    public BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(arrayListBluetoothDevices.size()<1) {
                    arrayListBluetoothDevices.add(device);
                    adapter.notifyDataSetChanged();
                } else {
                    boolean flag = true;
                    for(int i = 0; i<arrayListBluetoothDevices.size();i++) {
                        if(device.getAddress().equals(arrayListBluetoothDevices.get(i).getAddress())) {
                            flag = false;
                        }
                    }
                    if(flag) {
                        arrayListBluetoothDevices.add(device);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    private void startSearching() {
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myReceiver,intentFilter);
        //MainActivity.this.registerReceiver(myReceiver, intentFilter);
        bluetoothAdapter.startDiscovery();
    }
    private void onBluetooth() {
        if(!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
            Log.i("Log", "Bluetooth is Enabled");
        }
    }

    private void offBluetooth() {
        arrayListBluetoothDevices.clear();
        if(bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
        }
    }
    private void makeDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        Log.i("Log", "Discoverable ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceiver);
    }
}