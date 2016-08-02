package kong.qingwei.kqwbluetoothdemo.listener;

/**
 * Created by kqw on 2016/8/2.
 * 蓝牙消息传递的回调
 */
public interface OnMessageListener {

    // 发送消息
    void write(String message);

    // 接收消息
    void read(String message);
}
