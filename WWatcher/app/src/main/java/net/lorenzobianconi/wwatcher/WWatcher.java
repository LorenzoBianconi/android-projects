package net.lorenzobianconi.wwatcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.UUID;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.txusballesteros.widgets.FitChart;
import com.txusballesteros.widgets.FitChartValue;

public class WWatcher extends Activity {
	private final String WWATCHER_TAG = "WWATCHER_TAG";

	private class Sample {
		private float temp;
		private float rH;

		Sample(float temp, float rH) {
			this.temp = temp;
			this.rH = rH;
		}

		float getTemp() { return temp; }
		float getrH() { return rH; }
	}

	static final int RX_SAMPLE_MSG = 0;
	static final int BT_DISCONNECTED_MSG = 1;
	class WWHandler extends Handler {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case RX_SAMPLE_MSG:
					Sample samples = (Sample)msg.obj;
					float temp = samples.getTemp();
					float rH = samples.getrH();
					Log.d(WWATCHER_TAG, "temperature: " + temp + "C rH: " + rH + "%");
					Resources resources = getResources();
					Collection<FitChartValue> tempValues = new ArrayList<>();
					if (temp < 10)
						tempValues.add(new FitChartValue(temp + 30,
								resources.getColor(R.color.cold_temp)));
					else if (temp <= 25)
						tempValues.add(new FitChartValue(temp + 30,
								resources.getColor(R.color.good_temp)));
					else
						tempValues.add(new FitChartValue(temp + 30,
								resources.getColor(R.color.hot_temp)));
					tempChart.setValues(tempValues);
					currTemp.setText(resources.getString(R.string.temp) + ":\n" + temp + "C");
					Collection<FitChartValue> rHValues = new ArrayList<>();
					if (rH < 40)
						rHValues.add(new FitChartValue(rH, resources.getColor(R.color.low_rh)));
					else if (temp <= 55)
						rHValues.add(new FitChartValue(rH, resources.getColor(R.color.good_rh)));
					else
						rHValues.add(new FitChartValue(rH, resources.getColor(R.color.high_rh)));
					rhChart.setValues(rHValues);
					currRH.setText(resources.getString(R.string.rh) + ":\n"  + rH + "%");
					break;
				case BT_DISCONNECTED_MSG:
					linkInfo.setText("Device not connected");
					configureButton(true, false);
					break;
				default:
					super.handleMessage(msg);
			}
		}
	}

	class BtReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				switch (state) {
					case BluetoothAdapter.STATE_OFF:
						btEnabled = false;
						configureButton(false, false);
						break;
					case BluetoothAdapter.STATE_ON:
						btEnabled = true;
						configureButton(true, false);
						break;
					default:
						break;
				}
			} else if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				btScanProgressDialog.dismiss();
				Intent pickIntent = new Intent(WWatcher.this, BTScanList.class);
				pickIntent.putExtra("dev_list", getWWDeviceList());
				startActivityForResult(pickIntent, REQ_PICK_DEV);
			} else if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
				/* Found new BT deivce */
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.d(WWATCHER_TAG, "Found device " + device.getName() +
						" [" + device.getAddress() + "]");
				inqMap.put(device.getAddress(), device);
			}
		}
	}

	private class BtConnect extends AsyncTask<BluetoothDevice, Void, BluetoothSocket> {
		protected BluetoothSocket doInBackground(BluetoothDevice... device) {
			BluetoothSocket sock = null;
			try {
				btRadio.cancelDiscovery();
				sock = device[0].createRfcommSocketToServiceRecord(SPP_UUID);
				sock.connect();
				Log.d(WWATCHER_TAG, "Connected to " + device[0].getName() +
						" [" + device[0].getAddress() + "]");
			} catch (IOException e) {
				try {
					Log.d(WWATCHER_TAG, "Failed to connect to " + device[0].getName() +
							" [" + device[0].getAddress() + "]");
					sock.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			return sock;
		}

		protected void onPostExecute(BluetoothSocket sock) {
			btSock = sock;
			if (btSock == null) {
				linkInfo.setText("Device not connected");
			} else {
				linkInfo.setText("Connected to " + sock.getRemoteDevice().getName());
				configureButton(false, true);
				new SampleThread(handler).start();
			}
		}
	}

	private class SampleThread extends Thread {
		private final static String SAMPLING_TEMP = "t";
		private final static String SAMPLING_RH = "h";
		private OutputStream os;
		private InputStream is;
		private WWHandler handler;

		public SampleThread(WWHandler handler) {
			this.handler = handler;

			try {
				os = btSock.getOutputStream();
				is = btSock.getInputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(is));

				while (btSock.isConnected() == true) {
					/* get temperature info */
					os.write(SAMPLING_TEMP.getBytes());
					sleep(500);

					float temp = Float.parseFloat(br.readLine());

					/* get relative humidity info */
					os.write(SAMPLING_RH.getBytes());
					sleep(500);

					float rH = Float.parseFloat(br.readLine());

					/* send data to UI thread */
					Message msg = Message.obtain(handler, RX_SAMPLE_MSG, new Sample(temp, rH));
					msg.sendToTarget();

					sleep(SAMPLING_TO);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				Message msg = Message.obtain(handler, BT_DISCONNECTED_MSG);
				msg.sendToTarget();
			}
		}
	}

	private final static int REQ_ENABLE_BT = 1;
	private final static int REQ_PICK_DEV = 2;

	private final static float TEMP_MIN_VALUE = 0f;
	private final static float TEMP_MAX_VALUE = 80f;

	private final static float RH_MIN_VALUE = 0f;
	private final static float RH_MAX_VALUE = 100f;

	private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final long SAMPLING_TO = 30000;
	private boolean btEnabled;
	private BluetoothAdapter btRadio;
	private BtReceiver btReceiver;
	private ProgressDialog btScanProgressDialog;
	private Hashtable<String, BluetoothDevice> inqMap;
	private BluetoothSocket btSock;

	private WWHandler handler;

	private Button disconButton;
	private Button scanButton;
	private TextView linkInfo;
	private TextView currTemp;
	private TextView currRH;
	private FitChart tempChart;
	private FitChart rhChart;
	private ViewFlipper sampleFlipper;
	private float lastX;


	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wwatcher);

		linkInfo = (TextView)findViewById(R.id.link_info);
		currTemp = (TextView)findViewById(R.id.temp_tv);
		currRH = (TextView)findViewById(R.id.rh_tv);
		tempChart = (FitChart)findViewById(R.id.temp_view);
		tempChart.setMinValue(TEMP_MIN_VALUE);
		tempChart.setMaxValue(TEMP_MAX_VALUE);
		rhChart = (FitChart)findViewById(R.id.rh_view);
		rhChart.setMinValue(RH_MIN_VALUE);
		rhChart.setMaxValue(RH_MAX_VALUE);
		sampleFlipper = (ViewFlipper)findViewById(R.id.sample_flipper);
		disconButton = (Button)findViewById(R.id.bt_disconnect_button);
		disconButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					linkInfo.setText("Device not connected");
					configureButton(true, false);
					btSock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		scanButton = (Button)findViewById(R.id.bt_scan_button);
		scanButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (btEnabled == true) {
					btScanProgressDialog.setMessage(WWatcher.this.getString(R.string.bt_start_scan));
					btScanProgressDialog.setCancelable(false);
					btScanProgressDialog.show();

					inqMap.clear();
					btRadio.startDiscovery();
				} else {
					Toast.makeText(WWatcher.this, R.string.bt_disabled, Toast.LENGTH_LONG).show();
				}
			}
		});

		inqMap = new Hashtable<String, BluetoothDevice>();
		btScanProgressDialog = new ProgressDialog(this);
		btReceiver = new BtReceiver();

		handler = new WWHandler();

		btRadio = BluetoothAdapter.getDefaultAdapter();
		if (btRadio == null) {
			/* BT stack not supported */
			Toast.makeText(this, R.string.bt_not_available, Toast.LENGTH_LONG).show();
			finish();
		} else if (btRadio.isEnabled() == false) {
			/* Request to enable BT */
			btEnabled = false;
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(intent, REQ_ENABLE_BT);
		} else {
			btEnabled = true;
			configureButton(true, false);
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.wwatcher, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public boolean onTouchEvent(MotionEvent touchevent) {
		switch (touchevent.getAction()) {
			case MotionEvent.ACTION_DOWN:
				lastX = touchevent.getX();
				break;
			case MotionEvent.ACTION_UP:
				float currentX = touchevent.getX();
				if (lastX < currentX) {
					/* Handling left to right screen swap */
					if (sampleFlipper.getDisplayedChild() == 0)
						break;
					sampleFlipper.setInAnimation(this, R.anim.slide_in_from_left);
					sampleFlipper.setOutAnimation(this, R.anim.slide_out_to_right);
					sampleFlipper.showNext();
				} else {
					/* Handling right to left screen swap */
					if (sampleFlipper.getDisplayedChild() == 1)
						break;

					sampleFlipper.setInAnimation(this, R.anim.slide_in_from_right);
					sampleFlipper.setOutAnimation(this, R.anim.slide_out_to_left);
					sampleFlipper.showPrevious();
				}
				break;
		}
		return false;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQ_ENABLE_BT:
				btEnabled = (resultCode == RESULT_OK);
				configureButton(btEnabled, false);
				break;
			case REQ_PICK_DEV:
				if (data != null) {
					String addr = data.getExtras().getString("device");
					BluetoothDevice device = inqMap.get(addr);
					new BtConnect().execute(device);
				}
				break;
			default:
				break;
		}
	}

	protected void onResume() {
		super.onResume();
		registerReceiver(btReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
		registerReceiver(btReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		registerReceiver(btReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
	}

	protected void onPause() {
		super.onPause();
		unregisterReceiver(btReceiver);
	}

	private void configureButton(boolean scan, boolean disconnect) {
		scanButton.setClickable(scan);
		disconButton.setClickable(disconnect);
	}

	private ArrayList<WWDevice> getWWDeviceList() {
		ArrayList<WWDevice> wwList = new ArrayList<WWDevice>();
		for (BluetoothDevice device : inqMap.values())
			wwList.add(new WWDevice(device.getName(), device.getAddress()));
		return wwList;
	}
}
