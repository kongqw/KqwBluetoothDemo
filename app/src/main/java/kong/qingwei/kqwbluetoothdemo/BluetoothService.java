package kong.qingwei.kqwbluetoothdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import kong.qingwei.kqwbluetoothdemo.listener.OnConnectListener;
import kong.qingwei.kqwbluetoothdemo.listener.OnConnectStateListener;
import kong.qingwei.kqwbluetoothdemo.listener.OnMessageListener;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothService {
    // Debugging
    private static final String TAG = "BluetoothService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    //    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // 什么都没做
    public static final int STATE_LISTEN = 1;     // 服务端等待客户端连接
    public static final int STATE_CONNECTING = 2; // 正在连接
    public static final int STATE_CONNECTED = 3;  // 已经连接

    public OnConnectStateListener mOnConnectStateListener;
    public OnConnectListener mOnConnectListener;
    public OnMessageListener mOnMessageListener;
    public Activity mActivity;

    /**
     * 构造方法
     *
     * @param activity Context
     *                 // @param handler handler
     */
    public BluetoothService(Activity activity/*, Handler handler*/) {
        mActivity = activity;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
//        mHandler = handler;
    }

    /**
     * 设置蓝牙连接状态
     *
     * @param state 状态
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        // mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();

        // 连接状态变化的回调
        if (null != mOnConnectStateListener) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (mState) {
                        case STATE_NONE:
                            mOnConnectStateListener.stateNone();
                            break;
                        case STATE_LISTEN:
                            mOnConnectStateListener.stateListening();
                            break;
                        case STATE_CONNECTING:
                            mOnConnectStateListener.stateConnecting();
                            break;
                        case STATE_CONNECTED:
                            mOnConnectStateListener.stateConnected();
                            break;
                    }
                }
            });
        }
    }

    /**
     * 返回蓝牙连接状态
     *
     * @return 连接状态
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // 取消之前的连接线程
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // 取消之前的消息收发线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // 设置蓝牙状态为等待连接
        setState(STATE_LISTEN);

        // 服务端开启等待连接线程
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }
    }

    /**
     * 连接蓝牙设备
     *
     * @param device 蓝牙设备
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);

        // 取消之前的连接线程
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // 取消之前的消息收发线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // 客户端发起连接请求
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();

        // 修改蓝牙状态为正在连接
        setState(STATE_CONNECTING);
    }

    /**
     * 蓝牙连接成功以后开启消息传递的线程
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, final BluetoothDevice device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // 关闭客户端的连接线程
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // 关闭消息收发的线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // 关闭客户端等待连接线程
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // 开启新的消息收发线程
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
//        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
//        Bundle bundle = new Bundle();
//        bundle.putString(Constants.DEVICE_NAME, device.getName());
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);

        if (null != mOnConnectListener) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mOnConnectListener.connectionSuccess(device);
                }
            });
        }

        // 设置蓝牙状态为已经连接
        setState(STATE_CONNECTED);
    }

    /**
     * 关闭所有线程
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        // 关闭客户端连接线程
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // 关闭消息收发线程
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // 关闭服务端等待连接线程
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // 设置蓝牙为断开连接状态
        setState(STATE_NONE);
    }

    /**
     * 发送消息
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    /**
     * 连接失败
     */
    private void connectionFailed() {
        if (null != mOnConnectListener) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mOnConnectListener.connectionFailed();
                }
            });
        }

//        // Send a failure message back to the Activity
//        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
//        Bundle bundle = new Bundle();
//        bundle.putString(Constants.TOAST, "Unable to connect device");
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        // BluetoothService.this.start();
    }

    /**
     * 连接失效
     */
    private void connectionLost() {
        if (null != mOnConnectListener) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mOnConnectListener.connectionLost();
                }
            });
        }
//        // Send a failure message back to the Activity
//        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
//        Bundle bundle = new Bundle();
//        bundle.putString(Constants.TOAST, "Device connection was lost");
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        // BluetoothService.this.start();
    }

    /**
     * 服务端等待连接的线程
     */
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(), mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);
        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * 客户端发起连接请求的线程
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                if (null != mOnConnectListener) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mOnConnectListener.connectionStart();
                        }
                    });
                }
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                e.printStackTrace();
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType + " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * 蓝牙连接成功以后消息传递的线程
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            final byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    // mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    if (null != mOnMessageListener) {
                        final int finalBytes = bytes;
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String readMessage = new String(buffer, 0, finalBytes);
                                mOnMessageListener.read(readMessage);
                            }
                        });
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    // BluetoothService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(final byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                // mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
                if (null != mOnMessageListener) {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String writeMessage = new String(buffer);
                            mOnMessageListener.write(writeMessage);
                        }
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


    /**
     * 添加蓝牙连接状态变化的监听
     *
     * @param listener 回掉接口
     */
    public void setOnConnectStateListener(OnConnectStateListener listener) {
        mOnConnectStateListener = listener;
    }

    /**
     * 添加连接蓝牙的回调
     *
     * @param listener 回调接口
     */
    public void setOnConnectListener(OnConnectListener listener) {
        mOnConnectListener = listener;
    }

    /**
     * 添加消息传递的回调
     *
     * @param listener 回调接口
     */
    public void setOnMessageListener(OnMessageListener listener) {
        mOnMessageListener = listener;
    }
}
