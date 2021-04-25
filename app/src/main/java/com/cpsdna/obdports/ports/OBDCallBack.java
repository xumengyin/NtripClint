/**
 * 
 */
package com.cpsdna.obdports.ports;

/**
 *
 */
public abstract class OBDCallBack {

	protected void onStart() {

	}

	protected abstract void onReceive(short id, byte[] data, byte[] allData);

	protected void onError(Exception e) {

	}

}
