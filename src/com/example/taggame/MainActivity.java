package com.example.taggame;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity
        implements View.OnClickListener {
    private Button discoverableButton, serverStartButton, clientStartButton;
    private TextView statusTextView;
    private BroadcastReceiver bluetoothStatusBroadcastReceiver;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        discoverableButton
                = (Button) findViewById(R.id.bDiscoverable);
        discoverableButton.setOnClickListener(this);
        serverStartButton = (Button) findViewById(R.id.bCreate);
        serverStartButton.setOnClickListener(this);
        clientStartButton = (Button) findViewById(R.id.bJoin);
        clientStartButton.setOnClickListener(this);
        statusTextView = (TextView) findViewById(R.id.tvStatus);
    }

    /**
     * Called when the activity is started.
     */
    @Override
    public void onStart() {
        super.onStart();
        // check whether device supports Bluetooth
        BluetoothAdapter bluetoothAdapter
                = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null)
            statusTextView.setText("Bluetooth is not supported");
        else {  // create a broadcast receiver notified when Bluetooth status
            // changes
            if (bluetoothStatusBroadcastReceiver == null)
                bluetoothStatusBroadcastReceiver
                        = new BluetoothStatusBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            intentFilter.addAction
                    (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            registerReceiver(bluetoothStatusBroadcastReceiver,
                    intentFilter);
            if (!bluetoothAdapter.isEnabled()) {
                statusTextView.setText("Bluetooth is OFF!");
                // try to enable Bluetooth on device
                Intent enableBluetoothIntent
                        = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBluetoothIntent);
            } else
                statusTextView.setText("Bluetooth is ON!");
        }
    }

    /**
     * Called when the activity is stopped.
     */
    @Override
    public void onStop() {
        super.onStop();
        if (bluetoothStatusBroadcastReceiver != null)
            unregisterReceiver(bluetoothStatusBroadcastReceiver);
    }

    // implementation of OnClickListener method
    public void onClick(View view) {
        if (view == discoverableButton) {  // make the device discoverable
            Intent discoverableIntent
                    = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra
                    (BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600); //5 min
            startActivity(discoverableIntent);
        } else if (view == serverStartButton) {  // start as a Host
            BattlePlayer battlePlayer = new BattleHost();
            Intent intent = new Intent(this, BattlefieldActivity.class);
            intent.putExtra(BattlePlayer.class.getName(), battlePlayer);
            startActivity(intent);
        } else if (view == clientStartButton) {  // start as a Warrior
            BattlePlayer battlePlayer = new BattleWarrior();
            Intent intent = new Intent(this, BattlefieldActivity.class);
            intent.putExtra(BattlePlayer.class.getName(), battlePlayer);
            startActivity(intent);
        }
    }

    // inner class that receives Bluetooth state and scan mode changes
    public class BluetoothStatusBroadcastReceiver
            extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals
                    (BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int newState
                        = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                switch (newState) {
                    case BluetoothAdapter.STATE_OFF:
                        statusTextView.setText("Bluetooth is OFF!");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        statusTextView.setText("Bluetooth is turning ON");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        statusTextView.setText("Bluetooth is ON!");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        statusTextView.setText("Bluetooth is turning off");
                        break;
                    default:
                }
            } else if (intent.getAction().equals
                    (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int newScanMode = intent.getIntExtra
                        (BluetoothAdapter.EXTRA_SCAN_MODE, -1);
                switch (newScanMode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        statusTextView.setText
                                ("Bluetooth is discoverable");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        statusTextView.setText("Bluetooth is connectable but not discoverable");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        statusTextView.setText
                                ("Bluetooth is neither connectable nor discoverable");
                        break;
                    default:
                }
            }
        }
    }
}