package net.lorenzobianconi.achat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class AChatService extends Service {
	/**
	 * Message type
	 */
	static final int MSG_REGISTER_CMD = 0;
	static final int MSG_UNREGISTER_CMD = 1;
	static final int MSG_SEND_DATA = 2;
	static final int MSG_GET_SUMMARY = 3;
	static final int MSG_TRY_CONNECT = 4;

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
				_aChatMess = msg.replyTo;
				if (_sock != null)
					sendActivityMsg(AchatActivity.MSG_CONNECTED, 0, 0, null);
				break;
			case MSG_UNREGISTER_CMD:
				_aChatMess = null;
				break;
			case MSG_SEND_DATA:
				AChatMessage.sendMsg(_sock, _nick, (String)msg.obj, AChatMessage.ACHAT_DATA);
				break;
			case MSG_GET_SUMMARY:
				AChatMessage.sendMsg(_sock, _nick, "", AChatMessage.ACHAT_REQ_SUMMARY);
				break;
			case MSG_TRY_CONNECT:
				new AChatServerConn().execute();
				break;
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
					sendActivityMsg(AchatActivity.MSG_RX_FRM, type, 0, data);
				}
			} catch (IOException e) {
			} finally {
				String info = "--- Connection to lorenzobianconi.net failed ---";
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
		final static String ACHAT_URI = "lorenet.dyndns.org";

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
				String info = "--- Connection to lorenzobianconi.net failed ---";
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

	private static final int MAX_DELAY_MS = 180000;
	private static final int DELAY_MS = 15000;
	/**
	 * User nickname
	 */
	private String _nick = null;
	
	private int _attemptCounter = 1;
	/**
	 * Network socket
	 */
	private Socket _sock = null;
	private IncomingHandler _aChatServiceHnadler = null;
	/**
	 * Public target for AChat messages
	 */
	public Messenger _aChatServiceMess = null;
	private Looper _aChatServiceLooper = null;
	/**
	 * Messenger for communicating with AChat
	 */
	private Messenger _aChatMess = null;
	/**
	 * Frame reader
	 */
	private AChatReader _reader = null;
	
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
}
