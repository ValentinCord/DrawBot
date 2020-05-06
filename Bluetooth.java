package com.f.drawbot01;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class Bluetooth extends Activity implements OnClickListener{

    // interface
    private TextView statusBar = null;
    private Button enableButton = null;
    private Button scanButton = null;
    private Button connectButton = null;
    private Spinner deviceSpinner = null;
    private ArrayAdapter <CharSequence> spinnerAdapter = null;
    private TextView listTxt= null;



    //bluetooth
    private BluetoothAdapter btAdapter = null;
    private BluetoothDevice btTarget = null;
    private static BluetoothSocket btSocket = null;
    private Pattern macPattern = null;
    private Boolean btSocketStatus = false;
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private InputStream receiveStream = null;
    private OutputStream sendStream = null;

    final Handler receptHandler = new Handler(){
        public void handleMessage(Message msg) {
            int data = msg.getData().getInt("data");
            receive(data);
        }
    };

    private btReceiverThread btReceiverThread = new btReceiverThread(receptHandler);

    private Boolean discoveringBool=true;
    private Boolean enablingBool=true;

    Handler h;
    Runnable r;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth2);
        Toast.makeText(this, "DrawBot0.1" , Toast.LENGTH_LONG).show();
        // interface
        statusBar = (TextView) findViewById(R.id.logView);


        enableButton = (Button) findViewById(R.id.enableBT);
        scanButton = (Button) findViewById(R.id.scanBT);
        connectButton = (Button) findViewById(R.id.connectButton);
        deviceSpinner = (Spinner) findViewById(R.id.spinnerBT);
        listTxt = (TextView) findViewById(R.id.btListTextView);
        spinnerAdapter  = new ArrayAdapter <CharSequence> (this, android.R.layout.simple_spinner_item);
        enableButton.setOnClickListener(this);
        scanButton.setOnClickListener(this);
        connectButton.setOnClickListener(this);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceSpinner.setAdapter(spinnerAdapter);


        // bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter!=null){
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(receiver, filter);


            macPattern = Pattern.compile("((([0-9a-fA-F]){1,2}[-:]){5}([0-9a-fA-F]){1,2})");
            if(btAdapter.isEnabled()){
                enablingBool =false;
                enableButton.setText(R.string.disableBtButton);
                btListPaired();
            }
        }

        r = new Runnable(){
            public void run(){
                //  getDirectionOrder();
                h.postDelayed(this, 100);
            }
        };
        h = new Handler();

        onConfigurationChanged(Resources.getSystem().getConfiguration());
    }


    @Override
    public void onStop(){
        super.onStop();

        btSocketStatus = false;
        h.removeCallbacks(r);
        btReceiverThread.halt();
        connectButton.setText(R.string.connectButton);
        onConfigurationChanged(Resources.getSystem().getConfiguration());
        log(getString(R.string.BluetoothDisconnected));

    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        h.removeCallbacks(r);
        if(btAdapter!=null)
            unregisterReceiver(receiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if(btSocketStatus){
                showBluetoothInterface(false);
            }
            else{
                showBluetoothInterface(true);
            }

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            showBluetoothInterface(true);
        }
        if(btSocketStatus==false)
            resetConnection();
    }



    // ----------------------------- Interface -----------------------------
    public void onClick(View v) {
        /*int speed = speedBar.getProgress();
        speedBar.setMax(59);*/
        switch(v.getId()){
            case R.id.enableBT:
                btEnable(enablingBool);
                break;

            case R.id.scanBT:
                btDiscovering(discoveringBool);
                break;

            case R.id.connectButton:
                btConnect(!btSocketStatus);
                break;
            default:
                break;
        }
    }

    private void log(String l){
        Log.i(getString(R.string.app_name) + " log",l);
        statusBar.setText(l);
    }

    private void addDeviceToList(BluetoothDevice dev){

        String d = dev.getName() + " - " + dev.getAddress();

        boolean unique = true;
        for(int i=0;i<spinnerAdapter.getCount();i++){
            CharSequence T = spinnerAdapter.getItem(i);

            if(regexMac(d).equals(regexMac(T.toString()))){
                unique = false;
                break;
            }
        }
        if(unique)
            spinnerAdapter.add(d);
    }

    private String regexMac(String txt){
        String mac = "";
        Matcher m = macPattern.matcher(txt);
        if(m.find())
            mac = m.group(1);
        return mac;
    }


    private String targetMacAddress(){
        String mac = "";
        Object t= deviceSpinner.getSelectedItem();
        if(t!=null){
            mac = regexMac(t.toString());
            return mac;
        }
        else{
            log(getString(R.string.errNoDeviceSelected));
            return null;
        }
    }

    private void receive(int data) {

        switch(data){
            case 249 :
                break;
            case 250 :
                break;
            case 251 :
                break;
            case 252 :
                break;
            default :
                break;


        }
    }

    private void showBluetoothInterface(Boolean s){
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        int v;
        if(s)
            v = View.VISIBLE;
        else
            v = View.INVISIBLE;

        enableButton.setVisibility(v);
        scanButton.setVisibility(v);
        listTxt.setVisibility(v);
        deviceSpinner.setVisibility(v);
    }


  private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // Bluetooth adapter state changed
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        log(getString(R.string.BluetoothDisabled));
                        enablingBool = true;
                        enableButton.setText(R.string.enableBtButton);
                        onConfigurationChanged(Resources.getSystem().getConfiguration());
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        log(getString(R.string.BluetoothDisabling));
                        break;
                    case BluetoothAdapter.STATE_ON:
                        log(getString(R.string.BluetoothEnabled));
                        enablingBool = false;
                        enableButton.setText(R.string.disableBtButton);
                        btListPaired();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        log(getString(R.string.BluetoothEnabling));
                        break;
                }
            }

            // Bluetooth discovering started
            else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                discoveringBool = false;
                scanButton.setText(R.string.startdiscovButton);
                log(getString(R.string.BluetoothDiscovStarted));
            }

            // Bluetooth discovering stopped
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                discoveringBool = true;
                scanButton.setText(R.string.startdiscovButton);
                log(getString(R.string.BluetoothDiscovStopped));
            }

            // New bluetooth device discovered
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addDeviceToList(device);
            }

            // Bluetooth device connected
            else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)){
                btSocketStatus = true;
                connectButton.setText(R.string.disconnectButton);
                h.postDelayed(r, 100);
                btReceiverThread.start();

                onConfigurationChanged(Resources.getSystem().getConfiguration());
                log(getString(R.string.BluetoothConnected));
            }

            // Bluetooth device disconnection have been requested
            else if(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)){
                log(getString(R.string.BluetoothDiconnectionRequested));
            }

            // Bluetooth device disconnected
            else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)){
                onConfigurationChanged(Resources.getSystem().getConfiguration());
                btSocketStatus = false;
                h.removeCallbacks(r);
                btReceiverThread.halt();
                connectButton.setText(R.string.connectButton);
                log(getString(R.string.BluetoothDisconnected));
            }
        }
    };




    // ----------------------------- Bluetooth -----------------------------
    private void btEnable(Boolean s) {
        if(btAdapter==null){
            log(getString(R.string.errBluetoothNotSupported));
            return;
        }

        if(s){
            if (btAdapter.isEnabled()){
                log(getString(R.string.BluetoothAlreadyEnabled));
                return;
            }

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        else{
            btAdapter.disable();
        }

    }



    private void btListPaired(){
        if(btAdapter==null){
            log(getString(R.string.errBluetoothNotSupported));
            return;
        }

        if(!btAdapter.isEnabled()){
            log(getString(R.string.errBluetoothNotEnabled));
            return;
        }

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices)
                addDeviceToList(device);
        }
    }



    private void btDiscovering(Boolean s){
        if(btAdapter==null){
            log(getString(R.string.errBluetoothNotSupported));
            return;
        }

        if(!btAdapter.isEnabled()){
            log(getString(R.string.errBluetoothNotEnabled));
            return;
        }

        if(s)
            btAdapter.startDiscovery();
        else
            btAdapter.cancelDiscovery();

    }


    private void resetConnection() {
        if (receiveStream != null) {
            btSend(0);
            try {
                receiveStream.close();
            }
            catch (Exception e) {
                log(e.toString());e.printStackTrace();
            }
            receiveStream = null;
        }

        if (sendStream != null) {
            try {
                sendStream.close();
            }
            catch (Exception e) {
                log(e.toString());e.printStackTrace();
            }
            sendStream = null;
        }

        if (btSocket != null) {
            try {
                btSocket.close();
            }
            catch (Exception e) {
                log(e.toString());e.printStackTrace();
            }
            btSocket = null;
        }

    }
    private void btConnect(Boolean s){

        if(btAdapter == null){
            log(getString(R.string.errBluetoothNotSupported));
            return;
        }

        if(!btAdapter.isEnabled()){
            log(getString(R.string.errBluetoothNotEnabled));
            return;
        }

        if(s){
            resetConnection();
            String mac = targetMacAddress();
            if(mac == null)
                return;

            try{
                btTarget = btAdapter.getRemoteDevice(mac);
            }
            catch(IllegalArgumentException e){
                log(e.toString());e.printStackTrace();
            }

            try {
                btSocket = btTarget.createRfcommSocketToServiceRecord(SPP_UUID);
            }
            catch (IOException e) {
                log(e.toString());e.printStackTrace();
            }
            try {
                if(btSocket!=null){
                    receiveStream = btSocket.getInputStream();
                    sendStream = btSocket.getOutputStream();
                }
            }
            catch (IOException e) {
                log(e.toString());e.printStackTrace();
            }

            if(btAdapter.isDiscovering())
                btAdapter.cancelDiscovery();

            try {
                btSocket.connect();
            }
            catch (IOException e) {
                log(e.toString());e.printStackTrace();
            }
        }
        else{
            resetConnection();
        }
    }

    public static BluetoothSocket getSocket() {
        return btSocket;
    }


    public void btSend(int data){
        byte d;
        d = (byte) data;
        if(btSocketStatus){
            try {
                if(sendStream != null){
                    sendStream.write(d);
                    sendStream.flush();
                }
            } catch (IOException e) {
                log(e.toString());e.printStackTrace();
            }
        }
        else
            log(getString(R.string.errBluetoothNotConnected));
    }

    private class btReceiverThread extends Thread {

        private Boolean isRunning;
        Handler h = null;

        btReceiverThread(Handler h){
            this.h = h;
        }

        @Override
        public void run() {
            isRunning=true;
            while(isRunning) {
                try {
                    int k = receiveStream.read();	//read one byte
                    if(k > -1 && h!=null){
                        Message msg = h.obtainMessage();
                        Bundle b = new Bundle();
                        b.putInt("data", k);
                        msg.setData(b);
                        h.sendMessage(msg);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void halt() {
            isRunning = false;
        }

    }

}
