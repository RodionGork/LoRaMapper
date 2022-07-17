package com.github.rodiongork.loramapper;

import android.*;
import android.app.*;
import android.os.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.bluetooth.*;
import android.location.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {

    public static class DeviceDescription {
        
        private String name, mac;
        
        private DeviceDescription(BluetoothDevice d) {
            name = d.getName();
            mac = d.getAddress();
        }
        
        public String toString() {
            return name;
        }
        
    }

    private TextView textGps, textLora, textSignal;
    private Spinner devs;
    private Timer timer;
    private LocationManager locMgr;
    private BluetoothSocket bts;
    private String received = "";
    private Deque<String> rcvLines = new LinkedBlockingDeque<>();

    LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location loc) {
            textGps.setText(loc.getLatitude() + " " + loc.getLongitude());
        }
        @Override
        public void onProviderDisabled(String provider) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.main);
        textGps = (TextView) findViewById(R.id.text_gps);
        textSignal = (TextView) findViewById(R.id.text_signal);
        textLora = (TextView) findViewById(R.id.text_lora);
        locMgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 13);
        populateDeviceList();
        Button btn = (Button) findViewById(R.id.connect_btn);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DeviceDescription dev = (DeviceDescription) devs.getSelectedItem();
                connectDevice(dev.mac);
                sendText("AT+CJOIN=1,0,8,1\r\n");
			}
        });
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                onTimer();
            }
        }, 3000, 3000);
    }

    private void populateDeviceList() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        devs = (Spinner) findViewById(R.id.dev_list);
        List<DeviceDescription> devDescr = new ArrayList<>();
        for (BluetoothDevice d : bta.getBondedDevices()) {
            devDescr.add(new DeviceDescription(d));
        }
        ArrayAdapter<DeviceDescription> devsAda = new ArrayAdapter(
                this, android.R.layout.simple_spinner_item, devDescr);
        devsAda.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        devs.setAdapter(devsAda);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, listener);
    }

    private void connectDevice(String mac) {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        try {
            BluetoothDevice btd = bta.getRemoteDevice(mac);
            bts = btd.createInsecureRfcommSocketToServiceRecord(
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            bts.connect();
        } catch (Exception e) {
            bts = null;
            Toast.makeText(this, "Error connecting to BlueTooth: " + e, Toast.LENGTH_LONG).show();
        }
    }

    private void onTimer() {
        receiveSomething();
        sendText("AT+DTRX=1,2,2,41\r\n");
    }

    private void receiveSomething() {
        if (bts == null) {
            return;
        }
        try {
            InputStream in = bts.getInputStream();
            while (in.available() > 0) {
                char c = (char) in.read();
                if (c == '\n' || c == '\r') {
                    parseReceived();
                    received = "";
                } else {
                    received += c;
                }
                
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error reading from bluetooth: " + e, Toast.LENGTH_LONG).show();
        }
    }

    private void parseReceived() {
        if (received.matches("^\\s*$")) {
            return;
        }
        if (received.contains("rssi")) {
            String signal = received.substring(received.indexOf("rssi"));
            runOnUiThread(() -> { textSignal.setText(signal); });
        }
        rcvLines.addLast(received);
        while (rcvLines.size() > 13) {
            rcvLines.removeFirst();
        }
        runOnUiThread(() -> { textLora.setText(String.join("\n", rcvLines)); });
    }

    private void sendText(String text) {
        if (bts == null) {
            return;
        }
        try {
            bts.getOutputStream().write(text.getBytes());
        } catch (IOException e) {
            Toast.makeText(this, "Error sending to bluetooth: " + text, Toast.LENGTH_LONG).show();
            bts = null;
        }
    }
}
