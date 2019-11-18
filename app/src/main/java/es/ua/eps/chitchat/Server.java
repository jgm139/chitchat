package es.ua.eps.chitchat;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends AppCompatActivity {

    private ServerSocket serverSocket;
    public static final int SERVER_PORT = 1331;
    Thread serverThread;
    private TextView textMessages;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textMessages = findViewById(R.id.textMessages);
        ipserver();

        serverThread = new Thread(new ServerThread());

        serverThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ipserver() {
        WifiManager wifiManager;
        String ip;

        wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ip = getIpFormat(wifiManager.getConnectionInfo().getIpAddress());

        Log.d("INFORMATION", "IP Server: " + ip);
    }

    private static String getIpFormat(int code) {
        String result;

        result = String.format("%d.%d.%d.%d", (code & 0xff), (code >> 8 & 0xff), (code >> 16 & 0xff), (code >> 24 & 0xff));

        return result;
    }


    class ServerThread implements Runnable {

        @Override
        public void run() {
            Socket socket = null;

            try {
                Log.d("INFORMATION", "Abriendo ServerSocket en el puerto " + SERVER_PORT);
                serverSocket = new ServerSocket(SERVER_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!Thread.currentThread().isInterrupted()) {

                try {
                    socket = serverSocket.accept();

                    CommunicationThread communicationThread = new CommunicationThread(socket);
                    communicationThread.execute();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    class CommunicationThread extends AsyncTask {
        private Socket clientSocket;
        private DataInputStream input;
        private String read;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;

            try {
                this.input = new DataInputStream(this.clientSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            try {
                read = input.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            textMessages.setText("Client Says: " + read);
        }
    }

}
