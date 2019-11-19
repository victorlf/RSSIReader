package com.example.rssireader.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import com.example.rssireader.Constants;
import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;
import java.util.List;

public class BleScanner {

    private BluetoothLeScanner scanner = null;
    private BluetoothAdapter bluetooth_adapter = null;
    private Handler handler = new Handler();
    private ScanResultsConsumer scan_results_consumer;
    private Context context;
    private boolean scanning=false;
    private String device_name_start="";

    // Graph
    private Runnable mTimerGraph;

    // Class Constructor and Checking the Status of the Bluetooth Adapter
    public BleScanner(Context context) {
        this.context = context;
        final BluetoothManager bluetoothManager = (BluetoothManager)
                context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetooth_adapter = bluetoothManager.getAdapter();
        // check bluetooth is available and on
        if (bluetooth_adapter == null || !bluetooth_adapter.isEnabled()) {
            Log.d(Constants.TAG, "Bluetooth is NOT switched on");
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(enableBtIntent);
        }
        Log.d(Constants.TAG, "Bluetooth is switched on");
    }

    // Requires a time limit for scanning to be specified as an argument as well as an instance of
    // our ScanResultsConsumer interface so that callbacks can be made to its methods during scanning.
    // Scanning used in Main Activity
    public void startScanning(final ScanResultsConsumer scan_results_consumer, long
            stop_after_ms) {
        if (scanning) {
            Log.d(Constants.TAG, "Already scanning so ignoring startScanning request");
            return;
        }
        if (scanner == null) {
            scanner = bluetooth_adapter.getBluetoothLeScanner();
            Log.d(Constants.TAG, "Created BluetoothScanner object");
        }
        // Retirei a função que para o scanning quando o stop_after_ms ter chegado
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (scanning) {
                    Log.d(Constants.TAG, "Stopping scanning");
                    scanner.stopScan(scan_callback);
                    setScanning(false);
                }
            }
        }, stop_after_ms);
        this.scan_results_consumer = scan_results_consumer;
        Log.d(Constants.TAG,"Scanning");
        List<ScanFilter> filters;
        filters = new ArrayList<ScanFilter>();
        // Creating a filter
        /*if (filter_by_bdaddress != null && !filter_by_bdaddress.equals("")) {
            ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(filter_by_bdaddress).build();
            filters.add(filter);
        }*/

        ScanSettings settings = new
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        setScanning(true);
        scanner.startScan(filters, settings, scan_callback);
    }

    // Scanning used in Peripherical Control Activity
    // Here we don't want to stop the scanning adn want to pass a Address for filtering
    public void startScanningNoStop(final ScanResultsConsumer scan_results_consumer, final String filter_by_bdaddress){//, final long
            //stop_after_ms ) {

        //mTimerGraph = new Runnable() {

            //@Override
            //public void run() {
        if (scanning) {
            Log.d(Constants.TAG, "Already scanning so ignoring startScanning request");
            return;
        }
        if (scanner == null) {
            scanner = bluetooth_adapter.getBluetoothLeScanner();
            Log.d(Constants.TAG, "Created BluetoothScanner object");
        }

        BleScanner.this.scan_results_consumer = scan_results_consumer;
        Log.d(Constants.TAG, "Scanning");
        List<ScanFilter> filters;
        filters = new ArrayList<ScanFilter>();

        // Creating a filter
        if (filter_by_bdaddress != null && !filter_by_bdaddress.equals("")) {
            ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(filter_by_bdaddress).build();
            filters.add(filter);
        }

        ScanSettings settings = new
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        setScanning(true);
        scanner.startScan(filters, settings, scan_callback);
                //handler.postDelayed(this, stop_after_ms);
           // }
       // };
       // handler.post(mTimerGraph);
    }

    // Simple method by which scanning can be terminated.
    public void stopScanning() {
        setScanning(false);
        Log.d(Constants.TAG, "Stopping scanning");
        scanner.stopScan(scan_callback);
    }

    // ScanCallback object that our BluetoothLeScanner object needs;
    // onScanResult is going to be called every time the scanner collects a Bluetooth advertising
    // packet which complies with our filtering criteria.
    private ScanCallback scan_callback = new ScanCallback() {
        public void onScanResult(int callbackType, final ScanResult result) {
            if (!scanning) {
                return;
            }
            scan_results_consumer.candidateBleDevice(result.getDevice(),
                    result.getScanRecord().getBytes(), result.getRssi());
        }
    };

    public boolean isScanning() {
        return scanning;
    }

    // Informs the ScanResultsConsumer object of changes in the scanning state;
    // This is useful so that the UI can be updated accordingly.
    void setScanning(boolean scanning) {
        this.scanning = scanning;
        if (!scanning) {
            scan_results_consumer.scanningStopped();
        } else {
            scan_results_consumer.scanningStarted();
        }
    }


}
