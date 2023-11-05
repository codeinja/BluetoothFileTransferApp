package com.example.project;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;

public class SendReceive extends AppCompatActivity {
//
//    Button receive = findViewById(R.id.receive);
//    BluetoothSocket socket = BluetoothSocketHolder.getInstance().getBluetoothSocket();
//    byte[] buffer = new byte[1024];
//    int bytesRead;
//    InputStream inputStream;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_receive);

//        BluetoothSocket socket = BluetoothSocketHolder.getInstance().getBluetoothSocket();
//            try {
//                inputStream = socket.getInputStream();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        receive();
//    }
//
//    public void receive(){
//        receive.setOnClickListener(v ->{
//            new ReadDataThread(socket,getApplicationContext()).start();
//        });
    }
}
