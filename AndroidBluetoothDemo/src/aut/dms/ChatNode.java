/**
   An interface that represents one chat node in the Bluetooth network
   @see AndroidBluetoothDemo.java
*/
package aut.dms;

import java.io.Serializable;
import java.util.UUID;

public interface ChatNode extends Runnable, Serializable
{
   // uuid for the Bluetooth application
   public static final UUID SERVICE_UUID
      = UUID.fromString("aa7e561f-591f-4767-bf26-e4bff3f0895f");
   // name for the Bluetooth application
   public static final String SERVICE_NAME = "Android Bluetooth Demo";
   // forward a message to all chat nodes in the Bluetooth network
   public void forward(String message);
   // stop this chat node and clean up
   public void stop();
   // registers or unregisters (if null) a ChatActivity for display
   public void registerActivity(ChatActivity chatActivity);
}
