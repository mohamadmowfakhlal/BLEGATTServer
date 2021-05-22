package com.example.androidthings.gattserver;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Nonces implements Serializable {
public String CNonce;
public String SNonce;
public String MAC;
public String getCNonce() {
	return CNonce;
}
public void setCNonce(String cNonce) {
	CNonce = cNonce;
}
public String getSNonce() {
	return SNonce;
}
public void setSNonce(String sNonce) {
	SNonce = sNonce;
}
public String getMAC() {
	return MAC;
}
public void setMAC(String mAC) {
	MAC = mAC;
}

	public JSONObject toJSON() throws JSONException {

		JSONObject jo = new JSONObject();
		jo.put("SNonce", SNonce);
		jo.put("MAC", MAC);
		jo.put("CNonce",CNonce);

		return jo;
	}
}