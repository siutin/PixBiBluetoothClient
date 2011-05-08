package com.example.pixbibluetooth;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import com.example.thinbtclient.R;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
 
public class PixBiBluetoothClient extends Activity {
     
     private static final String TAG = "PixBiBluetoothClient";
     private static final boolean D = true;
     private BluetoothAdapter mBluetoothAdapter = null;
     private BluetoothSocket btSocket = null;
     private OutputStream outStream = null;
     
     private BluetoothService btService = null;
     
     // Well known SPP UUID (will *probably* map to
     // RFCOMM channel 1 (default) if not in use);
     // see comments in onResume().
     private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 
     // ==> hardcode your server's MAC address here <==
     private static String address = "00:10:06:29:00:86";
 
     /** Called when the activity is first created. */
     @Override
     public void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.main);
 
          if (D)
               Log.e(TAG, "+++ ON CREATE +++");
 
          mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
          if (mBluetoothAdapter == null) {
               Toast.makeText(this,"Bluetooth is not available.",  Toast.LENGTH_LONG).show();
               finish();
               return;
          }

     }
 
     @Override
     public void onStart() {
          super.onStart();
          if (D)
               Log.e(TAG, "++ ON START ++");
          
          if (!mBluetoothAdapter.isEnabled()) {
              Toast.makeText(this,
                   "Please enable your BT and re-run this program.",
                   Toast.LENGTH_LONG).show();
              finish();
              return;
         }
          setupClient();          
         if (D)
              Log.e(TAG, "+++ DONE IN ON CREATE, GOT LOCAL BT ADAPTER +++");
     }
 
     private void setupClient() {
    	 Log.d(TAG, "setupClient()");
    	 btService = new BluetoothService(this,null);
	}
     private void connectDevice() {
         // Get the BluetoothDevice object
         BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
         // Attempt to connect to the device
         btService.connect(device);
     }
	@Override
     public void onResume() {
          super.onResume();
 
          if (D) {
               Log.e(TAG, "+ ON RESUME +");
               Log.e(TAG, "+ ABOUT TO ATTEMPT CLIENT CONNECT +");
          }
          
          if(btService != null){
        	  btService.start();
        	  connectDevice();
          }
          // When this returns, it will 'know' about the server,
          // via it's MAC address.
        
          
          /*
          try {
               btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
          } catch (IOException e) {
               Log.e(TAG, "ON RESUME: Socket creation failed.", e);
          }
          Log.e(TAG,"btSocket: " + btSocket);

          mBluetoothAdapter.cancelDiscovery();

          // Blocking connect, for a simple client nothing else can
          // happen until a successful connection is made, so we
          // don't care if it blocks.
          boolean connectStatus = false;
          try {
               btSocket.connect();
               connectStatus = true;
               Log.e(TAG, "ON RESUME: BT connection established, data transfer link open.");               
          } catch (IOException e) {
               try {
                    btSocket.close();
               } catch (IOException e2) {
                    Log.e(TAG,"ON RESUME: Unable to close socket during connection failure", e2);                    
               }
          }
          if(!connectStatus) {
        	  Log.e(TAG,"ON RESUME: Error return .");
        	  return;
          }
        	          	  
          // Create a data stream so we can talk to server.
          if (D)
               Log.e(TAG, "+ ABOUT TO SAY SOMETHING TO SERVER +");
 
          try {
               outStream = btSocket.getOutputStream();
          } catch (IOException e) {
               Log.e(TAG, "ON RESUME: Output stream creation failed.", e);
          }
 
          String message = "Hello message from client to server.";
          byte[] msgBuffer = message.getBytes();
          try {
               outStream.write(msgBuffer);
          } catch (IOException e) {
               Log.e(TAG, "ON RESUME: Exception during write.", e);
          }*/
     }
 
     @Override
     public void onPause() {
          super.onPause();
 
          if (D)
               Log.e(TAG, "- ON PAUSE -");
 
          if (outStream != null) {
               try {
                    outStream.flush();
               } catch (IOException e) {
                    Log.e(TAG, "ON PAUSE: Couldn't flush output stream.", e);
               }
          }
 
          try  {
               btSocket.close();
          } catch (IOException e2) {
               Log.e(TAG, "ON PAUSE: Unable to close socket.", e2);
          }
     }
 
     @Override
     public void onStop() {
          super.onStop();
          if (D)
               Log.e(TAG, "-- ON STOP --");
     }
 
     @Override
     public void onDestroy() {
          super.onDestroy();
          if (btService != null) btService.stop();
          if (D)
               Log.e(TAG, "--- ON DESTROY ---");
     }
}