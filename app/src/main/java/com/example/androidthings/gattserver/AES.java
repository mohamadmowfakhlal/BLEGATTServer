package com.example.androidthings.gattserver;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AES {

	public  byte[] encrypt(byte[] input, byte[] key) {
		byte[] crypted = null;
		try {

			SecretKeySpec skey = new SecretKeySpec(key, "AES");

			Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, skey);
			crypted = cipher.doFinal(input);
		} catch (Exception e) {
			System.out.println(e.toString());
		}

		return crypted;
	}

	public static byte[] decrypt(byte[] input, byte[] key) {
		byte[] output = null;
		try {
			SecretKeySpec skey = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, skey);
			output = cipher.doFinal(input);
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return  output;
	}
	public  byte[] encryptwihpadding(byte[] input, byte[] key) {
		byte[] crypted = null;
		try {

			SecretKeySpec skey = new SecretKeySpec(key, "AES");

			Cipher cipher = Cipher.getInstance("AES/ECB/pkcs5padding ");
			cipher.init(Cipher.ENCRYPT_MODE, skey);
			crypted = cipher.doFinal(input);
		} catch (Exception e) {
			System.out.println(e.toString());
		}

		return crypted;
	}


	public static byte[] decryptwithpadding(byte[] input, byte[] key) {
		byte[] output = null;
		try {
			SecretKeySpec skey = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/pkcs5padding");
			cipher.init(Cipher.DECRYPT_MODE, skey);
			output = cipher.doFinal(input);
		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return  output;
	}
	public static Mac mac;
	public  static void initMAC(SecretKeySpec key) {
		byte[] output = null;
		try {
			mac = Mac.getInstance("HmacMD5");
			String algorithm  = "RawBytes";
			//SecretKeySpec macKey = new SecretKeySpec(key, algorithm);
			mac.init(key);
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	public  static byte[]  calculateMAC(byte[] data) {
		byte[] macBytes = null;
		try {
			macBytes = mac.doFinal(data);

		} catch (Exception e) {
			System.out.println(e.toString());
		}
		return  macBytes;
	}
}