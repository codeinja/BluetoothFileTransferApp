package com.example.project;
public class CRC16Calculator {

    private static final int[] CRC_TABLE = new int[256];
    private static final int CRC16_POLY = 0x1021;

    public static void generateCRCTable() {
        for (int i = 0; i < 256; i++) {
            int crc = i << 8;
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ CRC16_POLY;
                } else {
                    crc <<= 1;
                }
            }
            CRC_TABLE[i] = crc & 0xFFFF;
        }
    }

    public static int calculateCRC(String input) {
        int crc = 0xFFFF;

        byte[] bytes = input.getBytes();
        for (byte data : bytes) {
            crc = (crc << 8) ^ CRC_TABLE[((crc >> 8) ^ (data & 0xFF)) & 0xFF];
        }
        return crc & 0xFFFF;
    }
}


