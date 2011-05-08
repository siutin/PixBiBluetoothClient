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
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener; 

public class PixBiBluetoothClient extends Activity {
     
     private static final String TAG = "PixBiBluetoothClient";
     private static final boolean D = true;
     private BluetoothAdapter mBluetoothAdapter = null;
     private BluetoothSocket btSocket = null;
     private OutputStream outStream = null;
     private Button mReConnBtn = null;
     private Button mDisConnBtn = null;
     
     
     // Message types sent from the BluetoothService Handler
     public static final int MESSAGE_STATE_CHANGE = 1;
     public static final int MESSAGE_READ = 2;
     public static final int MESSAGE_WRITE = 3;
     public static final int MESSAGE_DEVICE_NAME = 4;
     public static final int MESSAGE_TOAST = 5;
     
     // Key names received from the BluetoothChatService Handler
     public static final String DEVICE_NAME = "device_name";
     public static final String TOAST = "toast";
     
     // Name of the connected device
     private String mConnectedDeviceName = null;
     private TextView rMsgBox = null;
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
          
          rMsgBox = (TextView)findViewById(R.id.rMsgBox);
          rMsgBox.setText("Hello world!\n");
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
    	 
    	 mReConnBtn = (Button) findViewById(R.id.reconnBtn);
    	 mReConnBtn.setOnClickListener(new OnClickListener(){
    		public void onClick(View v){
    	          if (btService != null) btService.stop();
            	  btService.start();
            	  connectDevice();
    	          Log.e(TAG, "--- CLICKED RECONNECT BUTTON ---");
    		}
    	 });
    	 
    	 mDisConnBtn = (Button) findViewById(R.id.disconnBtn);
    	 mDisConnBtn.setOnClickListener(new OnClickListener(){
    		public void onClick(View v){
    	          if (btService != null) btService.stop();
    	          Log.e(TAG, "--- CLICKED DISCONNECT BUTTON ---");
    		}
    	 });
    	 
    	 btService = new BluetoothService(this,mHandler);
    	 
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
     }
 
     @Override
     public void onPause() {
          super.onPause();
 
          if (D)
               Log.e(TAG, "- ON PAUSE -");
     }

     // The Handler that gets information back from the BluetoothChatService
     private final Handler mHandler = new Handler() {
         @Override
         public void handleMessage(Message msg) {
             switch (msg.what) {
             case MESSAGE_STATE_CHANGE:
                 if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                 switch (msg.arg1) {
                 case BluetoothService.STATE_CONNECTED:
                     //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                     //mConversationArrayAdapter.clear();
                	 rMsgBox.append("Bluetooth connected.\n");
                     break;
                 case BluetoothService.STATE_CONNECTING:
                     //setStatus(R.string.title_connecting);
                	 rMsgBox.append("Bluetooth connecting..\n");
                     break;
                 case BluetoothService.STATE_NONE:
                     //setStatus(R.string.title_not_connected);
                	 rMsgBox.append("Bluetooth not connected.\n");
                     break;
                 }
                 break;
             case MESSAGE_WRITE:
                 //byte[] writeBuf = (byte[]) msg.obj;
                 // construct a string from the buffer
                 //String writeMessage = new String(writeBuf);
                 //mConversationArrayAdapter.add("Me:  " + writeMessage);
                 break;
             case MESSAGE_READ:
                 byte[] readBuf = (byte[]) msg.obj;
                 // construct a string from the valid bytes in the buffer
                 String readMessage = new String(readBuf, 0, msg.arg1);
                 rMsgBox.append("Read Message: " + readMessage+"\n");
                 break;
             case MESSAGE_DEVICE_NAME:
                 // save the connected device's name
                 mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                 Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                 break;
             case MESSAGE_TOAST:
                 Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                                Toast.LENGTH_SHORT).show();
                 break;
             }
         }
     };
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