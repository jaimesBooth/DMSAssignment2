/**
   A class that represents a server for the Bluetooth chat network
   @see AndroidBluetoothDemo.java
 */
package aut.dms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class ChatServer implements ChatNode
{
   private boolean stopRequested;
   private List<ClientHandler> clientConnections;
   private List<String> messages;//list of messages still to be mailed
   private ChatActivity chatActivity;
   private static final long serialVersionUID = 1;

   public ChatServer()
   {  chatActivity = null;
      clientConnections = new ArrayList<ClientHandler>();
      messages = new ArrayList<String>();
   }

   // implementation of ChatNode method
   public void run()
   {  stopRequested = false;
      clientConnections.clear();
      messages.clear();
      BluetoothServerSocket serverSocket = null;
      try
      {  BluetoothAdapter bluetoothAdapter
            = BluetoothAdapter.getDefaultAdapter();
         serverSocket
            = bluetoothAdapter.listenUsingRfcommWithServiceRecord
            (ChatNode.SERVICE_NAME, ChatNode.SERVICE_UUID);
      }
      catch (IOException e)
      {  Log.e("ChatServer", "IOException: " + e);
         return;
      }
      // prepare the mailer that will handle outgoing messages
      Mailer mailer = new Mailer();
      Thread mailerThread = new Thread(mailer);
      mailerThread.start();
      // listen for connections
      while (!stopRequested)
      {  try
         {  //block upto 500ms timeout to get incoming connected socket
            BluetoothSocket socket = serverSocket.accept(500);
            chatActivity.showReceivedMessage
               ("SERVER: Client connected");
            Log.w("ChatServer", "New client connection accepted");
            // handle the client connection in a separate thread
            ClientHandler clientHandler = new ClientHandler(socket);
            clientConnections.add(clientHandler);
            Thread clientThread = new Thread(clientHandler);
            clientThread.start();
         }
         catch (IOException e)
         { // ignore
         }
      }
      // close the server socket
      try
      {  serverSocket.close();
      }
      catch (IOException e)
      { // ignore
      }
   }

   // implementation of ChatNode method
   public void forward(String message)
   {  synchronized (messages)
      {  messages.add(message);
         // notify waiting threads that there is a new message to send
         messages.notifyAll();
      }
   }
   
   // implementation of ChatNode method
   public void stop()
   {  stopRequested = true;
      synchronized (messages)
      {  messages.notifyAll();
      }
      for (ClientHandler clientConnection : clientConnections)
         clientConnection.closeConnection();
   }
   
   // implementation of ChatNode method
   public void registerActivity(ChatActivity chatActivity)
   {  this.chatActivity = chatActivity;
   }
   
   // inner class that handles incoming communication with a client
   private class ClientHandler implements Runnable
   {
      private BluetoothSocket socket;
      private PrintWriter pw;
      
      public ClientHandler(BluetoothSocket socket)
      {  this.socket = socket;
         try
         {  pw = new PrintWriter(new BufferedWriter
               (new OutputStreamWriter(socket.getOutputStream())));
         }
         catch (IOException e)
         {  chatActivity.showReceivedMessage
               ("SERVER: Error creating pw");
            Log.e("ChatServer", "ClientHandler IOException: " + e);
         }
      }
      
      // repeatedly listens for incoming messages
      public void run()
      {  try
         {  BufferedReader br = new BufferedReader
               (new InputStreamReader(socket.getInputStream()));
            // loop until the connection closes or stop requested
            while (!stopRequested)
            {  String message = br.readLine(); // blocking
               forward(message); // have server forward message
            }
         }
         catch (IOException e)
         {  chatActivity.showReceivedMessage
               ("SERVER: Client disconnecting");
            Log.w("ChatServer", "Client Disconnecting");
         }
         finally
         {  closeConnection();
         }
      }
      
      public void send(String message) throws IOException
      {  pw.println(message);
         pw.flush();
      }
      
      public void closeConnection()
      {  try
         {  socket.close();
         }
         catch (IOException e)
         { // ignore
         }
         clientConnections.remove(this);
      }
   }
   
   // inner class handles sending messages to all client chat nodes
   private class Mailer implements Runnable
   {
      public void run()
      {  while (!stopRequested)
         {  // get a message
            String message;
            synchronized (messages)
            {  while (messages.size() == 0)
               {  try
                  {  messages.wait();
                  }
                  catch (InterruptedException e)
                  { // ignore
                  }
                  if (stopRequested)
                     return;
               }
               message = messages.remove(0);
            }
            // put message on server display
            if (chatActivity != null)
               chatActivity.showReceivedMessage("RECEIVED: "+message);
            // pass message to all clients
            for (ClientHandler clientHandler : clientConnections)
            {  try
               {  clientHandler.send(message);
                  chatActivity.showReceivedMessage
                     ("SERVER: sending message " + message);	
               }
               catch (IOException e)
               {  chatActivity.showReceivedMessage
                     ("SERVER: Error sending message");
                  Log.e("ChatServer",
                     "Mailer Message Dropped: " + message);
               }
            }
         }
      }
   }
}
