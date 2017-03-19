package com.example.asd.btransceiver;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /** Called when the user clicks the VFO button */
    public void vfoF(View view) {

        Intent i = new Intent(this, FrequencyActivity.class);
        startActivity(i);
    }
    /** Called when the user clicks the BANDS button */
    public void bandsF(View view) {

        Intent i = new Intent(this, ConfigurationActivity.class);
        startActivity(i);
    }
    /** Called when the user clicks the configuration button */
    public void configurationF(View view) {

        Intent intent = new Intent(this, MorseSpeedActivity.class);
        startActivity(intent);
    }
    /** Called when the user clicks the messages button */
    public void messageF(View view) {

        Intent intent = new Intent(this, MessageActivity.class);
        startActivity(intent);
    }
    /** Called when the user clicks the connect button */
    public void connectF(View view) {

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter != null) {

            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }else{

                Intent openActivityIntent = new Intent(this, ConnectActivity.class);
                startActivity(openActivityIntent);

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //ako se upali bt onda mozemo raditi dalje...
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {

                Intent openActivityIntent = new Intent(this, ConnectActivity.class);
                startActivity(openActivityIntent);

            }
        }
    }//onActivityResult
}
