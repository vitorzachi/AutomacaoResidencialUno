package br.com.camtwo.automacaoresidencial;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    TextView myLabel;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    Button openButton;
    private Button closeButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openButton = (Button) findViewById(R.id.open);
        closeButton = (Button) findViewById(R.id.close);
        myLabel = (TextView) findViewById(R.id.label);

        desabilitaBotoes();
        //Open Button
        openButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected void onPreExecute() {
                        MainActivity.this.preexecute();
                        super.onPreExecute();
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        MainActivity.this.postexecute();
                        super.onPostExecute(aVoid);
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        Looper.prepare();
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.connect();
                            }
                        });

                        return null;
                    }
                }.execute();
            }
        });


        //Close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    closeBT();
                } catch (IOException ex) {
                }
            }
        });
    }

    void connect() {
        try {
            findBT();
            openBT();
        } catch (Exception ex) {
            int i = 0;
            Log.e("BLUE", "erro", ex);
            myLabel.setText(ex.getMessage());
        }
    }

    void preexecute() {
        openButton.setEnabled(false);
        (findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
    }

    void postexecute() {

        (findViewById(R.id.progressBar)).setVisibility(View.GONE);

        habilitaBotoes();
    }

    void findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            myLabel.setText("Bluetooth não está disponível");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);

        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("CRIUS_BT")) {
                    mmDevice = device;
                    myLabel.setText("Dispositivo encontrado. Ainda não conectado");
                    break;
                }
            }
        }
    }

    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID

        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();

            beginListenForData();
            sendData("z"); // comando enviado para o arduino para saber de todos os status


        } catch (NullPointerException e) {
            Log.e("BLUE", "ERRO", e);
        }catch (IOException e) {
            myLabel.setText(e.getMessage());
        }
    }

    void beginListenForData() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            trataRetorno(data);
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void trataRetorno(String s) {
        s = s.replaceAll("[\n\r]", "");
        if (s.length() > 2) {
            String[] status = s.split(",");

            for (String statusIndividual : status) {
                trataRetorno(statusIndividual);
            }
        } else {

            Button b = null;
            char id = s.charAt(0);
            char c = s.charAt(1);
            switch (id) {
                case 'a':
                    b = (Button) findViewById(R.id.solteiro);
                    break;
                case 'b':
                    b = (Button) findViewById(R.id.casal);
                    break;
                case 'c':
                    b = (Button) findViewById(R.id.sala);
                    break;
                case 'd':
                    b = (Button) findViewById(R.id.cozinha);
                    break;
                case 'e':
                    b = (Button) findViewById(R.id.garagem);
                    break;
                case 'f':
                    b = (Button) findViewById(R.id.banheiro);
                    break;
                default:
                    break;
            }

            if (b != null) {
                if (c == '1') {
                    b.setBackgroundColor(Color.parseColor("#74e174"));
                    Toast.makeText(this, b.getText() + " ligado", Toast.LENGTH_SHORT).show();
                } else {
                    b.setBackgroundColor(Color.GRAY);
                    Toast.makeText(this, b.getText() + " desligado", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    void desabilitaBotoes() {

        openButton.setText("Conectar");
        openButton.setEnabled(true);
        closeButton.setText("Desconectado");
        (findViewById(R.id.solteiro)).setEnabled(false);
        findViewById(R.id.casal).setEnabled(false);
        findViewById(R.id.sala).setEnabled(false);
        findViewById(R.id.cozinha).setEnabled(false);
        findViewById(R.id.garagem).setEnabled(false);
        findViewById(R.id.banheiro).setEnabled(false);
    }

    void habilitaBotoes() {
        myLabel.setText("Conexão estabelecida");
        openButton.setText("Conectado");
        openButton.setEnabled(false);
        closeButton.setText("Desconectar");
        (findViewById(R.id.solteiro)).setEnabled(true);
        findViewById(R.id.casal).setEnabled(true);
        findViewById(R.id.sala).setEnabled(true);
        findViewById(R.id.cozinha).setEnabled(true);
        findViewById(R.id.garagem).setEnabled(true);
        findViewById(R.id.banheiro).setEnabled(true);
    }

    void sendData(String msg) throws IOException {
        //msg += "\n";
        try{
            mmOutputStream.write(msg.getBytes());
            myLabel.setText("Comando enviado");
        }catch (NullPointerException e){
            myLabel.setText("Ops, conecte novamente");
            desabilitaBotoes();
        }
    }

    void closeBT() throws IOException {
        try {
            stopWorker = true;
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
            myLabel.setText("Bluetooth fechado");
            openButton.setText("Conectar");
            openButton.setEnabled(true);
            closeButton.setText("Desconectado");
            desabilitaBotoes();
        }catch (NullPointerException e){
            myLabel.setText("Bluetooth fechado");
            openButton.setText("Conectar");
            openButton.setEnabled(true);
            closeButton.setText("Desconectado");
        }
    }

    public void solteiro(View view) {
        try {
            sendData("a");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void casal(View view) {
        try {
            sendData("b");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sala(View view) {
        try {
            sendData("c");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cozinha(View view) {
        try {
            sendData("d");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void garagem(View view) {
        try {
            sendData("e");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void banheiro(View view) {
        try {
            sendData("f");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}