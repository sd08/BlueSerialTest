/*
 * Released under MIT License http://opensource.org/licenses/MIT
 * Copyright (c) 2013 Plasty Grove
 * Refer to file LICENSE or URL above for full text 
 */

package com.blueserial;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.blueserial.R;

import android.app.Activity;
import android.app.Notification;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.MediaStore;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;
import android.app.Activity;  
import android.telephony.TelephonyManager;
import com.android.internal.telephony.*;

public class MainActivity extends Activity {

	private static final String TAG = "BlueTest5-MainActivity";
	private int mMaxChars = 50000;//Default
	private UUID mDeviceUUID;
	private BluetoothSocket mBTSocket;
	private ReadInput mReadThread = null;
	private String callingNumber=null;
	private boolean isRinging=false;
	
	private boolean mIsUserInitiatedDisconnect = false;
	
	// All controls here
	private TextView mTxtReceive;
	//private EditText mEditSend;
	private Button mBtnDisconnect;
	//private Button mBtnSend;
	/*private Button mBtnClear;
	private Button mBtnClearInput;
	private ScrollView scrollView;
	private CheckBox chkScroll;
	private CheckBox chkReceiveText;*/

	private boolean mIsBluetoothConnected = false;

	private BluetoothDevice mDevice;

	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ActivityHelper.initialize(this);

		Intent intent = getIntent();
		Bundle b = intent.getExtras();
		mDevice = b.getParcelable(Homescreen.DEVICE_EXTRA);
		mDeviceUUID = UUID.fromString(b.getString(Homescreen.DEVICE_UUID));
		mMaxChars = b.getInt(Homescreen.BUFFER_SIZE);

		Log.d(TAG, "Ready");

		mBtnDisconnect = (Button) findViewById(R.id.btnDisconnect);
		mTxtReceive = (TextView) findViewById(R.id.txtReceive);
		
		PhoneCallListener phoneListener = new PhoneCallListener();
		TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(phoneListener,PhoneStateListener.LISTEN_CALL_STATE);

		mBtnDisconnect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mIsUserInitiatedDisconnect = true;
				new DisConnectBT().execute();
			}
		});

	}

	private class ReadInput implements Runnable {

		private boolean bStop = false;
		private Thread t;

		public ReadInput() {
			t = new Thread(this, "Input Thread");
			t.start();
		}

		public boolean isRunning() {
			return t.isAlive();
		}

		@Override
		public void run() {
			InputStream inputStream;

			try {
				inputStream = mBTSocket.getInputStream();
				while (!bStop) {
					byte[] buffer = new byte[256];
					if (inputStream.available() > 0) {
						inputStream.read(buffer);
						int i = 0;
						/*
						 * This is needed because new String(buffer) is taking the entire buffer i.e. 256 chars on Android 2.3.4 http://stackoverflow.com/a/8843462/1287554
						 */
						for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
						}
						final String strInput = new String(buffer, 0, i);
						Log.d(TAG, "Received string "+strInput);
						
						switch(strInput.charAt(0))
						{
						case 'A':
							Intent callIntent = new Intent(Intent.ACTION_CALL);
							callIntent.setData(Uri.parse("tel:2489796579"));
							startActivity(callIntent);
							break;
						case 'B':
							//String uri1 = "tel:" + CallLog.Calls.getLastOutgoingCall(getApplicationContext()).trim();
							String uri1 = "tel:" + CallLog.Calls.getLastOutgoingCall(getApplicationContext());
                            Intent intent1 = new Intent(Intent.ACTION_CALL);
                            intent1.setData(Uri.parse(uri1));
                            startActivity(intent1);
                            break;
						case 'C':
							Intent emgIntent = new Intent(Intent.ACTION_CALL);
							emgIntent.setData(Uri.parse("tel:4049845615"));
							startActivity(emgIntent);
							break;
						case 'D':
							Intent intent = new Intent("android.intent.action.MUSIC_PLAYER");
							startActivity(intent);
							break;
						case 'E':
							try{
							    Context context = getApplicationContext();
								TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
							    Class c = Class.forName(tm.getClass().getName());
							    Method m = c.getDeclaredMethod("getITelephony");
							    m.setAccessible(true);
							    com.android.internal.telephony.ITelephony telephonyService = (ITelephony) m.invoke(tm);
							    telephonyService.endCall();
							}catch (Exception e) {
							    e.printStackTrace();
							}
						}
						
						/*
						 * If checked then receive text, better design would probably be to stop thread if unchecked and free resources, but this is a quick fix
						 */

						/*if (chkReceiveText.isChecked()) {
							mTxtReceive.post(new Runnable() {
								@Override
								public void run() {
									mTxtReceive.append(strInput);
									//Uncomment below for testing
									//mTxtReceive.append("\n");
									//mTxtReceive.append("Chars: " + strInput.length() + " Lines: " + mTxtReceive.getLineCount() + "\n");
									
									int txtLength = mTxtReceive.getEditableText().length();  
									if(txtLength > mMaxChars){
										mTxtReceive.getEditableText().delete(0, txtLength - mMaxChars);
									}

									if (chkScroll.isChecked()) { // Scroll only if this is checked
										scrollView.post(new Runnable() { // Snippet from http://stackoverflow.com/a/4612082/1287554
													@Override
													public void run() {
														scrollView.fullScroll(View.FOCUS_DOWN);
													}
												});
									}
								}
							});
						}*/

					}
					
					Thread.sleep(500);
					if(mIsBluetoothConnected==false) stop();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public void stop() {
			bStop = true;
		}

	}
	

	private class DisConnectBT extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Void doInBackground(Void... params) {

			if (mReadThread != null) {
				mReadThread.stop();
				while (mReadThread.isRunning())
					; // Wait until it stops
				mReadThread = null;

			}

			try {
				mBTSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mIsBluetoothConnected = false;
			if (mIsUserInitiatedDisconnect) {
				finish();
			}
		}

	}

	private void msg(String s) {
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onResume() {
		if (mBTSocket == null || !mIsBluetoothConnected) {
			new ConnectBT().execute();
		}
		Log.d(TAG, "Resumed");
		super.onResume();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "Stopped");
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	private class ConnectBT extends AsyncTask<Void, Void, Void> {
		private boolean mConnectSuccessful = true;

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(MainActivity.this, "Hold on", "Connecting");// http://stackoverflow.com/a/11130220/1287554
		}

		@Override
		protected Void doInBackground(Void... devices) {

			try {
				if (mBTSocket == null || !mIsBluetoothConnected) {
					mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
					mBTSocket.connect();
				}
			} catch (IOException e) {
				// Unable to connect to device
				e.printStackTrace();
				mConnectSuccessful = false;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			if (!mConnectSuccessful) {
				Toast.makeText(getApplicationContext(), "Could not connect to device. Is it a Serial device? Also check if the UUID is correct in the settings", Toast.LENGTH_LONG).show();
				finish();
			} else {
				msg("Connected to device");
				mIsBluetoothConnected = true;
				mReadThread = new ReadInput(); // Kick off input reader
			}

			progressDialog.dismiss();
		}

	}
	
	//monitor phone call activities
		private class PhoneCallListener extends PhoneStateListener {
	 
			private boolean isPhoneCalling = false;
	 
			String LOG_TAG = "LOGGING 123";
	 
			@Override
			public void onCallStateChanged(int state, String incomingNumber) {
	 
				if (TelephonyManager.CALL_STATE_RINGING == state) {
					// phone ringing
					Log.i(LOG_TAG, "RINGING, number: " + incomingNumber);
					callingNumber=incomingNumber;
					isRinging=true;
				}
	 
				if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
					// active
					Log.i(LOG_TAG, "OFFHOOK");
	 
					isPhoneCalling = true;
				}
	 
				if (TelephonyManager.CALL_STATE_IDLE == state) {
					// run when class initial and phone call ended, 
					// need detect flag from CALL_STATE_OFFHOOK
					Log.i(LOG_TAG, "IDLE");
	 
					if (isPhoneCalling) {
	 
						Log.i(LOG_TAG, "restart app");
	 
						// restart app
						Intent i = getBaseContext().getPackageManager()
							.getLaunchIntentForPackage(
								getBaseContext().getPackageName());
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(i);
	 
						isPhoneCalling = false;
					}
	 
				}
			}
		}
}
