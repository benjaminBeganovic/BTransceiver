package com.example.asd.btransceiver;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Set;
import com.example.asd.btransceiver.BTService.LocalBinder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class ConnectActivity extends AppCompatActivity {

    private BTService btService;
    private boolean sBound = false;
    private Context contextForS = this;

    private ServiceConnection myConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            btService = binder.getService();
            btService.registerR(contextForS);
            sBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            btService = null;
            sBound = false;
        }
    };

    public void serviceF(View view) {

        if(sBound){

            Spinner spiner = (Spinner) findViewById(R.id.spinnerDevices);
            String adresa = spiner.getSelectedItem().toString();

            if(!adresa.equals(""))
            {
                String macAddress = adresa.substring(adresa.length()-17,adresa.length());
                btService.startBT(macAddress);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void associateF(View view) {

        //otvaranje bluetooth settings-a
        Intent intentOpenBluetoothSettings = new Intent();
        intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivity(intentOpenBluetoothSettings);

    }

    public void refreshPairedDevicesF() {

        Spinner spiner = (Spinner) findViewById(R.id.spinnerDevices);
        ArrayList<String> bluetoothPairedDevices = new ArrayList<String>();

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, bluetoothPairedDevices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //da li ima bluetooth device?...
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices

        if(pairedDevices.size() == 0)
            adapter.add("");
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                adapter.add(device.getName() + "\n" + device.getAddress());
                //ovdje bi se mogla prikazati samo imena
                //a mac adrese da se cuvaju u nekom nizu
                //adapter.add(device.getName());
            }
        }

        spiner.setAdapter(adapter);

    }
    @Override
    public void onPause()
    {
        super.onPause();
        if(btService != null)
            btService.unregisterR();
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshPairedDevicesF();

        if(btService != null)
            btService.registerR(contextForS);

        if (!sBound) {

            Intent intent = new Intent(this, BTService.class);
            bindService(intent, myConnection, BIND_AUTO_CREATE);
        }
    }

    public void disconnectF(View view)
    {
        if(btService != null)
            btService.disconnect();
    }
}



