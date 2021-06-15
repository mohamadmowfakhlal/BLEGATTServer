package com.example.androidthings.gattserver;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class SecurityProfile {
    public static final UUID SECURITY_UUID = UUID.fromString("fb340001-8000-0080-0010-00000d180000");
    public static final UUID KEY_UUID =  UUID.fromString("fb340002-8000-0080-0010-00000d180000");
    public static final UUID GATTCLIENT_NONCE_UUID = UUID.fromString("fb340003-8000-0080-0010-00000d180000");
    public static final UUID GATTSERVER_NONCE_UUID = UUID.fromString("fb340004-8000-0080-0010-00000d180000");
    public static final UUID MAC_UUID = UUID.fromString("fb340005-8000-0080-0010-00000d180000");
    public static final UUID REST_SERVER_NONCE_UUID = UUID.fromString("fb340006-8000-0080-0010-00000d180000");
    public static final UUID SESSIONNUMBER_UUID = UUID.fromString("fb340007-8000-0080-0010-00000d180000");
    public static final UUID GattSessionRestServerNonce_UUID = UUID.fromString("fb340008-8000-0080-0010-00000d180000");
    public static final UUID DEVICEID_UUID =  UUID.fromString("fb340009-8000-0080-0010-00000d180000");
    public static final UUID REALDATA_UUID =  UUID.fromString("fb340010-8000-0080-0010-00000d180000");
    public static BluetoothGattService createSecurityService(){
        BluetoothGattService service = new BluetoothGattService(SECURITY_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic clientnonce = new BluetoothGattCharacteristic(GATTCLIENT_NONCE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE |  BluetoothGattCharacteristic.PROPERTY_READ |  BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                 BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY);

        BluetoothGattCharacteristic MAC = new BluetoothGattCharacteristic(MAC_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE |  BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ );

        BluetoothGattCharacteristic restServerNonce = new BluetoothGattCharacteristic(REST_SERVER_NONCE_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE |  BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ );



        BluetoothGattCharacteristic GattServerNonce = new BluetoothGattCharacteristic(GATTSERVER_NONCE_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ  | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE  );

        BluetoothGattCharacteristic deviceID = new BluetoothGattCharacteristic(DEVICEID_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE  );

        BluetoothGattCharacteristic keyID = new BluetoothGattCharacteristic(KEY_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE  );

        BluetoothGattCharacteristic GattSessionRestServerNonce = new BluetoothGattCharacteristic(GattSessionRestServerNonce_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE  );

        BluetoothGattCharacteristic sessionNumber = new BluetoothGattCharacteristic(SESSIONNUMBER_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE  );

        BluetoothGattCharacteristic realData = new BluetoothGattCharacteristic(REALDATA_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE | BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE  );
        service.addCharacteristic(clientnonce);
        //service.addCharacteristic(GattServerNonce);
        service.addCharacteristic(MAC);
        //service.addCharacteristic(restServerNonce);
        service.addCharacteristic(deviceID);
        service.addCharacteristic(keyID);
        service.addCharacteristic(GattSessionRestServerNonce);
        service.addCharacteristic(realData);
        service.addCharacteristic(sessionNumber);
        //service.addCharacteristic(GattSessionRestServerNonce);
        return service;
    }
}
