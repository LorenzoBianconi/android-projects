package net.lorenzobianconi.achat;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AChatMessage {
	static final int ACHAT_HDR_LEN = 8;
	/**
	 * AChat message type
	 */
	static final int ACHAT_AUTH_REQ = 0;
	static final int ACHAT_AUTH_REP = 1;
	static final int ACHAT_DATA = 2;
	static final int ACHAT_USER_SUMMARY = 3;
	/**
	 * AChat Authentication Reply results 
	 */
	static final int AUTH_DENY = 0;
	static final int AUTH_SUCC = 1;
	
	private static void setChatHeader(ByteBuffer msg, int type, int len) {
		msg.putInt(type);
		msg.putInt(len);
	}
	
	private static void setNickInfo(ByteBuffer msg, String nick, int nicklen) {
		msg.putInt(nicklen);
		msg.put(nick.getBytes());
	}
	
	private static void setData(ByteBuffer msg, String data) {
		msg.put(data.getBytes());
	}
	
	public static String makeMsg(int type, String nick, String data) {
		int dlen = 4 + nick.length() + data.length();
		byte[] buff = new byte[8 + dlen];
		ByteBuffer msg = ByteBuffer.wrap(buff);
		
		msg.clear();
		msg.order(ByteOrder.BIG_ENDIAN);
		
		setChatHeader(msg, type, dlen);
		setNickInfo(msg, nick, nick.length());
		switch (type) {
		case ACHAT_AUTH_REQ:
			break;
		case ACHAT_DATA:
			setData(msg, data);
			break;
		default:
			break;
		}
		return new String(buff);
	}
	
	public static void sendMsg(Socket sock, String nick, String data,
							   int type) {
		try {
			OutputStream stream = sock.getOutputStream();
			PrintStream os = new PrintStream(new BufferedOutputStream(stream));
			os.println(makeMsg(type, nick, data));
			os.flush();
		} catch (IOException e) {}
	}
	
	public static boolean checkType(int type) {
		return (type >= 0 && type <= 3);
	}
}
