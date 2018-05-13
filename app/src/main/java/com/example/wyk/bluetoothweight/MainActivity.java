package com.example.wyk.bluetoothweight;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Spinner mSpinner;
    private Button mBtSearch;
    private Button mBtConnect;
    private Switch mSwitch;
    private TextView text_state;
    private TextView text_msg;
    private TextView text_send_msg;
    private Button mBtForward;
    private Button mBtBack;
    private Button mBtStop;
    private Button mBtLeft;
    private Button mBtRight;
    private Button mBtSlow;
    private Button mBtMiddle;
    private Button mBtFast;

    private BluetoothConnect bluetoothConnect;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private ArrayAdapter<BluetoothDevice> mAdapter;
    private List<BluetoothDevice> mSpinnerList = new ArrayList<BluetoothDevice>();
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private ConnectThread connectThread;
    private ListenThread listenThread;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "BlueToothWeight";
    private int mDevicePosition;
    private int BUFFER_SIZE = 1024;

    private IntentFilter filter;
//    filter.addAction("android.intent.action.bluetooth.admin.bluetooth");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_car);
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
        text_send_msg = findViewById(R.id.bluetooth_tv_send_msg);

        mBtForward = findViewById(R.id.bluetooth_forward);
        mBtBack = findViewById(R.id.bluetooth_backward);
        mBtLeft = findViewById(R.id.bluetooth_left);
        mBtRight = findViewById(R.id.bluetooth_right);
        mBtStop = findViewById(R.id.bluetooth_stop);
        mBtSlow = findViewById(R.id.bluetooth_slow_speed);
        mBtMiddle = findViewById(R.id.bluetooth_middle_speed);
        mBtFast = findViewById(R.id.bluetooth_fast_speed);

        mBtConnect.setOnClickListener(this);
        mBtSearch.setOnClickListener(this);
        mBtForward.setOnClickListener(this);
        mBtBack.setOnClickListener(this);
        mBtLeft.setOnClickListener(this);
        mBtRight.setOnClickListener(this);
        mBtStop.setOnClickListener(this);
        mBtFast.setOnClickListener(this);
        mBtMiddle.setOnClickListener(this);
        mBtSlow.setOnClickListener(this);
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
        //蓝牙列表
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("spinner", mSpinnerList.get(position).getAddress());
                mDevicePosition = position;
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    //按钮点击事件监听
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //搜索蓝牙设备
            case R.id.bluetooth_search:
                Log.d("click", "search");
                //检测蓝牙状态
                bluetoothConnect.checkBluetoothStatus(MainActivity.this, mBluetoothAdapter);
                //搜索蓝牙设备
                registerReceiver(mReceiver, filter);
                //添加已配对过的设备
                bluetoothConnect.addBondedDevice(mBluetoothAdapter, mAdapter);
                break;
            //连接蓝牙设备
            case R.id.bluetooth_connect:
                Log.d("click", "connect");
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                if (mAdapter.getCount() > 0) {
                    final BluetoothDevice device = mAdapter.getItem(mDevicePosition);
                    //新线程连接
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            connectDevice(device);
                        }
                    }).start();
                }

                break;
            case R.id.bluetooth_forward:
                sendMsg("ONA");
                break;
            case R.id.bluetooth_backward:
                sendMsg("ONB");
                break;
            case R.id.bluetooth_left:
                sendMsg("ONC");
                break;
            case R.id.bluetooth_right:
                sendMsg("OND");
                break;
            case R.id.bluetooth_stop:
                Log.d("click", "stop");
                sendMsg("ONF");
                break;
            case R.id.bluetooth_slow_speed:
                sendMsg("ON1");
                break;
            case R.id.bluetooth_middle_speed:
                sendMsg("ON2");
                break;
            case R.id.bluetooth_fast_speed:
                sendMsg("ON3");
                break;
            default:
                break;

        }
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
            mBluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            //启动连接线程
            connectThread = new ConnectThread(mBluetoothSocket, true);
            connectThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class ConnectThread extends Thread {
        private String str = null;
        private boolean start = false;
        int len = 0;


        private BluetoothSocket socket;
        private boolean activeConnect;

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
                mInputStream = socket.getInputStream();
                mOutputStream = socket.getOutputStream();
//                byte[] buffer = new byte[BUFFER_SIZE];
//                int bytes;

                while (true) {
                    byte[] buff = new byte[1];
                    len = mInputStream.read(buff);
                    String strBuff = new String(buff);


                    if (strBuff.equals("$")) {
                        start = true;
                        str = "";
                    }
                    if (start == true) {
                        str += strBuff;

                    }
                    if (strBuff.equals("#")) {
                        start = false;

                        text_msg.post(new Runnable() {
                            @Override
                            public void run() {
                                text_msg.setText("接收数据：" + new String(str));
                                Log.d("message", "receive:" + new String(str));
                            }
                        });
                    }
                    //读取数据
//                try {
//                    bytes = mInputStream.read(buffer);
//                    if (bytes > 0) {
//                        final byte[] data = new byte[bytes];
//                        System.arraycopy(buffer, 0, data, 0, bytes);
//                        text_msg.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                text_msg.setText("接收数据：" + new String(data));
//                                Log.d("aaaa", "receive:" + new String(data));
//                            }
//                        });
//                    }
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
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

    }



    //发送数据
    public void sendMsg(final String msg) {
        try {
            mOutputStream = mBluetoothSocket.getOutputStream();
            byte[] bytes = msg.getBytes();
            if (mOutputStream != null) {
                try {
                    //发送数据
                    mOutputStream.write(bytes);
                    text_send_msg.post(new Runnable() {
                        @Override
                        public void run() {
                            text_send_msg.setText("发送数据：" + msg);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    text_msg.post(new Runnable() {
                        @Override
                        public void run() {
                            text_send_msg.setText("发送数据错误：" + msg);
                        }
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
