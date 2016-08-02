package kong.qingwei.kqwbluetoothdemo.listener;

import android.bluetooth.BluetoothDevice;

/**
 * Created by kqw on 2016/8/2.
 * 蓝牙连接的回调
 */
public interface OnConnectListener {

    // 开始连接
    void connectionStart();

    // 蓝牙连接成功
    void connectionSuccess(BluetoothDevice device);

    // 蓝牙连接失败
    void connectionFailed();

    // 连接失效 连接丢失
    void connectionLost();
}
