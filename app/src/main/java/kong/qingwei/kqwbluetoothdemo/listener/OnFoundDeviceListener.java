package kong.qingwei.kqwbluetoothdemo.listener;

import android.bluetooth.BluetoothDevice;

/**
 * Created by kqw on 2016/8/2.
 * 扫描到蓝牙设备的回调
 */
public interface OnFoundDeviceListener {

    // 扫描到设备
    void foundDevice(BluetoothDevice device);

    // 扫描完成
    void discoveryFinished();
}
