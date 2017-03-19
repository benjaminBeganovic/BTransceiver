package com.example.asd.btransceiver;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import com.example.asd.btransceiver.BTService.LocalBinder;
import android.text.method.ScrollingMovementMethod;
import android.os.Handler;
import android.os.IBinder;
import android.widget.EditText;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.view.View;

    public class MessageActivity extends AppCompatActivity {

        char[] allowedChars = new char[] {'.',',','?','-','=','/','\n','\'',':',' '};
        protected static final int MESSAGE_READ = 5;
        protected static final int MESSAGE_CONNECTED = 3;
        protected static final int MESSAGE_CONNECTION_ERROR = 0;
        private static Handler h = null;
        private static BTService btService = null;
        private static String poruke = "";
        private static final char END_OF_MESSAGE_CHAR = (char)4;
        private static int sendingIndex = -1;
        private boolean previousSent = true;

        private ServiceConnection myConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                LocalBinder binder = (LocalBinder) service;
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
            setContentView(R.layout.activity_message);

            //etSlanje, etPrimanje
            final EditText etPrimanje = (EditText) findViewById(R.id.editTextPoruke);
            etPrimanje.setKeyListener(null);
            etPrimanje.setScroller(new Scroller(getBaseContext()));
            etPrimanje.setVerticalScrollBarEnabled(true);
            etPrimanje.setMovementMethod(new ScrollingMovementMethod());

            final TextView tvMessage = (TextView) findViewById(R.id.textViewStatus);


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
                            tvMessage.setBackgroundColor(Color.WHITE);
                            tvMessage.setTextColor(Color.RED);
                            tvMessage.setText("Disconnected!");
                            break;
                    }
                };
            };


            Intent intent = new Intent(this, BTService.class);
            bindService(intent, myConnection, BIND_AUTO_CREATE);

            //filter sa stackoverflow-a
            //http://stackoverflow.com/questions/3349121/how-do-i-use-inputfilter-to-limit-characters-in-an-edittext-in-android
            InputFilter filter = new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                    boolean keepOriginal = true;
                    StringBuilder sb = new StringBuilder(end - start);
                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);
                        if (isCharAllowed(c))
                            sb.append(c);
                        else
                            keepOriginal = false;
                    }
                    if (keepOriginal)
                        return null;
                    else {
                        if (source instanceof Spanned) {
                            SpannableString sp = new SpannableString(sb);
                            TextUtils.copySpansFrom((Spanned) source, start, sb.length(), null, sp, 0);
                            return sp;
                        } else {
                            return sb;
                        }
                    }
                }

                private boolean isCharAllowed(char c) {

                    if(Character.isLetterOrDigit(c))
                        return true;

                    for(int i = 0; i < allowedChars.length; i++)
                        if(allowedChars[i] == c)
                            return true;

                    return false;
                }
            };


            EditText e = (EditText) findViewById(R.id.editTextPoruka);
            e.setFilters(new InputFilter[] { filter });
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

        private void changeStatus()
        {
            TextView tvMessage = (TextView) findViewById(R.id.textViewStatus);
            if(btService != null)
            {
                btService.registerR(this);
                btService.setMessageHandler(h);
                btService.resumeMessageHandler();
                btService.resumeMessageHandler2();

                if(btService.isLaunched()){
                    btService.bThread.getUnreadMessages();
                    tvMessage.setText("Connected!");
                    tvMessage.setBackgroundColor(Color.GREEN);
                    tvMessage.setTextColor(Color.BLUE);
                }else{
                    tvMessage.setText("Disconnected!");
                    tvMessage.setBackgroundColor(Color.WHITE);
                    tvMessage.setTextColor(Color.RED);
                }
            }
            else
            {
                tvMessage.setText("Disconnected!");
                tvMessage.setBackgroundColor(Color.WHITE);
                tvMessage.setTextColor(Color.RED);
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            EditText etPrimanje = (EditText) findViewById(R.id.editTextPoruke);
            etPrimanje.append(poruke);

            changeStatus();
        }

        @Override
        public void onPause() {
            super.onPause();

            if(btService != null)
            {
                btService.pauseMessageHandler();
                btService.pauseMessageHandler2();
                btService.unregisterR();
            }
        }

        public void messageF2(View view) {

            if(!previousSent)
            {
                Toast.makeText(getBaseContext(), "Please wait...", Toast.LENGTH_SHORT).show();
                return;
            }

            final EditText etPrimanje = (EditText) findViewById(R.id.editTextPoruke);
            final EditText etPoruka = (EditText) findViewById(R.id.editTextPoruka);
            String poruka = etPoruka.getText().toString();

            int spaces = 0;
            while(spaces < poruka.length())
            {
                if(poruka.charAt(spaces) == ' ' || poruka.charAt(spaces) == '\n')
                    spaces++;
                else
                    break;
            }
            if(spaces >= poruka.length())
            {
                Toast.makeText(getBaseContext(), "Your message is empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            if(!poruka.equals(""))
            {
                if(btService != null)
                {
                    if(btService.isLaunched())
                    {
                        etPoruka.setText("");
                        sendingIndex = poruke.length()+1;
                        poruke += "\nSending...\n"+ poruka +"\n";
                        etPrimanje.setText("");
                        etPrimanje.append(poruke);
                        previousSent = false;
                        btService.sendMessage("P"+poruka + END_OF_MESSAGE_CHAR);

                    }else{
                        Toast.makeText(getBaseContext(), "No connection!", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getBaseContext(), "No connection!", Toast.LENGTH_SHORT).show();
                }
            }
        }
        public void dolaznaPoruka(String s)
        {
            final EditText etPrimanje = (EditText) findViewById(R.id.editTextPoruke);
            if(s.substring(0,1).equals("P"))
            {
                try {
                    poruke += "\nReceived\n" + s.substring(1, s.length()) + "\n";
                    etPrimanje.append("\nReceived\n" + s.substring(1, s.length()) + "\n");
                } catch (Exception e) {return;}
            }
            else if(s.substring(0,1).equals("T") && sendingIndex > -1){

                previousSent = true;

                try {

                    poruke = poruke.substring(0,sendingIndex)+"Sent      "+poruke.substring(sendingIndex+10, poruke.length());
                    etPrimanje.setText("");
                    etPrimanje.append(poruke);

                } catch (Exception e) {return;}
            }
        }

    }
