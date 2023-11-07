package com.example.project;

import static com.example.project.BluetoothSocketHolder.setBluetoothSocket;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button buttonON;
    BluetoothAdapter mybt;
    ActivityResultLauncher<Intent> bluetoothEnableLauncher;
    ListView deviceListView;
    ArrayAdapter<String> deviceListAdapter;
    ListView discoveredListView;
    ArrayAdapter<String> discoveredListAdapter;
    BluetoothDevice selectedDevice;

    private static final int REQUEST_BLUETOOTH_PERMISSION = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mybt = BluetoothAdapter.getDefaultAdapter();

        buttonON = findViewById(R.id.btON);

        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        bluetoothEnableLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        showMessage("Bluetooth was ENABLED");

                        if (mybt.isEnabled()) {
                            buttonON.setText("Scan");
                            buttonON.setBackgroundColor(Color.rgb(44, 225, 180));
                            buttonON.setTextColor(Color.rgb(255, 255, 255));
                        }
                    } else {
                        showMessage("Error occurred while enabling Bluetooth");
                    }
                });

        bluetoothON();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();

                discoveredListAdapter.add("\nDevice Name: " + deviceName + "\nMAC Address: " + deviceHardwareAddress + "\n");
            }
        }
    };

    private void bluetoothON() {
        buttonON.setOnClickListener(v -> {
            requestBluetoothPermission();
            if (mybt == null) {
                showMessage("Bluetooth is NOT supported on this device");
            } else if (!mybt.isEnabled()) {
                bluetoothEnableLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            } else if (mybt.isEnabled()) {
                deviceListView = findViewById(R.id.deviceListView);
                deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
                deviceListView.setAdapter(deviceListAdapter);

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                Set<BluetoothDevice> pairedDevices = mybt.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress();
                        deviceListAdapter.add("\nDevice Name: " + deviceName + "\nMAC Address: " + deviceHardwareAddress + "\n");
                    }
                }

                buttonON.setBackgroundColor(Color.rgb(44, 225, 180));
                buttonON.setTextColor(Color.rgb(255, 255, 255));

                buttonON.setText("Scan");
                startDiscovery();
            } else {
                buttonON.setText("Scan");
                startDiscovery();
            }
        });
    }

    private void startDiscovery() {

        discoveredListView = findViewById(R.id.discoveredDevicesView);
        discoveredListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        discoveredListView.setAdapter(discoveredListAdapter);

        if (mybt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (mybt.startDiscovery()) {
                showMessage("Bluetooth discovery started");
            } else {
                showMessage("Failed to start Bluetooth discovery");
            }
        }

        discoveredListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDevice = (String) parent.getItemAtPosition(position);
            String deviceName = selectedDevice.substring(13);
            int start = deviceName.indexOf(":");
            int end = deviceName.lastIndexOf(":");
            String macAddress = deviceName.substring(start + 2, end + 3);

            BluetoothDevice deviceToPair = mybt.getRemoteDevice(macAddress);

            if (deviceToPair.getBondState() != BluetoothDevice.BOND_BONDED) {
                try {
                    createBond(deviceToPair);
                    Toast.makeText(getApplicationContext(), "Pairing with " + macAddress, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                connectToDevice(deviceToPair);
            }
        });
    }

    public boolean createBond(BluetoothDevice btDevice)
            throws Exception {
        Class btdevice = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = btdevice.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        if (mybt.isDiscovering()) {
            mybt.cancelDiscovery();
        }

        UUID MY_UUID = UUID.fromString("00001105-0000-1000-8000-00805F9B34FB");

        Thread connectionThread = new Thread(() -> {
            BluetoothSocket socket = null;

            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                setBluetoothSocket(socket);
                socket.connect();
                Log.d("Bluetooth", "Connection established");

                // If the connection is successful, start a new activity
                runOnUiThread(() -> {
                    showMessage("Connected to " + device.getName());
                    Intent intent = new Intent(getApplicationContext(), SendReceive.class);
                    startActivity(intent);
                });

            } catch (IOException e) {
                Log.e("Bluetooth", "Connection failed: " + e.getMessage());
                try {
                    socket.close();
                } catch (IOException closeException) {
                    Log.e("Bluetooth", "Failed to close the socket: " + closeException.getMessage());
                }
            }
        });

        connectionThread.start();
    }

    private void requestBluetoothPermission() {
        if (!hasBluetoothPermissions()) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, REQUEST_BLUETOOTH_PERMISSION);
        }
    }

    private boolean hasBluetoothPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    public void showMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
