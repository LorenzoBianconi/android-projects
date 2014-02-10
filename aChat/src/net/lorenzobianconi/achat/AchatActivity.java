package net.lorenzobianconi.achat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.widget.Toast;

public class AchatActivity extends FragmentActivity {
	/**
	 * Page adapter implementation
	 */
	class PageAdatper extends FragmentStatePagerAdapter {
		private List <Fragment> _fragments;
		public PageAdatper(FragmentManager fm, List<Fragment> fragments) {
			super(fm);
			_fragments = fragments;
		}
		public Parcelable saveState() {
			return null;
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
	 * AChatServer connector
	 */
	private class AChatServerConn extends AsyncTask<Void, Void, Socket> {
		/**
		 * AChat server address info
		 */
		final static int ACHAT_PORT = 9999;
		final static String ACHAT_URI = "lorenet.dyndns.org";
		private SocketAddress _achatServer = null;
		private Socket sock = new Socket();
		
		protected void onPreExecute() {
			 _sock = new Socket();
		}
		protected Socket doInBackground(Void... arg0) {
			try {
				_achatServer = new InetSocketAddress(ACHAT_URI, ACHAT_PORT);
				sock.connect(_achatServer);
				return sock;
			} catch (IOException e) {
				return null;
			}
		}
		protected void onPostExecute(Socket sock) {
			if (sock != null) {
				_sock = sock;
				_aChatBound = true;
				sendMessage(_aChatServiceMess, AChatService.MSG_REGISTER_CMD,
						0, 0, sock);
				/**
				 * Server authentication
				 */
				AChatMessage.sendMsg(sock, _nick, "",
									 AChatMessage.ACHAT_AUTH_REQ);
			} else
				showAlert("ERROR", "Connection to server failed");
		}
	}
	/**
	 * Message type
	 */
	static final int MSG_RX_FRM = 0;
	static final int MSG_SET_SOCK = 1;
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
    		case MSG_CONN_ERR:
    			showAlert("ERROR", "Connection to server failed");
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
			new AChatServerConn().execute();
		}

		public void onServiceDisconnected(ComponentName name) {
			_aChatServiceMess = null;
			_aChatBound = false;
			showAlert("ERROR", "Connection to server failed");
		}
	};
	/**
	 * User nickname
	 */
	public String _nick = "lorenzo";
	/**
	 * Network socket
	 */
	private Socket _sock = null;
	private ServiceConnection _aChatConn = null;
	private boolean _onLine = false;
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
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_achat);
		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(new UserChatFragment());
		fragments.add(new UserListFragment());
		
		_pAdapter = new PageAdatper(super.getSupportFragmentManager(), fragments);
		ViewPager vPager = (ViewPager)findViewById(R.id.view_pager);
		vPager.setAdapter(_pAdapter);
		vPager.setPageMargin(20);
		/**
		 * Start AChatService if NetworkConnection is available
		 */
		_onLine = isOnLine();
		if (_onLine == true) {
			_aChatConn = new AChatServiceConnection();
			_aChatMess = new Messenger(new IncomingHandler());
		} else
			showAlert("WARNING", "Network Connection not available");
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.achat, menu);
		return true;
	}
	
	protected void onStart() {
		super.onStart();
		if (_onLine == true)
			bindService(new Intent(this, AChatService.class),
						_aChatConn, Context.BIND_AUTO_CREATE);
	}
	
	protected void onStop() {
		super.onStop();
		try {
			if (_aChatBound == true) {
				unbindService(_aChatConn);
				_aChatBound = false;
			}
			_sock.close();
		} catch (IOException e) {}
	}
	
	private boolean isOnLine() {
		ConnectivityManager connMgr = (ConnectivityManager)
				getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo nInfo = connMgr.getActiveNetworkInfo();
		return (nInfo != null && nInfo.isConnected());
	}
	
	private void showAlert(String title, String msg) {
		 AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		 alertDialog.setTitle(title);
		 alertDialog.setMessage(msg);
		 alertDialog.setNeutralButton("OK", new DialogInterface.OnClickListener() {
			 public void onClick(DialogInterface dialog, int which) {
				 dialog.cancel();
				 if (_aChatBound == true)
					 unbindService(_aChatConn);
				 finish();
			 }
		 });
		 alertDialog.show();
	}
	
	public void displayText(String user, String text, int type) {
		UserChatFragment uChatFrag = (UserChatFragment)_pAdapter.getItem(0);
		uChatFrag.appendText(user, text, type);
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
				showAlert("ERROR", "Authentication Failed");
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

	private void sendMessage(Messenger messenger, int type, int arg1,
							 int arg2, Object obj) {
		try {
			Message msg = Message.obtain(null, type, arg1, arg2, obj);
			msg.replyTo = _aChatMess;
			_aChatServiceMess.send(msg);
		} catch (RemoteException e) {}
	}

	public void sendText(String text) {
		AChatMessage.sendMsg(_sock, _nick, text, AChatMessage.ACHAT_DATA);
	}
}
