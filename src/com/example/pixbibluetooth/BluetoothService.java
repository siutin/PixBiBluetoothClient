package com.example.pixbibluetooth;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue ;
import java.util.concurrent.BlockingQueue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BluetoothService {
    // Debugging
    private static final String TAG = "PixBiBluetoothClientService";
    private static final boolean D = true;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private BufferThread bufferThread;
	private BlockingQueue<String> bufferqueue;
	
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private int mState;
    
	public BluetoothService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }
	/**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(PixBiBluetoothClient.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }
    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    public synchronized void start() {
        Log.d(TAG, " ++ ConnectThread start ++");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
      
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        
        // Cancel any thread currently running a connection
        if (bufferThread != null) {mConnectedThread = null;}
        
        setState(STATE_LISTEN);
    }

    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }
        
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        
        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
  
        setState(STATE_CONNECTING);        
    }
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice	 device) {
    	Log.d(TAG,"Device Name: "+ device.getName());
        Log.d(TAG, "++ connected to BluetoothBee!!! ++");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        
    	bufferqueue = new LinkedBlockingQueue<String>();
        
    	
    	bufferThread = new BufferThread();
    	bufferThread.start();
    	
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(PixBiBluetoothClient.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(PixBiBluetoothClient.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
        setState(STATE_CONNECTED);
    }
	public synchronized void stop() {
        Log.d(TAG, " ++ ConnectThread stop ++ ");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        
        if (bufferThread != null){
        	bufferThread = null;
        }
        
        setState(STATE_NONE);
	}
	
	/**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
	
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
    	Log.d(TAG, " ++ connection Failed ++ ");
    	
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(PixBiBluetoothClient.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(PixBiBluetoothClient.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
        // Start the service over to restart listening mode
        BluetoothService.this.start();
    }
    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
    	Log.d(TAG, " ++ connection Lost ++ ");
    	
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(PixBiBluetoothClient.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(PixBiBluetoothClient.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
        // Start the service over to restart listening mode
    	BluetoothService.this.start();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);             
            } catch (IOException e) {
                Log.e(TAG, "ERROR: Socket Type: create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "ERROR: unable to close() socket during connection failure", e2);
                }
                connectionFailed();
      
                Log.e(TAG, "ERROR: Unable to connect device");
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "ERROR: close() of connect socket failed", e);
            }
        }
    }
    
    private class BufferThread extends Thread{
    	private StringBuilder sb = null;
    	private Pattern pattern = null;
    	private Matcher matcher = null;
    	private String res;
    	private InputStream inStream;
    	private final String patternstr = "Temperature: %s C Relative Humidity: %s % Light Intensity: %s ";
		public BufferThread(){    
			sb = new StringBuilder();
			pattern = Pattern.compile("T: (\\d{1,3}.\\d{1}) RH: (\\d{1,3}.\\d{1}) LI: (\\d{1,3}.\\d{1}) \\$");

		}    	
    	public void run(){
            Log.i(TAG, "BEGIN mbufferThread");
            setName("bufferThread");
			matcher = pattern.matcher("T: 30.7 RH: 78.5 LI: 184 $T: 30.7 RH: 78.4 LI: 182 $T: 30.6 RH: 78.4 LI: 189 $T: 30.6 RH: 78.4 LI: 181 $T: 30.7 RH: 78.5 LI: 189 $T: 30.7 RH: 78.4 LI: 183 $");
			Log.i(TAG, "matches :"+ matcher.matches());
			Log.i(TAG, "groupcount :"+ matcher.groupCount());
			Log.i(TAG, "find :"+ matcher.find()+":"+matcher.group(1));
			Log.i(TAG, "find :"+ matcher.find()+":"+matcher.group(3));
			Log.i(TAG, "groupcount :"+ matcher.groupCount());
            byte[] buffer = new byte[1024];
    		String buf="";
    		int bytes;
    		
    		while(true){
    			  
					try {						
						while(( buf = bufferqueue.take()) != null){
							Log.i(TAG, "bufferqueue.take : "+ buf );
							sb.append(buf);
							Log.i(TAG, "sb : "+ sb );
							matcher = pattern.matcher(sb);	
							
							//Log.i(TAG,"matcher.matches(): "+ matcher.matches());
							if(matcher.find()){
								if(matcher.groupCount() == 3){
										
									//res=String.format(patternstr,matcher.group(1),matcher.group(2),matcher.group(3));
									res="Temperature: "+matcher.group(1)+" C Relative Humidity: "+matcher.group(2)+" % Light Intensity: "+matcher.group(3);
									
									inStream = new ByteArrayInputStream(res.getBytes()); 
									bytes = inStream.read(buffer);
									
									mHandler.obtainMessage(PixBiBluetoothClient.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
	
				                    String readMessage = new String(buffer,0,bytes);			                  			                 
				                    
				                    Log.i(TAG, "match read message: "+ readMessage );
				                    sb = new StringBuilder();
				                    
								}
							}	
						}
					} catch (InterruptedException e) {						
						Log.e(TAG, "Erro Interrupt Eception", e);
					}
					catch (IOException e) {
						Log.e(TAG, "Exception get result bytes", e);
					}
    		}
    	}
    	

    }
    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {           
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    
                    // Send the obtained bytes to the UI Activity
                    //mHandler.obtainMessage(PixBiBluetoothClient.MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                    String readMessage = new String(buffer,0,bytes);
                    
                    bufferqueue.add(readMessage);
                    
                    Log.i(TAG, "read message: "+ readMessage );
                    
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(PixBiBluetoothClient.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
                
                String writeMessage = new String(buffer);
                Log.i(TAG, "write message: "+ writeMessage );
                
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
