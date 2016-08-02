package kong.qingwei.kqwbluetoothdemo.adapter;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import kong.qingwei.kqwbluetoothdemo.R;

/**
 * Created by kqw on 2016/8/2.
 * 扫描到蓝牙设备列表的数据适配器
 */
public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {

    private ArrayList<BluetoothDevice> devices;

    // RecyclerView.ViewHolder
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView deviceName;
        private final TextView deviceMacAddress;

        public ViewHolder(View v) {
            super(v);
            deviceName = (TextView) v.findViewById(R.id.device_name);
            deviceMacAddress = (TextView) v.findViewById(R.id.device_mac_address);
        }
    }

    // 初始化
    public DeviceListAdapter() {
        devices = new ArrayList<>();
    }

    // 清空列表
    public void clearDevice() {
        devices.clear();
        notifyDataSetChanged();
    }

    // 添加设备
    public void addDevice(BluetoothDevice device) {
        devices.add(device);
        notifyDataSetChanged();
    }

    /**
     * 获取设备MAC地址
     *
     * @param position 第几个设备
     * @return MAC地址
     */
    public String getMacAddress(int position) {
        try {
            return devices.get(position).getAddress();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 用来创建新视图（由布局管理器调用）
    @Override
    public DeviceListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false));
    }

    // 用来替换视图的内容（由布局管理器调用）
    @Override
    public void onBindViewHolder(DeviceListAdapter.ViewHolder holder, int position) {
        holder.deviceName.setText(devices.get(position).getName());
        holder.deviceMacAddress.setText(devices.get(position).getAddress());
    }

    // 返回数据集的大小（由布局管理器调用）
    @Override
    public int getItemCount() {
        return devices.size();
    }
}
