package net.lorenzobianconi.irrigationcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Intent;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

public class ICActivity extends AppCompatActivity {
    private final String ICONTROLLER_TAG = "ICONTROLLER_TAG";

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private Button mDisconButton;
    private Button mScanButton;
    private TextView mStatusText;

    private LinearLayout mProgressBarLayout;

    private final static int REQ_ENABLE_BT = 1;
    private final static int REQ_PICK_BT_DEV = 3;
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private ArrayList<BluetoothDevice> mScanList = new ArrayList<>();
    private RFChannelTread mRFChannel;
    private BluetoothSocket mSocket;
    private BluetoothAdapter mRadio;

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                /* Found new BT device */
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(ICONTROLLER_TAG, "Found Bluetooth device " + device.getName() +
                        " [" + device.getAddress() + "]");
                if (mScanList.contains(device) == false)
                    mScanList.add(device);
            } else if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Log.d(ICONTROLLER_TAG, "Bluetooth scanning terminated");
                mProgressBarLayout.setVisibility(View.GONE);
                mViewPager.setVisibility(View.VISIBLE);
                Intent pickIntent = new Intent(ICActivity.this, ICBluetoothDeviceList.class);
                pickIntent.putExtra("DevList", ICDevice.getICDeviceList(mScanList));
                startActivityForResult(pickIntent, REQ_PICK_BT_DEV);
                mScanButton.setClickable(true);
            } else if (action.equals(ACTION_IC_BT_RESET)) {
                try {
                    Log.d(ICONTROLLER_TAG, "Bluetooth link reset");
                    mDisconButton.setClickable(false);
                    mStatusText.setText("Not connected");
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private class ConnectTask extends AsyncTask<BluetoothDevice, Void, BluetoothSocket> {
        protected BluetoothSocket doInBackground(BluetoothDevice... device) {
            BluetoothSocket sock = null;

            try {
                mRadio.cancelDiscovery();
                sock = device[0].createRfcommSocketToServiceRecord(SPP_UUID);
                sock.connect();
                Log.d(ICONTROLLER_TAG, "Connected to " + device[0].getName() +
                        " [" + device[0].getAddress() + "]");
            } catch (IOException e) {
                try {
                    Log.d(ICONTROLLER_TAG, "Failed to connect to " + device[0].getName() +
                            " [" + device[0].getAddress() + "]");
                    sock.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return sock;
        }

        protected void onPostExecute(BluetoothSocket sock) {
            mSocket = sock;
            if (mScanButton != null) {
                mStatusText.setText("Connected");
                mDisconButton.setClickable(true);
                mRFChannel = new RFChannelTread();
                mRFChannel.start();
                mRFChannel.write("GET");
            }
        }
    }

    private class RFChannelTread extends Thread {
        public void run() {
            try {
                BufferedReader bReader = new BufferedReader(
                        new InputStreamReader(mSocket.getInputStream()));
                while (true) {
                    String buffer = bReader.readLine();
                    /* send data to UI thread */
                    Intent intent = new Intent(ACTION_IC_RX_CONF);
                    intent.putExtra("RX_CONF", buffer);
                    sendBroadcast(intent);

                    sleep(500);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                Intent intent = new Intent(ACTION_IC_BT_RESET);
                sendBroadcast(intent);
            }
        }

        public void write(String data) {
            try {
                OutputStream mOutStream = mSocket.getOutputStream();
                mOutStream.write(data.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void txMessage(String conf) {
        if (mRFChannel != null && mSocket.isConnected())
            mRFChannel.write(conf);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ic);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mProgressBarLayout = (LinearLayout)findViewById(R.id.progress_bar_layout);
        mProgressBarLayout.setVisibility(View.GONE);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mDisconButton = (Button)findViewById(R.id.disconnect);
        mDisconButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDisconButton.setClickable(false);
                mStatusText.setText("Not connected");
                mRFChannel.close();
            }
        });
        mDisconButton.setClickable(false);

        mStatusText = (TextView)findViewById(R.id.statusTextView);

        mScanButton = (Button)findViewById(R.id.scanning);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(ICONTROLLER_TAG, "Start Bluetooth scanning ");
                mProgressBarLayout.setVisibility(View.VISIBLE);
                mViewPager.setVisibility(View.GONE);
                mScanList.clear();
                mScanButton.setClickable(false);
                mRadio.startDiscovery();
            }
        });
        mScanButton.setClickable(false);

        mRadio = BluetoothAdapter.getDefaultAdapter();
        if (mRadio == null) {
			/* BT stack not supported */
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
            finish();
        } else if (mRadio.isEnabled() == false) {
			/* Request to enable BT */
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQ_ENABLE_BT);
        } else {
            if (checkLocationPermission() == true)
                mScanButton.setClickable(true);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ic, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_ENABLE_BT:
                if (requestCode == RESULT_OK && checkLocationPermission() == true)
                    mScanButton.setClickable(true);
                break;
            case REQ_PICK_BT_DEV:
                if (data != null) {
                    int index = data.getExtras().getInt("position");
                    new ConnectTask().execute(mScanList.get(index));
                }
                break;
            default:
                break;
        }
    }

    static final String ACTION_IC_BT_RESET = "BT_RESET";

    protected void onResume() {
        super.onResume();
        registerReceiver(mBluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(mBluetoothReceiver, new IntentFilter(ACTION_IC_BT_RESET));
        registerReceiver(mBluetoothReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBluetoothReceiver);
    }

    private final static int REQ_CODE_LOC = 2;

    private boolean checkLocationPermission() {
        /* request discovery permissions if necessary */
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{ android.Manifest.permission.ACCESS_COARSE_LOCATION },
                    REQ_CODE_LOC);
            return false;
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] results) {
        switch (requestCode) {
            case REQ_CODE_LOC:
                if (results.length > 0) {
                    for (int r : results) {
                        if (r != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "Location permission not granted",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    mScanButton.setClickable(true);
                }
                break;
            default:
                return;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class RelayChannelFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public class UIRelay {
            private class UIRelayTimeSlot {
                private Switch mEnabled;
                private EditText mStart;
                private EditText mStop;

                public UIRelayTimeSlot(Switch enabled, EditText start, EditText stop) {
                    mEnabled = enabled;
                    mStart = start;
                    mStop = stop;
                }

                public void setEnable(boolean enable) {
                    mEnabled.setChecked(enable);
                    mStart.setEnabled(enable);
                    mStop.setEnabled(enable);
                }

                public void update(String start, String stop, boolean enable) {
                    mEnabled.setChecked(enable);
                    mStart.setText(start);
                    mStop.setText(stop);
                }
            }

            private ArrayList<UIRelayTimeSlot> mTimeSlotList;
            private Switch mSwitch;

            public UIRelay(View view, final int index) {
                mSwitch = (Switch) view.findViewById(R.id.switchEnable);
                mSwitch.setText("Enable channel " + index);
                mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
                        if (enabled == true) {
                            mActivity.txMessage("");
                        }
                        for (UIRelayTimeSlot phase: mTimeSlotList) {
                            phase.setEnable(!enabled);
                        }
                    }
                });

                mTimeSlotList = new ArrayList<UIRelayTimeSlot>();
                mTimeSlotList.add(new UIRelayTimeSlot((Switch) view.findViewById(R.id.p1Enable),
                        (EditText) view.findViewById(R.id.p1StartTime),
                        (EditText) view.findViewById(R.id.p1StopTime)));
                mTimeSlotList.add(new UIRelayTimeSlot((Switch) view.findViewById(R.id.p2Enable),
                        (EditText) view.findViewById(R.id.p2StartTime),
                        (EditText) view.findViewById(R.id.p2StopTime)));
                mTimeSlotList.add(new UIRelayTimeSlot((Switch) view.findViewById(R.id.p3Enable),
                        (EditText) view.findViewById(R.id.p3StartTime),
                        (EditText) view.findViewById(R.id.p3StopTime)));
                mTimeSlotList.add(new UIRelayTimeSlot((Switch) view.findViewById(R.id.p4Enable),
                        (EditText) view.findViewById(R.id.p4StartTime),
                        (EditText) view.findViewById(R.id.p4StopTime)));
            }

            public void updateTimeSlot(int index, String start, String stop, boolean enable) {
                UIRelayTimeSlot UITimeSlot = mTimeSlotList.get(index);
                UITimeSlot.update(start, stop, enable);
            }

            public void updateEnable(boolean enable) {
                mSwitch.setChecked(enable);
            }
        }

        private ICActivity mActivity;
        private UIRelay mUIRelay;

        public RelayChannelFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static RelayChannelFragment newInstance(int sectionNumber) {
            RelayChannelFragment fragment = new RelayChannelFragment();
            Bundle args = new Bundle();
            
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView;
            int index = getArguments().getInt(ARG_SECTION_NUMBER);

            rootView = inflater.inflate(R.layout.channel_ic, container, false);
            mUIRelay = new UIRelay(rootView, index);
            return rootView;
        }

        public void onActivityCreated(Bundle saveInstance) {
            super.onActivityCreated(saveInstance);
            mActivity = (ICActivity) getActivity();
            mActivity.txMessage("GET");
        }

        public void updateUIRelay(boolean enable, int index,
                                  String startTime, String stopTime,
                                  boolean tsEnabled) {
            mUIRelay.updateEnable(enable);
            mUIRelay.updateTimeSlot(index, startTime, stopTime, tsEnabled);
        }
    }

    static final String ACTION_IC_RX_CONF = "IC_RX_CONF";
    static final int RELAY_DEPTH = 4;
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {
        private ArrayList<RelayChannelFragment> mFragments;
        private XmlPullParser mXmlConfParser;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);

            mFragments = new ArrayList<RelayChannelFragment>();

            try {
                mXmlConfParser = XmlPullParserFactory.newInstance().newPullParser();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
            registerReceiver(mUIReceiver, new IntentFilter(ACTION_IC_RX_CONF));
        }

        void parseRxMessage(String data) throws XmlPullParserException, IOException {
            InputStream is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

            try {
                mXmlConfParser.setInput(is, null);
                int event = mXmlConfParser.getEventType();

                while (event != XmlPullParser.END_DOCUMENT)  {
                    String name = mXmlConfParser.getName();
                    if (name == null || name.equals("time_slot") == false) {
                        event = mXmlConfParser.next();
                        continue;
                    }

                    int channel =
                            Integer.parseInt(mXmlConfParser.getAttributeValue(null, "channel"));
                    boolean enabled =
                            mXmlConfParser.getAttributeValue(null, "channel_enabled").equals("1");
                    int index = Integer.parseInt(mXmlConfParser.getAttributeValue(null, "index"));
                    boolean tsEnabled =
                            mXmlConfParser.getAttributeValue(null, "enabled").equals("1");
                    String startTime = mXmlConfParser.getAttributeValue(null, "start_time");
                    String stopTime = mXmlConfParser.getAttributeValue(null, "stop_time");

                    switch (event) {
                        case XmlPullParser.START_TAG:
                            break;
                        case XmlPullParser.END_TAG: {
                            if (mFragments.size() > channel) {
                                mFragments.get(channel).updateUIRelay(enabled, index, startTime,
                                        stopTime, tsEnabled);
                            }
                            break;
                        }
                    }
                    event = mXmlConfParser.next();
                }
            } finally {
                is.close();
            }
        }

        private final BroadcastReceiver mUIReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(ACTION_IC_RX_CONF)) {
                    String buffer = intent.getStringExtra("RX_CONF");
                    if (buffer.length() > 0)
                        try {
                            mSectionsPagerAdapter.parseRxMessage(intent.getStringExtra("RX_CONF"));
                        } catch (XmlPullParserException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
            }
        };

        public Fragment getItem(int position) {
            mFragments.add(position, RelayChannelFragment.newInstance(position + 1));
            return mFragments.get(position);
        }

        public int getCount() {
            return RELAY_DEPTH;
        }
    }
}
