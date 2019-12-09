package com.example.rssireader.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.util.Pools;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rssireader.Constants;
import com.example.rssireader.R;
import com.example.rssireader.bluetooth.BleScanner;
import com.example.rssireader.bluetooth.ScanResultsConsumer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private List<String> nodesScanned = new ArrayList<String>();

    // saveDB
    private String m_Text;



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

        // Scan for 5 seconds
        onScan(findViewById(R.id.content));


        //
        //select3Nodes(findViewById(R.id.content));
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
                String deviceName = device.getName();
                bancoDados.execSQL("INSERT INTO rssi(name, measurement) VALUES ( '" + deviceName + "', " + rssi + ")");

                //
                if(!nodesScanned.contains(deviceName)){
                    nodesScanned.add(deviceName);
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
        select3Nodes(findViewById(R.id.content));
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

    public void saveDB(View view) {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setTitle("File Name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        dialog.setView(input);

        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = input.getText().toString();
                System.out.println("Texto inserido: " + m_Text);

                exportDatabase("app", m_Text);

            }
        });

        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        dialog.show();
    }

    public void exportDatabase(String databaseName, String databaseBackupName) {
        try {
            //File sd = Environment.getExternalStorageDirectory();
            File path = new File(Environment.getExternalStorageDirectory(), "MyDirName");
            File data = Environment.getDataDirectory();

            if (path.canWrite()) {
                String currentDBPath = "//data//"+getPackageName()+"//databases//"+databaseName+"";
                String backupDBPath = databaseBackupName + ".db";
                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(path, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {

        }
    }

    private class Node implements Comparable<Node> {
        private Integer max_rssi;
        private String name;

        Node(String name, Integer max_rssi) {
            this.name = name;
            this.max_rssi = max_rssi;
        }

        public String getName() {
            return this.name;
        }

        public int getMaxRssi() {
            return max_rssi;
        }

        @Override
        public int compareTo(Node o) {
            //return this.getMaxRssi().compareTo(o.getMaxRssi());
            return Integer.compare(this.getMaxRssi(), o.getMaxRssi());
        }

    }




    public void select3Nodes(View view) {

        List<Node> nodes = new ArrayList<Node>();
        List<Node> nodes_3 = new ArrayList<Node>();

        // Open connaction with DB
        SQLiteDatabase bancoDados = openOrCreateDatabase("app", MODE_PRIVATE, null);

        // Save every node scanned with its respective max_rssi into list nodes
        Iterator iter = nodesScanned.iterator();
        while (iter.hasNext()) {
            // Recover data
            String name_temp = iter.next().toString();
            Cursor cursor = bancoDados.rawQuery("SELECT measurement FROM rssi WHERE name = '" + name_temp + "'", null);
            int indiceMedida = cursor.getColumnIndex("measurement");

            List<Integer> rssi_values = new ArrayList<>();

            // The cursor is at the last position so we move to next
            cursor.moveToFirst();
            while (!cursor.isLast()) {
                int medida = cursor.getInt(indiceMedida);

                // Add to the list of RSSI values to calculate its mode
                rssi_values.add(medida);

                System.out.println("Resultado: " + name_temp + "," + medida);

                cursor.moveToNext();
            }

            // Mode of the RSSI values
            int max_rssi  = mode(rssi_values, rssi_values.size());
            //Collections.sort(rssi_values);
            //int max_rssi = rssi_values.get(rssi_values.size() - 1);

            Node node = new Node(name_temp, max_rssi);
            nodes.add(node);

        }

        // Sort the list nodes so the Nodes with 3 biggest RSSI will be the last elements
        // of the list
        Collections.sort(nodes);

        System.out.println("node list sorted");
        Iterator<Node> iter2 = nodes.iterator();
        while (iter2.hasNext()) {
            Node n = iter2.next();
            System.out.println("Resultado Final: " + n.getName() + ", " + n.getMaxRssi());
        }

        //
        for (int i = 0; i < 3; i++) {
            nodes_3.add(nodes.get(nodes.size() - 1));
            nodes.remove(nodes.size() - 1);
        }

        String final_measurements = "";

        System.out.println("node_3 : last 3 elements of nodes");
        Iterator<Node> iter3 = nodes_3.iterator();
        while (iter3.hasNext()) {
            Node n = iter3.next();
            double distance = CalculateDistance(n.getMaxRssi());
            System.out.println("Resultado Final: " + n.getName() + ", " + n.getMaxRssi() + ", " + distance);
            final_measurements += n.getName() + ", " + n.getMaxRssi() + ", " + distance + "\n";
        }

        TextView textMeasurements = findViewById(R.id.textMeasurements);
        textMeasurements.setText(final_measurements);

        System.out.println("Calculate the position in the X,Y");

        double r1 = 0;
        double r2 = 0;
        double r3 = 0;

        Iterator<Node> iter4 = nodes_3.iterator();
        while (iter4.hasNext()) {
            Node n = iter4.next();
            switch (n.getName()){
                case "node1":
                    r1 = CalculateDistance(n.getMaxRssi());
                    System.out.println("R1 found");
                case "node2":
                    r2 = CalculateDistance(n.getMaxRssi());
                    System.out.println("R2 found");
                case "node3":
                    r3 = CalculateDistance(n.getMaxRssi());
                    System.out.println("R3 found");
            }
        }

        CalculatePosition position = new CalculatePosition(4.96, 0, r1, 0, 2.6, r2, 4.332, 4.23, r3);

        double x = position.getX();
        double y = position.getY();

        System.out.println("The position is (" + x + ", " + y + ")");

        TextView textPosition = findViewById(R.id.textPosition);
        textPosition.setText("The position is (x = " + x + ", y = " + y + ")");

    }

    private double CalculateDistance(int max_rssi) {
        // Calculate distance
        // Total Path Loss = Tx-power less RSSI
        int l_total = -21 - max_rssi;
        // Depends on the frequency equals 2400MHz
        double log = Math.log10(2400);
        double l_d0 = 20*log - 28;
        // Operations
        double l_final = l_total - l_d0;
        // Depends on the same frequency and the environment
        int n_for_Office = 30;
        // Operations
        l_final = l_final/n_for_Office;
        // Final Operation for distance
        double distance = Math.pow(10, l_final);

        // Print Total Path Loss and Distance
        //TextView textRssiMode = findViewById(R.id.textRssiMode);
        //textRssiMode.setText("RSSI = " + l_total + " e Distância = " + distance);

        return distance;
    }

    private class CalculatePosition {

        private double A, B, C, D, E, F;

        public CalculatePosition (double x1, double y1, double r1, double x2, double y2, double r2, double x3, double y3, double r3) {

            this.A = 2*x2 - 2*x1;
            this.B = 2*y2 - 2*y1;
            this.C = Math.pow(r1, 2) - Math.pow(r2, 2) - Math.pow(x1, 2) + Math.pow(x2, 2) - Math.pow(y1, 2) + Math.pow(y2, 2);
            this.D = 2*x3 - 2*x2;
            this.E = 2*y3 - 2*y2;
            this.F = Math.pow(r2, 2) - Math.pow(r3, 2) - Math.pow(x2, 2) + Math.pow(x3, 2) - Math.pow(y2, 2) + Math.pow(y3, 2);

        }

        public double getX() {
            double x = (C*E - F*B) / (E*A - B*D);
            return x;
        }

        public double getY() {
            double y = (C*D - A*F) / (B*D - A*E);
            return y;
        }

    }

}
