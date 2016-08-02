# Android蓝牙通信
## 效果图
> 两台真机设备

![G1](http://img.blog.csdn.net/20160802162530186)

## 源码

[GitHub](https://github.com/kongqw/KqwBluetoothDemo)


 * 关于蓝牙的**开关控制**，设置**设备可见**、**搜索附近的蓝牙设备**，已经封装到了 [BluetoothManager](https://github.com/kongqw/KqwBluetoothDemo/blob/master/app/src/main/java/kong/qingwei/kqwbluetoothdemo/BluetoothManager.java)
 类

 * 关于**设备的连接**、**通信**。已经封装到了 [BluetoothService](https://github.com/kongqw/KqwBluetoothDemo/blob/master/app/src/main/java/kong/qingwei/kqwbluetoothdemo/BluetoothService.java) 类


**注：下面的全部内容，主要是思路，具体的可以参考上面的源码，如果对你有帮助记得给个赞哦。**

## 权限

```
<!-- 蓝牙的权限 -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
```

## 蓝牙的打开与关闭

### 开启蓝牙


```
mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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
```

### 关闭蓝牙

```
mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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
```


## 设置蓝牙设备可见
> 设置设备可见对于服务端是必须的，客户端设不设置无所谓。
>
> 如果服务端不可见，配对过的设备也搜索到并可以连接上，但是不能通信，没有配对过的设备连搜索都搜索不到。
>
> 可见时间的取值范围是0到120，单位是秒，0表示永久可见（在小米手机上测试设置0是不起效果的，并没有永久可见，可能是个例）。

```
/**
 * 设置设备可见
 * 0 ~ 120
 */
public void setDuration() {
    Intent duration = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    duration.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
    mActivity.startActivity(duration);
}
```


## 扫描附近的蓝牙设备
> 扫描附近设备的设备，需要注册一个广播接收者，来接收扫描到的结果。

> **需要注意的是，接收扫描结果的广播接收者必须使用动态注册，不能在清单文件里注册！**

### 注册搜索蓝牙设备的广播接收者

```
// 获取设备的广播接收者
FoundDeviceBroadcastReceiver mFoundDeviceBroadcastReceiver = new FoundDeviceBroadcastReceiver();

// 注册receiver监听
IntentFilter mFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

/**
 * 注册搜索蓝牙设备的广播接收者
 */
public void registerFoundDeviceReceiver() {
    mActivity.registerReceiver(mFoundDeviceBroadcastReceiver, mFilter);
}
```

### 反注册搜索蓝牙设备的广播接收者

```
// 获取设备的广播接收者
FoundDeviceBroadcastReceiver mFoundDeviceBroadcastReceiver = new FoundDeviceBroadcastReceiver();

/**
 * 反注册搜索蓝牙设备的广播接收者
 */
public void unregisterReceiver() {
    mActivity.unregisterReceiver(mFoundDeviceBroadcastReceiver);
}
```

### 广播接收者

```
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

        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // 扫描发现的设备
            if (null != mOnFoundDeviceListener) {
                mOnFoundDeviceListener.foundDevice(btDevice);
            }
        }
        ……
    }

    public void setOnFoundDeviceListener(OnFoundDeviceListener listener) {
        mOnFoundDeviceListener = listener;
    }
}
```

### 开始扫描附近的蓝牙设备

```
/**
 * 开始扫描设备
 */
public void startDiscovery() {
    Log.i(TAG, "startDiscovery: ");
    if (mBluetoothAdapter.isDiscovering()) {
        mBluetoothAdapter.cancelDiscovery();
    } else {
		// TODO 这里可以先获取已经配对的设备
		// Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // 开始扫描设备
        mBluetoothAdapter.startDiscovery();
    }
}
```


### 获取已经配对的设备
> 扫描附近的蓝牙设备是一个很消耗性能的操作，在扫描之前，可以先获取已经配对过的设备，如果已经配对过，就不用再扫描了。

```
BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
```


## 蓝牙连接

> 获取到附近的设备以后，就可以通过MAC地址进行配对连接了。

### 配对
> 没有配对过的设备，在连接之前是需要配对的，配对成功才可以连接、通信。
>
> 配对可以手动点击，根据配对码进行配对，也可以设置自动配对。手动配对没什么好说的，这里介绍自动配对

> 还是用到上面的蓝牙广播接收者，我们在清单文件里添加Action

```
<!-- 蓝牙广播接收者 -->
<receiver android:name=".receiver.FoundDeviceBroadcastReceiver">
    <intent-filter>
        <!-- 添加配对请求 -->
        <action android:name="android.bluetooth.device.action.PAIRING_REQUEST" />
    </intent-filter>
</receiver>
```

> 注意：这里的自动配对仅支持4.4.2以上系统，以下的版本想要实现需要用到反射

```
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
        }
        ……
    }
}
```

### 连接

#### 服务端等待连接
> 服务端开启连接，需要开启一个阻塞线程，等待客户端的连接，类似这样

```
try {
       // 等待客户端连接 阻塞线程 连接成功继续向下执行 连接失败抛异常
       socket = mmServerSocket.accept();
   } catch (IOException e) {
       Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
       break;
   }
```


#### 客户端发起连
> 客户端发起连接，如果没有配对过，需要先进行配对，连接同样是一个阻塞线程，连接成功会继续向下执行，连接失败会抛异常，类似这样

```
try {
    ……
    // 开始连接 阻塞线程 连接成功继续执行 连接失败抛异常
    mmSocket.connect();
} catch (IOException e) {
    // 连接失败
    e.printStackTrace();
    try {
        mmSocket.close();
    } catch (IOException e2) {
        Log.e(TAG, "unable to close() " + mSocketType + " socket during connection failure", e2);
    }
    ……
}
```

## 通信

### 接收数据
> 连接成功以后，需要开启一个线程，一直循环读取数据流，类似这样

```
// 只有蓝牙处于连接状态就一直循环读取数据
while (mState == STATE_CONNECTED) {
    try {
        // Read from the InputStream
        bytes = mmInStream.read(buffer);

        // 读取到数据的回调
        ……
    } catch (IOException e) {
        // 读取数据出现异常
        Log.e(TAG, "disconnected", e);
        ……
    }
}
```


### 发送数据

```
/**
 * 发数据
 *
 * @param buffer 发送内容
 */
public void write(final byte[] buffer) {
    try {
        mmOutStream.write(buffer);
        // 发送数据的回调
        ……
    } catch (IOException e) {
        // 发送数据出现失败
        Log.e(TAG, "Exception during write", e);
    }
}
```



## 坑

有时候当你重复的连接、断开、连接、断开……

你会发现出现连接失败，或者可以连接成功，但是不能通信了，这个时候你要考虑你得服务端是不是已经不可见了。

上面已经提过，如果两个设备已经配对，即使服务端是不可见的，也同样可以搜索到并连接上，但是是不能通信的。

而如果两个设备没有配对过，是连搜索都搜索不到服务端的。

当然，如果在服务端可见的时候，连接上就是连接上了，只要连接不断开，即使连接上以后服务端变为不可见了，也一样可以一直通信。
