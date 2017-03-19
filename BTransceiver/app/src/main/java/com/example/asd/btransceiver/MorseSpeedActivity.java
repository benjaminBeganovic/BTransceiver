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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MorseSpeedActivity extends AppCompatActivity {

    private static final char END_OF_MESSAGE_CHAR = (char)4;
    protected static final int MESSAGE_READ = 5;
    protected static final int MESSAGE_CONNECTED = 3;
    protected static final int MESSAGE_CONNECTION_ERROR = 0;
    private static Handler h;
    private static BTService btService = null;
    private boolean dobioCSpeed = false;
    private boolean onRes = true;

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
        setContentView(R.layout.activity_morseconfig);

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

        final TextView t = (TextView)findViewById(R.id.textView6);
        final SeekBar sk=(SeekBar) findViewById(R.id.seekBar);
        sk.setProgress(19);
        sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                t.setText("Set new speed: " + Integer.toString(progress + 1) + " [WPM]");
            }
        });

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
            if(btService.isLaunched() && !dobioCSpeed)
            {
                dobioCSpeed = true;
                getCurrentSpeed();
            }
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
    public void getCurrentSpeed()
    {
        posalji("W" + END_OF_MESSAGE_CHAR);
    }
    private synchronized void dolaznaPoruka(String strIncom) {

        try {
            //uzimamo samo poruku za brzinu
            if (strIncom.substring(0, 1).equals("W"))
            {
                if(!onRes)
                    Toast.makeText(getBaseContext(), "Speed has been changed!", Toast.LENGTH_SHORT).show();

                onRes = false;
                String speed = strIncom.substring(1,strIncom.length());
                int pom = Integer.parseInt(speed);
                final SeekBar sk=(SeekBar) findViewById(R.id.seekBar);
                sk.setProgress(pom-1);
                TextView t = (TextView) findViewById(R.id.textView4);
                t.setText("Current speed: "+speed+" [WPM]");
            }
        } catch (Exception e) { return; }
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
    public void setNewSpeedF(View view)
    {
        TextView ctw = (TextView)findViewById(R.id.textView4);
        TextView ntw = (TextView)findViewById(R.id.textView6);
        String ctwS = ctw.getText().toString();
        String ntwS = ntw.getText().toString();
        String ctwSS = getSpeedFromStr(ctwS);
        String ntwSS = getSpeedFromStr(ntwS);
        if(ctwSS.equals(ntwSS))
            Toast.makeText(getBaseContext(), "It's current!", Toast.LENGTH_SHORT).show();
        else
            posalji("W" + ntwSS + END_OF_MESSAGE_CHAR);
    }
    private String getSpeedFromStr(String s)
    {
        String pom ="";
        int i;
        for(i = 0; i < s.length(); i++)
        {
            if(s.charAt(i)>='1' && s.charAt(i)<='9')
            {
                pom+=s.substring(i,i+1);
                if(s.charAt(i+1)>='0' && s.charAt(i+1)<='9')
                    pom+=s.substring(i+1,i+2);

                return pom;
            }
        }
        return pom;
    }

}
