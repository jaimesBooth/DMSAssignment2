package com.example.taggame;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class BattlefieldActivity extends Activity
        implements OnClickListener {
    static BattlePlayer battlePlayer;
    private Button readyButton;
    private TextView updateTextView, currentStats, previousAction;
    private List<String> receivedMessages;
    private String hpLable, actionLable;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battlefield);
        // obtain the battlePlayer passed in with the intent
        Intent intent = getIntent();
        battlePlayer = (BattlePlayer) intent.getExtras().get
                (BattlePlayer.class.getName());
        readyButton = (Button) findViewById(R.id.bReady);
        readyButton.setOnClickListener(this);
        updateTextView = (TextView) findViewById(R.id.tvUpdates);
        receivedMessages = new ArrayList<String>();

        currentStats = (TextView) findViewById(R.id.tvHp);
        previousAction = (TextView) findViewById(R.id.tvMove);
        hpLable = ""+battlePlayer.MAX_HP;
        actionLable = battlePlayer.getPlayerName() + ", you are in perfect condition.";

        currentStats.setText("Your current HP: " + hpLable);
        previousAction.setText(actionLable);
        battlePlayer.registerActivity(this);
        Thread thread = new Thread(battlePlayer);
        thread.start();
    }

    public void onStart() {
        super.onStart();
    }

    public void onStop() {
        super.onStop();
    }

    public synchronized void receivedUpdate(String message) {
        receivedMessages.add(0, message);
        final String update = interpret(message);

        // update the received TextView in the UI thread
        updateTextView.post(new Runnable() {
            public void run() {
                updateTextView.setText(update + "\n" + updateTextView.getText().toString());
                currentStats.setText("Your current HP: " + hpLable);
                previousAction.setText(actionLable);
            }
        });

    }

    private String interpret(String s) {
        StringTokenizer tokenizer = new StringTokenizer(s,"_");
        String type = tokenizer.nextToken();
        if(type.equalsIgnoreCase("update")) {
            String attackerID = tokenizer.nextToken();
            String attackerHP = tokenizer.nextToken();
            String enemyID = tokenizer.nextToken();
            String enemyHP = tokenizer.nextToken();
            String result = tokenizer.nextToken().toLowerCase();
            if (battlePlayer.getPlayerName().equals(attackerID)) {
                attackerID = "YOU";
                hpLable = attackerHP;
            }
            if (battlePlayer.getPlayerName().equals(enemyID)) {
                enemyID = "YOU";
                hpLable = enemyHP;
            }
            String translation = "";

            switch (result) {
                case "hit":
                    translation = attackerID + "[" + attackerHP + "]" + " attacked " + enemyID + "[" + enemyHP + "]";
                    break;
                case "counter":
                    translation = enemyID + "[" + enemyHP + "]" + " counterattacked " + attackerID + "[" + attackerHP + "]";
                    break;
                case "missed":
                    translation = enemyID + "[" + enemyHP + "]" + " dodged the attack from " + attackerID + "[" + attackerHP + "]";
                    break;
                default:
                    translation = "Something strange thing just happened in the Battlefield!!";
                    break;
            }
            if (attackerID.equals("YOU") || enemyID.equals("YOU")) {
                actionLable = translation;
            }

            return translation;
        }else
            return s;


    }

    // implementation of OnClickListener method
    public void onClick(View view) {
        if (view == readyButton) {
            Intent intent = new Intent(this, BattleActivity.class);
            startActivity(intent);

        } 
    }


}
