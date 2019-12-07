package com.example.rssireader.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rssireader.Constants;
import com.example.rssireader.R;
import com.example.rssireader.bluetooth.BleScanner;
import com.example.rssireader.bluetooth.ScanResultsConsumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocationActivity extends AppCompatActivity implements ScanResultsConsumer {

    public static final String EXTRA_NAME_1 = "name";
    public static final String EXTRA_ID_1 = "id";

    public static final String EXTRA_NAME_2 = "name";
    public static final String EXTRA_ID_2 = "id";

    public static final String EXTRA_NAME_3 = "name";
    public static final String EXTRA_ID_3 = "id";

    private boolean ble_scanning = false;
    private Handler handler = new Handler();
    private ListAdapter ble_device_list_adapter;
    private BleScanner ble_scanner;
    // It's passed but not used by the method in BleScanner at the moment
    private static final long SCAN_TIMEOUT = 5000;
    private static final int REQUEST_LOCATION = 0;
    private static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_COARSE_LOCATION};
    private boolean permissions_granted=false;
    private int device_count=0;
    private Toast toast;

    static class ViewHolder {
        public TextView text;
        public TextView bdaddr;
        public TextView bdrssi;
    }

    //private String[] nodes = {"node1", "node2", "node3", "node4", "node5", "node6"};
    // Convert String Array to List
    //List<String> nodesList = Arrays.asList(nodes);
    private List<String> nodesScanned;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        ble_device_list_adapter = new LocationActivity.ListAdapter();
        ListView listView = (ListView) this.findViewById(R.id.deviceListOfInterest);
        listView.setAdapter(ble_device_list_adapter);
        ble_scanner = new BleScanner(this.getApplicationContext());

        // Data Base
        // Creates DB
        SQLiteDatabase bancoDados = openOrCreateDatabase("app", MODE_PRIVATE, null);
        // Deletes Table
        bancoDados.execSQL("DROP TABLE IF EXISTS rssi");
        // Creates Table
        //bancoDados.execSQL("CREATE TABLE IF NOT EXISTS rssi (id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR, addr VARCHAR, measurement INT(3))");
        bancoDados.execSQL("CREATE TABLE rssi(id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR, measurement INT(3))");
        // Inserts data
        //bancoDados.execSQL("INSERT INTO rssi(medida) VALUES ('')");

        onScan(findViewById(R.id.content));
    }

    // Any details passed to it by the BleScanner object are stored in the ListAdapter and are
    // shown on the UI if not already there.
    @Override
    public void candidateBleDevice(final BluetoothDevice device, byte[] scan_record, final int rssi, int tx_power) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                ble_device_list_adapter.addDevice(device, rssi);
                ble_device_list_adapter.notifyDataSetChanged();
                device_count++;

                //if (nodesList.contains(device.getName())) {
                // Cria BD
                SQLiteDatabase bancoDados = openOrCreateDatabase("app", MODE_PRIVATE, null);
                // Inseri dados
                //bancoDados.execSQL("INSERT INTO rssi(name, addr, measurement) VALUES (" + device.getName() + ", " + device.getAddress() + ", " + rssi + ")");
                bancoDados.execSQL("INSERT INTO rssi(name, measurement) VALUES (" + device.getName() + ", " + rssi + ")");

                //
                if(!nodesScanned.contains(device.getName())){
                    nodesScanned.add(device.getName());
                }
                //}


            }
        });
    }

    // The BleScanner object will tell our MainActivity object whenever it starts to perform
    // scanning or stops scanning by calling the corresponding methods of the ScanResultsConsumer
    // interface which MainActivity implements
    @Override
    public void scanningStarted() {
        //setScanState(true);
    }

    @Override
    public void scanningStopped() {
        //if (toast != null) {
        //    toast.cancel();
        //}
        //setScanState(false);
    }

    public void onScan(View view) {
        if (!ble_scanner.isScanning()) {
            device_count=0;
            // It's checking the version of Android we’re running on and
            // if it’s greater than or equal to 6(M)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissions_granted = false;
                    requestLocationPermission();
                } else {
                    Log.i(Constants.TAG, "Location permission has already been granted. Starting scanning.");
                    permissions_granted = true;
                }
            } else {
                // the ACCESS_COARSE_LOCATION permission did not exist before M so....
                permissions_granted = true;
            }
            startScanning();
        } else {
            ble_scanner.stopScanning();
        }
    }

    // Handles requesting permission to perform scanning from the user
    private void requestLocationPermission() {
        Log.i(Constants.TAG, "Location permission has NOT yet been granted. Requesting permission.");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)){
            Log.i(Constants.TAG, "Displaying location permission rationale to provide additional context.");
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Permission Required");
            builder.setMessage("Please grant Location access so this application can perform Bluetooth scanning");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    Log.d(Constants.TAG, "Requesting permissions after explanation");
                    ActivityCompat.requestPermissions(LocationActivity.this, new
                            String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                }
            });
            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new
                    String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }
    }

    // A list adapter that serves to store a list of BluetoothDevices that are found during
    // scanning, and provides the data for the list view. The adapter will use the list_row.xml
    // layout we added earlier and return that view with the name of the found device for the list

    private class CustomObject {
        private int rssi;
        private BluetoothDevice device;

        CustomObject(BluetoothDevice device, int rssi) {
            this.device = device;
            this.rssi = rssi;
        }

        public BluetoothDevice getDevice() {
            return this.device;
        }

        public int getRssi() {
            return this.rssi;
        }

        public void setRssi(int rssi) {
            this.rssi = rssi;
        }
    }

    // to display in the row.
    private class ListAdapter extends BaseAdapter {
        private ArrayList<CustomObject> ble_devices;
        public ListAdapter() {
            super();
            ble_devices = new ArrayList<CustomObject>();
        }

        public void addDevice(BluetoothDevice device, int rssi) {

            // Create the object with the informations about the device
            CustomObject customObject = new CustomObject(device, rssi);

            //System.out.println("Resultado1: " + device.getName());
            /*if (!ble_devices.contains(customObject)) {
                ble_devices.add(customObject);
            }*/

            if (ble_devices.isEmpty()) {
                ble_devices.add(customObject);
            }
            else {
                int count = 0;
                for (CustomObject c : ble_devices) {
                    //System.out.println("Resultado2: " + device.getName());
                    if (c.getDevice().getAddress().equals(device.getAddress())) {
                        //ble_devices.add(customObject);
                        count++;
                    }
                    //else if (c.getDevice().getName().equals(device.getName())) {
                    //    c.setRssi(rssi);
                    //}
                }
                if (count == 0) {
                    ble_devices.add(customObject);
                }
            }

            /*for(int i = 0; i < ble_devices.size(); i++) {
                System.out.println("Resultado2: " + device.getName());

                CustomObject customObjectTeste = ble_devices.get(i);
                BluetoothDevice deviceTeste = customObjectTeste.getDevice();
                String deviceTesteName = deviceTeste.getName();
                if (!device.getName().equals(deviceTesteName)){
                    ble_devices.add(customObject);
                }

            }*/
        }
        //public boolean contains(BluetoothDevice device) {
        //    return ble_devices.contains(device);
        //}
        public CustomObject getDevice(int position) {
            return ble_devices.get(position);
        }
        public void clear() {
            ble_devices.clear();
        }
        @Override
        public int getCount() {
            return ble_devices.size();
        }
        @Override
        public Object getItem(int i) {
            return ble_devices.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = LocationActivity.this.getLayoutInflater().inflate(R.layout.list_row,
                        null);
                viewHolder = new ViewHolder();
                viewHolder.text = (TextView) view.findViewById(R.id.textView);
                viewHolder.bdaddr = (TextView) view.findViewById(R.id.bdaddr);
                //
                viewHolder.bdrssi = (TextView) view.findViewById(R.id.bdrssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            //BluetoothDevice device = ble_devices.get(i);
            CustomObject customObject = ble_devices.get(i);
            //String deviceName = device.getName();
            BluetoothDevice device = customObject.device;
            String deviceName = device.getName();
            String deviceRSSI = Integer.toString(customObject.getRssi());
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.text.setText(deviceName);
            } else {
                viewHolder.text.setText("unknown device");
            }
            viewHolder.bdaddr.setText(device.getAddress());
            viewHolder.bdrssi.setText(deviceRSSI);
            return view;
        }
    }

    private void simpleToast(String message, int duration) {
        toast = Toast.makeText(this, message, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    // Checks that permissions have been granted, clears the UI device list and
    // then tells the BleScanner object to start scanning.
    private void startScanning() {
        if (permissions_granted) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ble_device_list_adapter.clear();
                    ble_device_list_adapter.notifyDataSetChanged();
                }
            });
            // Set the Toast message
            simpleToast(Constants.SCANNING, 2000);
            ble_scanner.startScanning(this, SCAN_TIMEOUT);
        } else {
            Log.i(Constants.TAG, "Permission to perform Bluetooth scanning was not yet granted");
        }
    }

    // Mode for the RSSI values
    static int mode(List<Integer> a, int size) {
        int maxValue = 0, maxCount = 0, i, j;

        for (i = 0; i < size; ++i) {
            int count = 0;
            for (j = 0; j < size; ++j) {
                if (a.get(j) == a.get(i))
                    ++count;
            }

            if (count > maxCount) {
                maxCount = count;
                maxValue = a.get(i);
            }
        }
        return maxValue;
    }

}
