package com.example.project;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;

public class SendReceive extends AppCompatActivity {

    Button sendbt;
    Button receivebt;
    TextView txtResult;
    InputStream inputStream;
    OutputStream outputStream;
    String line;
    File file;
    BluetoothSocket socket = BluetoothSocketHolder.getInstance().getBluetoothSocket();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_receive);

        txtResult = findViewById(R.id.pathDisplay);
        sendbt = findViewById(R.id.sendbutton);
        receivebt = findViewById(R.id.receivebutton);
        if (socket != null && socket.isConnected()) {
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        sendbt.setOnClickListener(v -> {
            String sendCmd = "~SEND";
            try {
                if (outputStream != null) {
                    outputStream.write(sendCmd.getBytes());
                } else {
                    finish();
                    Toast.makeText(getApplicationContext(), "Error Occurred!!", Toast.LENGTH_SHORT).show();
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
        String path;
        Uri uri;
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            uri = data.getData();
            path = uri.getPath();
            file = new File(path);
            txtResult.setText("Path: " + path + "\n" + "\n" + "File name: " + file.getName());
            String filename = file.getName();
            String new_filename = "/"+filename+"\n";
            try {
                outputStream.write(new_filename.getBytes());
                Log.d("Filename",new_filename);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String read_data = readFile(uri);
            String calculatedCRC = calculateCRC16(read_data);
            try {
                outputStream.write("e49c".getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Log.d("Read Data", read_data);
            Log.d("CRC-16", String.format("0x%04X", calculatedCRC));
        }
    }

    protected String readFile(Uri path) {
        StringBuilder stringBuilder = new StringBuilder();

        try {
            ContentResolver contentResolver = getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(path);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String full_line = line+"\n";
                outputStream.write(full_line.getBytes());
                stringBuilder.append(line).append('\n');
            }

            String eofCmd = "~EOF\n";
            outputStream.write(eofCmd.getBytes());
            bufferedReader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "Error reading the file";
        }

        return stringBuilder.toString();
    }

    protected String calculateCRC16(String input) {
        int crc = 0x0000; // Initial value
        int polynomial = 0x1021; // CRC-16 polynomial
        byte[] bytes = input.getBytes();

        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;

            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ polynomial;
                } else {
                    crc = crc << 1;
                }
            }
        }
        crc &= 0xFFFF; // Ensure the CRC value is a 16-bit value
        return String.valueOf(crc); // Convert CRC to a 4-character hexadecimal string
    }
}
