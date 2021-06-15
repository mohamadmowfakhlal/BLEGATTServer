/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.gattserver;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

public class GattServerActivity extends Activity {
    private static final String TAG = GattServerActivity.class.getSimpleName();
    /* Local UI */
    private TextView mLocalTimeView;
    /* Bluetooth API */
    private BluetoothManager mBluetoothManager;
    //private BluetoothConfigManager configManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    /* Collection of notification subscribers */
    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();
    private byte[] ReceivedClientNonce = null;
    private byte[] GattServerNonce = null;
    AES aesinstance;
    private String index;
    private byte[] key = null;
    private boolean clientauthenticated;
    private HashMap<BluetoothDevice,String> masterSessionKeys= new HashMap<BluetoothDevice, String>();

    private byte[] sessionKey = new byte[16];
    private byte[] MAC = null;
    private byte[] restServerNonce;
    //private byte[] restEncryptedServerNonce;
    private byte[] deviceID = "0000000000".getBytes();
    private boolean authenticated= false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        mLocalTimeView = (TextView) findViewById(R.id.text_time);

        // Devices with a display should not go to sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        //configManager = BluetoothConfigManager.getInstance();
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        // We can't continue without proper Bluetooth support
        if (!checkBluetoothSupport(bluetoothAdapter)) {
            finish();
        }

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
        //connectionManager.registerConnectionCallback(connectionCallback);
        //connectionManager.registerPairingCallback(pairingCallback);
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services");
            startAdvertising();
            startServer();
        }
        aesinstance = new AES();
        /*
        SQLiteDatabase mydatabase = openOrCreateDatabase("securityDatabase",MODE_PRIVATE,null);
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS connectedDevices(deviceID VARCHAR,symKey BLOB);");
        mydatabase.execSQL("INSERT INTO connectedDevices VALUES('0000000000','0000000000000000');");
        Cursor resultSet = mydatabase.rawQuery("Select * from connectedDevices",null);
        resultSet.moveToFirst();*/
        try {
            createSecretFile();
            key = readKeyInFile().getBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //writeKeyInFile("1111222233334444");
        //key = readKeyFromFile().getBytes();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Register for system clock events
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

        registerReceiver(mTimeReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mTimeReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            stopServer();
            stopAdvertising();
        }

        unregisterReceiver(mBluetoothReceiver);
        //connectionManager.unregisterPairingCallback(pairingCallback);
        //connectionManager.unregisterConnectionCallback(connectionCallback);
    }

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     *
     * @param bluetoothAdapter System {@link BluetoothAdapter}.
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {

        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }

    /**
     * Listens for system time changes and triggers a notification to
     * Bluetooth subscribers.
     */
    private BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte adjustReason;
            switch (intent.getAction()) {
                case Intent.ACTION_TIME_CHANGED:
                    adjustReason = TimeProfile.ADJUST_MANUAL;
                    break;
                case Intent.ACTION_TIMEZONE_CHANGED:
                    adjustReason = TimeProfile.ADJUST_TIMEZONE;
                    break;

                default:
                case Intent.ACTION_TIME_TICK:
                    adjustReason = TimeProfile.ADJUST_NONE;
                    break;

            }
            long now = System.currentTimeMillis();
            notifyRegisteredDevices(now, adjustReason);
            updateLocalUi(now);
        }
    };

    /**
     * Listens for Bluetooth adapter events to enable/disable
     * advertising and server functionality.
     */
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    //configManager.setIoCapability(0);
                    startAdvertising();
                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopServer();
                    stopAdvertising();
                    break;
                default:
                    // Do nothing
            }

        }
    };

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            System.out.println("does not support");
        }
        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            //Device does not support Bluetooth LE
            System.out.println("does not support");
        }

        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(TimeProfile.TIME_SERVICE))
                .build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, mAdvertiseCallback);
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    private void startServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            return;
        }

        mBluetoothGattServer.addService(TimeProfile.createTimeService());

        // Initialize the local UI
        updateLocalUi(System.currentTimeMillis());
        mBluetoothGattServer.addService(SecurityProfile.createSecurityService());
        //mBluetoothGattServer.addService(ConfigurationProfile.createConfigurationService());

    }

    /**
     * Shut down the GATT server.
     */
    private void stopServer() {
        if (mBluetoothGattServer == null) return;

        mBluetoothGattServer.close();
    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: " + errorCode);
        }
    };

    /**
     * Send a time service notification to any devices that are subscribed
     * to the characteristic.
     */
    private void notifyRegisteredDevices(long timestamp, byte adjustReason) {
        if (mRegisteredDevices.isEmpty()) {
            Log.i(TAG, "No subscribers registered");
            return;
        }
        byte[] exactTime = TimeProfile.getExactTime(timestamp, adjustReason);

        Log.i(TAG, "Sending update to " + mRegisteredDevices.size() + " subscribers");
        for (BluetoothDevice device : mRegisteredDevices) {
            BluetoothGattCharacteristic timeCharacteristic = mBluetoothGattServer
                    .getService(TimeProfile.TIME_SERVICE)
                    .getCharacteristic(TimeProfile.CURRENT_TIME);
            timeCharacteristic.setValue(exactTime);
            mBluetoothGattServer.notifyCharacteristicChanged(device, timeCharacteristic, false);
        }
    }



    /**
     * Update graphical UI on devices that support it with the current time.
     */
    private void updateLocalUi(long timestamp) {
        Date date = new Date(timestamp);
        String displayDate = DateFormat.getMediumDateFormat(this).format(date)
                + "\n"
                + DateFormat.getTimeFormat(this).format(date);
        mLocalTimeView.setText(displayDate);
    }

    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {


        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: " + device);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                masterSessionKeys.remove(device);
                Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
                //Remove device from any active subscriptions
                mRegisteredDevices.remove(device);
            }
        }
        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            System.out.println("mtu value" +mtu);
        }
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
        if (SecurityProfile.DEVICEID_UUID.equals(characteristic.getUuid())) {
                Log.i(TAG, "Read deviceID");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        deviceID);
            } else if (SecurityProfile.SESSIONNUMBER_UUID.equals(characteristic.getUuid())) {
        }
         else if (SecurityProfile.REALDATA_UUID.equals(characteristic.getUuid())) {
                System.out.println("real data encrypted at server side");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        "realData".getBytes());
            } else if (SecurityProfile.KEY_UUID.equals(characteristic.getUuid())) {
                System.out.println("key  at server side");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        key);
            } else   if(SecurityProfile.GattSessionRestServerNonce_UUID.equals(characteristic.getUuid())) {
                System.out.println("GattSessionRestServerNonce_UUID  at server side");
                //encrypt client nonce
                byte[] encryptedClientNonce = aesinstance.encrypt(ReceivedClientNonce, key);
                //generate a server nonce
                GattServerNonce = generateNonce();
                //encrypt the server nonce
                byte[] encryptedGattServerNonce = aesinstance.encrypt(GattServerNonce, key);
                byte[] concatenatednonces = new byte[encryptedClientNonce.length + encryptedGattServerNonce.length+deviceID.length];
                System.arraycopy(encryptedClientNonce, 0, concatenatednonces, 0, encryptedClientNonce.length);
                System.arraycopy( encryptedGattServerNonce, 0, concatenatednonces, encryptedClientNonce.length,  encryptedGattServerNonce.length);
                System.arraycopy( deviceID, 0, concatenatednonces, encryptedClientNonce.length+encryptedGattServerNonce.length,  deviceID.length);

                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        concatenatednonces);
              }
              else{
                // Invalid characteristic
                Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            if (TimeProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                Log.d(TAG, "Config descriptor read");
                byte[] returnValue;
                if (mRegisteredDevices.contains(device)) {
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                } else {
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                }
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        returnValue);
            } else {
                Log.w(TAG, "Unknown descriptor read request");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
            if (TimeProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: " + device);
                    mRegisteredDevices.add(device);
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: " + device);
                    mRegisteredDevices.remove(device);
                }

                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            } else {
                Log.w(TAG, "Unknown descriptor write request");
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value) {

            if (SecurityProfile.GATTCLIENT_NONCE_UUID.equals(characteristic.getUuid())) {
                System.out.println("client nonce is received" + value.toString());
                ReceivedClientNonce = value;
            }else if (SecurityProfile.MAC_UUID.equals(characteristic.getUuid())) {
                if(Arrays.equals(MAC,value)){
                    System.out.println("Data is authenticated");
                }
            } else if(SecurityProfile.DEVICEID_UUID.equals(characteristic.getUuid())){
                //if(authenticated){
                    byte [] decrypteddeviceID = aesinstance.decryptwithpadding(value,masterSessionKeys.get(device).getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
                    deviceID= decrypteddeviceID;
                    characteristic.setValue(deviceID);
                //}
            }else  if(SecurityProfile.KEY_UUID.equals(characteristic.getUuid())){
                byte [] decryptedKey= aesinstance.decrypt(value,masterSessionKeys.get(device).getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
                key = decryptedKey;
                final String newkey = new String(key,java.nio.charset.StandardCharsets.ISO_8859_1);
                System.out.println("new key"+newkey);
                writeKeyInFile(newkey);
            }else  if(SecurityProfile.GattSessionRestServerNonce_UUID.equals(characteristic.getUuid())) {
                byte[] GATTNONCE = new byte[16];
                //byte[] RestNONCE = new byte[16];
                System.arraycopy(value, 0, GATTNONCE, 0, 16);
                System.arraycopy(value, 16, sessionKey, 0, 16);
                //System.arraycopy(value, 32, RestNONCE, 0, 16);
                //System.arraycopy(value, 48, deviceadd, 0, 16);
                //System.out.println("rest server nonce ");
                if (Arrays.equals(GattServerNonce, GATTNONCE)) {
                    clientauthenticated = true;
                    System.out.println("server are sure about the client is real one");
                    // I suggest to change the key in gatt server and rest server every time we connect so we have a fresh key that change autmatically at
                    // rest server and gatt client
                    sessionKey = aesinstance.decrypt(sessionKey, key);
                    //int sessionNumber = masterSessionKeys.size()+1;
                    //index = String.valueOf(sessionNumber);
                    if(!masterSessionKeys.containsKey(device))
                        masterSessionKeys.put(device,new String(sessionKey,java.nio.charset.StandardCharsets.ISO_8859_1));
                    //System.out.println("does  generate session key"+sessionKey+index);
                    //restServerNonce = aesinstance.decrypt(RestNONCE, key);
                    //restEncryptedServerNonce = aesinstance.encrypt(restServerNonce, masterSessionKeys.get(device1));

                }else{
                    System.out.println("fake client");
                    //disconnect it
                }


            } else if (SecurityProfile.REALDATA_UUID.equals(characteristic.getUuid())){
                //value = aesinstance.decryptwithpadding(value, masterSessionKeys.get(device).getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
                /*for (Map.Entry<BluetoothDevice,String> entry : masterSessionKeys.entrySet()) {
                    BluetoothDevice key = entry.getKey();
                    System.out.println(""+key);
                    String value1 = entry.getValue();
                    System.out.println(""+value1);
                    // do stuff
                }*/
                //System.out.println("write Real data value" + masterSessionKeys.get(device));
                SecretKeySpec macKey = new SecretKeySpec(masterSessionKeys.get(device).getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), "HmacMD5");
                aesinstance.initMAC(macKey);
                MAC = aesinstance.calculateMAC(value);
                System.out.println( new String(value, java.nio.charset.StandardCharsets.ISO_8859_1));
            }else if (SecurityProfile.SESSIONNUMBER_UUID.equals(characteristic.getUuid())) {
                //index = new String(value,java.nio.charset.StandardCharsets.ISO_8859_1);
            }

        }        // util to print bytes in hex
    };

    public byte[] generateNonce() {
        byte[] Snonce = new byte[16];
        new SecureRandom().nextBytes(Snonce);
        return Snonce;
    }

    public void createSecretFile() throws IOException {
        try {
            // catches IOException below
            final String key = new String("0000000000000000");
            FileOutputStream fOut = openFileOutput("secretfile.txt",
                    MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            // Write the string to the file
            osw.write(key);

            /* ensure that everything is
             * really written out and close */
            osw.flush();
            osw.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public String readKeyInFile() {
        String readString = null;
        try {
//Reading the file back...

            /* We have to use the openFileInput()-method
             * the ActivityContext provides.
             * Again for security reasons with
             * openFileInput(...) */
            //final String TESTSTRING = new String("Hello Android");

            FileInputStream fIn = openFileInput("secretfile.txt");
            InputStreamReader isr = new InputStreamReader(fIn);

            /* Prepare a char-Array that will
             * hold the chars we read back in. */
            char[] inputBuffer = new char[16];

            // Fill the Buffer with data from the file
            isr.read(inputBuffer);

            // Transform the chars to a String
             readString = new String(inputBuffer);

            // Check if we read back the same chars that we had written out
            //boolean isTheSame = TESTSTRING.equals(readString);

            //Log.i("File Reading stuff", "success = " + isTheSame);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return readString;

}
    public void writeKeyInFile(String key) {
        try {
            // catches IOException below
            //final String key = new String("1111222233334444");

            /* We have to use the openFileOutput()-method
             * the ActivityContext provides, to
             * protect your file from others and
             * This is done for security-reasons.
             * We chose MODE_WORLD_READABLE, because
             *  we have nothing to hide in our file */
            FileOutputStream fOut = openFileOutput("secretfile.txt",
                    MODE_PRIVATE);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);

            // Write the string to the file
            osw.write(key);

            /* ensure that everything is
             * really written out and close */
            osw.flush();
            osw.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

}
