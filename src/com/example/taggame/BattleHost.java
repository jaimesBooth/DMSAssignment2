package com.example.taggame;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class BattleHost implements BattlePlayer {
    private static final long serialVersionUID = 1;
    private boolean stopRequested;
    private List<ClientHandler> clientConnections;
    private List<String> messages;//list of messages still to be mailed
    private BattlefieldActivity battlefield;


    private int hp;
    private String playerName;
    private HashMap<String, Integer> playerHpMap;
    private List<String> results;

    public BattleHost() {

        battlefield = null;
        clientConnections = new ArrayList<ClientHandler>();
        messages = new ArrayList<String>();
        hp = MAX_HP;
        playerHpMap = new HashMap<String, Integer>();
        playerName = BluetoothAdapter.getDefaultAdapter().getName();
        playerHpMap.put(playerName, MAX_HP);
        playerHpMap.put("other", MAX_HP);
        results = new ArrayList<String>();
    }

    // implementation of BattlePlayer method
    public void run() {
        stopRequested = false;
        clientConnections.clear();
        messages.clear();
        BluetoothServerSocket serverSocket = null;
        try {
            BluetoothAdapter bluetoothAdapter
                    = BluetoothAdapter.getDefaultAdapter();
            serverSocket
                    = bluetoothAdapter.listenUsingRfcommWithServiceRecord
                    (BattlePlayer.SERVICE_NAME, BattlePlayer.SERVICE_UUID);
        } catch (IOException e) {
//            Log.e("BattleHost", "IOException: " + e);
            return;
        }
        // prepare the mailer that will handle outgoing messages
        Mailer mailer = new Mailer();
        Thread mailerThread = new Thread(mailer);
        mailerThread.start();
        // listen for connections
        while (!stopRequested) {
            try {  //block upto 500ms timeout to get incoming connected socket
                BluetoothSocket socket = serverSocket.accept(500);
                battlefield.receivedUpdate
                        ("SERVER: Client connected");
//                Log.w("BattleHost", "New client connection accepted");
                // handle the client connection in a separate thread
                ClientHandler clientHandler = new ClientHandler(socket);
                clientConnections.add(clientHandler);
                if(playerHpMap.isEmpty() || !playerHpMap.containsKey(socket.getRemoteDevice().getName())){
                    playerHpMap.put(socket.getRemoteDevice().getName(), MAX_HP);
                }
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            } catch (IOException e) { // ignore
            }
        }
        // close the server socket
        try {
            serverSocket.close();
        } catch (IOException e) { // ignore
        }
    }

    // implementation of BattlePlayer method
    public void forward(String message) {
        interpret(message);

    }

    // implementation of BattlePlayer method
    public void stop() {
        stopRequested = true;
        synchronized (messages) {
            messages.notifyAll();
        }
        for (ClientHandler clientConnection : clientConnections)
            clientConnection.closeConnection();
    }

    // implementation of BattlePlayer method
    public void registerActivity(BattlefieldActivity battlefield) {
        this.battlefield = battlefield;
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    private void interpret(String message) {
    	
    	
        StringTokenizer tokenizer = new StringTokenizer(message, "_");
        String attackerID = tokenizer.nextToken();
        String enemyID = tokenizer.nextToken();
        String result = tokenizer.nextToken().toLowerCase();
        int hp = 0;

        Log.e("INTERPRETATION", "-------------------" + message);
        switch (result) {
            case "hit":
                hp = playerHpMap.get(enemyID) - 2;
                if (hp < 0) hp = 0;
                playerHpMap.put(enemyID, hp);
                break;
            case "counter":
                hp = playerHpMap.get(attackerID) - 1;
                if (hp < 0) hp = 0;
                playerHpMap.put(attackerID, hp);
                break;
            case "missed":
                break;
            default:
                Log.e("INTERPRETATION", "-------not working");
                break;
        }
        String attackerInfo = attackerID + "_" + playerHpMap.get(attackerID);
        String enemyInfo = enemyID + "_" + playerHpMap.get(enemyID);
        synchronized (messages) {
            messages.add("update_"+attackerInfo + "_" + enemyInfo + "_" + result);
            // notify waiting threads that there is a new message to send
            messages.notifyAll();
        }
    }

    // inner class that handles incoming communication with a client
    private class ClientHandler implements Runnable {
        private BluetoothSocket socket;
        private PrintWriter pw;


        public ClientHandler(BluetoothSocket socket) {
            this.socket = socket;
            try {
                pw = new PrintWriter(new BufferedWriter
                        (new OutputStreamWriter(socket.getOutputStream())));
            } catch (IOException e) {
                battlefield.receivedUpdate
                        ("SERVER: Error creating pw");
//                Log.e("BattleHost", "ClientHandler IOException: " + e);
            }
        }

        // repeatedly listens for incoming messages
        public void run() {
            try {
                BufferedReader br = new BufferedReader
                        (new InputStreamReader(socket.getInputStream()));
                // loop until the connection closes or stop requested
                while (!stopRequested) {
                    String message = br.readLine(); // blocking
                    interpret(message);
                }
            } catch (IOException e) {
                battlefield.receivedUpdate
                        ("SERVER: Client disconnecting");
//                Log.w("BattleHost", "Client Disconnecting");
            } finally {
                closeConnection();
            }
        }

        public void send(String message) throws IOException {
            pw.println(message);
            pw.flush();
        }

        public void closeConnection() {
            try {
                socket.close();
            } catch (IOException e) { // ignore
            }
            clientConnections.remove(this);
        }
    }

    // inner class handles sending messages to all client chat nodes
    private class Mailer implements Runnable {
        public void run() {
            while (!stopRequested) {  // get a message
                String message;
                synchronized (messages) {
                    while (messages.size() == 0) {
                        try {
                            messages.wait();
                        } catch (InterruptedException e) { // ignore
                        }
                        if (stopRequested)
                            return;
                    }
                    message = messages.remove(0);
                }
                // put message on server display
                if (battlefield != null)
                    battlefield.receivedUpdate(message);
                // pass message to all clients
                for (ClientHandler clientHandler : clientConnections) {
                    try {
                        clientHandler.send(message);
//                        battlefield.receivedUpdate
//                                ("SERVER: sending message " + message);
                    } catch (IOException e) {
                        battlefield.receivedUpdate
                                ("SERVER: Error sending message");
//                        Log.e("BattleHost",
//                                "Mailer Message Dropped: " + message);
                    }
                }
            }
        }
    }
}
