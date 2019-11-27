package com.example.rssireader.bluetooth;

/* MainActivity.java needs to implement this interface so that it can receive and process data
produced by the Bluetooth scanning process.*/

import android.bluetooth.BluetoothDevice;

public interface ScanResultsConsumer {
    //public void candidateBleDevice(BluetoothDevice device, byte[] scan_record, int rssi);
    public void candidateBleDevice(BluetoothDevice device, byte[] scan_record, int rssi, int tx_power);
    public void scanningStarted();
    public void scanningStopped();
}

