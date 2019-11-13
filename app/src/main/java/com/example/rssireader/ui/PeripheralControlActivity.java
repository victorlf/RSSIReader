package com.example.rssireader.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.example.rssireader.Constants;
import com.example.rssireader.R;
//import com.example.rssireader.bluetooth.BleAdapterService;

import java.sql.Timestamp;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.example.rssireader.bluetooth.BleScanner;
import com.example.rssireader.bluetooth.ScanResultsConsumer;


public class PeripheralControlActivity extends Activity implements ScanResultsConsumer {
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";

    private String device_name;
    private String device_address;
    private Timer mTimer;
    private boolean sound_alarm_on_disconnect = false;
    private int alert_level;
    private boolean back_requested = false;
    private boolean share_with_server = false;
    private Switch share_switch;

    // RSSI Reader
    private boolean ble_scanning = false;
    private BleScanner ble_scanner;
    // It's passed but not used by the method in BleScanner at the moment
    private static final long SCAN_TIMEOUT = 5000;


    /*private BleAdapterService bluetooth_le_adapter;

    private final ServiceConnection service_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetooth_le_adapter = ((BleAdapterService.LocalBinder) service).getService();
            bluetooth_le_adapter.setActivityHandler(message_handler);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetooth_le_adapter = null;
        }
    };*/

    /*@SuppressLint("HandlerLeak")
    private Handler message_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle;
            String service_uuid = "";
            String characteristic_uuid = "";
            byte[] b = null;
            // message handling logic
            switch (msg.what) {
                case BleAdapterService.MESSAGE:
                    bundle = msg.getData();
                    String text = bundle.getString(BleAdapterService.PARCEL_TEXT);
                    showMsg(text);
                    break;
                case BleAdapterService.GATT_CONNECTED:
                    ((Button) PeripheralControlActivity.this
                            .findViewById(R.id.connectButton)).setEnabled(false);
                    // we're connected
                    showMsg("CONNECTED");
                    // Added for serivce descovering part
                    bluetooth_le_adapter.discoverServices();
                    break;
                case BleAdapterService.GATT_DISCONNECT:
                    PeripheralControlActivity.this
                            .findViewById(R.id.connectButton).setEnabled(true);
                    // we're disconnected
                    showMsg("DISCONNECTED");
                    // hide the rssi distance colored rectangle
                    ((LinearLayout) PeripheralControlActivity.this
                            .findViewById(R.id.rectangle))
                            .setVisibility(View.INVISIBLE);
                    // stop the rssi reading timer
                    stopTimer();
                    if (back_requested) {
                        PeripheralControlActivity.this.finish();
                    }
                    break;
                case BleAdapterService.GATT_SERVICES_DISCOVERED:
                    startReadRssiTimer();
                    showMsg("Reading RSSI");
                    ((LinearLayout) PeripheralControlActivity.this
                            .findViewById(R.id.rectangle))
                            .setVisibility(View.VISIBLE);
                    break;
                case BleAdapterService.GATT_REMOTE_RSSI:
                    bundle = msg.getData();
                    int rssi = bundle.getInt(BleAdapterService.PARCEL_RSSI);
                    PeripheralControlActivity.this.updateRssi(rssi);
                    break;
            }
        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peripheral_control);

        // read intent data
        final Intent intent = getIntent();
        device_name = intent.getStringExtra(EXTRA_NAME);
        device_address = intent.getStringExtra(EXTRA_ID);

        // show the device name
        ((TextView) this.findViewById(R.id.nameTextView)).setText("Device : "+device_name+" " + "["+device_address+"]");

        // hide the coloured rectangle used to show green/amber/red rssi
        // distance
        // It Doesn't nedd to be put invisible because we don't connect to the device anymore
        //((LinearLayout) this.findViewById(R.id.rectangle)).setVisibility(View.INVISIBLE);

        /*// connect to the Bluetooth adapter service
        Intent gattServiceIntent = new Intent(this, BleAdapterService.class);
        bindService(gattServiceIntent, service_connection, BIND_AUTO_CREATE);
        showMsg("READY");*/

        // RSSI READER
        ble_scanner = new BleScanner(this.getApplicationContext());
        ble_scanner.startScanningNoStop(this, device_address);


    }

    private void setScanState(boolean value) {
        ble_scanning = value;
    }

    @Override
    public void candidateBleDevice(BluetoothDevice device, byte[] scan_record, final int rssi) {

        final Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                ((TextView) findViewById(R.id.rssiTextView)).setText("RSSI = " + Integer.toString(rssi) + " Time = " + timestamp.getTime());
                LinearLayout layout = ((LinearLayout) PeripheralControlActivity.this
                        .findViewById(R.id.rectangle));
                byte proximity_band = 3;
                if (rssi < -80) {
                    layout.setBackgroundColor(0xFFFF0000);
                } else if (rssi < -50) {
                    layout.setBackgroundColor(0xFFFF8A01);
                    proximity_band = 2;
                } else {
                    layout.setBackgroundColor(0xFF00FF00);
                    proximity_band = 1;
                }
                layout.invalidate();
            }
        });
    }

    @Override
    public void scanningStarted() {
        setScanState(true);
    }

    @Override
    public void scanningStopped() {
        setScanState(false);
    }

    // TextView above the button MAKE A NOISE
    /*private void showMsg(final String msg) {
        Log.d(Constants.TAG, msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.msgTextView)).setText(msg);
            }
        });
    }*/

    // Will be automatically called when the Activity is destroyed as part of
    // the standard Android lifecycle
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    /*public void onConnect(View view) {
        showMsg("onConnect");
        if (bluetooth_le_adapter != null) {
            if (bluetooth_le_adapter.connect(device_address)) {
                ((Button) PeripheralControlActivity.this
                        .findViewById(R.id.connectButton)).setEnabled(false);
            } else {
                showMsg("onConnect: failed to connect");
            }
        } else {
            showMsg("onConnect: bluetooth_le_adapter=null");
        }
    }*/

    /*public void onBackPressed() {
        Log.d(Constants.TAG, "onBackPressed");
        back_requested = true;
        if (bluetooth_le_adapter.isConnected()) {
            try {
                bluetooth_le_adapter.disconnect();
            } catch (Exception e) {
            }
        } else {
            finish();
        }
    }*/

    // New onBackPressed
    public void onBackPressed() {
        Log.d(Constants.TAG, "onBackPressed");
        ble_scanner.stopScanning();
        finish();
    }

    /*// Read RSSI
    // Initiates regular sampling of the RSSI
    private void startReadRssiTimer() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                bluetooth_le_adapter.readRemoteRssi();
            }
        }, 0, 2000);
    }
    // Stops the sampling process
    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }
    // Updates the UI with RSSI values
    private void updateRssi(int rssi) {
        ((TextView) findViewById(R.id.rssiTextView)).setText("RSSI = " + Integer.toString(rssi));
        LinearLayout layout = ((LinearLayout) PeripheralControlActivity.this
                .findViewById(R.id.rectangle));
        byte proximity_band = 3;
        if (rssi < -80) {
            layout.setBackgroundColor(0xFFFF0000);
        } else if (rssi < -50) {
            layout.setBackgroundColor(0xFFFF8A01);
            proximity_band = 2;
        } else {
            layout.setBackgroundColor(0xFF00FF00);
            proximity_band = 1;
        }
        layout.invalidate();
    }*/

}
