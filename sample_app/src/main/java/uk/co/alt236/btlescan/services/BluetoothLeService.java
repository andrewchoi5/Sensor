package uk.co.alt236.btlescan.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BluetoothLeService extends Service {
    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_READ_RSSI = "com.example.bluetooth.le.ACTION_READ_RSSI"; //Created (D.P.)
    public final static String EXTRA_DATA_RAW = "com.example.bluetooth.le.EXTRA_DATA_RAW";
    public final static String EXTRA_UUID_CHAR = "com.example.bluetooth.le.EXTRA_UUID_CHAR";
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private final IBinder mBinder = new LocalBinder();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private String mBluetoothDeviceAddress1;
    private String mBluetoothDeviceAddress2;
    private String mBluetoothDeviceAddress3;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGatt mBluetoothGatt1;
    private BluetoothGatt mBluetoothGatt2;
    private BluetoothGatt mBluetoothGatt3;
//    private BluetoothGatt mBluetoothGatt2;
//    private BluetoothGatt mBluetoothGatt3;
    private int mConnectionState = STATE_DISCONNECTED;
    private Timer mRssiTimer; //added Feb 5
    private ArrayList<String> addresses;
    private int[] rssiArray = new int[4];
    final private int time = 500; // in ms. 700ms = 0.7s




    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    public final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        public boolean isRssiConnected;
        public int rssiValue = -1; //rssi magnitude
        public int rssiStatus = -1; //rssi Status
        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            gatt.readRemoteRssi(); //added code;
            Log.e("BluetoothLeService", "onCharacteristicChanged Called");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            final String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED){
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;

                TimerTask task = new TimerTask(){
                    boolean isConnected;
                    @Override
                    public void run(){
                        isConnected = gatt.readRemoteRssi();
                    }
                };
                mRssiTimer = new Timer(); // Broadcast Timer
                mRssiTimer.schedule(task, time, time); // My Timer. 500 ms = 1 s
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }
        @Override //added code callback function;
        public void onReadRemoteRssi(final BluetoothGatt gatt, final int rssi, final int status) {
            final Intent intent1 = new Intent(ACTION_GATT_SERVICES_DISCOVERED);
//            if(addresses == null) {
//                addresses = new ArrayList<String>();
//            }
//            if(!addresses.contains(gatt.getDevice().getAddress())) {
//                addresses.add(gatt.getDevice().getAddress());
//            }
//            if(addresses.indexOf(gatt.getDevice().getAddress()) == 0) {
                rssiArray[0] = rssi;
                intent1.putExtra("rssiVal", rssiArray);
                intent1.putExtra("address", gatt.getDevice().getAddress());
//            } else {
//                intent1.putExtra("rssiVal" + addresses.indexOf(gatt.getDevice().getAddress()), rssi);
//            }
            sendBroadcast(intent1);
//            broadcastUpdate(intentAction);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, String.format("BluetoothGatt ReadRssi[%d]", rssi));
                this.rssiStatus = status;
                this.rssiValue = rssi;
            }
        }
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
    };








    public final BluetoothGattCallback mGattCallback1 = new BluetoothGattCallback() {
        public boolean isRssiConnected;
        public int rssiValue = -1; //rssi magnitude
        public int rssiStatus = -1; //rssi Status
        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            gatt.readRemoteRssi(); //added code;
            Log.e("BluetoothLeService", "onCharacteristicChanged Called");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            final String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED){
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                TimerTask task = new TimerTask(){
                    boolean isConnected;
                    @Override
                    public void run(){
                        isConnected = gatt.readRemoteRssi();
                    }
                };
                mRssiTimer = new Timer(); // Broadcast Timer
                mRssiTimer.schedule(task, time, time); // My Timer. 500 ms = 1 s
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }
        @Override //added code callback function;
        public void onReadRemoteRssi(final BluetoothGatt gatt, final int rssi, final int status) {
            final Intent intent1 = new Intent(ACTION_GATT_SERVICES_DISCOVERED);
            rssiArray[1] = rssi;
            intent1.putExtra("rssiVal1", rssiArray);
            intent1.putExtra("address1", gatt.getDevice().getAddress());
//            sendBroadcast(intent1);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, String.format("BluetoothGatt ReadRssi[%d]", rssi));
                this.rssiStatus = status;
                this.rssiValue = rssi;
            }
        }
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
              //  broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
    };















    public final BluetoothGattCallback mGattCallback2 = new BluetoothGattCallback() {
        public boolean isRssiConnected;
        public int rssiValue = -1; //rssi magnitude
        public int rssiStatus = -1; //rssi Status
        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            gatt.readRemoteRssi(); //added code;
            Log.e("BluetoothLeService", "onCharacteristicChanged Called");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            final String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED){
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                TimerTask task = new TimerTask(){
                    boolean isConnected;
                    @Override
                    public void run(){
                        isConnected = gatt.readRemoteRssi();
                    }
                };
                mRssiTimer = new Timer(); // Broadcast Timer
                mRssiTimer.schedule(task, time, time); // My Timer. 500 ms = 1 s
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }
        @Override //added code callback function;
        public void onReadRemoteRssi(final BluetoothGatt gatt, final int rssi, final int status) {
            final Intent intent1 = new Intent(ACTION_GATT_SERVICES_DISCOVERED);
            rssiArray[2] = rssi;
            intent1.putExtra("rssiVal2", rssiArray);
            intent1.putExtra("address2", gatt.getDevice().getAddress());
//            sendBroadcast(intent1);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, String.format("BluetoothGatt ReadRssi[%d]", rssi));
                this.rssiStatus = status;
                this.rssiValue = rssi;
            }
        }
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
              //  broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
    };













    public final BluetoothGattCallback mGattCallback3 = new BluetoothGattCallback() {
        public boolean isRssiConnected;
        public int rssiValue = -1; //rssi magnitude
        public int rssiStatus = -1; //rssi Status
        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            gatt.readRemoteRssi(); //added code;
            Log.e("BluetoothLeService", "onCharacteristicChanged Called");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            final String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED){
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                TimerTask task = new TimerTask(){
                    boolean isConnected;
                    @Override
                    public void run(){
                        isConnected = gatt.readRemoteRssi();
                    }
                };
                mRssiTimer = new Timer(); // Broadcast Timer
                mRssiTimer.schedule(task, time, time); // My Timer. 500 ms = 1 s
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }
        @Override //added code callback function;
        public void onReadRemoteRssi(final BluetoothGatt gatt, final int rssi, final int status) {
            final Intent intent1 = new Intent(ACTION_GATT_SERVICES_DISCOVERED);
            rssiArray[3] = rssi;
            intent1.putExtra("rssiVal3", rssiArray);
            intent1.putExtra("address3", gatt.getDevice().getAddress());
          //  sendBroadcast(intent1);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, String.format("BluetoothGatt ReadRssi[%d]", rssi));
                this.rssiStatus = status;
                this.rssiValue = rssi;
            }
        }
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
               // broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
    };


















    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID_CHAR, characteristic.getUuid().toString());

        // Always try to add the RAW value
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            intent.putExtra(EXTRA_DATA_RAW, data);
        }

        sendBroadcast(intent);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null && mBluetoothGatt1 == null && mBluetoothGatt2 == null && mBluetoothGatt3 == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt1.close();
        mBluetoothGatt2.close();
        mBluetoothGatt3.close();

        mBluetoothGatt = null;
        mBluetoothGatt1 = null;
        mBluetoothGatt2 = null;
        mBluetoothGatt3 = null;

        if(mBluetoothGatt1 != null ){
            mBluetoothGatt1.close();
            mBluetoothGatt1 = null;
        }
        if(mBluetoothGatt2 != null ){
            mBluetoothGatt2.close();
            mBluetoothGatt2 = null;
        }
        if(mBluetoothGatt3 != null ){
            mBluetoothGatt3.close();
            mBluetoothGatt3 = null;
        }

    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {

            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }







    public boolean connect1(final String address, final String address1, final String address2, final String address3) {
        if (mBluetoothAdapter == null || address == null || address1 == null || address2 == null || address3 == null ) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect() && mBluetoothGatt1.connect() && mBluetoothGatt2.connect() && mBluetoothGatt3.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        final BluetoothDevice device1 = mBluetoothAdapter.getRemoteDevice(address1);
        final BluetoothDevice device2 = mBluetoothAdapter.getRemoteDevice(address2);
        final BluetoothDevice device3 = mBluetoothAdapter.getRemoteDevice(address3);
        if (device == null && device1 == null && device2 == null && device3 == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mBluetoothGatt1 = device1.connectGatt(this, false, mGattCallback1);
        mBluetoothGatt2 = device2.connectGatt(this, false, mGattCallback2);
        mBluetoothGatt3 = device3.connectGatt(this, false, mGattCallback3);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mBluetoothDeviceAddress1 = address1;
        mBluetoothDeviceAddress2 = address2;
        mBluetoothDeviceAddress3 = address3;
        mConnectionState = STATE_CONNECTING;
//        mConnectionState = STATE_CONNECTING;
        return true;
    }











    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void disconnect1() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || mBluetoothGatt1 == null || mBluetoothGatt2 == null || mBluetoothGatt3 == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        mBluetoothGatt1.disconnect();
        mBluetoothGatt2.disconnect();
        mBluetoothGatt3.disconnect();

    }


    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        if(mBluetoothGatt1 != null) {
            mBluetoothGatt1.readCharacteristic(characteristic);
        }
        if(mBluetoothGatt2 != null) {
            mBluetoothGatt2.readCharacteristic(characteristic);
        }
        if(mBluetoothGatt3 != null) {
            mBluetoothGatt3.readCharacteristic(characteristic);
        }

    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if(mBluetoothGatt1 != null){
            mBluetoothGatt1.setCharacteristicNotification(characteristic, enabled);
        }
        if(mBluetoothGatt2 != null){
            mBluetoothGatt2.setCharacteristicNotification(characteristic, enabled);
        }
        if(mBluetoothGatt3 != null){
            mBluetoothGatt3.setCharacteristicNotification(characteristic, enabled);
        }
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
}