package es.ua.eps.chitchat;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Server extends AppCompatActivity {

    private ServerSocket serverSocket;
    public static final int SERVER_PORT = 1331;
    private Thread serverThread;
    private TextView textMessages;
    private TextView textInfoServer;
    private HashMap<String, Socket> clientes;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textMessages = findViewById(R.id.textMessages);
        textInfoServer = findViewById(R.id.textInfoServer);
        clientes = new HashMap<>();

        textInfoServer.setText("Server with IP: " + ipserver() + ", working in port: " + SERVER_PORT);

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

    private String ipserver() {
        WifiManager wifiManager;
        String ip;

        wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ip = getIpFormat(wifiManager.getConnectionInfo().getIpAddress());

        Log.d("INFORMATION", "IP Server: " + ip);

        return ip;
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

                    String id_client = socket.getInetAddress().toString();

                    clientes.put(id_client, socket);

                    ReadThread readThread = new ReadThread(id_client, socket);
                    readThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    class ReadThread extends AsyncTask {
        private Socket clientSocket;
        private String id_writer;
        private DataInputStream input;
        private String read;
        String line;

        public ReadThread(String id, Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.id_writer = id;

            try {
                this.input = new DataInputStream(this.clientSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            while (!this.clientSocket.isClosed()) {
                try {
                    read = input.readUTF();
                    line = id_writer + "$" + read;

                    this.publishProgress();

                    if(read != null && !read.equals("")) {
                        WriteThread writeThread = new WriteThread(id_writer, line);
                        writeThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            textMessages.append(line + "\n");
        }
    }

    class WriteThread extends AsyncTask {
        private ArrayList<DataOutputStream> outputs;
        String messageToSend;

        public WriteThread(String id_writer, String messageToSend) {
            this.messageToSend = messageToSend;
            outputs = new ArrayList<>();

            for (Map.Entry<String, Socket> client : clientes.entrySet()) {
                if(!client.getKey().equals(id_writer)) {
                    try {
                        this.outputs.add(new DataOutputStream(client.getValue().getOutputStream()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            for (DataOutputStream output : outputs) {
                try {
                    output.writeUTF(messageToSend);
                    output.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }

}
