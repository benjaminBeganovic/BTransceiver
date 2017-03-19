package com.example.asd.btransceiver;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigurationActivity extends AppCompatActivity {

    private long[] bands = {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};
    private long[] bandsTemp = {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};
    private static final long A_MIN = 0;
    private static final long NUMBER_OF_BANDS = 8;
    private static final long A_MAX = 100000000L;
    private static final String[] bandsNames = {"80", "40", "30", "20", "17", "15", "12", "10"};
    private int currentBand = 0;
    private static final char END_OF_MESSAGE_CHAR = (char)4;
    protected static final int MESSAGE_READ = 5;
    protected static final int MESSAGE_CONNECTED = 3;
    protected static final int MESSAGE_CONNECTION_ERROR = 0;
    private static Handler h;
    private static BTService btService = null;

    private ServiceConnection myConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            BTService.LocalBinder binder = (BTService.LocalBinder) service;
            btService = binder.getService();
            changeStatus();
        }

        public void onServiceDisconnected(ComponentName className) {
            btService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);

        Button bLeft = (Button) findViewById(R.id.button3);
        bLeft.setEnabled(false);

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {

                switch (msg.what) {
                    case MESSAGE_READ:
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);
                        dolaznaPoruka(strIncom);
                        break;
                    case MESSAGE_CONNECTED:
                        changeStatus();
                        break;
                    case MESSAGE_CONNECTION_ERROR:
                        Toast.makeText(getBaseContext(), "Connection error!", Toast.LENGTH_SHORT).show();
                        break;
                }
            }


        };

        Intent intent = new Intent(this, BTService.class);
        bindService(intent, myConnection, BIND_AUTO_CREATE);
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

    private void changeStatus()
    {
        if(btService != null)
        {
            btService.registerR(this);
            btService.setMessageHandler(h);
            btService.resumeMessageHandler();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        changeStatus();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (btService != null)
        {
            btService.unregisterR();
            btService.pauseMessageHandler();
        }

    }
    public void getBands(View view)
    {
        posalji("?" + END_OF_MESSAGE_CHAR);
    }
    public void setBand(View view)
    {
        try {
            EditText etMin = (EditText) findViewById(R.id.editTextMin);
            EditText etMax = (EditText) findViewById(R.id.editTextMax);
            String minS = etMin.getText().toString();
            String maxS = etMax.getText().toString();
            long min = Long.parseLong(minS);
            long max = Long.parseLong(maxS);

            if( (min >= max ) || (currentBand > 0 && currentBand < 7) && (min <= bands[currentBand*2-1] || max >= bands[currentBand*2+2]))
            {
                Toast.makeText(getBaseContext(), "Band isn't valid!", Toast.LENGTH_SHORT).show();
                return;
            }

            if((currentBand == 0 && (min <= A_MIN || max >= bands[2])) || ((currentBand == 7) && (max >= A_MAX || min <= bands[13])))
            {
                Toast.makeText(getBaseContext(), "Band isn't valid!", Toast.LENGTH_SHORT).show();
                return;
            }

            //upisati u temp i poslati
            bandsTemp[currentBand*2] = min;
            bandsTemp[currentBand*2+1] = max;
            String s = "S" + Integer.toString(currentBand);
            posalji(s + minS + "," + maxS + END_OF_MESSAGE_CHAR);

        }catch (Exception e){ return; }

    }

    private synchronized void dolaznaPoruka(String strIncom) {

        try {
            //uzimamo samo poruke koje su za konfiguraciju
            if (strIncom.substring(0, 1).equals("P"))
                return;

            String pom = "";
            pom = strIncom.substring(0, 1);
            if (pom.equals("O")) {
                String b = strIncom.substring(1, 2);
                int bInt = Integer.parseInt(b);
                if (!isNumber07(bInt))
                    return;

                b = bandsNames[bInt] + "-meter band has been changed.";

                if (bandsTemp[bInt * 2] > 0 && bandsTemp[bInt * 2 + 1] > 0) {
                    bands[bInt * 2] = bandsTemp[bInt * 2];
                    bands[bInt * 2 + 1] = bandsTemp[bInt * 2 + 1];
                    bandsTemp[bInt * 2] = 0;
                    bandsTemp[bInt * 2 + 1] = 0;
                    Toast.makeText(getBaseContext(), b, Toast.LENGTH_SHORT).show();
                }

            } else if (pom.equals("N")) {
                String b = strIncom.substring(1, 2);
                int bInt = Integer.parseInt(b);
                if (!isNumber07(bInt))
                    return;

                b = bandsNames[bInt] + "-meter band has not been changed. Try again!";
                Toast.makeText(getBaseContext(), b, Toast.LENGTH_SHORT).show();

            } else if (pom.equals("A")) {
                String[] parts = strIncom.split(",");

                for (int i = 1; i < 17; i++)
                    bandsTemp[i - 1] = Integer.parseInt(parts[i]);

                if (bandsTemp[0] <= A_MIN || bandsTemp[15] > A_MAX)
                    return;

                //validacija
                for (int i = 0; i < (NUMBER_OF_BANDS * 2 - 1); i++)
                    if (bandsTemp[i] >= bandsTemp[i + 1])
                        return;

                for (int i = 0; i < NUMBER_OF_BANDS * 2; i++) {
                    bands[i] = bandsTemp[i];
                    bandsTemp[i] = 0;
                }

                printCurrentBand();
            }

        } catch (Exception e) { return; }
    }
    public void leftButton(View view)
    {
        EditText etName = (EditText) findViewById(R.id.editText);

        if (currentBand == 1) {
            Button b = (Button) findViewById(R.id.button3);
            b.setEnabled(false);
        }
        if (currentBand == 7) {
            Button b = (Button) findViewById(R.id.button2);
            b.setEnabled(true);
        }

        if(currentBand == 0)
            currentBand = 7;
        else
            currentBand--;

        etName.setText(bandsNames[currentBand] + "-meter band");
        printCurrentBand();
    }
    public void rightButton(View view)
    {
        if (currentBand == 0) {
            Button b = (Button) findViewById(R.id.button3);
            b.setEnabled(true);
        }
        if (currentBand == 6) {
            Button b = (Button) findViewById(R.id.button2);
            b.setEnabled(false);
        }
        if(currentBand == 7)
            currentBand = 0;
        else
            currentBand++;

        EditText etName = (EditText) findViewById(R.id.editText);
        etName.setText(bandsNames[currentBand] + "-meter band");
        printCurrentBand();
    }
    void printCurrentBand()
    {
        //poor check if bands loaded
        for(int i = 0; i < NUMBER_OF_BANDS*2; i++)
            if(bands[i] == 0)
                return;
        EditText etMin = (EditText) findViewById(R.id.editTextMin);
        EditText etMax = (EditText) findViewById(R.id.editTextMax);
        TextView tvMin = (TextView) findViewById(R.id.textViewMn);
        TextView tvMax = (TextView) findViewById(R.id.textViewMx);

        etMin.setText(Long.toString(bands[currentBand * 2]));
        etMax.setText(Long.toString(bands[currentBand * 2 + 1]));

        if (currentBand == 0)
            tvMin.setText(addDots(Long.toString(A_MIN)));
        else
            tvMin.setText(addDots(Long.toString(bands[currentBand * 2 - 1])));

        if (currentBand == 7)
            tvMax.setText(addDots(Long.toString(A_MAX)));
        else
            tvMax.setText(addDots(Long.toString(bands[currentBand * 2 + 2])));
    }

    public void posalji(String s)
    {
        if(btService != null)
        {
            if(btService.isLaunched())
            {
                btService.sendMessage(s);

            }else{
                Toast.makeText(getBaseContext(), "No connection!", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(getBaseContext(), "No connection!", Toast.LENGTH_SHORT).show();
        }
    }
    boolean isNumber07(int broj)
    {
        if(broj >= 0 && broj < 8)
            return true;

        return false;
    }
    private String addDots(String s)
    {
        String t = "";
        int br = 0;
        for(int i = s.length()-1; i>0; i--)
        {
            t += s.charAt(i);
            br++;
            if(br == 3)
            {
                br = 0;
                t += ".";
            }
        }
        t += s.charAt(0);
        return new StringBuilder(t).reverse().toString();
    }

}
