package com.example.taggame;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * http://www.wikihow.com/Make-a-Rock%2C-Paper%2C-Scissors-Game-in-Java
 * https://developer.android.com/guide/topics/connectivity/nfc/nfc.html
 *
 * @author jaimesbooth 20150504
 * @modified jaimesbooth 20150505 Imported to Android project from Java CLI
 */
public class BattleActivity extends Activity implements CreateNdefMessageCallback {

    private NfcAdapter mNfcAdapter;
    private TextView textView;

    private String myID;
    private String myMove;
    private String underAttack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle);
        // Associate the player's text field to the variable text view
        textView = (TextView) findViewById(R.id.attackText);
        myID = BluetoothAdapter.getDefaultAdapter().getName();
        myMove = "";
        underAttack = "";
        // game.startGame();

        // Set up NFC when battlefield is started
        //TextView textView = (TextView) findViewById(R.id.textView1);
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG)
                    .show();
            finish();
            return;
        }
        /*
         * Call setNdefPushMessageCallback passing in a NfcAdapter in the onCreate() method
		 * of your activity. These methods require at least one activity that you want to
		 * enable with Android Beam, along with an optional list of other activities to activate.
		 */
        // Register callback
        mNfcAdapter.setNdefPushMessageCallback(this, this);
    }

    /**
     * Creates an NdefMessage that contains the NdefRecords (attack and player ID)
     * that are to be pushed onto the other device.
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        //String playerId = "0001";
        // Get the player's attack move
        if (myMove.length() == 0) {
            myMove = randomMove();
//            textView.setText("You chose " + myMove);
        }
        String textMessage = (myID + "_" + myMove);
        NdefMessage msg = new NdefMessage(new NdefRecord[]{createMimeRecord(
                "text/plain", textMessage.getBytes())});
        return msg;
    }


    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the opponents
     * TextView
     */
    void processIntent(Intent intent) {
        textView = (TextView) findViewById(R.id.attackText);
        Parcelable[] rawMsgs = intent
                .getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        underAttack = new String(msg.getRecords()[0].getPayload());
//        textView.setText(underAttack + "\nYou chose " + myMove);

        textView.setText("You are under attack, \nselect your move to react");

    }

    private String randomMove() {
        Random random = new Random();
        int choice = random.nextInt(3);
        switch (choice) {
            case 0:
                return "ROCK";
            case 1:
                return "PAPER";
            case 2:
                return "SCISSORS";
            default:
                return "ERROR";
        }
    }

    public String computeResult(String moveA, String moveB) {
        // Tie
        if (moveA.equalsIgnoreCase(moveB)) {
            return "missed";
        }

        switch (moveA) {
            case "ROCK":
                return (moveB.equalsIgnoreCase("SCISSORS") ? "hit" : "counter");
            case "PAPER":
                return (moveB.equalsIgnoreCase("ROCK") ? "hit" : "counter");
            case "SCISSORS":
                return (moveB.equalsIgnoreCase("PAPER") ? "hit" : "counter");
        }

        // Should never reach here
        return "ERROR";
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Creates a custom MIME type encapsulated in an NDEF record
     *
     * @param mimeType
     */
    public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
        NdefRecord mimeRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
                mimeBytes, new byte[0], payload);
        return mimeRecord;
    }


    //    @Override
    public void onClick(View v) {
        if (v == findViewById(R.id.bRock))
            myMove = "ROCK";
        else if (v == findViewById(R.id.bPaper))
            myMove = "PAPER";
        else if (v == findViewById(R.id.bScisssors))
            myMove = "SCISSORS";
        textView.setText("You chose " + myMove);
        if (underAttack.length() > 0) {
            takeReaction();
        }
    }

    private void takeReaction() {
        StringTokenizer st = new StringTokenizer(underAttack, "_");
        String enemyID = st.nextToken();
        String enemyMove = st.nextToken();

        String result = computeResult(enemyMove, myMove);
        String resultMessage = enemyID + "_" + myID + "_" + result;
        textView.setText(resultMessage);

        Intent i = new Intent(this, BattlefieldActivity.class);
        i.putExtra("resultMessage", resultMessage);
        Log.e("SENDING ", "> " + i.getExtras().getString("resultMessage"));
        setResult(RESULT_OK, i);
        finish();

    }


}
