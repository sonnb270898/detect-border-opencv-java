package com.boxes.uvc.bluetooth;


public interface BluetoothLeInterface {
    void getData(String var1, String var2, String var3, String var4);

    void getConnectionState(String var1, boolean var2);

    void getConnectionInfo(String var1, String var2);

    void getBluetoothLeService(BluetoothLeService var1);
}