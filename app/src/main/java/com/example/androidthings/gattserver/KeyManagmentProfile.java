package com.example.androidthings.gattserver;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class KeyManagmentProfile {
    public static final UUID KEYMANAGMENT_UUID = UUID.fromString("fb440001-8000-0080-0010-00000d180000");
    public static final UUID NEWKEY_UUID = UUID.fromString("fb440003-8000-0080-0010-00000d180000");
    public static final UUID OLDKEY_UUID =  UUID.fromString("fb440004-8000-0080-0010-00000d180000");
    public static final UUID DEVICEID_UUID =  UUID.fromString("fb440002-8000-0080-0010-00000d180000");

    public static BluetoothGattService createKeyManagmentService(){

        BluetoothGattService service = new BluetoothGattService(KEYMANAGMENT_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic oldKey = new BluetoothGattCharacteristic(OLDKEY_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE |BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_WRITE| BluetoothGattCharacteristic.PERMISSION_READ);
        BluetoothGattCharacteristic newKey = new BluetoothGattCharacteristic(NEWKEY_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE  |BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_WRITE| BluetoothGattCharacteristic.PERMISSION_READ);

        BluetoothGattCharacteristic deviceID = new BluetoothGattCharacteristic(DEVICEID_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE  |BluetoothGattCharacteristic.PROPERTY_READ ,
                 BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ  );
        //service.addCharacteristic(oldKey);
        //service.addCharacteristic(newKey);
        service.addCharacteristic(deviceID);
        return service;
    }
}
