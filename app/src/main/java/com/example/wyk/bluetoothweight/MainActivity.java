package com.example.wyk.bluetoothweight;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Spinner mSpinner;
    private Button mBtSearch;
    private Button mBtConnect;
    private Switch mSwitch;

    private BluetoothConnect bluetoothConnect;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mAdapter;
    private List<String> mSpinnerList = new ArrayList<String>();
    private String mMacAdress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_weigth);

        mSwitch = findViewById(R.id.bluetooth_switch);
        mSpinner = findViewById(R.id.bluetooth_spinner);
        mBtConnect = findViewById(R.id.bluetooth_connect);
        mBtSearch = findViewById(R.id.bluetooth_search);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothConnect = new BluetoothConnect();
        //检测蓝牙状态，自动打开蓝牙
        bluetoothConnect.checkBluetoothStatus(MainActivity.this, mBluetoothAdapter);
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mSpinnerList);
        mSpinner.setAdapter(mAdapter);

        setClickListener();

    }

    //设置监听
    public void setClickListener() {
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Log.d("ischecked", "true");
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivity(intent);
                    } else {
                        Toast.makeText(MainActivity.this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    mBluetoothAdapter.disable();
                    Toast.makeText(MainActivity.this, "蓝牙已关闭", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mBtSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //检测蓝牙状态
                bluetoothConnect.checkBluetoothStatus(MainActivity.this, mBluetoothAdapter);
                //搜索蓝牙设备
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver, filter);
                //添加已配对过的设备
                bluetoothConnect.addBondedDevice(mBluetoothAdapter, mAdapter);
            }
        });
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mMacAdress = mAdapter.getItem(position);
                mMacAdress = mSpinnerList.get(position);
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    //    搜索蓝牙设备
//    BluetoothAdapter.ACTION_DISCOVERY_STARTED、
//    BluetoothDevice.ACTION_FOUND、
//    BluetoothAdapter.ACTION_DISCOVERY_FINISHED。
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //避免重复添加已经绑定过的设备，已配对过的跳过
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    //这里只获取了蓝牙设备的address
                    mAdapter.add(device.getAddress());
                    mAdapter.notifyDataSetChanged();
                } else if (mBluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    Toast.makeText(MainActivity.this, "开始搜索蓝牙设备", Toast.LENGTH_SHORT).show();
                } else if (mBluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Toast.makeText(MainActivity.this, "搜索完毕", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

}
