/*package com.example.rssireader.bluetooth;

// It will act as a high level API for Bluetooth interactions
// with the peripheral device.

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.os.Binder;
import android.os.IBinder;
//import android.support.annotation.Nullable; // Cannot resolve
import androidx.annotation.Nullable;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import com.example.rssireader.Constants;

import java.util.List;


public class BleAdapterService extends Service {

    private BluetoothAdapter bluetooth_adapter;
    private BluetoothGatt bluetooth_gatt;
    private BluetoothManager bluetooth_manager;
    private Handler activity_handler = null;
    private BluetoothDevice device;

    private BluetoothGattDescriptor descriptor;
    private boolean connected = false;
    public boolean alarm_playing = false;

    // For an Activity to be able to use an Android Service it must be able to “bind” to it.
    private final IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public BleAdapterService getService() {
            return BleAdapterService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    // We will use a message Handler object to communicate from the BleAdapterService object to
    // the Activity which uses its services.

    // messages sent back to activity
    public static final int GATT_CONNECTED = 1;
    public static final int GATT_DISCONNECT = 2;
    public static final int GATT_SERVICES_DISCOVERED = 3;
    public static final int GATT_CHARACTERISTIC_READ = 4;
    public static final int GATT_CHARACTERISTIC_WRITTEN = 5;
    public static final int GATT_REMOTE_RSSI = 6;
    public static final int MESSAGE = 7;
    public static final int NOTIFICATION_OR_INDICATION_RECEIVED = 8;

    // message parms
    public static final String PARCEL_DESCRIPTOR_UUID = "DESCRIPTOR_UUID";
    public static final String PARCEL_CHARACTERISTIC_UUID = "CHARACTERISTIC_UUID";
    public static final String PARCEL_SERVICE_UUID = "SERVICE_UUID";
    public static final String PARCEL_VALUE = "VALUE";
    public static final String PARCEL_RSSI = "RSSI";
    public static final String PARCEL_TEXT = "TEXT";


    // Set activity that will receive the messages
    public void setActivityHandler(Handler handler) {
        activity_handler = handler;
    }

    // It will allow the service to send text messages to the activity, which
    // we’ll display on the screen
    private void sendConsoleMessage(String text) {
        Message msg = Message.obtain(activity_handler, MESSAGE);
        Bundle data = new Bundle();
        data.putString(PARCEL_TEXT, text);
        msg.setData(data);
        msg.sendToTarget();
    }


    public boolean isConnected() {
        return connected;
    }


    // To allow Bluetooth operations to be performed, amongst other things, we need a
    // BluetoothAdapter object which can be obtained from a BluetoothManager object.
    // We’ll acquire a BluetoothAdapter object when the BleAdapterService object is created.
    // When we create a BleAdapterService object, this code will execute.
    @Override
    public void onCreate() {
        if (bluetooth_manager == null) {
            bluetooth_manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetooth_manager == null) {
                return;
            }
        }
        bluetooth_adapter = bluetooth_manager.getAdapter();
        if (bluetooth_adapter == null) {
            return;
        }
    }

    // Connect to the device
    public boolean connect(final String address) {
        if (bluetooth_adapter == null || address == null) {
            sendConsoleMessage("connect: bluetooth_adapter=null");
            return false;
        }
        device = bluetooth_adapter.getRemoteDevice(address);
        if (device == null) {
            sendConsoleMessage("connect: device=null");
            return false;
        }
        bluetooth_gatt = device.connectGatt(this, false, gatt_callback);
        return true;
    }

    // Disconnect from device
    public void disconnect() {
        sendConsoleMessage("disconnecting");
        if (bluetooth_adapter == null || bluetooth_gatt == null) {
            sendConsoleMessage("disconnect: bluetooth_adapter|bluetooth_gatt null");
            return;
        }
        if (bluetooth_gatt != null) {
            bluetooth_gatt.disconnect();
        }
    }

    // The Bluetooth operations which we’ll implement in the BleAdapterService class and which will
    // be used by our PeripheralControlActivity are all asynchronous operations. This means that
    // after initiating an operation, our code will not block while it waits for the result, but
    // will continue executing in the same thread of execution. Later on (usually milliseconds),
    // after communication over Bluetooth with the peripheral device has completed, we’ll receive
    // the result of the operation via a call back. In the Android Bluetooth APIs, to receive such
    // call backs, we need to extend an abstract class called BluetoothGattCallback and override
    // any methods which we’re interested in.
    private final BluetoothGattCallback gatt_callback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Log.d(Constants.TAG, "onConnectionStateChange: status=" + status);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(Constants.TAG, "onConnectionStateChange: CONNECTED");
                connected = true;
                Message msg = Message.obtain(activity_handler, GATT_CONNECTED);
                msg.sendToTarget();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(Constants.TAG, "onConnectionStateChange: DISCONNECTED");
                connected = false;
                Message msg = Message.obtain(activity_handler, GATT_DISCONNECT);
                msg.sendToTarget();
                if (bluetooth_gatt != null) {
                    Log.d(Constants.TAG,"Closing and destroying BluetoothGatt object");
                    bluetooth_gatt.close();
                    bluetooth_gatt = null;
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            sendConsoleMessage("Services Discovered");
            Message msg = Message.obtain(activity_handler,
                    GATT_SERVICES_DISCOVERED);
            msg.sendToTarget();
        }

        //These methods will be called when Bluetooth read and write procedures have completed and
        // they’ll pass results to the Activity using our message handler object.
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Bundle bundle = new Bundle();
                bundle.putString(PARCEL_CHARACTERISTIC_UUID, characteristic.getUuid()
                        .toString());
                bundle.putString(PARCEL_SERVICE_UUID,
                        characteristic.getService().getUuid().toString());
                bundle.putByteArray(PARCEL_VALUE, characteristic.getValue());
                Message msg = Message.obtain(activity_handler,
                        GATT_CHARACTERISTIC_READ);
                msg.setData(bundle);
                msg.sendToTarget();
            } else {
                Log.d(Constants.TAG, "failed to read characteristic:"+characteristic.getUuid().toString()+
                        " of service "+characteristic.getService().getUuid().toString()+" : status="+status);
                sendConsoleMessage("characteristic read err:"+status);
            }
        }
        //
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status)
        {
            Log.d(Constants.TAG, "onCharacteristicWrite");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Bundle bundle = new Bundle();
                bundle.putString(PARCEL_CHARACTERISTIC_UUID,
                        characteristic.getUuid().toString());
                bundle.putString(PARCEL_SERVICE_UUID,
                        characteristic.getService().getUuid().toString());
                bundle.putByteArray(PARCEL_VALUE, characteristic.getValue());
                Message msg = Message.obtain(activity_handler, GATT_CHARACTERISTIC_WRITTEN);
                msg.setData(bundle);
                msg.sendToTarget();
            } else {
                sendConsoleMessage("characteristic write err:" + status);
            }
        }

        // Read RSSI
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                sendConsoleMessage("RSSI read OK");
                Bundle bundle = new Bundle();
                bundle.putInt(PARCEL_RSSI, rssi);
                Message msg = Message
                        .obtain(activity_handler, GATT_REMOTE_RSSI);
                msg.setData(bundle);
                msg.sendToTarget();
            } else {
                sendConsoleMessage("RSSI read err:"+status);
            }
        }

    };

    // Discover services
    public void discoverServices() {
        if (bluetooth_adapter == null || bluetooth_gatt == null) {
            return;
        }
        Log.d(Constants.TAG,"Discovering GATT services");
        bluetooth_gatt.discoverServices();
    }

    // Receive list of discovered services
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetooth_gatt == null)
            return null;
        return bluetooth_gatt.getServices();
    }

    // Activity call this methods to initiate reading from or wirting to a characteristic,
    // specified using a service UUID and a characteristic UUID.
    public boolean readCharacteristic(String serviceUuid,
                                      String characteristicUuid) {

        Log.d(Constants.TAG,"readCharacteristic:"+characteristicUuid+" of service "
                +serviceUuid);

        if (bluetooth_adapter == null || bluetooth_gatt == null) {
            sendConsoleMessage("readCharacteristic: bluetooth_adapter|bluetooth_gatt null");
            return false;
        }

        BluetoothGattService gattService = bluetooth_gatt
                .getService(java.util.UUID.fromString(serviceUuid));

        if (gattService == null) {
            sendConsoleMessage("readCharacteristic: gattService null");
            return false;
        }

        BluetoothGattCharacteristic gattChar = gattService
                .getCharacteristic(java.util.UUID.fromString(characteristicUuid));

        if (gattChar == null) {
            sendConsoleMessage("readCharacteristic: gattChar null");
            return false;
        }

        return bluetooth_gatt.readCharacteristic(gattChar);
    }
    //
    public boolean writeCharacteristic(String serviceUuid,
                                       String characteristicUuid, byte[] value) {
        Log.d(Constants.TAG,"writeCharacteristic:"+characteristicUuid+" of service "
                +serviceUuid);
        if (bluetooth_adapter == null || bluetooth_gatt == null) {
            sendConsoleMessage("writeCharacteristic: bluetooth_adapter|bluetooth_gatt null");
            return false;
        }

        BluetoothGattService gattService = bluetooth_gatt
                .getService(java.util.UUID.fromString(serviceUuid));

        if (gattService == null) {
            sendConsoleMessage("writeCharacteristic: gattService null");
            return false;
        }

        BluetoothGattCharacteristic gattChar = gattService
                .getCharacteristic(java.util.UUID.fromString(characteristicUuid));

        if (gattChar == null) {
            sendConsoleMessage("writeCharacteristic: gattChar null");
            return false;
        }

        gattChar.setValue(value);
        return bluetooth_gatt.writeCharacteristic(gattChar);
    }

    // Read RRSI
    public void readRemoteRssi() {
        if (bluetooth_adapter == null || bluetooth_gatt == null) {
            return;
        }
        bluetooth_gatt.readRemoteRssi();
    }

}*/