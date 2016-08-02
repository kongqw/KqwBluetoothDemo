package kong.qingwei.kqwbluetoothdemo.receiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import kong.qingwei.kqwbluetoothdemo.listener.OnFoundDeviceListener;
import kong.qingwei.kqwbluetoothdemo.utils.ConfigUtil;

/**
 * Created by kqw on 2016/8/2.
 * 蓝牙的广播接收者
 */
public class FoundDeviceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "FoundDeviceBroadcast";
    private static OnFoundDeviceListener mOnFoundDeviceListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // 获取设备
        BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
            if (new ConfigUtil(context).getPairingConfirmation()) {
                // 收到配对请求
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // 同意请求
                    btDevice.setPairingConfirmation(true);
                } else {
                    Log.i(TAG, "onReceive: 4.4.2 以下版本的设备需要通过反射实现自动配对");
                }
            }
        } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // 扫描发现的设备
            if (null != mOnFoundDeviceListener) {
                mOnFoundDeviceListener.foundDevice(btDevice);
            }
        } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            // 扫描附近的蓝牙设备完成
            if (null != mOnFoundDeviceListener) {
                mOnFoundDeviceListener.discoveryFinished();
            }
        } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
            switch (btDevice.getBondState()) {
                case BluetoothDevice.BOND_BONDING:
                    Log.i(TAG, "正在配对......");
                    break;
                case BluetoothDevice.BOND_BONDED:
                    Log.i(TAG, "完成配对");
                    // connect(device);//连接设备
                    break;
                case BluetoothDevice.BOND_NONE:
                    Log.i(TAG, "取消配对");
                default:
                    break;
            }
        }
    }

    public void setOnFoundDeviceListener(OnFoundDeviceListener listener) {
        mOnFoundDeviceListener = listener;
    }
}
