package com.example.asd.btransceiver;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

public class BTService extends Service{

    private static final int MESSAGE_CONNECTION_ERROR = 0;
    private static final int MESSAGE_CONNECTED = 3;
    public static BThread bThread = null;
    public static String staraMACAdresa = "";// sada ne moze null
    public static String pomocnaMACAdresa = "";// sada ne moze null
    public static Handler messageHandler = null;
    public static Handler h = null;
    public static boolean messageHandlerPaused = true;
    public Context contextForReceiver = null;

    //atributi za servis
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        BTService getService() {
            return BTService.this;
        }
    }

    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                try {
                    disconnect();
                    if(messageHandler != null && !messageHandlerPaused)
                        messageHandler.obtainMessage(MESSAGE_CONNECTION_ERROR).sendToTarget();

                }catch (Exception e){}

            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startBT(String macAddress){

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {

                switch (msg.what) {
                    case MESSAGE_CONNECTED:
                        Toast.makeText(getBaseContext(), "Connected!", Toast.LENGTH_SHORT).show();
                        break;
                    case MESSAGE_CONNECTION_ERROR:
                        Toast.makeText(getBaseContext(), "Device is unreachable!", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        if(!staraMACAdresa.equals(macAddress)){
            Toast.makeText(getBaseContext(), "Connecting...", Toast.LENGTH_LONG).show();
            pomocnaMACAdresa = macAddress;
            if(bThread != null)
                bThread.cancel();

            bThread = new BThread(h, macAddress);
            bThread.start();
        }else{
            Toast.makeText(getBaseContext(), "Connected!", Toast.LENGTH_SHORT).show();
        }

    }
    public void setMessageHandler(Handler h){
        messageHandler = h;
        if(bThread != null){
            bThread.setMessageHandler(h);
        }
    }
    public void sendMessage(String message){
        if(bThread != null){
            bThread.send(message);
        }
    }
    public boolean isLaunched(){
        if(bThread != null)
            if(bThread.getStatus().equals("launched"))
                return true;

        return false;
    }
    public void pauseMessageHandler(){
        messageHandlerPaused = true;
        if(bThread != null){
            bThread.pauseMessageHandler();
        }
    }
    public void resumeMessageHandler(){
        messageHandlerPaused = false;
        if(bThread != null){
            bThread.resumeMessageHandler();
        }
    }
    public void pauseMessageHandler2(){
        messageHandlerPaused = true;
        if(bThread != null){
            bThread.pauseMessageHandler2();
        }
    }
    public void resumeMessageHandler2(){
        messageHandlerPaused = false;
        if(bThread != null){
            bThread.resumeMessageHandler2();
        }
    }
    public void disconnect(){
        if(bThread != null){
            bThread.cancel();
            staraMACAdresa = "";
            pomocnaMACAdresa = "";
            Toast.makeText(getBaseContext(), "Disconnected!", Toast.LENGTH_SHORT).show();

        }
    }
    public void registerR(Context c)
    {
        if(c.equals(contextForReceiver))
            return;

        contextForReceiver = c;
        try {

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            contextForReceiver.registerReceiver(mReceiver, filter);

        }catch (Exception e){return;}
    }
    public void unregisterR()
    {
        try {

            contextForReceiver.unregisterReceiver(mReceiver);
            contextForReceiver = null;

        }catch (Exception e){return;}
    }
}
