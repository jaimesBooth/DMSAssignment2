package com.example.taggame;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BattleWarrior implements BattlePlayer {
    private static final long serialVersionUID = 1;
    private boolean stopRequested;
    private List<BluetoothDevice> devices;
    private BluetoothSocket socket;
    private List<String> messages;//list of messages still to be mailed
    private BattlefieldActivity battlefield;
    private BroadcastReceiver deviceDiscoveryBroadcastReceiver;
    private int hp;
    private String resultMessage;
    private String playerName;

    public BattleWarrior() {
        devices = new ArrayList<BluetoothDevice>();
        socket = null;
        messages = new ArrayList<String>();
        battlefield = null;
        deviceDiscoveryBroadcastReceiver = null;
        resultMessage = "";
        hp = MAX_HP;
        playerName = BluetoothAdapter.getDefaultAdapter().getName();
    }

    // implementation of BattlePlayer method
    public void run() {
        stopRequested = false;
        devices.clear();
        messages.clear();
        // start device discovery(could instead first try paired devices)
        deviceDiscoveryBroadcastReceiver
                = new DeviceDiscoveryBroadcastReceiver();
        IntentFilter discoveryIntentFilter = new IntentFilter();
        discoveryIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        discoveryIntentFilter.addAction
                (BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        discoveryIntentFilter.addAction
                (BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        battlefield.registerReceiver(deviceDiscoveryBroadcastReceiver,
                discoveryIntentFilter);
        BluetoothAdapter bluetoothAdapter
                = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.startDiscovery();
        // make this thread wait until device discovery has finished
        synchronized (devices) {
            try {
                devices.wait();
            } catch (InterruptedException e) {  // ignore
            }
        }
        if (devices.size() == 0 && !stopRequested) {
            battlefield.receivedUpdate
                    ("CLIENT: no devices discovered, restart client");
//            Log.w("BattleWarrior", "No devices discovered");
            stopRequested = true;
            return;
        }
        // now check each device for the Bluetooth application UUID
        // note only newer API support fetchUuidsWithSdp to perform SDP
        socket = null;
        for (BluetoothDevice device : devices) {  // try to open a connection to device using UUID
            try {
                battlefield.receivedUpdate
                        ("CLIENT: checking for server on " + device.getName());
//                Log.w("BattleWarrior", "Checking for server on "
//                        + device.getName());
                socket = device.createRfcommSocketToServiceRecord
                        (BattlePlayer.SERVICE_UUID);
                // open the connection
                socket.connect();
                bluetoothAdapter.cancelDiscovery();
                break;
            } catch (IOException e) {  // ignore and try next device
                socket = null;
            }
        }
        if (socket == null) {
            battlefield.receivedUpdate
                    ("CLIENT: no server found, restart client");
//            Log.e("BattleWarrior", "No server service found");
            stopRequested = true;
            return;
        }
        battlefield.receivedUpdate("CLIENT: chat server found");
//        Log.w("BattleWarrior", "Chat server service found");
//        Mailer mailer = new Mailer();
//        Thread mailerThread = new Thread(mailer);
//        mailerThread.start();
        // listen for incoming messages
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader
                    (socket.getInputStream()));
            // loop until the connection closes or stop requested
            while (!stopRequested) {
                String message = br.readLine(); // blocking
                // put message on client display
                if (battlefield != null)
                    battlefield.receivedUpdate(message);
            }
        } catch (IOException e) {
            battlefield.receivedUpdate
                    ("CLIENT: Client disconnecting");
//            Log.w("BattleWarrior", "Client Disconnecting");
        } finally {
            try {
                socket.close();
            } catch (IOException e) { // ignore
            }
            socket = null;
        }
    }

    // implementation of BattlePlayer method
    public void forward(String message) {

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new BufferedWriter
                    (new OutputStreamWriter(socket.getOutputStream())));
        } catch (IOException e) {
            Log.e("BattleWarrior", "forward IOException: " + e);
            stop();
        }catch (NullPointerException e){

            Log.e("BattleWarrior", "forward NullPointerException: " + e);
            stop();
        }
        if (message.length() > 0 && pw != null) {
            pw.println(message);
            pw.flush();
        }
    }

    // implementation of BattlePlayer method
    public void stop() {
        stopRequested = true;
        if (deviceDiscoveryBroadcastReceiver != null) {
            battlefield.unregisterReceiver
                    (deviceDiscoveryBroadcastReceiver);
            deviceDiscoveryBroadcastReceiver = null;
        }
        synchronized (devices) {
            devices.notifyAll();
        }
        synchronized (resultMessage) {
            resultMessage.notifyAll();
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) { // ignore
            }
        }
    }

    // implementation of BattlePlayer method
    public void registerActivity(BattlefieldActivity battlefield) {
        this.battlefield = battlefield;
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }


    // inner class that receives device discovery changes
    public class DeviceDiscoveryBroadcastReceiver
            extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {  // a device has been found
                BluetoothDevice device = intent.getParcelableExtra
                        (BluetoothDevice.EXTRA_DEVICE);
                synchronized (devices) {
                    devices.add(device);
                }
                // note newer API can use device.fetchUuidsWithSdp for SDP
                battlefield.receivedUpdate
                        ("CLIENT: device discovered " + device.getName());
//                Log.w("BattleWarrior", "Device discovered " + device.getName());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals
                    (action)) {
                battlefield.receivedUpdate
                        ("CLIENT: device discovery started");
//                Log.w("BattleWarrior", "Device discovery started");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals
                    (action)) {  // notify chat client that device discovery has finished
                synchronized (devices) {
                    devices.notifyAll();
                }
                battlefield.receivedUpdate
                        ("CLIENT: device discovery finished");
//                Log.w("BattleWarrior", "Device discovery finished");
            }
        }
    }
}
