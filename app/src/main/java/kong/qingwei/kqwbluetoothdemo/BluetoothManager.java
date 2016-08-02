package kong.qingwei.kqwbluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.Set;

import kong.qingwei.kqwbluetoothdemo.listener.OnFoundDeviceListener;
import kong.qingwei.kqwbluetoothdemo.receiver.FoundDeviceBroadcastReceiver;

/**
 * Created by kqw on 2016/7/27.
 * 机器人端蓝牙管理器
 */
public class BluetoothManager {

    private Activity mActivity;
    private static final String TAG = "BluetoothManager";
    private final BluetoothAdapter mBluetoothAdapter;
    private FoundDeviceBroadcastReceiver mFoundDeviceBroadcastReceiver;
    private final IntentFilter mFilter;

    public BluetoothManager(Activity activity) {
        mActivity = activity;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 获取设备的广播接收者
        mFoundDeviceBroadcastReceiver = new FoundDeviceBroadcastReceiver();

        // 注册receiver监听
        mFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    }

    /**
     * 注册搜索蓝牙设备的广播接收者
     */
    public void registerFoundDeviceReceiver() {
        mActivity.registerReceiver(mFoundDeviceBroadcastReceiver, mFilter);
    }

    /**
     * 反注册搜索蓝牙设备的广播接收者
     */
    public void unregisterReceiver() {
        mActivity.unregisterReceiver(mFoundDeviceBroadcastReceiver);
    }

    /**
     * 开启蓝牙
     */
    public void openBluetooth() {
        try {
            mBluetoothAdapter.enable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭蓝牙
     */
    public void closeBluetooth() {
        try {
            mBluetoothAdapter.disable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置设备可见
     * 0 ~ 120
     */
    public void setDuration() {
        Intent duration = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        duration.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
        mActivity.startActivity(duration);
    }


    /**
     * 开始扫描设备
     */
    public void startDiscovery() {
        Log.i(TAG, "startDiscovery: ");
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        } else {
//            // 获取已经配对过的设备
//            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
//            if (pairedDevices.size() > 0) {
// TODO
//            }
            // 开始扫描设备
            mBluetoothAdapter.startDiscovery();
        }
    }

    /**
     * 添加搜索蓝牙设备的回调
     *
     * @param listener 回调接口
     */
    public void setOnFoundDeviceListener(OnFoundDeviceListener listener) {
        mFoundDeviceBroadcastReceiver.setOnFoundDeviceListener(listener);
    }

    public Set<BluetoothDevice> getBondedDevices() {
        return mBluetoothAdapter.getBondedDevices();
    }
}
