package org.rassel.deploy.blueproxyyves;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity implements LocationListener {
    private static final int UDP_PORT = 61283;
    private TextView dataTextView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private LocationManager locationManager;


    private Location location = null;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        context = getApplicationContext();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        this.location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        setContentView(R.layout.activity_main);

        dataTextView = findViewById(R.id.dataTextView);

        // Starten Sie den UDP-Listener in einem separaten Thread
        new Thread(new UdpListenerThread()).start();

    }


    @Override
    public void onLocationChanged(@NonNull Location location) {
        this.location = location;
    }

    private class UdpListenerThread implements Runnable {
        @Override
        public void run() {
            try {
                DatagramSocket socket = new DatagramSocket(UDP_PORT);

                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);


                dataTextView.setText("Warte auf Nachricht");

                while (true) {
                    socket.receive(packet);
                    final String receivedMessage = new String(buffer, 0, packet.getLength());

                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            dataTextView.setText("Nachricht empfangen.");
                            // Zeigen Sie die empfangene Nachricht in der TextView an
                            dataTextView.append("\n" + receivedMessage);
                            if (location != null) {
                                dataTextView.append("\n" + location.getLatitude() + "\n" + location.getLongitude());
                                writeToFile(System.currentTimeMillis() + "\t" + receivedMessage.substring(7) + "\t" + location.getLatitude() + "\t" + location.getLongitude() + "\n");
                            } else {
                                dataTextView.append("\nNo location.");
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();

                dataTextView.setText("Error: " + e.getMessage());
            }
        }

        private void writeToFile(String data) {
            try {
                File p = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                dataTextView.append("\nTry writing file");
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "loraLog.csv"), true));
                outputStreamWriter.write(data);
                outputStreamWriter.close();
                dataTextView.append("\nFile written.");
            } catch (IOException e) {
                e.printStackTrace();

                dataTextView.append("\nError writing file: " + e.getMessage());
            }
        }
    }


}
