package net.lorenzobianconi.achat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

public class AChatService extends Service {
	/**
	 * Message type
	 */
	static final int MSG_REGISTER_CMD = 0;
	static final int MSG_UNREGISTER_CMD = 1;
	static final int MSG_SET_BACKGROUND = 2;
	static final int MSG_SEND_DATA = 3;
	static final int MSG_GET_SUMMARY = 4;
	static final int MSG_TRY_CONNECT = 5;
	static final int MSG_CHANGE_NICK = 6;

	/**
	 * Handler for AChat Incoming messages
	 */
	class IncomingHandler extends Handler {
		public IncomingHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_REGISTER_CMD:
				_aChatMess = (Messenger)msg.obj;
				if (_sock != null)
					sendActivityMsg(AchatActivity.MSG_CONNECTED, 0, 0, null);
				break;
			case MSG_UNREGISTER_CMD:
				_aChatMess = null;
				break;
			case MSG_SET_BACKGROUND:
				_backbround = (Boolean)msg.obj;
				break;
			case MSG_SEND_DATA:
				AChatMessage.sendMsg(_sock, _nick, (String)msg.obj,
									 AChatMessage.ACHAT_DATA);
				break;
			case MSG_GET_SUMMARY:
				AChatMessage.sendMsg(_sock, _nick, "",
									 AChatMessage.ACHAT_REQ_SUMMARY);
				break;
			case MSG_TRY_CONNECT:
				new AChatServerConn().execute();
				break;
			case MSG_CHANGE_NICK:
				AChatMessage.sendMsg(_sock, _nick, (String)msg.obj,
									 AChatMessage.ACHAT_CHANGE_NICK);
				_nick = (String)msg.obj;
			default:
				super.handleMessage(msg);
			}
		}
	}

	/**
	 * AChat frame reader
	 */
	class AChatReader implements Runnable {
		private boolean _running = false;
		private Socket _sock = null;

		AChatReader(Socket sock) {
			_sock = sock;
			Thread achatReader = new Thread(this, "AChatReader");
			achatReader.start();
		}

		public boolean getRunning() { return _running; }
		public void setRunning(boolean running) { _running = running; }

		public void run() {
			try {
				_running = true;
				InputStream stream = _sock.getInputStream();
				InputStreamReader isr = new InputStreamReader(stream);
				BufferedReader ib = new BufferedReader(isr);

				while (_running == true) {
					char[] c_header = new char[AChatMessage.ACHAT_HDR_LEN];
					/* get chat header */
					if (ib.read(c_header) < 0) {
						_sock.close();
						break;
					}
					String cheader = new String(c_header);
					ByteBuffer ChatHeader = ByteBuffer.wrap(cheader.getBytes());
					int type = ChatHeader.getInt();
					int datalen = ChatHeader.getInt();
					if (datalen < 0 || !AChatMessage.checkType(type))
						continue;
					char[] c_data = new char[datalen];
					ib.read(c_data);
					ByteBuffer data = ByteBuffer.wrap(new String(c_data).getBytes());
					if (_backbround == false)
						sendActivityMsg(AchatActivity.MSG_RX_FRM, type, 0, data);
					else if (type == AChatMessage.ACHAT_DATA) {
						/**
						 * send notification to the system
						 */
						buildNotification(data);
					}
				}
			} catch (IOException e) {
			} finally {
				String info = "connection to lorenzobianconi.net failed";
				sendActivityMsg(AchatActivity.MSG_CONN_ERR, 0, 0, info);
				Message msg = Message.obtain(null, MSG_TRY_CONNECT);
				_aChatServiceHnadler.sendMessageDelayed(msg, DELAY_MS);
				_sock = null;
			}
		}
	}

	/**
	 * AChat server connection
	 */
	private class AChatServerConn extends AsyncTask<Void, Void, Socket> {
		/**
		 * AChat server address info
		 */
		final static int ACHAT_PORT = 9999;
		final static String ACHAT_URI = "lorenzobianconi.no-ip.org";

		protected Socket doInBackground(Void... arg0) {
			try {
				SocketAddress achatServer = new InetSocketAddress(ACHAT_URI, ACHAT_PORT);
				Socket sock = new Socket();
				sock.connect(achatServer);
				/**
				 * start frame reader
				 */
				_reader = new AChatReader(sock);
				/**
				 * AChatServer authentication
				 */
				AChatMessage.sendMsg(sock, _nick, "", AChatMessage.ACHAT_AUTH_REQ);
				return sock;
			} catch (IOException e) {
				return null;
			}
		}

		protected void onPostExecute(Socket sock) {
			if (sock == null) {
				String info = "connection to lorenzobianconi.net failed";
				int delay = _attemptCounter * DELAY_MS;
				if (delay < MAX_DELAY_MS)
					_attemptCounter++;
				else
					delay = MAX_DELAY_MS;
				Message msg = Message.obtain(null, MSG_TRY_CONNECT);
				_aChatServiceHnadler.sendMessageDelayed(msg, delay);
				sendActivityMsg(AchatActivity.MSG_CONN_ERR, 0, 0, info);
			} else {
				_attemptCounter = 1;
				_sock = sock;
				sendActivityMsg(AchatActivity.MSG_CONNECTED, 0, 0, null);
			}
		}
	}

	public static final int NOTIFICATION_ID = 1;
	private static final int MAX_DELAY_MS = 180000;
	private static final int DELAY_MS = 15000;
	/**
	 * User nickname
	 */
	private String _nick;
	
	private int _attemptCounter = 1;
	/**
	 * Network socket
	 */
	private Socket _sock = null;
	private IncomingHandler _aChatServiceHnadler;
	/**
	 * Public target for AChat messages
	 */
	public Messenger _aChatServiceMess;
	private Looper _aChatServiceLooper;
	/**
	 * Messenger for communicating with AChat
	 */
	private Messenger _aChatMess = null;
	/**
	 * Frame reader
	 */
	private AChatReader _reader;
	
	private boolean _backbround = true;
	
	public void onCreate() {
		HandlerThread thread = new HandlerThread("AChatService");
		thread.start();
		_aChatServiceLooper = thread.getLooper();
		_aChatServiceHnadler = new IncomingHandler(_aChatServiceLooper);
		_aChatServiceMess = new Messenger(_aChatServiceHnadler);
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		_nick = AchatActivity.updateNick(this);
		/**
		 * Start network connection
		 */
		new AChatServerConn().execute();
		return START_STICKY;
	}

	public IBinder onBind(Intent intent) {
		return _aChatServiceMess.getBinder();
	}
	
	public void onDestroy() {
		_reader.setRunning(false);
	}
	
	private void sendActivityMsg(int type, int arg1, int arg2, Object obj) {
		try {
			if (_aChatMess != null) {
				Message msg = Message.obtain(null, type, arg1, arg2, obj);
				_aChatMess.send(msg);
			}
		} catch (RemoteException e) {}
	}
	
	private void buildNotification(ByteBuffer buff) {
		int nickLen = buff.getInt();
		byte[] nick = new byte[nickLen];
		buff.get(nick);
		String user = new String(nick);
		
		byte[] data = new byte[buff.remaining()];
		while (buff.remaining() > 0)
			buff.get(data);
		String text = new String(data, Charset.defaultCharset());
		
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setSmallIcon(R.drawable.ic_launcher);
		mBuilder.setContentTitle(user);
		mBuilder.setContentText(text);
		mBuilder.setAutoCancel(true);
		
		Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		mBuilder.setSound(soundUri);
		
    	try {
    		/* save AChat History */
    		String msg = "&lt;" + user + "&gt; " + text + "<br>";
    		FileOutputStream oS = openFileOutput("CHAT_HISTORY", Context.MODE_APPEND);
    		oS.write(msg.getBytes());
    		oS.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {}
		
		Intent intent = new Intent(this, AchatActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent,
														  PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(pIntent);	
		NotificationManager notifyMgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notifyMgr.notify(NOTIFICATION_ID, mBuilder.build());
	}
}
