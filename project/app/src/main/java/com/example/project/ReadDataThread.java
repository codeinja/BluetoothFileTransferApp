package com.example.project;

import static android.app.ProgressDialog.show;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

public class ReadDataThread extends Thread {
    private BluetoothSocket socket;
    Context context;

    public ReadDataThread(BluetoothSocket socket, Context applicationContext) {
        this.socket = socket;
        this.context = applicationContext;
    }

    @Override
    public void run() {
        try {
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;

            while (true) {
                bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) {
                    showMessage("Read failed: End of stream or error");
                    break;
                }

                String receivedData = new String(buffer, 0, bytesRead);
                // Process the received data (e.g., display it in your app)

                Log.d("Received Data", receivedData);
            }
        } catch (IOException e) {
            showMessage("Read failed: " + e.getMessage());
        }
    }
    public void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
