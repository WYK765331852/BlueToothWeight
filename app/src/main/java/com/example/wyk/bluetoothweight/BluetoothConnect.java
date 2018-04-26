package com.example.wyk.bluetoothweight;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnect {
    private BluetoothAdapter bluetoothAdapter;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //打开蓝牙设备
    public void checkBluetoothStatus(Activity activity, BluetoothAdapter bluetoothAdapter1) {
        this.bluetoothAdapter = bluetoothAdapter1;
        if (bluetoothAdapter == null) {
            Toast.makeText(activity, "当前设备不支持蓝牙设备", Toast.LENGTH_SHORT).show();
        }
        if (!bluetoothAdapter.enable()) {
            //提示用户开启蓝牙
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivity(intent);
        }
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //设置为一直开启。当我们需要设置具体可被发现的时间时，最多只能设置300秒。
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            activity.startActivity(intent);
        }

    }

    //添加已配对过的蓝牙设备
    public void addBondedDevice(BluetoothAdapter bluetoothAdapter2, ArrayAdapter adapter){
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter2.getBondedDevices();
        if (bondedDevices.size()>0){
            for (BluetoothDevice device: bondedDevices){
                adapter.remove(device);
                adapter.add(device);
            }
        }
    }




}
