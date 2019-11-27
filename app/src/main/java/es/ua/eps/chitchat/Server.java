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

    //Variables globales y atributos de la clase
    private ServerSocket serverSocket;// socket del servidor
    public static final int SERVER_PORT = 1331; //número de puerto del socket
    private Thread serverThread; //hilo de la ejecución del servidor
    private TextView textMessages; //vista donde se muestran los mensajes llegados al servidor
    private TextView textInfoServer;
    private HashMap<String, Socket> clientes; // mapeo de los diferentes clientes conectados al servidor


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //selección de la vista para el activity

        textMessages = findViewById(R.id.textMessages); // asignación del textview a su variable
        textInfoServer = findViewById(R.id.textInfoServer);
        clientes = new HashMap<>(); //inicializa la variable de mapeo

        //se utiliza el método para obtener la ip del dispositivo
        textInfoServer.setText("Server with IP: " + ipserver() + ", working in port: " + SERVER_PORT);

        serverThread = new Thread(new ServerThread()); //se inicializa el hilo del servidor
        serverThread.start(); //se ejecuta el hilo del servidor
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            serverSocket.close(); //se cierra el socket al parar la aplicación
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String ipserver() {
        WifiManager wifiManager;
        String ip;

        wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE); //se obtiene el manejador del parámetros del wifi
        ip = getIpFormat(wifiManager.getConnectionInfo().getIpAddress()); //se obtiene la ip formateada

        Log.d("INFORMATION", "IP Server: " + ip);

        return ip;
    }

    private static String getIpFormat(int code) {
        String result;

        //se utilizan desplazamientos de bits para formatear la ip
        result = String.format("%d.%d.%d.%d", (code & 0xff), (code >> 8 & 0xff), (code >> 16 & 0xff), (code >> 24 & 0xff));

        return result;
    }


    class ServerThread implements Runnable {

        @Override
        public void run() {
            Socket socket = null;

            try {
                Log.d("INFORMATION", "Abriendo ServerSocket en el puerto " + SERVER_PORT);
                serverSocket = new ServerSocket(SERVER_PORT); //se instancia el socket del servidor indicando el puerto que va a utilizar
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!Thread.currentThread().isInterrupted()) {

                try {
                    socket = serverSocket.accept(); //acepta conexiones de sockets de clientes

                    String id_client = socket.getInetAddress().toString(); //guardamos la ip del cliente

                    clientes.put(id_client, socket); //añadimos al mapeo de clientes el socket y la ip que identifica al cliente

                    ReadThread readThread = new ReadThread(id_client, socket); //instanciamos un hilo asíncrono para leer mensajes de clientes
                    readThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); //ejecutamos el hilo asíncrono

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    class ReadThread extends AsyncTask {
        private Socket clientSocket; //socket del cliente
        private String id_writer; //ip cliente
        private DataInputStream input; //flujo de datos de entrada
        private String read; //mensaje recibido
        String line; // mensaje que muestra el servidor indicando qué mensaje y de quién ha llegado

        //constructor para inicializar atributos de la clase
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
            while (!this.clientSocket.isClosed()) { //mientras la conexión el cliente esté abierta
                try {
                    read = input.readUTF(); //lee los mensajes recibidos
                    line = id_writer + "$" + read; //construye el mensaje que muestra el servidor

                    this.publishProgress(); //actualiza la vista de texto del servidor con el nuevo mensaje llegado

                    if(read != null && !read.equals("")) { //cuando ha llegado un mensaje inicia el hilo de respuesta
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
        private ArrayList<DataOutputStream> outputs; //flujos de mensajes de salida para distintos clientes
        String messageToSend; //mensaje a enviar

        //constructor para inicializar atributos de la clase
        public WriteThread(String id_writer, String messageToSend) {
            this.messageToSend = messageToSend;
            outputs = new ArrayList<>();

            //creamos un flujo de datos de salida por cada cliente que hay conectado
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
            for (DataOutputStream output : outputs) {//recorremos todos los flujos de datos creados para cada cliente
                try {
                    output.writeUTF(messageToSend); //escribimos el mensaje que vamos a enviar en el flujo de datos de salida
                    output.flush(); //enviamos el flujo de datos con el nuevo mensaje
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }

}
