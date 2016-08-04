package kong.qingwei.kqwbluetoothdemo;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import kong.qingwei.kqwbluetoothdemo.listener.OnConnectListener;
import kong.qingwei.kqwbluetoothdemo.listener.OnConnectStateListener;
import kong.qingwei.kqwbluetoothdemo.listener.OnMessageListener;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    private ProgressDialog progressDialog;

    public static final String TYPE = "type";
    public static final String MAC_ADDRESS = "address";
    // 服务端
    public static final int TYPE_SERVICE = 0;
    // 客户端
    public static final int TYPE_CLIENT = 1;
    // 未知
    public static final int TYPE_UNKNOWN = 2;

    private BluetoothService mBluetoothService;
    private Button mSendButton;
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        // 发送按钮
        mSendButton = (Button) findViewById(R.id.send);
        if (mSendButton != null) {
            mSendButton.setOnClickListener(this);
        }
        // 编辑框
        mEditText = (EditText) findViewById(R.id.editText);

        // 蓝牙连接 消息传递的类
        mBluetoothService = new BluetoothService(this);

        // 连接状态变化
        mBluetoothService.setOnConnectStateListener(new OnConnectStateListener() {
            @Override
            public void stateNone() {
                cancelProgressDialog();
            }

            @Override
            public void stateListening() {
                buildProgressDialog("等待客户端连接");
            }

            @Override
            public void stateConnecting() {
                buildProgressDialog("正在连接");
            }

            @Override
            public void stateConnected() {
                cancelProgressDialog();
            }
        });

        // 连接监听
        mBluetoothService.setOnConnectListener(new OnConnectListener() {
            @Override
            public void connectionStart() {
                buildProgressDialog("正在连接");
            }

            @Override
            public void connectionSuccess(BluetoothDevice device) {
                cancelProgressDialog();
                // 设置发送按钮可用
                mSendButton.setEnabled(true);
                mEditText.setEnabled(true);
                // Toast.makeText(ChatActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void connectionFailed() {
                cancelProgressDialog();
                finish();
                Toast.makeText(ChatActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void connectionLost() {
                cancelProgressDialog();
                finish();
                Toast.makeText(ChatActivity.this, "连接丢失", Toast.LENGTH_SHORT).show();
            }
        });

        // 消息传递的监听
        mBluetoothService.setOnMessageListener(new OnMessageListener() {
            @Override
            public void write(String message) {
                Toast.makeText(ChatActivity.this, "发送了消息:" + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void read(String message) {
                Toast.makeText(ChatActivity.this, "接收到了消息:" + message, Toast.LENGTH_SHORT).show();
            }
        });

        int type = getIntent().getIntExtra(TYPE, TYPE_UNKNOWN);
        if (TYPE_SERVICE == type) { // 服务端
            // 服务端设置设备可见
            new BluetoothManager(this).setDuration();
            // 等待连接
            mBluetoothService.start();

        } else if (TYPE_CLIENT == type) { // 客户端
            // 获取MAC地址
            String macAddress = getIntent().getStringExtra(MAC_ADDRESS);
            if (TextUtils.isEmpty(macAddress)) {
                finish();
            } else {
                // 连接设备
                BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
                mBluetoothService.connect(device, true);
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothService.stop();
    }

    /**
     * 显示加载框
     */
    public void buildProgressDialog(String text) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        progressDialog.setMessage(text);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    /**
     * 关闭下载框
     */
    public void cancelProgressDialog() {
        if (null != progressDialog) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

    /**
     * 点击事件
     *
     * @param v v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send:
                // 发送消息
                String message = mEditText.getText().toString();
                if (TextUtils.isEmpty(message)) {
                    Toast.makeText(this, "消息不能为空", Toast.LENGTH_SHORT).show();
                } else {
                    // 发送消息
                    byte[] send = message.getBytes();
                    mBluetoothService.write(send);
                    mEditText.setText(null);
                }
                break;
            default:
                break;
        }
    }
}
