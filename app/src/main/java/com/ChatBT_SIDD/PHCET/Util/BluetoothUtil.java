package com.ChatBT_SIDD.PHCET.Util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.ChatBT_SIDD.PHCET.Bean.BlueToothBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothUtil {

    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    public BluetoothUtil(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean isBluetoothEnable() {
        return bluetoothAdapter.isEnabled();
    }

    public void openBluetooth() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        context.startActivity(intent);
    }

    public void disableBluetooth() {
        bluetoothAdapter.disable();
    }

    public List<BlueToothBean> getDevicesList() {
        List<BlueToothBean> list = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC && device.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.PHONE) {
                    list.add(new BlueToothBean(device.getName(), device.getAddress()));
                }
            }
        }
        return list;
    }

    public void startDiscovery() {

        bluetoothAdapter.startDiscovery();
    }

    public BluetoothDevice getBluetoothDevice(String mac) {
        return bluetoothAdapter.getRemoteDevice(mac);
    }

    public void close(){
        bluetoothAdapter.cancelDiscovery();
    }

}
