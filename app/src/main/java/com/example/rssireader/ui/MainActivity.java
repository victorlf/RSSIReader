package com.example.rssireader.ui;

import androidx.annotation.NonNull;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.rssireader.Constants;
import com.example.rssireader.R;
import com.example.rssireader.bluetooth.BleScanner;
import com.example.rssireader.bluetooth.ScanResultsConsumer;
//import com.example.rssireader.ui.PeripheralControlActivity;

import java.util.ArrayList;
import java.util.IdentityHashMap;

/*
* Passos a Seguir:
* Limpar enxugar o código ao máximo;(Check)
* Replicar o retângulo que mudar de cor do exemplo;(Check)
* Ler sem estar conectado;(Check)
* Ler scanning continuamente; (Check)
* Atualizar rssi com timeStamp() (timer() schedule->updateRssi); (Está contanto o timestamp)
* Fazer gráfico;
* * */

public class MainActivity extends AppCompatActivity implements ScanResultsConsumer {


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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Set button text
        setButtonText();
        ble_device_list_adapter = new ListAdapter();
        ListView listView = (ListView) this.findViewById(R.id.deviceList);
        listView.setAdapter(ble_device_list_adapter);
        ble_scanner = new BleScanner(this.getApplicationContext());


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // Needs to stop the scanning so can start another filtering the chosen device
                if (ble_scanning) {
                    setScanState(false);
                    ble_scanner.stopScanning();
                }

                CustomObject customObject = ble_device_list_adapter.getDevice(position);
                BluetoothDevice device = customObject.getDevice();
                if (toast != null) {
                    toast.cancel();
                }
                Intent intent = new Intent(MainActivity.this,
                        PeripheralControlActivity.class);
                intent.putExtra(PeripheralControlActivity.EXTRA_NAME, device.getName());
                intent.putExtra(PeripheralControlActivity.EXTRA_ID, device.getAddress());
                startActivity(intent);
            }
        });
    }

    // Sets or resets the text showing on the button
    private void setButtonText() {
        String text = "";
        text = Constants.FIND;
        final String button_text = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView)
                        MainActivity.this.findViewById(R.id.scanButton)).setText(button_text);
            }
        });
    }

    // Changes the text on the scan screen’s button according to whether or not
    // scanning is currently being performed
    private void setScanState(boolean value) {
        ble_scanning = value;
        ((Button) this.findViewById(R.id.scanButton)).setText(value ? Constants.STOP_SCANNING :
                Constants.FIND);
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
            }
        });
    }

    // The BleScanner object will tell our MainActivity object whenever it starts to perform
    // scanning or stops scanning by calling the corresponding methods of the ScanResultsConsumer
    // interface which MainActivity implements
    @Override
    public void scanningStarted() {
        setScanState(true);
    }

    @Override
    public void scanningStopped() {
        if (toast != null) {
            toast.cancel();
        }
        setScanState(false);
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
                view = MainActivity.this.getLayoutInflater().inflate(R.layout.list_row,
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

    // Respond to the Find button being pressed
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

    /*public void onScan(View view) {
        Intent intent = new Intent(MainActivity.this,
                GraphViewActivity.class);
        startActivity(intent);
    }*/


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
                    ActivityCompat.requestPermissions(MainActivity.this, new
                            String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                }
            });
            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new
                    String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            Log.i(Constants.TAG, "Received response for location permission request.");
            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
            // Location permission has been granted
                Log.i(Constants.TAG, "Location permission has now been granted. Scanning.....");
                permissions_granted = true;
                if (ble_scanner.isScanning()) {
                    startScanning();
                }
            }else{
                Log.i(Constants.TAG, "Location permission was NOT granted.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // Instanciate the Toast alert
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


}

