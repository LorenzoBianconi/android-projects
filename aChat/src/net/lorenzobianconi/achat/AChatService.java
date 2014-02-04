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
				/**
				 * Start network connection
				 */
				connect();
				break;
			case MSG_UNREGISTER_CMD:
				_aChatMess = null;
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	/**
	 * Achat server address info
	 */
	final static int ACHAT_PORT = 9999;
	final static String ACHAT_URI = "lorenet.dyndns.org";
	private SocketAddress _achatServer = null;
	/**
	 * Network socket
	 */
	private Socket _sock = new Socket();
	/**
	 * Public target for AChat messages
	 */
	public Messenger _aChatServiceMess;
	private Looper _aChatServiceLooper;
	/**
	 * Messenger for communicating with AChat
	 */
	private Messenger _aChatMess = null;
	
	public void onCreate() {
		HandlerThread thread = new HandlerThread("AChatService");
		thread.start();
		_aChatServiceLooper = thread.getLooper();
		_aChatServiceMess = new Messenger(new IncomingHandler(_aChatServiceLooper));
	}
	
	public IBinder onBind(Intent intent) {
		return _aChatServiceMess.getBinder();
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	public void onDestroy() {
		try {
			if (_sock.isConnected())
				_sock.close();
		} catch (IOException e) {}
	}
	
	private void connect() {
		try {
			_achatServer = new InetSocketAddress(ACHAT_URI, ACHAT_PORT);
			if (!_sock.isConnected())
				_sock.connect(_achatServer);
			/**
			 * Start reader loop
			 */
			startReader(_sock);
		} catch (IOException e) {
			Message msg = Message.obtain(null, AchatActivity.MSG_CONN_ERR);
			try {
				_aChatMess.send(msg);
				stopSelf();
			} catch (RemoteException re) {}
		}
	}
	
	private void startReader(Socket sock) {
		try {
			InputStream stream = sock.getInputStream();
			InputStreamReader isr = new InputStreamReader(stream);
			BufferedReader ib = new BufferedReader(isr);
			/**
			 * Set socket in AchatActivity
			 */
			Message msg = Message.obtain(null, AchatActivity.MSG_SET_SOCK,
					 					 _sock);
			_aChatMess.send(msg);

			while (true) {
				char[] c_header = new char[AChatMessage.ACHAT_HDR_LEN];
				/* get chat header */
				if (ib.read(c_header) < 0)
					continue;
				String cheader = new String(c_header); 
				ByteBuffer ChatHeader = ByteBuffer.wrap(cheader.getBytes());
				int type = ChatHeader.getInt();
				int datalen = ChatHeader.getInt();
				if (datalen < 0 || !AChatMessage.checkType(type))
					continue;
				char[] c_data = new char[datalen];
				ib.read(c_data);
				ByteBuffer data = ByteBuffer.wrap(new String(c_data).getBytes());
				msg = Message.obtain(null, AchatActivity.MSG_RX_FRM, type, 0, data);
				_aChatMess.send(msg);
			}
		} catch (IOException e) {
			Message msg = Message.obtain(null, AchatActivity.MSG_CONN_ERR);
			try {
				_aChatMess.send(msg);
				stopSelf();
			} catch (RemoteException re) {}
		} catch (RemoteException e) {}
	}
}
