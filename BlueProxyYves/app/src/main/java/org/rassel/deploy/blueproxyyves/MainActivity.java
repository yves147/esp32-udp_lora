package org.rassel.deploy.blueproxyyves;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class MainActivity extends Activity {
    private static final int UDP_PORT = 61283;
    private TextView dataTextView;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataTextView = findViewById(R.id.dataTextView);

        // Starten Sie den UDP-Listener in einem separaten Thread
        new Thread(new UdpListenerThread()).start();
    }

    private class UdpListenerThread implements Runnable {
        @Override
        public void run() {
            try {
                DatagramSocket socket = new DatagramSocket(UDP_PORT);

                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while (true) {
                    socket.receive(packet);
                    final String receivedMessage = new String(buffer, 0, packet.getLength());

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Zeigen Sie die empfangene Nachricht in der TextView an
                            Log.d("UDP_DEBUG", "ja");
                            dataTextView.setText("Empfangene Nachricht" + receivedMessage);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
