package com.example.ardmonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.example.ardmonitor.R;
 
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;
 
public class MainActivity extends Activity implements OnClickListener {
 
	// GUI elements
	Button connect;
	String pinName[] = {"D2","D3","D4","D5","D6","D7","D8","D9","D10","D11","D12","D13","A0","A1","A2","A3","A4","A5"};
	int numPins = 18;
	EditText pinVal[] = new EditText[numPins];
	Spinner pinMode[] = new Spinner[numPins];
		   
	// BT stuff
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothSocket btSocket = null;
	private static String address = "98:D3:31:60:14:85";
	
	// input/output
	private InputStream inStream = null;
	private OutputStream outStream = null;

	// reading byte stuff
	byte delimiter = 10;
	int readBufferPosition = 0;
	byte[] readBuffer = new byte[1024];
		
	// debug
	private static final String TAG = "BT-Ard";
	
	// is it already connected?
	boolean connected = false;
	
	// worker (thread) stuff
	boolean stopWorker = false;
	Handler handler = new Handler();
		
	// UI handler stuff so we don't tie up the main thread
	Handler UIHandler = new Handler(Looper.getMainLooper()) {
		// what the handler should do when we send it a message
        @Override
        public void handleMessage(Message inputMessage) {
            // get the object sent in the message and cast it to a String (because we are sending a string)
            String data = (String) inputMessage.obj; 
            String[] indData = data.split("\\s");
            if (indData != null && indData.length >= 2) {
            	int pinIndex = java.util.Arrays.asList(pinName).indexOf(indData[0].trim());
            	if (pinIndex >= 0) {
            		int modeNum = pinMode[pinIndex].getSelectedItemPosition();
            		if (modeNum % 2 == 1) pinVal[pinIndex].setText(indData[1]);
	            	
	    	        if (pinMode[pinIndex].getSelectedItemPosition() == 0) {
	    	        	pinVal[pinIndex].setBackgroundColor(getApplicationContext().getResources().getColor(R.color.gray));
	    	        }
	    	        else {
	    	        	pinVal[pinIndex].setBackgroundColor(getApplicationContext().getResources().getColor(R.color.white));	        	
	    	        }
	            	
	            	// send a reply
	            	sendData(pinIndex);
	            	
            	}
            }
        }
	};
 
    // on app creation, do this...
	@Override
    protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         setContentView(R.layout.activity_main);
         
         ViewGroup main = (ViewGroup)findViewById(R.id.main);
         LayoutInflater inflater;
         LinearLayout pinControl;
         TextView pinNameTemp;
         ArrayAdapter<CharSequence> adapter;
         
         for (int i=0; i<numPins; i++) {
        	 inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	 pinControl = (LinearLayout) inflater.inflate(R.layout.pin_control, main);
             pinNameTemp = (TextView)pinControl.findViewById(R.id.pin_name);
             pinNameTemp.setId(i);
             pinNameTemp.setText(pinName[i]);
             pinVal[i] = (EditText)pinControl.findViewById(R.id.val);
             pinVal[i].setId(i);
             pinMode[i] = (Spinner)pinControl.findViewById(R.id.mode);
             pinMode[i].setId(i);
             
             // add the adapter to the Spinner
             // Create an ArrayAdapter using the string array and a default spinner layout
             if (i < 12) adapter = DigitalModes.createFromResource(this, R.array.mode_names, android.R.layout.simple_spinner_item);
             else adapter = AnalogModes.createFromResource(this, R.array.mode_names, android.R.layout.simple_spinner_item);

             // Specify the layout to use when the list of choices appears
             adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
             // Apply the adapter to the spinner
             pinMode[i].setAdapter(adapter);
         }
         
         
         // get our GUI items
         connect = (Button) findViewById(R.id.connect);
         
         // set the click listeners for the buttons
         connect.setOnClickListener(this);
 
         // check if BT adapter is enabled and working
         checkBt();
         
         // don't know what this does (logs something, but what?)
         BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
         Log.e("BT", device.toString());
    }
 
    // what to do if a view is clicked
	@Override
    public void onClick(View control) {
        // if Connect button is clicked, connect with BT
		if (control.getId() == R.id.connect) {
        	connect();
        }
    }
 
    // check if BT adapter is working
	private void checkBt() {
        // display an alert (Toast) if BT is disabled or null
    	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
 
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth is disabled!", Toast.LENGTH_SHORT).show();
        }
        if (mBluetoothAdapter == null) {
        	Toast.makeText(getApplicationContext(), "Bluetooth adapter is not available!", Toast.LENGTH_SHORT).show();
        }
    }
       
    // try to connect with BT
	public void connect() {		
		// if not connected, connect
		if (!connected) {
	        // connect
			BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
	        
	        // not sure what this does
	        mBluetoothAdapter.cancelDiscovery();
	       
	        try {
	        	// create BT socket from our device and connect to it
	        	btSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
	            btSocket.connect();
	            Log.d(TAG, "Connection made.");
	            
	            // set button so clicking it will now disconnect/exit
	            connect.setText(R.string.disconnect_label);
	            connected = true;
	        } catch (Exception e) {
	            // if there's a problem, close the socket
	        	try {
	            	btSocket.close();
	            } catch (IOException e2) {
	                Log.d(TAG, "Unable to end the connection");
	            }
	            Log.d(TAG, "Socket creation failed");
	            e.printStackTrace();
	        }
	       
	        // start listening for data
	        beginListenForData();        
    	}
    	else {
    		connect.setText(R.string.connect_label);
            connected = false;
            
            // already connected, so disconnect
    		try {
    			btSocket.close();
	        } catch (IOException e2) {
	            Log.d(TAG, "Unable to end the connection");
	        }

    		btSocket = null;

    	}
    }
       
    // send data to Arduino
	private void sendData(int currPin) {
        // get output stream
		try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Log.d(TAG, "Bug BEFORE Sending stuff", e);
        }
 
		// send (for each pin): name, mode, value (which is 0 if mode is output)
		// modes: 0 = off, 1 = digital input, 2 = digital output, 3 = analog input, 4 = analog output, 5 = input w/pullup
		// we only send half of the pins, beginning with beginPinIndex

		String message = pinName[currPin] + " " + pinMode[currPin].getSelectedItemPosition() + " " + pinVal[currPin].getText().toString() + "\n";

		// convert message to byte buffer for sending, then write to output stream
		byte[] msgBuffer = message.getBytes();
        try {
        	outStream.write(msgBuffer);
        } catch (IOException e) {
        	Log.d(TAG, "Bug while sending stuff", e);
        }
    }
 
    // do this when closed...
	@Override
    protected void onDestroy() {
    	super.onDestroy();
   
    	// close BT socket
    	try {
    		btSocket.close();
        } catch (IOException e) {
        }
    }
       
    // listen for data coming from Arduino
	public void beginListenForData()   {
    	// get input stream
		try {
    		inStream = btSocket.getInputStream();
    		// skip what's already there in case there's a pileup
    		inStream.skip(inStream.available());
    	} catch (IOException e) {
    	}
             
    	// make a new thread to process the stuff we get
		Thread workerThread = new Thread(new Runnable()
		{
    		public void run() {                
               // while the thread is going, get data
    			while(!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        // see how much is available
                    	int bytesAvailable = inStream.available();      
                        
                        // if there's anything to read...
                    	if(bytesAvailable > 0) {
                            // read it
                    		byte[] packetBytes = new byte[bytesAvailable];
                            inStream.read(packetBytes);
                            
                            // put each byte back into the String format
                            for(int i=0;i<bytesAvailable;i++) {
                                byte b = packetBytes[i];
                                if(b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    /*handler.post(new Runnable()
                                    {
                                       	public void run()
                                       	{*/
                                        	// send data to UI handler using a message
                                    		Message completeMessage = UIHandler.obtainMessage(0, data);
                                            completeMessage.sendToTarget();
                                        /*}
                                    });*/
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
  		});
 
        workerThread.start();
   	}
}

class DigitalModes extends ArrayAdapter<CharSequence> {

	static Resources resources;
	static CharSequence[] strings;
	static Context context;
	
	public DigitalModes(Context context, int textViewResId, CharSequence[] strings) {
        super(context, textViewResId, strings);
        this.context = context;
    }

    public static DigitalModes createFromResource(Context context, int textArrayResId, int textViewResId) {

        resources = context.getResources();
        strings = resources.getTextArray(textArrayResId);
        CharSequence[] strings = resources.getTextArray(textArrayResId);

        return new DigitalModes(context, textViewResId, strings);
    }

    public boolean areAllItemsEnabled() {
        return false;
    }
    
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent){
    	 View v = convertView;
         if (v == null) {
            Context mContext = this.getContext();
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.row, null);
         }

         TextView tv = (TextView) v.findViewById(R.id.spinnerTarget);
         tv.setText(strings[position]);
         
         if (!isEnabled(position)) tv.setTextColor(context.getResources().getColor(R.color.gray));
         else tv.setTextColor(context.getResources().getColor(R.color.black));
         
         return v;
    }  

    public boolean isEnabled(int position) {
        // return false if position == position you want to disable
    	// disable analogRead (mode 3)
    	if (position == 3) return false;
    	return true;
    }
}

class AnalogModes extends ArrayAdapter<CharSequence> {

	static Resources resources;
	static CharSequence[] strings;
	static Context context;
	
	public AnalogModes(
            Context context, int textViewResId, CharSequence[] strings) {
        super(context, textViewResId, strings);
        this.context = context;
	}

    public static AnalogModes createFromResource(Context context, int textArrayResId, int textViewResId) {

        resources = context.getResources();
        strings = resources.getTextArray(textArrayResId);

        return new AnalogModes(context, textViewResId, strings);
    }

    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent){
    	 View v = convertView;
         if (v == null) {
            Context mContext = this.getContext();
            LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.row, null);
         }

         TextView tv = (TextView) v.findViewById(R.id.spinnerTarget);
         tv.setText(strings[position]);
         
         if (!isEnabled(position)) tv.setTextColor(context.getResources().getColor(R.color.gray));
         else tv.setTextColor(context.getResources().getColor(R.color.black));
         
         return v;
    }              
    
    public boolean isEnabled(int position) {
        // return false if position == position you want to disable
    	if (position == 1 || position == 2 || position == 5) return false;
    	return true;
    }
}