package com.example.androidthings.gattserver;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class ConfigurationProfile {
    public static final UUID Configuration_UUID = UUID.fromString("fb340010-8000-0080-0010-00000d180000");
    public static final UUID DEVICEID_UUID =  UUID.fromString("fb340011-8000-0080-0010-00000d180000");
    public static final UUID KEY_UUID =  UUID.fromString("fb340014-8000-0080-0010-00000d180000");
   // public static final UUID NEWKEY_UUID =  UUID.fromString("fb440010-8000-0080-0010-00000d180000");
    public static BluetoothGattService createConfigurationService(){

        BluetoothGattService service = new BluetoothGattService(Configuration_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);


        BluetoothGattCharacteristic deviceID = new BluetoothGattCharacteristic(DEVICEID_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE  );

        BluetoothGattCharacteristic keyID = new BluetoothGattCharacteristic(KEY_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE  );

       // BluetoothGattCharacteristic NewkeyID = new BluetoothGattCharacteristic(NEWKEY_UUID,
       //         BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_READ ,
         //       BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE  );



        service.addCharacteristic(deviceID);
        service.addCharacteristic(keyID);
        //service.addCharacteristic(NewkeyID);
        return service;
    }
}
