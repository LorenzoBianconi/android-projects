package net.lorenzobianconi.achat;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import net.lorenzobianconi.achat.UserChatFragment.UserChatListener;
import net.lorenzobianconi.achat.UserListFragment.UserListListener;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class AchatActivity extends ActionBarActivity
	implements UserChatListener, UserListListener {
	/**
	 * Page adapter implementation
	 */
	class PageAdatper extends FragmentPagerAdapter {
		private List <Fragment> _fragments;

		public PageAdatper(FragmentManager fm, List<Fragment> fragments) {
			super(fm);
			_fragments = fragments;
		}

		public Fragment getItem(int position) {
			return _fragments.get(position);
		}

		public int getCount() {
			return _fragments.size();
		}

		public float getPageWidth(int position) {
			if (position == 0)
				return 1;
			return 0.75f;
		}
	}

	/**
	 * Message type
	 */
	static final int MSG_RX_FRM = 0;
	static final int MSG_CONNECTED = 1;
	static final int MSG_CONN_ERR = 2;

    /**
     * Handler of incoming messages from AChatService
     */
    class IncomingHandler extends Handler {
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
    		case MSG_RX_FRM:
    			parseMsg((ByteBuffer)msg.obj, msg.arg1);
    			break;
    		case MSG_CONNECTED:
    			_userChatFrag.enableButton(true);
    			AchatActivity.this.sendMessage(AChatService.MSG_SET_BACKGROUND, false);
    			AchatActivity.this.sendMessage(AChatService.MSG_GET_SUMMARY, 0);
    			break;
    		case MSG_CONN_ERR:
    			displayText("", (String)msg.obj, -1);
    			_userListFrag.clearList();
    			_userChatFrag.enableButton(false);
    			break;
    		default:
    			super.handleMessage(msg);
    		}
    	}
    }

	/**
	 * Class for interacting with the main interface of the AChatService
	 */
    class AChatServiceConnection implements ServiceConnection {
		public void onServiceConnected(ComponentName name, IBinder service) {
			_aChatServiceMess = new Messenger(service);
			sendMessage(AChatService.MSG_REGISTER_CMD, _aChatMess);
		}

		public void onServiceDisconnected(ComponentName name) {
			_aChatServiceMess = null;
			_aChatBound = false;
			_userListFrag.clearList();
			displayText("", "connection to lorenzobianconi.net failed", -1);
		}
	};

	private static final int SETTINGS_RESULT = 1;
	/**
	 * User nickname
	 */
	public String _nick = null;
	private ServiceConnection _aChatConn = null;
    /**
     * Public target for AChatService messages
     */
    public Messenger _aChatMess = null;
	/**
	 * Messenger for TX messages to AChatService
	 */
	private Messenger _aChatServiceMess = null;
	/**
	 * Flag indicating AChatService is bounded or not
	 */
	private boolean _aChatBound = false;
		
	/**
	 * UI page adapter
	 */
	PageAdatper _pAdapter = null;
	ViewPager _vPager = null;
	/**
	 * Fragments references
	 */
	UserChatFragment _userChatFrag = null;
	UserListFragment _userListFrag = null;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_achat);

		_vPager = (ViewPager)findViewById(R.id.view_pager);
		_vPager.setPageMargin(20);
		int id = _vPager.getId();
		
		if (savedInstanceState != null) {
			_userChatFrag = (UserChatFragment)getSupportFragmentManager().findFragmentByTag(
											"android:switcher:" + id + ":0");
			_userListFrag = (UserListFragment)getSupportFragmentManager().findFragmentByTag(
											"android:switcher:" + id + ":1");
		} else {
			_userChatFrag = new UserChatFragment();
			_userListFrag = new UserListFragment();
		}

		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(_userChatFrag);
		fragments.add(_userListFrag);

		_pAdapter = new PageAdatper(getSupportFragmentManager(), fragments);
		_vPager.setAdapter(_pAdapter);

		_aChatConn = new AChatServiceConnection();
		_aChatMess = new Messenger(new IncomingHandler());

		if (isAChatServiceRunning() == false) {
			Intent startService = new Intent(this, AChatService.class);
			startService(startService);
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.achat, menu);
		return super.onCreateOptionsMenu(menu);
	}

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_settings:
            Intent i = new Intent(this, AChatSettings.class);
            startActivityForResult(i, SETTINGS_RESULT);
            break;
        }

        return true;
    }
    
    protected void onActivityResult(int requestCode, int resultCode,
    								Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        SharedPreferences sharedPrefs;
    	
    	sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        switch (requestCode) {
        case SETTINGS_RESULT:
        	String nick = sharedPrefs.getString("NICK", _nick);
			if (nick != _nick)
				sendMessage(AChatService.MSG_CHANGE_NICK, nick);
			String depth = sharedPrefs.getString("DEPTH", "45");
			_userChatFrag.setDepth(Integer.parseInt(depth));
			sharedPrefs.edit().putString("DEPTH", depth).commit();
			sendMessage(AChatService.MSG_SET_BACKGROUND, false);
            break;
        } 
    }

	protected void onStart() {
		super.onStart();

		NotificationManager nm;

		nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(AChatService.NOTIFICATION_ID);
		sendMessage(AChatService.MSG_SET_BACKGROUND, false);
		_nick = updateNick(this);
		bindService(new Intent(this, AChatService.class),
					_aChatConn, Context.BIND_AUTO_CREATE);
	}
	
	protected void onStop() {
		super.onStop();

		sendMessage(AChatService.MSG_SET_BACKGROUND, true);
		if (_aChatBound == true) {
			unbindService(_aChatConn);
			_aChatBound = false;
		}
	}
	
	/**
	 * Parse AChat message payload
	 */
	public void parseMsg(ByteBuffer buff, int type) {
		int nickLen = buff.getInt();
		byte[] nick = new byte[nickLen];
		buff.get(nick);
		String user = new String(nick);
		
		switch (type) {
		case AChatMessage.ACHAT_AUTH_REP:
			int res = buff.getInt();
			if (res != AChatMessage.AUTH_SUCC) {
				displayText("", "authentication failed", -1);
				stopService(new Intent(this, AChatService.class));
			} else {
				Toast toast = Toast.makeText(this, "Connected to the Server",
											 Toast.LENGTH_LONG);
				toast.show();
			}
			break;
		case AChatMessage.ACHAT_DATA:
			byte[] data = new byte[buff.remaining()];
			while (buff.remaining() > 0)
				buff.get(data);
			String text = new String(data, Charset.defaultCharset());
			displayText(user, text, AChatMessage.ACHAT_DATA);
			break;
		case AChatMessage.ACHAT_USER_SUMMARY:
			ArrayList<String> userList = new ArrayList<String>();
			while (buff.remaining() > 0) {
				int ulen = buff.getInt();
				byte[] unick = new byte[ulen];
				buff.get(unick);
				userList.add(new String(unick, Charset.defaultCharset()));
			}
			UserListFragment uListFrag = (UserListFragment)_pAdapter.getItem(1);
			uListFrag.updateUserList(userList);
			break;
		default:
			break;
		}
	}

	public void displayText(String user, String text, int type) {
		UserChatFragment uChatFrag = (UserChatFragment)_pAdapter.getItem(0);
		uChatFrag.appendText(user, text, type);
	}

	private void sendMessage(int type, Object obj) {
		try {
			Message msg = Message.obtain(null, type, obj);
			if (_aChatServiceMess!= null)
				_aChatServiceMess.send(msg);
		} catch (RemoteException e) {}
	}

	public void sendText(String text) {
		sendMessage(AChatService.MSG_SEND_DATA, text);
	}

	private boolean isAChatServiceRunning() {
		String aChatService = "net.lorenzobianconi.achat.AChatService";
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (aChatService.equals(service.service.getClassName())) {
	             return true;
	        }
	    }
	    return false;
	}

	private static Account getAccount(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccountsByType("com.google");

		if (accounts.length > 0)
			return accounts[0];
		else
			return null;
	}

	public String getNick() {
		return _nick;
	}

	public static String updateNick(Context context) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		String nick = (getAccount(context) == null) ? "android" : getAccount(context).name;
		nick = sharedPrefs.getString("NICK", nick);
		sharedPrefs.edit().putString("NICK", nick).commit();
		
		return nick;
	}
}
