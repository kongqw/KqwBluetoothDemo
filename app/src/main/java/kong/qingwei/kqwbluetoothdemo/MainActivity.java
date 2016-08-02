package kong.qingwei.kqwbluetoothdemo;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.w3c.dom.Text;

import kong.qingwei.kqwbluetoothdemo.adapter.DeviceListAdapter;
import kong.qingwei.kqwbluetoothdemo.listener.OnFoundDeviceListener;
import kong.qingwei.kqwbluetoothdemo.utils.ConfigUtil;
import kong.qingwei.kqwbluetoothdemo.view.KqwRecyclerView;

public class MainActivity extends AppCompatActivity implements OnFoundDeviceListener, KqwRecyclerView.OnItemClickListener {

    private static final String TAG = "MainActivity";
    private BluetoothManager mBluetoothManager;
    private DeviceListAdapter mAdapter;
    // 加载框
    private ProgressDialog progressDialog;
    private ConfigUtil mConfigUtil;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 蓝牙管理对象
        mBluetoothManager = new BluetoothManager(this);
        // 添加扫描结果的回调
        mBluetoothManager.setOnFoundDeviceListener(this);
        // 注册接收扫描设备结果的广播接收者
        mBluetoothManager.registerFoundDeviceReceiver();
        // 配置工具类
        mConfigUtil = new ConfigUtil(this);

        // 初始化列表
        KqwRecyclerView recyclerView = (KqwRecyclerView) findViewById(R.id.recyclerView);
        if (recyclerView != null) {
            // 如果数据的填充不会改变RecyclerView的布局大小，那么这个设置可以提高RecyclerView的性能
            recyclerView.setHasFixedSize(true);
            // 设置这个RecyclerView是线性布局
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setOnItemClickListener(this);
            mAdapter = new DeviceListAdapter();
            recyclerView.setAdapter(mAdapter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 反注册接收扫描设备结果的广播接收者
        mBluetoothManager.unregisterReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_open_bluetooth) {
            // 打开蓝牙
            mBluetoothManager.openBluetooth();
            return true;
        } else if (id == R.id.action_duration) {
            // 进入聊天页面
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.TYPE, ChatActivity.TYPE_SERVICE);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_found_device) {
            // 客户端扫描附近设备
            // 清空列表
            mAdapter.clearDevice();
            // 扫描
            mBluetoothManager.startDiscovery();
            // Loading
            buildProgressDialog("正在扫描附近的蓝牙设备...");
            return true;
        } else if (id == R.id.action_close_bluetooth) {
            // 关闭蓝牙
            mBluetoothManager.closeBluetooth();
            return true;
        } else if (id == R.id.action_pairing_confirmation) {
            // 自动配对
            boolean b = mConfigUtil.getPairingConfirmation();
            mConfigUtil.setPairingConfirmation(!b);
            Toast.makeText(this, !b ? "开启自动配对" : "关闭自动配对", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 扫描附近的设备回调
     *
     * @param device 设备信息
     */
    @Override
    public void foundDevice(BluetoothDevice device) {
        Log.i(TAG, "foundDevice: " + device.getAddress());
        mAdapter.addDevice(device);
    }

    /**
     * 扫描附近的设备完成
     */
    @Override
    public void discoveryFinished() {
        Log.i(TAG, "discoveryFinished: ");
        cancelProgressDialog();
    }

    /**
     * RecyclerView Item 被点击
     *
     * @param v v
     */
    @Override
    public void onItemClick(RecyclerView.ViewHolder v) {
        String macAddress = mAdapter.getMacAddress(v.getAdapterPosition());
        if (!TextUtils.isEmpty(macAddress)) {
            Toast.makeText(this, "Address : " + macAddress, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.TYPE, ChatActivity.TYPE_CLIENT);
            intent.putExtra(ChatActivity.MAC_ADDRESS, macAddress);
            startActivity(intent);
        }
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
//        progressDialog.setCancelable(false);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    /**
     * 关闭下载框
     */
    public void cancelProgressDialog() {
        if (progressDialog != null)
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
    }
}
