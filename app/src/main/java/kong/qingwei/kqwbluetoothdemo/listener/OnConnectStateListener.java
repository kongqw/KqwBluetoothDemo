package kong.qingwei.kqwbluetoothdemo.listener;

/**
 * Created by kqw on 2016/8/2.
 * 蓝牙连接的回调
 */
public interface OnConnectStateListener {

    // 没有连接
    void stateNone();

    // 等待连接
    void stateListening();

    // 正在连接
    void stateConnecting();

    // 连接成功
    void stateConnected();
}
