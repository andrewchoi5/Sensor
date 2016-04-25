package uk.co.alt236.btlescan.activities;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import uk.co.alt236.bluetoothlelib.device.BluetoothLeDevice;
import uk.co.alt236.bluetoothlelib.resolvers.GattAttributeResolver;
import uk.co.alt236.bluetoothlelib.util.ByteUtils;
import uk.co.alt236.btlescan.R;
import uk.co.alt236.btlescan.services.BluetoothLeService;
public class DeviceControlActivity extends AppCompatActivity {
    public static final String EXTRA_DEVICE = "extra_device";
    public static final String EXTRA_DEVICE1 = "extra_device1";
    public static final String EXTRA_DEVICE2 = "extra_device2";
    public static final String EXTRA_DEVICE3 = "extra_device3";
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    private static final String LIST_NAME = "NAME";
    private static final String LIST_UUID = "UUID";
    private ArrayList<String> addresses;
    private ArrayList<String> addresses1;
    private ArrayList<String> addresses2;
    private ArrayList<String> addresses3;
    @Bind(R.id.gatt_services_list)
    protected ExpandableListView mGattServicesList;

    @Bind(R.id.connection_state)
    protected TextView mConnectionState;

//    @Bind(R.id.uuid)
    protected TextView mGattUUID;

//    @Bind(R.id.description)
    protected TextView mGattUUIDDesc;

    @Bind(R.id.rssiValue)
    protected TextView mRssiValue;

//    @Bind(R.id.data_as_string)
    protected TextView mDataAsString;

//    @Bind(R.id.data_as_array)
    protected TextView mDataAsArray;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothLeService mBluetoothLeService;
    private List<List<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(final ExpandableListView parent, final View v, final int groupPosition, final int childPosition, final long id) {
            if (mGattCharacteristics != null) {
                final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                final int charaProp = characteristic.getProperties();
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user interface.
                    if (mNotifyCharacteristic != null) {
                        mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);

                        mNotifyCharacteristic = null;
                    }
                    mBluetoothLeService.readCharacteristic(characteristic);
                }
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    mNotifyCharacteristic = characteristic;
                    mBluetoothLeService.setCharacteristicNotification(characteristic, true);

                }
                return true;
            }
            return false;
        }
    };

    private String mDeviceAddress;
    private String mDeviceAddress1;
    private String mDeviceAddress2;
    private String mDeviceAddress3;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName componentName, final IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect1(mDeviceAddress, mDeviceAddress1, mDeviceAddress2, mDeviceAddress3);
        }
        @Override
        public void onServiceDisconnected(final ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private String mDeviceName;
    private String mDeviceName1;
    private String mDeviceName2;
    private String mDeviceName3;
    private boolean mConnected = false;
    private String mExportString;
    boolean showSeries = true;


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        private int samplingCounter = 0;
        final private double lowerLimit = 2.35;
        final private double upperLimit = 16.2;
        private double[] timedX = new double[]{0.93,0.93,0.93,0.93}; //initial x for starting from the top.
        private double[] timedY = new double[]{upperLimit,upperLimit,upperLimit,upperLimit}; //initial y for starting from the top.
//        private double timedX = 0.8; //initial x for starting from the bottom.
//        private double timedY = 2.170000000000005; //initial y for starting from the bottom.
        private double[] oldAvg = new double[]{0,0,0,0};
        final double xIncrement = 0.27;
        final double yIncrement = 0.61;
        int dotNumber = -1;
        int dotNumber1 = -1;
        int dotNumber2 = -1;
        int dotNumber3 = -1;
        boolean[] firstTime = new boolean[4];
        double[] avg = new double[4];
        private int[] rssiReceivedArray;// = new int[4];
        double[] samplingArray = new double[9]; //can't be int[8] for some reason

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if(samplingCounter == 9){
                samplingCounter = 1;
            }
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                String address = intent.getStringExtra("address");
                String address1 = intent.getStringExtra("address1");
                String address2 = intent.getStringExtra("address2");
                String address3 = intent.getStringExtra("address3");
                if(addresses == null) {
                    addresses = new ArrayList<String>();
                }if(addresses1 == null) {
                    addresses1 = new ArrayList<String>();
                }if(addresses2 == null) {
                    addresses2 = new ArrayList<String>();
                }if(addresses3 == null) {
                    addresses3 = new ArrayList<String>();
                }
                if(!addresses.contains(address)) {
                    addresses.add(address);
                }if(!addresses1.contains(address1)) {
                    addresses1.add(address1);
                }if(!addresses2.contains(address2)) {
                    addresses2.add(address2);
                }if(!addresses3.contains(address3)) {
                    addresses3.add(address3);
                }
                dotNumber = addresses.indexOf(address);
                dotNumber1 = addresses1.indexOf(address1);
                dotNumber2 = addresses2.indexOf(address2);
                dotNumber3 = addresses3.indexOf(address3);
                rssiReceivedArray = new int[]{0,0,0,0};
                rssiReceivedArray = intent.getIntArrayExtra("rssiVal");
                double rssiValRaw,rssiValRaw1,rssiValRaw2,rssiValRaw3;
                rssiValRaw = rssiValRaw1= rssiValRaw2 = rssiValRaw3 = 0 ;
                if(rssiReceivedArray != null){
                    rssiValRaw = rssiReceivedArray[0];
                    rssiValRaw1 = rssiReceivedArray[1];
                    rssiValRaw2 = rssiReceivedArray[2];
                    rssiValRaw3 = rssiReceivedArray[3];
                }
                double rssiValRawNeg = rssiValRaw * (-1);
                double rssiValRawNeg1 = rssiValRaw1 * (-1);
                double rssiValRawNeg2 = rssiValRaw2 * (-1);
                double rssiValRawNeg3 = rssiValRaw3 * (-1);
                final double rssiVal = (100-0) * (rssiValRawNeg - 20) / (300-20) + 0;// 20 to 300. --> n * -1 + (300 + 20);
                final double rssiVal1 = (100-0) * (rssiValRawNeg1 - 20) / (300-20) + 0;// 20 to 300. --> n * -1 + (300 + 20);
                final double rssiVal2 = (100-0) * (rssiValRawNeg2 - 20) / (300-20) + 0;// 20 to 300. --> n * -1 + (300 + 20);
                final double rssiVal3 = (100-0) * (rssiValRawNeg3 - 20) / (300-20) + 0;// 20 to 300. --> n * -1 + (300 + 20);
                samplingArray[samplingCounter] = rssiVal;
                GraphView mGattRssi = (GraphView) findViewById(R.id.rssi);
                mGattRssi.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
                mGattRssi.setBackgroundResource(R.drawable.carousel);
                mGattRssi.setVisibility(View.VISIBLE);
                StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(mGattRssi);
                staticLabelsFormatter.setHorizontalLabels(new String[]{"", "", ""});
                staticLabelsFormatter.setVerticalLabels(new String[]{"", "", ""});
                mGattRssi.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
                mGattRssi.getGridLabelRenderer().setHighlightZeroLines(true);
                mGattRssi.getGridLabelRenderer().setVerticalLabelsVisible(true);
                mGattRssi.getViewport().setXAxisBoundsManual(true);
                mGattRssi.getViewport().setMinX(0);
                mGattRssi.getViewport().setMaxX(2.1); // orig 2
                mGattRssi.getViewport().setYAxisBoundsManual(true);
                mGattRssi.getViewport().setMinY(1.0); // original 1.0
                mGattRssi.getViewport().setMaxY(16.0); // original: 18.5
                mGattRssi.getViewport().setScrollable(true);
                for(int i = 0; i < 4; ++i){
                    avg[0] = rssiVal;
                    avg[1] = rssiVal1;
                    avg[2] = rssiVal2;
                    avg[3] = rssiVal3;
                    if(firstTime[i] == true){

                        timedX[i] = 1.34; //sweetspot for starting from top.
                    }
                    if(firstTime[i]){
                        if(timedY[i] <= 3.9 && timedX[i] >= 0.26 && timedX[i] <= 0.93){ //++--+-
                            timedX[i] += xIncrement;
                        }else if(timedY[i] <= 3.9 && timedX[i] >= 0.93 && timedX[i] <= 1.81 ){
                            timedX[i] += xIncrement;
                        }else if(timedY[i] >= 14.2 && timedX[i] >= 0.93 && timedX[i] <= 1.81){
                            timedX[i] -= xIncrement;
                        }else if(timedY[i] >= 14.2 && timedX[i] <= 0.93 && timedX[i] >= 0.27 ){
                            timedX[i] -= xIncrement;
                        }
                        if((timedX[i] >= 1.61 || timedX[i] >= 0.93) && timedY[i] <= upperLimit){
                            timedY[i] += yIncrement;
                        }else if((timedX[i] <= 0.27 || timedX[i] <= 0.93) && timedY[i] >= lowerLimit){
                            timedY[i] -= yIncrement;
                        }
                    }else{
                        double delta = 0.10; // 0.15
                        double calculatedDelta = Math.abs(avg[i] - oldAvg[i]) / Math.abs(oldAvg[i]);
//                        Log.e("DCA.java","oldAvg: " + oldAvg[i]);
//                        Log.e("DCA.java","avg: " + avg[i]);
//                        Log.e("DCA.java", "calculatedDelta: " + calculatedDelta);
                        if (Math.abs(avg[i] - oldAvg[i]) >= 0.0) {
                            if (timedY[i] <= 3.9 && timedX[i] >= 0.26 && timedX[i] <= 0.93) {
                                if(calculatedDelta >= delta) {
                                    timedX[i] += xIncrement;
                                }
                            }else if(timedY[i] <= 3.9 && timedX[i] >= 0.93 && timedX[i] <= 1.81) { // <=1.81
                                if(calculatedDelta >= delta) {
                                    timedX[i] += xIncrement;
                                }
                            }
                            else if (timedY[i] >= 14.2 && timedX[i] >= 0.93 && timedX[i] <= 1.81) {
                                if(calculatedDelta >= delta) {
                                    timedX[i] -= xIncrement; // 2.0
                                }
                            }
                            else if (timedY[i] >= 14.2 && timedX[i] <= 0.93 && timedX[i] >= 0.27) {
                                if(calculatedDelta >= delta) {
                                    timedX[i] -= xIncrement; // 2.0
                                }
                            }
                            if ((timedX[i] >= 1.61 || timedX[i] >= 0.93) && timedY[i] <= upperLimit) {
                                if(calculatedDelta >= delta) {
                                    timedY[i] += yIncrement;
                                }

                            } else if ((timedX[i] <= 0.27 || timedX[i] <= 0.93) && timedY[i] >= lowerLimit) {
                                if(calculatedDelta >= delta) {
                                    timedY[i] -= yIncrement;
                                }
                            }
                        }
                        if (avg[i] <= 7.0000000) { // <= 5.71415280
                            timedX[i] = 0.8; //initial x for starting from the bottom.
                            timedY[i] = 2.170000000000005;
                        }
                    }
                    mGattRssi.removeAllSeries();
                    PointsGraphSeries<DataPoint> series = new PointsGraphSeries<DataPoint>(new DataPoint[] {
                            new DataPoint(timedX[0], timedY[0])});
                    PointsGraphSeries<DataPoint> series1 = new PointsGraphSeries<DataPoint>(new DataPoint[] {
                            new DataPoint(timedX[1], timedY[1])});
                    PointsGraphSeries<DataPoint> series2 = new PointsGraphSeries<DataPoint>(new DataPoint[] {
                            new DataPoint(timedX[2], timedY[2])});
                    PointsGraphSeries<DataPoint> series3 = new PointsGraphSeries<DataPoint>(new DataPoint[] {
                            new DataPoint(timedX[3], timedY[3])});
                    Resources res = getResources();
                    Bitmap bitmap = BitmapFactory.decodeResource(res, R.drawable.redsuitcase);
                    Bitmap bitmap1 = BitmapFactory.decodeResource(res, R.drawable.yellow_suitcase);
                    Bitmap bitmap2 = BitmapFactory.decodeResource(res, R.drawable.green_suitcase);
                    Bitmap bitmap3 = BitmapFactory.decodeResource(res, R.drawable.blue_suitcase);
                   // Bitmap b = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length)
                    bitmap = Bitmap.createScaledBitmap(bitmap, 130, 170, false);
                    bitmap1 = Bitmap.createScaledBitmap(bitmap1, 130, 170, false);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, 130, 170, false);
                    bitmap3 = Bitmap.createScaledBitmap(bitmap3, 130, 170, false);
                    series.setBitmap(bitmap);
                    series1.setBitmap(bitmap1);
                    series2.setBitmap(bitmap2);
                    series3.setBitmap(bitmap3);
                    series.setShape(PointsGraphSeries.Shape.POINT);
                    series1.setShape(PointsGraphSeries.Shape.POINT);
                    series2.setShape(PointsGraphSeries.Shape.POINT);
                    series3.setShape(PointsGraphSeries.Shape.POINT);
                    series.setSize(38);
                    series1.setSize(38);
                    series2.setSize(38);
                    series3.setSize(38);
                    series.setColor(Color.CYAN);
                    series1.setColor(Color.WHITE);
                    series2.setColor(Color.YELLOW);
                    series3.setColor(Color.BLUE);
                    if(showSeries == true) {
                        mGattRssi.addSeries(series);
                        mGattRssi.addSeries(series1);
                        mGattRssi.addSeries(series2);
                        mGattRssi.addSeries(series3);
//                    mGattRssi.removeAllSeries();
                    }

//                    mRssiValue.setText(String.valueOf(rssiValRaw) + " dB (Raw Data)");
                    oldAvg[i] = avg[i];
                    firstTime[i] = false;
                }


            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                final String noData = getString(R.string.no_data);
                final String uuid = intent.getStringExtra(BluetoothLeService.EXTRA_UUID_CHAR);
                final byte[] dataArr = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA_RAW);
              //  mGattUUID.setText(tryString(uuid, noData));
                mGattUUIDDesc.setText(GattAttributeResolver.getAttributeName(uuid, getString(R.string.unknown)));
                mDataAsArray.setText(ByteUtils.byteArrayToHexString(dataArr));
                mDataAsString.setText(new String(dataArr));
            } else if(BluetoothLeService.ACTION_READ_RSSI.equals(action)) {
                final int rssiVal = intent.getIntExtra("rssiVal", 0);
                mGattUUID.setText(String.valueOf(rssiVal));
            }
            ++samplingCounter;
        }
    };

    private void clearUI() {
//        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
//        mGattUUID.setText(R.string.no_data);
//        mGattUUIDDesc.setText(R.string.no_data);
//        mDataAsArray.setText(R.string.no_data);
//        mDataAsString.setText(R.string.no_data);
//        mRssiValue.setText(R.string.no_data);
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(final List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        generateExportString(gattServices);

        String uuid = null;
        final String unknownServiceString = getResources().getString(R.string.unknown_service);
        final String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        final List<Map<String, String>> gattServiceData = new ArrayList<>();
        final List<List<Map<String, String>>> gattCharacteristicData = new ArrayList<>();
        mGattCharacteristics = new ArrayList<>();

        // Loops through available GATT Services.
        for (final BluetoothGattService gattService : gattServices) {
            final Map<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, GattAttributeResolver.getAttributeName(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            final List<Map<String, String>> gattCharacteristicGroupData = new ArrayList<>();
            final List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            final List<BluetoothGattCharacteristic> charas = new ArrayList<>();

            // Loops through available Characteristics.
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                final Map<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, GattAttributeResolver.getAttributeName(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }

            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        final SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );

        mGattServicesList.setAdapter(gattServiceAdapter);
        invalidateOptionsMenu();
    }

    private void generateExportString(final List<BluetoothGattService> gattServices) {
        final String unknownServiceString = getResources().getString(R.string.unknown_service);
        final String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        final StringBuilder exportBuilder = new StringBuilder();

        exportBuilder.append("Device Name: ");
        exportBuilder.append(mDeviceName);
        exportBuilder.append('\n');
        exportBuilder.append("Device Address: ");
        exportBuilder.append(mDeviceAddress);
        exportBuilder.append('\n');
        exportBuilder.append('\n');

        exportBuilder.append("Services:");
        exportBuilder.append("--------------------------");
        exportBuilder.append('\n');

        String uuid = null;
        for (final BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();

            exportBuilder.append(GattAttributeResolver.getAttributeName(uuid, unknownServiceString));
            exportBuilder.append(" (");
            exportBuilder.append(uuid);
            exportBuilder.append(')');
            exportBuilder.append('\n');

            final List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();

                exportBuilder.append('\t');
                exportBuilder.append(GattAttributeResolver.getAttributeName(uuid, unknownCharaString));
                exportBuilder.append(" (");
                exportBuilder.append(uuid);
                exportBuilder.append(')');
                exportBuilder.append('\n');
            }

            exportBuilder.append('\n');
            exportBuilder.append('\n');
        }

        exportBuilder.append("--------------------------");
        exportBuilder.append('\n');

        mExportString = exportBuilder.toString();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        // TODO: 16-03-03 add hidden button for showing/hiding the luggages
        super.onCreate(savedInstanceState);
        showSeries = false;
        setContentView(R.layout.activity_gatt_services);
        final Intent intent = getIntent();
        final BluetoothLeDevice device = intent.getParcelableExtra(EXTRA_DEVICE);
        final BluetoothLeDevice device1 = intent.getParcelableExtra(EXTRA_DEVICE1);
        final BluetoothLeDevice device2 = intent.getParcelableExtra(EXTRA_DEVICE2);
        final BluetoothLeDevice device3 = intent.getParcelableExtra(EXTRA_DEVICE3);
        mDeviceName = device.getName();
        mDeviceName1 = device1.getName();
        mDeviceName2 = device2.getName();
        mDeviceName3 = device3.getName();
        mDeviceAddress  = device.getAddress();
        mDeviceAddress1 = device1.getAddress();
        mDeviceAddress2 = device2.getAddress();
        mDeviceAddress3 = device3.getAddress();
        ButterKnife.bind(this);
//        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress); //For showing MAC Address
        mGattServicesList.setOnChildClickListener(servicesListClickListner);


//        getSupportActionBar().setTitle(mDeviceName);
        getSupportActionBar().setTitle("Your Baggages");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final Intent gattServiceIntent = new Intent(this, BluetoothLeService.class); //TODO:
        final Intent gattServiceIntent1 = new Intent(this, BluetoothLeService.class); //TODO:
        final Intent gattServiceIntent2 = new Intent(this, BluetoothLeService.class); //TODO:
        final Intent gattServiceIntent3 = new Intent(this, BluetoothLeService.class); //TODO:
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        bindService(gattServiceIntent1, mServiceConnection, BIND_AUTO_CREATE);
        bindService(gattServiceIntent2, mServiceConnection, BIND_AUTO_CREATE);
        bindService(gattServiceIntent3, mServiceConnection, BIND_AUTO_CREATE);

        Button b = (Button) findViewById(R.id.hideShowbtn);
        b.setVisibility(View.VISIBLE);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideShow(v);
            }
        });


    }

    public void hideShow(View v) {
        if(showSeries == true){
            showSeries = false;
        }else{
            showSeries = true;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu); //// TODO: 16-02-25  
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false); // added
//            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
//            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);

            menu.findItem(R.id.menu_connect).setVisible(false); // added
        }
        if (mExportString == null) {
            menu.findItem(R.id.menu_share).setVisible(false);
        } else {
            menu.findItem(R.id.menu_share).setVisible(true);
        }
        return true;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
//        mBluetoothLeService.connect(mDeviceAddress);
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect1(mDeviceAddress, mDeviceAddress1, mDeviceAddress2, mDeviceAddress3);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect1();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_share:
                final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                final String subject = getString(R.string.exporter_email_device_services_subject, mDeviceName, mDeviceAddress);

                intent.setType("text/plain");
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
                intent.putExtra(android.content.Intent.EXTRA_TEXT, mExportString);

                startActivity(Intent.createChooser(
                        intent,
                        getString(R.string.exporter_email_device_list_picker_text)));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect1(mDeviceAddress, mDeviceAddress1, mDeviceAddress2, mDeviceAddress3);
            Log.d(TAG, "Connect request result=" + result);
        }
    }
    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int colourId;
                switch (resourceId) {
                    case R.string.connected:
                        colourId = android.R.color.holo_green_dark;
                        break;
                    case R.string.disconnected:
                        colourId = android.R.color.holo_red_dark;
                        break;
                    default:
                        colourId = android.R.color.black;
                        break;
                }
//                mConnectionState.setText(resourceId);
//                mConnectionState.setTextColor(getResources().getColor(colourId));
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private static String tryString(final String string, final String fallback) {
        if (string == null) {
            return fallback;
        } else {
            return string;
        }
    }
    final public double computeAvg(double[] n) {
        double s = 0;
        for(int i=1; i<n.length; ++i) s+=n[i];
        return (s / (n.length - 1)); //n.length - 1 = 8
    }
}