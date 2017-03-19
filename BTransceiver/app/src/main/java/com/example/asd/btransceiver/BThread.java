package com.example.asd.btransceiver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;

public class BThread extends Thread {

    private BluetoothSocket btSocket = null;
    private BluetoothDevice btDevice = null;
    private BluetoothAdapter btAdapter = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;
    private static String macAddress = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int MESSAGE_READ = 5;
    private static final int MESSAGE_CONNECTED = 3;
    private static final int MESSAGE_CONNECTION_ERROR = 0;
    private static final int BUFFER_SIZE = 1024;
    private static final byte END_OF_MESSAGE_CHAR = 4;
    private String error = "null";
    private String status = "null";
    private Handler handlerMessage = null;
    private Handler handlerConnection = null;
    private static int stateOfMessageHandler = 0; //1 upaljen, 0 ugasen
    private static String[] unreadMessages = new String[100];
    private static int numberOfunreadMessages = 0;
    private static int stateOfMessageHandler2 = 0;

    public BThread(Handler h, String adresa){

        macAddress = adresa;
        handlerConnection = h;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setMessageHandler(Handler h){
        handlerMessage = h;
    }
    public void pauseMessageHandler(){
        stateOfMessageHandler = 0;
    }
    public void resumeMessageHandler(){
        stateOfMessageHandler = 1;
    }

    public void pauseMessageHandler2(){
        stateOfMessageHandler2 = 0;
    }
    public void resumeMessageHandler2(){
        stateOfMessageHandler2 = 1;
    }

    public void run() {

        try {

            btDevice = btAdapter.getRemoteDevice(macAddress);

            if(Build.VERSION.SDK_INT >= 10)
            {
                final Method  m = btDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                btSocket = (BluetoothSocket) m.invoke(btDevice, MY_UUID);
            }
            else
                btSocket =  btDevice.createRfcommSocketToServiceRecord(MY_UUID);

            if(btAdapter.isDiscovering())
                btAdapter.cancelDiscovery();

            btSocket.connect();

            inStream = btSocket.getInputStream();
            outStream = btSocket.getOutputStream();

            if(handlerConnection != null)
                handlerConnection.obtainMessage(MESSAGE_CONNECTED).sendToTarget();

            if(handlerMessage != null)
                if(stateOfMessageHandler == 1)
                    handlerMessage.obtainMessage(MESSAGE_CONNECTED).sendToTarget();

            status = "launched";

            byte[] buffer = new byte[BUFFER_SIZE];
            byte znak;
            int brojac = 0, pom;

            while (true) {
                try {
                    pom = inStream.read();

                    if(pom != -1){

                        znak = (byte) pom;

                        if(znak != END_OF_MESSAGE_CHAR && brojac < BUFFER_SIZE - 10){

                            buffer[brojac] = znak;
                            brojac++;
                        }else if (znak != END_OF_MESSAGE_CHAR && brojac >= BUFFER_SIZE - 10){

                            buffer[brojac++] = (byte)' ';
                            buffer[brojac++] = (byte)'.';
                            buffer[brojac++] = (byte)'.';
                            buffer[brojac++] = (byte)'.';
                            if(handlerMessage != null)
                                if(stateOfMessageHandler == 1)
                                    handlerMessage.obtainMessage(MESSAGE_READ, brojac, -1, buffer).sendToTarget();
                            brojac = 0;


                        }else{

                            if(handlerMessage != null && stateOfMessageHandler == 1)
                                handlerMessage.obtainMessage(MESSAGE_READ, brojac, -1, buffer).sendToTarget();

                            if(stateOfMessageHandler2 == 0 && (buffer[0] == 'T' || buffer[0] == 'P'))
                            {
                                String temp = "";
                                for(int i = 0; i < brojac; i++)
                                    temp += (char)buffer[i];
                                unreadMessages[numberOfunreadMessages++] = temp;
                            }

                            brojac=0;

                        }
                    }

                } catch (Exception e) {
                    error = "inputStreamError";
                    break;
                }
            }

        } catch (Exception e) {
            try {
                btSocket.close();
            } catch (Exception ee) { error = "connectionError"; }

            error = "connectionError";
            status = "null";
            if(handlerConnection != null)
                handlerConnection.obtainMessage(MESSAGE_CONNECTION_ERROR).sendToTarget();

            if(handlerMessage != null)
                if(stateOfMessageHandler == 1)
                    handlerMessage.obtainMessage(MESSAGE_CONNECTION_ERROR).sendToTarget();
        }
    }

    public void getUnreadMessages()
    {
        if(numberOfunreadMessages > 0)
        {
            for(int i = 0; i < numberOfunreadMessages; i++)
            {
                byte[] temp = unreadMessages[i].getBytes();
                handlerMessage.obtainMessage(MESSAGE_READ, unreadMessages[i].length(), -1, temp).sendToTarget();
            }

            numberOfunreadMessages = 0;
        }
    }

    public void send(String poruka) {
        try {

            byte[] msgBuffer = poruka.getBytes();
            outStream.write(msgBuffer);
            outStream.flush();

        } catch (IOException e) { error = "outStreamError"; }
    }
    public String getError()
    {
        return error;
    }
    public String getStatus()
    {
        return status;
    }

    public void cancel() {

        status = "terminated";
        try {

            btSocket.close();

        } catch (Exception e) { error = "outStreamError"; }

        Thread.currentThread().interrupt();
        // kod za destroy threada !!!!!!!!!!!!!!!!
    }
}