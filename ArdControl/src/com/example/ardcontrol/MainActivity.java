// TODO: Progress dialog for searching for BT devices
// Disconnect button
// Better UI for BT stuff
// Maybe remove the picture when the mode is manually set
// Maybe disable illegal drops
// Maybe add more modes (tone? Serial?) or items (temp sensor, tilt, RGB LED, motor, shift register)

package com.example.ardcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.example.ardcontrol.R;
 
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.view.DragEvent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
	EditText pinLabel[] = new EditText[numPins];
	Spinner pinMode[] = new Spinner[numPins];
	LinearLayout dropTarget[] = new LinearLayout[numPins];
		   
	// BT stuff
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothSocket btSocket = null;
	ListView btDeviceList;
	ArrayAdapter<String> btDeviceAdapter = null;
	ArrayList<String> btDeviceAddresses = null;
	BroadcastReceiver mReceiver = null;
	ProgressDialog progress = null;
	
	// available parts list
	// TODO insert parts here
    int[] partIds = {R.id.led, R.id.pot, R.id.button, R.id.spst, R.id.photo, R.id.servo};
    int[] sourceIds = {R.id.ledSource, R.id.potSource, R.id.buttonSource, R.id.spstSource, R.id.photoSource, R.id.servoSource};
    int[] partDrawables = {R.drawable.led_on, R.drawable.pot, R.drawable.button, R.drawable.spst, R.drawable.photo, R.drawable.servo};
    int[] partModes = {2, 3, 5, 1, 3, 4};
    String[] partLabels = {"LED", "Potentiometer", "Button", "Switch", "Photoresistor", "Servo"};
    
    // ids so we don't duplicate
    int[] pinIds = {R.id.D2pin, R.id.D3pin, R.id.D4pin, R.id.D5pin, R.id.D6pin, R.id.D7pin, R.id.D8pin, R.id.D9pin, R.id.D10pin, R.id.D11pin, R.id.D12pin, R.id.D13pin, R.id.A0pin, R.id.A1pin, R.id.A2pin, R.id.A3pin, R.id.A4pin, R.id.A5pin};
    int[] pinValIds = {R.id.D2pinVal, R.id.D3pinVal, R.id.D4pinVal, R.id.D5pinVal, R.id.D6pinVal, R.id.D7pinVal, R.id.D8pinVal, R.id.D9pinVal, R.id.D10pinVal, R.id.D11pinVal, R.id.D12pinVal, R.id.D13pinVal, R.id.A0pinVal, R.id.A1pinVal, R.id.A2pinVal, R.id.A3pinVal, R.id.A4pinVal, R.id.A5pinVal};
    int[] pinLabelIds = {R.id.D2pinLabel, R.id.D3pinLabel, R.id.D4pinLabel, R.id.D5pinLabel, R.id.D6pinLabel, R.id.D7pinLabel, R.id.D8pinLabel, R.id.D9pinLabel, R.id.D10pinLabel, R.id.D11pinLabel, R.id.D12pinLabel, R.id.D13pinLabel, R.id.A0pinLabel, R.id.A1pinLabel, R.id.A2pinLabel, R.id.A3pinLabel, R.id.A4pinLabel, R.id.A5pinLabel};
    int[] pinModeIds = {R.id.D2pinMode, R.id.D3pinMode, R.id.D4pinMode, R.id.D5pinMode, R.id.D6pinMode, R.id.D7pinMode, R.id.D8pinMode, R.id.D9pinMode, R.id.D10pinMode, R.id.D11pinMode, R.id.D12pinMode, R.id.D13pinMode, R.id.A0pinMode, R.id.A1pinMode, R.id.A2pinMode, R.id.A3pinMode, R.id.A4pinMode, R.id.A5pinMode};
    int[] dropTargetIds = {R.id.D2dropTarget, R.id.D3dropTarget, R.id.D4dropTarget, R.id.D5dropTarget, R.id.D6dropTarget, R.id.D7dropTarget, R.id.D8dropTarget, R.id.D9dropTarget, R.id.D10dropTarget, R.id.D11dropTarget, R.id.D12dropTarget, R.id.D13dropTarget, R.id.A0dropTarget, R.id.A1dropTarget, R.id.A2dropTarget, R.id.A3dropTarget, R.id.A4dropTarget, R.id.A5dropTarget};
    int[] pinContainerIds = {R.id.D2pinContainer, R.id.D3pinContainer, R.id.D4pinContainer, R.id.D5pinContainer, R.id.D6pinContainer, R.id.D7pinContainer, R.id.D8pinContainer, R.id.D9pinContainer, R.id.D10pinContainer, R.id.D11pinContainer, R.id.D12pinContainer, R.id.D13pinContainer, R.id.A0pinContainer, R.id.A1pinContainer, R.id.A2pinContainer, R.id.A3pinContainer, R.id.A4pinContainer, R.id.A5pinContainer};
    
	// input/output
	private InputStream inStream = null;
	private OutputStream outStream = null;

	// reading byte stuff
	byte delimiter = 10;
	int readBufferPosition = 0;
	byte[] readBuffer = new byte[1024];
		
	// debug
	private static final String TAG = "BT-Ard";
	
	// worker (thread) stuff
	boolean stopWorker = false;
	Handler handler = new Handler();
	
	// selected part (led, pot, etc)
	int selectedPart = -1;
	
	// for context problems
	Context MainActivity = this;
	
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
    	 
    	 String dropTargetIdString="vals: ";
    	 for (int i=0;i<dropTargetIds.length;i++) {
    		 dropTargetIdString += dropTargetIds[i] + " ";
    	 }
    	 Log.d("ids", dropTargetIdString);
    	 
    	 for (int i=0; i<numPins; i++) {
        	 
        	 inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	 pinControl = (LinearLayout) inflater.inflate(R.layout.pin_control, main);
        	 pinControl.setId(pinContainerIds[i]);
             pinNameTemp = (TextView)pinControl.findViewById(R.id.pin_name);
             pinNameTemp.setId(pinIds[i]);
             pinNameTemp.setText(pinName[i]);
             
             pinVal[i] = (EditText)pinControl.findViewById(R.id.val);
             pinVal[i].setId(pinValIds[i]);
             pinLabel[i] = (EditText)pinControl.findViewById(R.id.name);
             pinLabel[i].setId(pinLabelIds[i]);
             pinMode[i] = (Spinner)pinControl.findViewById(R.id.mode);
             pinMode[i].setId(pinModeIds[i]);
             dropTarget[i] = (LinearLayout)pinControl.findViewById(R.id.drop_target);
             dropTarget[i].setId(dropTargetIds[i]);
             
             // add the adapter to the Spinner
             // Create an ArrayAdapter using the string array and a default spinner layout
             if (i < 12) adapter = DigitalModes.createFromResource(this, R.array.mode_names, android.R.layout.simple_spinner_item);
             else adapter = AnalogModes.createFromResource(this, R.array.mode_names, android.R.layout.simple_spinner_item);

             // Specify the layout to use when the list of choices appears
             adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
             // Apply the adapter to the spinner
             pinMode[i].setAdapter(adapter);

             // add the drag-and-drop functionality
    	     Drawable enterShape = ContextCompat.getDrawable(this, R.drawable.target_hover);
    	     Drawable normalShape = ContextCompat.getDrawable(this, R.drawable.target);
    	     dropTarget[i].setOnDragListener(new TargetOnDragListener(this, enterShape, normalShape, pinMode[i], pinLabel[i]));
         }
         
         
         // get our GUI items
         connect = (Button) findViewById(R.id.connect);
         btDeviceList = (ListView) findViewById(R.id.btDeviceList);
         
         // set the click listeners for the buttons
         connect.setOnClickListener(this);
 
         // check if BT adapter is enabled and working
         checkBt();
         
	     // Assign the touch listener to your view which you want to move
         for (int i=0; i<partIds.length; i++) {
        	 findViewById(partIds[i]).setOnTouchListener(new OnPartTouchListener(this, partIds[i]));
         }
	     
	     Drawable normalShape = ContextCompat.getDrawable(this, R.drawable.target);
	     Drawable enterTrash = ContextCompat.getDrawable(this, R.drawable.trash_hover);
	     Drawable normalTrash = ContextCompat.getDrawable(this, R.drawable.trash);
	     
	     for (int i=0; i<sourceIds.length; i++) {
	    	 findViewById(sourceIds[i]).setOnDragListener(new TargetOnDragListener(this, normalShape, normalShape));
	     }
	     findViewById(R.id.trash).setOnDragListener(new TargetOnDragListener(this, enterTrash, normalTrash));
	     
	     btDeviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
	 	 btDeviceAddresses = new ArrayList<String>();
	 	// Register the BroadcastReceiver
	 	IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
	 	registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
         
    }
 
    // what to do if a view is clicked
	@Override
    public void onClick(View control) {
        // if Connect button is clicked, connect with BT
		if (control.getId() == R.id.connect) {
        	findDevices();
        }
    }
 
    // check if BT adapter is working
	private void checkBt() {
        // display an alert (Toast) if BT is disabled or null
    	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
 
        if (!mBluetoothAdapter.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, 0);
        }
        if (mBluetoothAdapter == null) {
        	Toast.makeText(getApplicationContext(), "Bluetooth adapter is not available!", Toast.LENGTH_LONG).show();
        }
    }
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == 0){
			if (mBluetoothAdapter.isEnabled()) {
				Toast.makeText(getApplicationContext(), "Bluetooth is enabled.", Toast.LENGTH_SHORT).show();
			}
			else {
				Toast.makeText(getApplicationContext(), "Bluetooth is disabled! Please enable Bluetooth in order to connect your Arduino.", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	public boolean connectDevice(String address) {
		boolean returnVal = false;
		
		// cancel BT discovery because we selected a device
		mBluetoothAdapter.cancelDiscovery();
		
		// close any existing BT connection
		if (btSocket != null) {
			try {
				btSocket.close();
			} catch (IOException e2) {
	            Log.d(TAG, "Unable to end the connection");
	        }
		}
				
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
			       
        try {
        	// create BT socket from our device and connect to it
        	btSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
            btSocket.connect();
                        
            returnVal = true;

        } catch (Exception e) {
            // if there's a problem, close the socket
        	try {
            	btSocket.close();
            } catch (IOException e2) {
                Log.d(TAG, "Unable to end the connection");
            }
            
            e.printStackTrace();
        }
       
        // start listening for data
        beginListenForData();
        
        return returnVal;
    }

       
    // try to find BT devices
	public void findDevices() {		
		// delete anything already found so we don't get duplicates
		btDeviceAdapter.clear();
		btDeviceAddresses.clear();
		
		// start discovering devices
		mBluetoothAdapter.startDiscovery();
		
		btDeviceList.setAdapter(btDeviceAdapter);
		btDeviceList.setOnItemClickListener(new OnItemClickListener()
		{		    
		    @Override 
		    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)
		    { 
		    	final int pos = position;
		    	
		    	
	    		new Thread(new Runnable(){

                    public void run(){
                    	runOnUiThread(new Runnable() {
        	    			public void run() {
        	    				progress = ProgressDialog.show(MainActivity.this, "Connecting", "Connecting to " + btDeviceList.getItemAtPosition(pos).toString(), true);
                	    		progress.setCancelable(true);
        	    			}
        	    		});
                	    		
        	    		final boolean success = connectDevice(btDeviceAddresses.get(pos));
                	    
        	    		runOnUiThread(new Runnable() {
        	    			public void run() {
        	    				progress.dismiss();
        	    				
        	    				if (success) {
		        	    			Toast.makeText(MainActivity, "Connected!", Toast.LENGTH_SHORT).show();	   		
		        	        		
		        	        		// hide list of devices
		        	        		btDeviceList.setVisibility(View.GONE);            	    		
		        	    		}
		        	    		else {
		        	    			Toast.makeText(MainActivity, "Connection failed!", Toast.LENGTH_LONG).show();	   		
		        	    		}
        	    			}
        	    		});
                    }

                }).start();
	    			
		    }
		});
		
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
		}
		
		mReceiver = new BroadcastReceiver() {
		    public void onReceive(Context context, Intent intent) {
		        String action = intent.getAction();
		        // When discovery finds a device
		        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
		            // Get the BluetoothDevice object from the Intent
		            BluetoothDevice newDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		            // Add the name and address to an array adapter to show in a ListView
		            btDeviceAdapter.add(newDevice.getName() + "\n" + newDevice.getAddress());
		            btDeviceAddresses.add(newDevice.getAddress());
		        }
		    }
		};
		
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
	 	registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
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
   
    	if (btSocket != null) {
    		// close BT socket
    		try {
        		btSocket.close();
            } catch (IOException e) {
            }
    	}
    	
    	// unregister BT receiver
    	if (mReceiver != null) {
			unregisterReceiver(mReceiver);
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
	Context context;
	
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
    	// disable analogRead (mode 3)
    	if (position == 3) return false;
    	return true;
    }
}

class AnalogModes extends ArrayAdapter<CharSequence> {

	static Resources resources;
	static CharSequence[] strings;
	Context context;
	
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

class TargetOnDragListener implements OnDragListener {
	 
    Drawable enterShape;
    Drawable normalShape;
    Spinner pinMode;
    EditText pinLabel;
    MainActivity main;
    
    public TargetOnDragListener(MainActivity main, Drawable enterShape, Drawable normalShape, Spinner pinMode, EditText pinLabel) {
    	super();
    	this.enterShape = enterShape;
    	this.normalShape = normalShape;
    	this.pinMode = pinMode;
    	this.pinLabel = pinLabel;
    	this.main = main;
    }
    
    public TargetOnDragListener(MainActivity main, Drawable enterShape, Drawable normalShape) {
    	super();
    	this.enterShape = enterShape;
    	this.normalShape = normalShape;
    	this.pinMode = null;
    	this.pinLabel = null;
    	this.main = main;
    }
    
    @Override
    public boolean onDrag(View v, DragEvent event) {
      switch (event.getAction()) {
      case DragEvent.ACTION_DRAG_STARTED:
      // do nothing
        break;
      case DragEvent.ACTION_DRAG_ENTERED:
        v.setBackground(enterShape);
        break;
      case DragEvent.ACTION_DRAG_EXITED:        
        v.setBackground(normalShape);
        break;
      case DragEvent.ACTION_DROP:   
    	  
          
	    View partIconView = (View)event.getLocalState();
	    ViewGroup owner = (ViewGroup) partIconView.getParent();
	    
	    if (!isSourceId(owner.getId())) {
	    	// find the pin from which it was dragged-- Arrays.asList(x).indexOf(y) doesn't work
	    	int ownerIndex = owner.getId();
	    	for (int i=0; i<main.dropTargetIds.length; i++) {
	    		if (main.dropTargetIds[i] == ownerIndex) {
	    			ownerIndex = i;
	    			break;
	    		}
	    	}
	    	
	    	// set the pin from which it was dragged to not having anything
	    	main.pinMode[ownerIndex].setSelection(0, true);
	      	main.pinLabel[ownerIndex].setText("");
        	owner.removeView(partIconView);
        }
          
        if (!isSourceId(v.getId()))  {

    	    // add new part to the dropped group
	    	ImageView partIcon = new ImageView(pinMode.getContext());
	    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
	        partIcon.setLayoutParams(params);
	        
	        Drawable partDrawable = null;
	        int mode = 0;
	        String label = "";
	        int partId = 0;
	        
	        for (int i=0; i<main.partIds.length; i++) {
	        	
	        	if (main.selectedPart == main.partIds[i]) {
		    		partDrawable = ContextCompat.getDrawable(pinMode.getContext(), main.partDrawables[i]);
		    		mode = main.partModes[i];
			        label = main.partLabels[i];
			        partId = main.partIds[i];
		    	}
	        }
	    	
	        partIcon.setImageDrawable(partDrawable);
	        partIcon.setPadding(5,  5,  5,  5);
	        partIcon.setOnTouchListener(new OnPartTouchListener(main, partId));
	          
	        // Dropped, reassign View to ViewGroup
	        LinearLayout container = (LinearLayout) v;
	        if (container.getChildCount() > 0) container.removeAllViews();
	        container.addView(partIcon);
	        partIcon.setVisibility(View.VISIBLE);
	    
	        // set the pin to having an LED
	        if (pinMode != null) {
	        	pinMode.setSelection(mode, true);
	        	pinLabel.setText(label);
	        }
        }

        break;
      case DragEvent.ACTION_DRAG_ENDED:
        v.setBackground(normalShape);
      default:
        break;
      }
      return true;
    }
	
	public boolean isSourceId(int id) {
		switch(id) {
			case R.id.ledSource:
			case R.id.potSource:
			case R.id.buttonSource:
			case R.id.spstSource:
			case R.id.photoSource:
			case R.id.servoSource:
			case R.id.trash:
				return true;
			default:
				return false;
		}
	}
}

class OnPartTouchListener implements OnTouchListener {
	MainActivity main;
	int partId;
	
	public OnPartTouchListener(MainActivity main, int partId) {
		super();
		this.main = main;
		this.partId = partId;
	}
	
	public boolean onTouch(View view, MotionEvent motionEvent) {
      if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
        ClipData data = ClipData.newPlainText("", "");
        DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
        view.startDrag(data, shadowBuilder, view, 0);
        main.selectedPart = partId;
        
      } 
      else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
     	 view.setVisibility(View.VISIBLE);
      }
      return true;
    }
}