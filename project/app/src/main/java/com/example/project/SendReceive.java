package com.example.project;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SendReceive extends AppCompatActivity {

    Button sendbt;
    Button receivebt;
    TextView txtResult;
    InputStream inputStream;
    OutputStream outputStream;
    BluetoothSocket socket = BluetoothSocketHolder.getInstance().getBluetoothSocket();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_receive);

        txtResult = findViewById(R.id.pathDisplay);
        sendbt = findViewById(R.id.sendbutton);
        receivebt = findViewById(R.id.receivebutton);
        if(socket!=null && socket.isConnected()) {
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace(); // Handle the exception properly, e.g., log or show an error message
            }
        }

        sendbt.setOnClickListener(v -> {
            String sendCmd = "~SEND";
            try {
                if (outputStream != null) {
                    outputStream.write(sendCmd.getBytes());
                }else{
                    finish();
                    Toast.makeText(getApplicationContext(),"Error Occurred!!",Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            try {
                startActivityForResult(Intent.createChooser(intent, "Select a file"), 100);
            } catch (Exception exception) {
                Toast.makeText(this, "Please install a file manager.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String path = uri.getPath();
            File file = new File(path);
            txtResult.setText("Path: " + path + "\n" + "\n" + "File name: " + file.getName());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
