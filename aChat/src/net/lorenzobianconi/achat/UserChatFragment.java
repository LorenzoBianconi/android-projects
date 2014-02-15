package net.lorenzobianconi.achat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class UserChatFragment extends Fragment implements OnClickListener {
	private static final String CHAT_HISTORY_KEY = "CHAT_HISTORY";
	/**
	 * AChatActivity interface
	 */
	public interface UserChatListener {
		public String getNick();
        public void sendText(String text);
	}
	
	private EditText _msgEdit = null;
	private TextView _chatText = null;
	private ScrollView _scroll = null;

	private UserChatListener _uChatListener = null;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.user_chat_fragment, container, false);

		_msgEdit = (EditText)view.findViewById(R.id.msgTextEdit);
		_chatText = (TextView)view.findViewById(R.id.userChatTextView);
		_scroll = (ScrollView)view.findViewById(R.id.userChatScrollView);
		
		Button sndButton = (Button)view.findViewById(R.id.msgSendButton);
		sndButton.setOnClickListener(this);

		return view;
	}
	
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		_uChatListener = (UserChatListener)activity;
	}

	public void onClick(View view) {
		switch(view.getId()) {
		case R.id.msgSendButton:
			String text = _msgEdit.getText().toString();
			appendText(_uChatListener.getNick(), text, AChatMessage.ACHAT_DATA);
			_uChatListener.sendText(text);
			_msgEdit.setText("");
			break;
		default:
			break;
		}
	}
	
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        
        String msgHistory = _chatText.getText().toString();
        state.putString(CHAT_HISTORY_KEY, msgHistory);
    }
    
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	
    	if (savedInstanceState != null) {
    		String msgHistory = savedInstanceState.getString(CHAT_HISTORY_KEY);
    		_chatText.setText(msgHistory);
    	}
    }
    
    public void appendText(String user, String text, int type) {
    	int color;
    	String msg;
    	
    	if (type == AChatMessage.ACHAT_DATA) {
    		msg = "<" + user + ">: " + text + "\n";
    		color = (user.equals(_uChatListener.getNick())) ? Color.GREEN : Color.BLACK;
    	} else {
    		msg = "* " + user + text + "\n";
    		color = Color.GRAY;
    	}
		
		Spannable msgtoSpan = new SpannableString(msg);
		msgtoSpan.setSpan(new ForegroundColorSpan(color), 0, msgtoSpan.length(),
						  Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		_scroll.fullScroll(View.FOCUS_DOWN);
		_chatText.append(msgtoSpan);
    }
}
