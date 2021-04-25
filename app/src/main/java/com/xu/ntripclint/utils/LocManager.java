/**
 *
 */
package com.xu.ntripclint.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author w
 *
 */
public class LocManager {

    private static LocManager ourInstance;
    static Context mContext;

    private final List<LocChangeLisener> listeners = new ArrayList<LocChangeLisener>();
    private final List<LocChangeNmeaLisener> nmeaListeners = new ArrayList<>();
    String locationProvider = LocationManager.GPS_PROVIDER;

    public static LocManager getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new LocManager(context);
        }
        return ourInstance;
    }

    private LocManager(Context context) {
        mContext = context.getApplicationContext();
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
    }

    public LocationManager locationManager;
    public Location lastAMapLocation;
    public GpsStatus gpsStatus;
    public int gpsSatellitesNum = 0;
    private final GpsStatus.Listener statusListener = new GpsStatus.Listener() {
        @SuppressLint("MissingPermission")
        public void onGpsStatusChanged(int event) { // GPS状态变化时的回调，如卫星数
            gpsStatus = locationManager.getGpsStatus(gpsStatus); // 取当前状态
            updateGpsStatus(event, gpsStatus);

        }
    };
    private final GpsStatus.NmeaListener gpsNmeListener = new GpsStatus.NmeaListener() {

        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            Logs.d("onNmeaReceived222222222----" + nmea);
            for (LocChangeNmeaLisener nmeaListener : nmeaListeners) {
                nmeaListener.onLocationChanged(nmea, timestamp);
            }
        }
    };

    private void updateGpsStatus(int event, GpsStatus status) {
        if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
            int maxSatellites = status.getMaxSatellites();
            Iterator<GpsSatellite> it = status.getSatellites().iterator();
            //numSatelliteList.clear();
            int count = 0;
            while (it.hasNext() && count <= maxSatellites) {
                GpsSatellite s = it.next();
                //numSatelliteList.add(s);
                count++;
            }
            gpsSatellitesNum = count;
            Logs.d("gps status count:"+gpsSatellitesNum);
        }
    }

    LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            if (location != null) {
                lastAMapLocation = location;
                synchronized (listeners) {
                    for (LocChangeLisener listener : listeners) {
                        listener.onLocationChanged(location);
                    }
                }
                //Logs.w("location:" + location.toString());

//				} else {
//					if(lastAMapLocation!=null) {
//						lastAMapLocation.setErrorCode(amapLocation.getErrorCode());
//					}
//					Logs.w("location Error, ErrCode:"
//							+ amapLocation.getErrorCode() + ", errInfo:"
//							+ amapLocation.getErrorInfo());
//				}
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            Logs.w("location  onStatusChanged" + provider + "status:" + status);
        }

        public void onProviderEnabled(String provider) {
            Logs.w("location  onProviderEnabled:" + provider);
        }

        public void onProviderDisabled(String provider) {
            Logs.w("location  onProviderDisabled:" + provider);
        }
    };


    public Location getLastLocation() {
        return lastAMapLocation;
    }

    public GpsStatus getGpsStatus() {
        return gpsStatus;
    }

    public int getGpsSatellitesNum() {
        return gpsSatellitesNum;
    }

    public void openLocation(long timeInterval, float minDistance) {
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                OnNmeaMessageListener onNmeaMessageListener = new OnNmeaMessageListener() {
                    @Override
                    public void onNmeaMessage(String message, long timestamp) {
                        Logs.d("OnNmeaMessageListener:===" + message);
                        for (LocChangeNmeaLisener nmeaListener : nmeaListeners) {
                            nmeaListener.onLocationChanged(message, timestamp);
                        }
                    }
                };
                GnssStatus.Callback onGnssCallBack =new GnssStatus.Callback() {
                    @Override
                    public void onStarted() {
                        super.onStarted();
                        Logs.d("onGnssCallBack:===onStarted");
                    }

                    @Override
                    public void onStopped() {
                        super.onStopped();
                        Logs.d("onGnssCallBack:===onStopped");
                    }

                    @Override
                    public void onFirstFix(int ttffMillis) {
                        super.onFirstFix(ttffMillis);
                        Logs.d("onGnssCallBack:===onFirstFix");
                    }

                    @Override
                    public void onSatelliteStatusChanged(GnssStatus status) {
                        super.onSatelliteStatusChanged(status);
                        Logs.d("onGnssCallBack:===status::"+status.getSatelliteCount());
                    }
                };
                locationManager.registerGnssStatusCallback(onGnssCallBack);
                locationManager.addNmeaListener(onNmeaMessageListener);
            } else {
                locationManager.addNmeaListener(gpsNmeListener);
                locationManager.addGpsStatusListener(statusListener);
            }
            locationManager.requestLocationUpdates(locationProvider, timeInterval, minDistance, mLocationListener);
        }
//		if(mLocationClient == null){
//			mLocationClient = new AMapLocationClient(mContext);
//			mLocationClient.setLocationListener(mLocationListener);
//			mLocationOption = new AMapLocationClientOption();
//			// 设置定位模式 Hight_Accuracy为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
//			mLocationOption.setLocationMode(AMapLocationMode.Device_Sensors);
//			mLocationOption.setNeedAddress(true);
//			mLocationOption.setOnceLocation(false);
//			mLocationOption.setMockEnable(false);
//			mLocationOption.setInterval(1000);
//			mLocationClient.setLocationOption(mLocationOption);
//		}
//
//		if(!mLocationClient.isStarted()){
//			mLocationClient.startLocation();
//		}
    }

    public void stopLocation() {
        if (locationManager != null) {
            locationManager.removeUpdates(mLocationListener);
        }
//		if (mLocationClient != null) {
//			mLocationClient.stopLocation();
//			mLocationClient.onDestroy();
//        }
//		mLocationClient = null;
    }


    public void addListener(LocChangeLisener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void addListener(LocChangeNmeaLisener listener) {
        synchronized (nmeaListeners) {
            nmeaListeners.add(listener);
        }
    }

    public void removeListener(LocChangeNmeaLisener listener) {
        synchronized (nmeaListeners) {
            nmeaListeners.remove(listener);
        }
    }

    public void removeListener(LocChangeLisener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public interface LocChangeLisener {
        void onLocationChanged(Location amapLocation);
    }

    public interface LocChangeNmeaLisener {
        void onLocationChanged(String nmea, long time);
    }
}
