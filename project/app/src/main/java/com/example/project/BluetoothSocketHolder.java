package com.example.project;

import android.bluetooth.BluetoothSocket;

public class BluetoothSocketHolder {
    private static final BluetoothSocketHolder instance = new BluetoothSocketHolder();
    private static BluetoothSocket bluetoothSocket;

    private BluetoothSocketHolder() {
        // Private constructor to prevent instantiation
    }

    public static BluetoothSocketHolder getInstance() {
        return instance;
    }

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    public static void setBluetoothSocket(BluetoothSocket socket) {
        bluetoothSocket = socket;
    }
}

