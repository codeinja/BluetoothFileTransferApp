package com.example.project;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothSocket;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class SendReceive extends AppCompatActivity {
    Button sendbt;
    Button listFilesbt;
    Button deletebt;
    Button receivebt;
    ScrollView scrollView;
    InputStream inputStream;
    OutputStream outputStream;
    TextView receivedDataTextView;
    File file;
    BluetoothSocket socket = BluetoothSocketHolder.getInstance().getBluetoothSocket();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_receive);

        CRC16Calculator.generateCRCTable();

        listFilesbt = findViewById(R.id.listFiles);
        deletebt = findViewById(R.id.DeleteFile);
        scrollView = findViewById(R.id.filesScrollView);
        sendbt = findViewById(R.id.sendbutton);
        receivebt = findViewById(R.id.receivebutton);
        receivedDataTextView = findViewById(R.id.listFilesView);

        if (socket != null && socket.isConnected()) {
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Thread readThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    readFromInputStream();
                }
            });
            readThread.start();
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

        listFilesbt.setOnClickListener(v -> {
            try {
                outputStream.write("~LISTFILES".getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        receivebt.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter Filename");

            final EditText input = new EditText(this);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String filename = input.getText().toString();
                    if (!filename.isEmpty()) {
                        sendReceiveCommand(filename);
                    } else {
                        Toast.makeText(getApplicationContext(), "Please enter a filename", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        });

        deletebt.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter Filename");

            final EditText input = new EditText(this);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String filename = input.getText().toString();
                    if (!filename.isEmpty()) {
                        try {
                            outputStream.write("~DELETE\n".getBytes());
                            outputStream.write(("/"+filename+"\n").getBytes());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Please enter a filename", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        });

    }

    private void readFromInputStream() {
        byte[] buffer = new byte[1024];
        int bytesRead;

        try {
            while (true) {
                if (inputStream != null) {
                    bytesRead = inputStream.read(buffer);
                    if (bytesRead != -1) {
                        final String receivedData = new String(buffer, 0, bytesRead);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(receivedData.contains("~EOF")) {
                                    receivedDataTextView.append(receivedData.substring(0,receivedData.indexOf("~EOF")));
                                }else{
                                    receivedDataTextView.append(receivedData);
                                }
                                scrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                                    }
                                });
                            }
                        });
                    } else {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendReceiveCommand(String fileName) {
        try {
            outputStream.write("~RECEIVE\n".getBytes());
            outputStream.write(("/" + fileName).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sending receive command", Toast.LENGTH_SHORT).show();
        }
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
            String filename = file.getName();
            String new_filename = "/"+filename+"\n";
            try {
                outputStream.write(new_filename.getBytes());
                Log.d("Filename",new_filename);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String read_data = readFile(uri);

            int calculatedCRC = CRC16Calculator.calculateCRC(read_data);

            String CRC_to_send = String.format("0x%04X%n",calculatedCRC).substring(2);

            try {
                outputStream.write(CRC_to_send.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Log.d("Read Data", read_data);
            Log.d("CRC", String.format("0x%04X%n",calculatedCRC));
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
                String full_line = line + "\n";
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
}
