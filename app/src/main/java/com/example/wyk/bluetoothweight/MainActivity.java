package com.example.wyk.bluetoothweight;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private Spinner mSpinner;
    private Button mBtSearch;
    private Button mBtConnect;
    private Switch mSwitch;

    private BluetoothConnect bluetoothConnect;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<BluetoothDevice> mAdapter;
    private List<BluetoothDevice> mSpinnerList = new ArrayList<BluetoothDevice>();
    private String mMacAdress;
    private int mDevicePosition;
    private ConnectThread connectThread;
    private ListenThread listenThread;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "BlueToothWeight";
    private int BUFFER_SIZE = 1024;
    private TextView text_state;
    private TextView text_msg;

    private IntentFilter filter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_weigth);

        initView();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothConnect = new BluetoothConnect();
        //检测蓝牙状态，自动打开蓝牙
        bluetoothConnect.checkBluetoothStatus(MainActivity.this, mBluetoothAdapter);
        mAdapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_spinner_item, mSpinnerList);
        mSpinner.setAdapter(mAdapter);

        initFilter();
        setClickListener();

        listenThread = new ListenThread();
        listenThread.start();

    }

    private void initView() {
        mSwitch = findViewById(R.id.bluetooth_switch);
        mSpinner = findViewById(R.id.bluetooth_spinner);
        mBtConnect = findViewById(R.id.bluetooth_connect);
        mBtSearch = findViewById(R.id.bluetooth_search);
        text_state = findViewById(R.id.bluetooth_tv_status);
        text_msg = findViewById(R.id.bluetooth_tv_msg);
    }

    private void initFilter() {
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
    }

    //设置监听
    public void setClickListener() {
        //控制蓝牙状态
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
        //搜索蓝牙设备
        mBtSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //检测蓝牙状态
                bluetoothConnect.checkBluetoothStatus(MainActivity.this, mBluetoothAdapter);
                //搜索蓝牙设备
                registerReceiver(mReceiver, filter);
                //添加已配对过的设备
                bluetoothConnect.addBondedDevice(mBluetoothAdapter, mAdapter);
            }
        });
        //蓝牙列表
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("spinner", mSpinnerList.get(position).getAddress());
                mMacAdress = mSpinnerList.get(position).getAddress();
                mDevicePosition = position;
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //连接蓝牙
        mBtConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                final BluetoothDevice device = mAdapter.getItem(mDevicePosition);
                //新线程连接
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        connectDevice(device);
                    }
                }).start();
            }
        });
    }

    //    搜索蓝牙设备
    //    BluetoothAdapter.ACTION_DISCOVERY_STARTED、
    //    BluetoothDevice.ACTION_FOUND、
    //    BluetoothAdapter.ACTION_DISCOVERY_FINISHED。
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("bonded", "11");

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("bonded", "11");

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //避免重复添加已经绑定过的设备，已配对过的跳过
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    Log.d("bonded", device.getAddress());
                    mAdapter.add(device);
                    mAdapter.notifyDataSetChanged();
                } else if (mBluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    Toast.makeText(MainActivity.this, "开始搜索蓝牙设备", Toast.LENGTH_SHORT).show();
                } else if (mBluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Toast.makeText(MainActivity.this, "搜索完毕", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //取消搜索
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        //注销BroadcastReceiver，防止资源泄露
        unregisterReceiver(mReceiver);
    }

    //连接设备
    private void connectDevice(BluetoothDevice device) {

//        text_state.setText("正在连接");

        try {
            //创建Socket
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            //启动连接线程
            connectThread = new ConnectThread(socket, true);
            connectThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ConnectThread extends Thread {

        private BluetoothSocket socket;
        private boolean activeConnect;
        InputStream inputStream;
        OutputStream outputStream;

        private ConnectThread(BluetoothSocket socket, boolean connect) {
            this.socket = socket;
            this.activeConnect = connect;
        }

        @Override
        public void run() {
            try {
                //如果是自动连接 则调用连接方法
                if (activeConnect) {
                    socket.connect();
                }
                text_state.post(new Runnable() {
                    @Override
                    public void run() {
                        text_state.setText("连接状态：连接成功");
                    }
                });
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytes;
                while (true) {
                    //读取数据
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        final byte[] data = new byte[bytes];
                        System.arraycopy(buffer, 0, data, 0, bytes);
                        text_msg.post(new Runnable() {
                            @Override
                            public void run() {
                                text_msg.setText("接收数据：" + new String(data));
                            }
                        });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                text_state.post(new Runnable() {
                    @Override
                    public void run() {
                        text_state.setText("连接状态：接收数据连接错误");
                    }
                });
            }
        }

        //发送数据
        public void sendMsg(final String msg) {

            byte[] bytes = msg.getBytes();
            if (outputStream != null) {
                try {
                    //发送数据
                    outputStream.write(bytes);
                    text_msg.post(new Runnable() {
                        @Override
                        public void run() {
                            text_msg.setText("发送数据：" + msg);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    text_msg.post(new Runnable() {
                        @Override
                        public void run() {
                            text_msg.setText("发送数据错误：" + msg);
                        }
                    });
                }
            }
        }
    }

    //监听线程
    private class ListenThread extends Thread {
        private BluetoothServerSocket bluetoothServerSocket;
        private BluetoothSocket bluetoothSocket;

        @Override
        public void run() {
            super.run();
            try {
                bluetoothServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                while (true) {
                    //线程阻塞，等待别的设备连接
                    bluetoothSocket = bluetoothServerSocket.accept();
                    text_state.post(new Runnable() {
                        @Override
                        public void run() {
                            text_state.setText("连接状态：正在连接");
                        }
                    });

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
