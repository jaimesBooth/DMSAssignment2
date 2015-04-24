/**
 * Activity that represents the GUI display
 * @see AndroidBluetoothDemo.java
 */
package aut.dms;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ChatActivity extends Activity
   implements OnClickListener
{
   private ChatNode chatNode;
   private EditText postEditText;
   private Button sendButton;
   private TextView receivedTextView;
   private List<String> receivedMessages;
   private static final int MAX_RECEIVED_DISPLAY = 5;
   
   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState)
   {  super.onCreate(savedInstanceState);
      setContentView(R.layout.chat_activity);
      // obtain the chatNode passed in with the intent
      Intent intent = getIntent();
      chatNode = (ChatNode)intent.getExtras().get
         (ChatNode.class.getName());
      postEditText = (EditText) findViewById(R.id.post_edittext);
      sendButton = (Button) findViewById(R.id.send_button);
      sendButton.setOnClickListener(this);
      receivedTextView =(TextView)findViewById(R.id.received_textview);
      receivedMessages = new ArrayList<String>();
   }
   
   public void onStart()
   {  super.onStart();
      chatNode.registerActivity(this);
      Thread thread = new Thread(chatNode);
      thread.start();
   }
   
   public void onStop()
   {  super.onStop();
      chatNode.stop();
      chatNode.registerActivity(null);
   }
   
   public synchronized void showReceivedMessage(String message)
   {  receivedMessages.add(0, message);
      final StringBuilder stringBuilder = new StringBuilder();
      for (int i=0; i<receivedMessages.size()&&i<MAX_RECEIVED_DISPLAY;
         i++)
      {  stringBuilder.append(receivedMessages.get(i));
         stringBuilder.append("\n");
      }
      // update the received TextView in the UI thread
      receivedTextView.post(new Runnable()
      {  
         public void run()
         {  receivedTextView.setText(stringBuilder.toString());
         }
      });
   }

   // implementation of OnClickListener method
   public void onClick(View view)
   {  if (view == sendButton)
      {  String message = postEditText.getText().toString();
         chatNode.forward(message);
      }
   }
}
