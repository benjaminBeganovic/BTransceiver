package com.example.asd.btransceiver;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class FrequencyActivity extends AppCompatActivity {

    private long[] bands = {0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L};
    private static final int NUMBER_OF_BANDS = 8;
    private static final long A_MIN = 0;
    private static final long A_MAX = 100000000L;
    private static final String[] memNames = {"M0", "M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9"};
    private static final String[] bandsNames = {"80", "40", "30", "20", "17", "15", "12", "10"};
    private static final char END_OF_MESSAGE_CHAR = (char)4;
    protected static final int MESSAGE_READ = 5;
    protected static final int MESSAGE_CONNECTED = 3;
    protected static final int MESSAGE_CONNECTION_ERROR = 0;
    private static Handler h;
    private static BTService btService = null;
    private boolean dobioCF = false;

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
        setContentView(R.layout.activity_frequency);

        Button b = (Button) findViewById(R.id.buttonMleft);
        b.setEnabled(false);

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


        final EditText et = (EditText) findViewById(R.id.editText3);
        final EditText et5 = (EditText) findViewById(R.id.editText5);
        et.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {

                getBands(null);
                String pom = getBandForFreq(et.getText().toString());
                if(!pom.equals("Hz xx-m"))
                    et5.setText(pom);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        final EditText et2 = (EditText) findViewById(R.id.newFrequency);
        final EditText et22 = (EditText) findViewById(R.id.editText2);
        et2.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {

                getBands(null);
                String pom = getBandForFreq(et2.getText().toString());
                if (!pom.equals("Hz xx-m"))
                    et22.setText(pom);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
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
            if(btService.isLaunched() && !dobioCF)
            {
                dobioCF = true;
                getCurrentF();
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

    public void getBands(View view)
    {
        if(bands[5] == 0L)//moze bilo koji ne mora 5 samo da se vidi da li je postavljeno
            posalji("?" + END_OF_MESSAGE_CHAR);
    }
    public void getCurrentF()
    {
        posalji("C" + END_OF_MESSAGE_CHAR);
    }
    public void setMemAsCurrentF(View view)
    {
        EditText e = (EditText) findViewById(R.id.editTextMemory);
        String s = e.getText().toString().toUpperCase();
        posalji(s + END_OF_MESSAGE_CHAR);
    }
    public void setVFOABF(View view)
    {
        Button b = (Button) findViewById(R.id.changeVFO);
        String s = b.getText().toString().toUpperCase();
        if(s.equals("SET VFOA"))
            posalji("VFOA" + END_OF_MESSAGE_CHAR);
        else
            posalji("VFOB" + END_OF_MESSAGE_CHAR);
    }

    private synchronized void dolaznaPoruka(String strIncom) {

        try {
            //uzimamo samo poruke koje su za konfiguraciju
            if (strIncom.substring(0, 1).equals("P"))
                return;

            String pom, pom2;
            pom = strIncom.substring(0, 1);
            pom2 = strIncom.substring(0, 3);



            if (pom2.equals("VFO")) {
                String vfo = strIncom.substring(3, 4);
                String band = strIncom.substring(strIncom.length()-1, strIncom.length());
                String freq = strIncom.substring(5, strIncom.length()-2);

                if(!vfo.equals("A") && !vfo.equals("B"))
                    return;

                int bInt = Integer.parseInt(band);
                if (!isNumber07(bInt))
                    return;

                int freqNum = Integer.parseInt(freq);

                Button b = (Button) findViewById(R.id.changeVFO);

                if(vfo.equals("A"))
                    b.setText("SET VFOB");
                else
                    b.setText("SET VFOA");

                //azurirati editText
                EditText e = (EditText) findViewById(R.id.editText);
                String val = bandsNames[bInt] + "m " + addDots(freq) + "Hz " + vfo;
                e.setText(val);
                dobioCF = true;

            } else if (pom.equals("A")) {
                String[] parts = strIncom.split(",");

                for (int i = 1; i < 17; i++)
                    bands[i - 1] = Integer.parseInt(parts[i]);

                //validacija
                if (bands[0] <= A_MIN || bands[15] > A_MAX)
                {
                    for (int i = 0; i < NUMBER_OF_BANDS * 2; i++)
                        bands[i] = 0;
                    return;
                }

                for (int i = 0; i < (NUMBER_OF_BANDS * 2 - 1); i++)
                    if (bands[i] >= bands[i + 1])
                    {
                        for (int k = 0; k < NUMBER_OF_BANDS * 2; k++)
                            bands[k] = 0;
                        return;
                    }
            }
            else if(pom.equals("M")) {
                Toast.makeText(getBaseContext(), pom + strIncom.substring(2, 3) + " saved!", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) { return; }
    }
    public void loadConfigF(View view)
    {
        getCurrentF();
    }
    public void leftButton(View view)
    {
        try {
            EditText e = (EditText) findViewById(R.id.editTextMemory);
            int mem = Integer.parseInt(e.getText().toString().substring(1, 2));
            String newMem = "M" + Integer.toString(mem - 1);
            e.setText(newMem);

            if (mem == 1) {
                Button b = (Button) findViewById(R.id.buttonMleft);
                b.setEnabled(false);
            }
            if (mem == 9) {
                Button b = (Button) findViewById(R.id.buttonMright);
                b.setEnabled(true);
            }
        }catch (Exception e) {return;}
    }
    public void rightButton(View view)
    {
        try {
            EditText e = (EditText) findViewById(R.id.editTextMemory);
            int mem = Integer.parseInt(e.getText().toString().substring(1, 2));
            String newMem = "M" + Integer.toString(mem + 1);
            e.setText(newMem);

            if (mem == 0) {
                Button b = (Button) findViewById(R.id.buttonMleft);
                b.setEnabled(true);
            }
            if (mem == 8) {
                Button b = (Button) findViewById(R.id.buttonMright);
                b.setEnabled(false);
            }
        }catch (Exception e) {return;}
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
    private String getBandForFreq(String f)
    {
        try {
            int freq = Integer.parseInt(f);
            if (bands[5] != 0L) {
                for (int i = 0; i < NUMBER_OF_BANDS; i++) {
                    if (freq >= bands[2 * i] && freq <= bands[2 * i + 1])
                        return "Hz " + bandsNames[i] + "-m";
                }
                return "Hz nn-m";
            }
        }catch (Exception e){return "Hz er-m";}
        return "Hz xx-m";
    }
    public void setNewF(View view)
    {
        //pogledati da li je validna
        final EditText et2 = (EditText) findViewById(R.id.newFrequency);
        final EditText et22 = (EditText) findViewById(R.id.editText2);
        final EditText currentF = (EditText) findViewById(R.id.editText);
        String pom = et22.getText().toString();
        if(!pom.equals("Hz xx-m") && !pom.equals("Hz er-m") && !pom.equals("Hz nn-m"))
        {
            //da vidimo da li je vec ta
            String cf = currentF.getText().toString().toUpperCase();
            String s = et2.getText().toString();
            String band = ""+bandNameToInt(pom.substring(3, 5));
            String bezTackica = ""+s;
            s = addDots(s);
            if(s.equals(cf.substring(4, cf.indexOf("H"))))
                Toast.makeText(getBaseContext(), "It's current!", Toast.LENGTH_SHORT).show();
            else
                posalji("VFO" + cf.substring(cf.length()-1, cf.length()) + bezTackica + "_" + band + END_OF_MESSAGE_CHAR);
                //"VFOXfrekvencija_opseg"

        }
        else
        {
            Toast.makeText(getBaseContext(), "Frequence must be valid!", Toast.LENGTH_SHORT).show();
        }
    }
    private int bandNameToInt(String s)
    {
        for(int i = 0; i < NUMBER_OF_BANDS; i++)
            if(bandsNames[i].equals(s))
                return i;
        return NUMBER_OF_BANDS + 1;//samo mogu od 0 do 7 :D
    }

    public void setInMemF(View view)
    {
        //pogledati da li je validna
        final EditText et2 = (EditText) findViewById(R.id.editText3);
        final EditText et22 = (EditText) findViewById(R.id.editText5);
        final EditText etM = (EditText) findViewById(R.id.editTextMemory);
        String pom = et22.getText().toString();
        if(!pom.equals("Hz xx-m") && !pom.equals("Hz er-m") && !pom.equals("Hz nn-m"))
        {

            String frq = et2.getText().toString();
            String band = ""+bandNameToInt(pom.substring(3, 5));

            String memo = etM.getText().toString();
            posalji(memo + frq + "_" + band + END_OF_MESSAGE_CHAR);

        }
        else
        {
            Toast.makeText(getBaseContext(), "Frequence must be valid!", Toast.LENGTH_SHORT).show();
        }
    }

}
