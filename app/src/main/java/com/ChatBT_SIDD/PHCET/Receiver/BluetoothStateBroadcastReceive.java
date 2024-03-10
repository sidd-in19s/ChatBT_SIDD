package com.ChatBT_SIDD.PHCET.Receiver;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ChatBT_SIDD.PHCET.CallBack.BlueToothInterface;

public class BluetoothStateBroadcastReceive extends BroadcastReceiver {

    private BlueToothInterface blueToothInterface;

    public BluetoothStateBroadcastReceive(BlueToothInterface blueToothInterface) {
        this.blueToothInterface = blueToothInterface;
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        switch (action) {
            case BluetoothDevice.ACTION_FOUND:
                Log.i("ChatBT_SIDD bluetooth device", "scan to device" + device.getName());
                blueToothInterface.getBlueToothDevices(device);
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                Log.i("ChatBT_SIDD bluetooth device", "search complete");
                blueToothInterface.searchFinish();
                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                Log.i("ChatBT_SIDD bluetooth device", device.getName() + "connected");
                blueToothInterface.getConnectedBlueToothDevices(device);
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                Log.i("ChatBT_SIDD bluetooth device", device.getName() + "disconnected");
                blueToothInterface.getDisConnectedBlueToothDevices(device);
                break;
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch (blueState) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i("ChatBT_SIDD bluetooth device", "bluetooth is off");
                        blueToothInterface.disable();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i("ChatBT_SIDD bluetooth device", "bluetooth is on");
                        blueToothInterface.open();
                        break;
                }
                break;
        }
    }
}
