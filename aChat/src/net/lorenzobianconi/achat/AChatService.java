package net.lorenzobianconi.achat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
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
				_sock = (Socket)msg.obj;
				/**
				 * Start network reader
				 */
				startReader(_sock);
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
	 * Network socket
	 */
	private Socket _sock = null;
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
	
	private void sendActivityMsg(int type, int arg1, int arg2, Object obj) {
		try {
			Message msg = Message.obtain(null, type, arg1, arg2, obj);
			_aChatMess.send(msg);
		} catch (RemoteException e) {}
	}
	
	private void startReader(Socket sock) {
		try {
			InputStream stream = sock.getInputStream();
			InputStreamReader isr = new InputStreamReader(stream);
			BufferedReader ib = new BufferedReader(isr);

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
				sendActivityMsg(AchatActivity.MSG_RX_FRM, type, 0, data);
			}
		} catch (IOException e) {
			if (_sock.isClosed() == false)
				sendActivityMsg(AchatActivity.MSG_CONN_ERR, 0, 0, null);
		}
	}
}
