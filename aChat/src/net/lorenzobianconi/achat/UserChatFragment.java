package net.lorenzobianconi.achat;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
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
        public void getNotification();
	}
	
	private EditText _msgEdit = null;
	private TextView _chatText = null;
	private ScrollView _scroll = null;
	private Button _sndButton = null;
	private String _chatHistory = "";

	private UserChatListener _uChatListener = null;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.user_chat_fragment, container, false);
		_msgEdit = (EditText)view.findViewById(R.id.msgTextEdit);
		_chatText = (TextView)view.findViewById(R.id.userChatTextView);
		_scroll = (ScrollView)view.findViewById(R.id.userChatScrollView);
		
		_sndButton = (Button)view.findViewById(R.id.msgSendButton);
		_sndButton.setOnClickListener(this);
		_sndButton.setEnabled(false);

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
			appendText(_uChatListener.getNick(), text,
					   AChatMessage.ACHAT_DATA);
			_uChatListener.sendText(text);
			_msgEdit.setText("");
			break;
		default:
			break;
		}
	}
	
	public void enableButton(boolean enable) {
		_sndButton.setEnabled(enable);
	}
	
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        
        state.putString(CHAT_HISTORY_KEY, _chatHistory);
    }
    
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	
    	if (savedInstanceState != null) {
    		_chatHistory = savedInstanceState.getString(CHAT_HISTORY_KEY);
    		_chatText.setText(Html.fromHtml(_chatHistory));
    	}
    }

    public void appendText(String user, String text, int type) {
    	switch (type) {
    	case -1: /* Error */
    		_chatHistory += "<font color='#FF0000'><i>" + text +
    						"</i></font><br>";
    		break;
    	case AChatMessage.ACHAT_DATA:
    		String msg = "&lt;" + user + "&gt; " + text;
    		if (user.equals(_uChatListener.getNick()) == true)
    			msg = "<font color='#00FF00'>" + msg + "</font>";
    		_chatHistory += msg + "<br>";
    		break;
    	default: /* control message */
    		_chatHistory += "<font color='#FF00FF'><i>" + "* " + user +
			text + "</i></font><br>";
    		break;
    	}
     	_scroll.fullScroll(View.FOCUS_DOWN);
     	_chatText.setText(Html.fromHtml(_chatHistory));
    }
    
    public void appendNotification(String user, String text) {
		_chatHistory += ("&lt;" + user + "&gt; " + text + "<br>");
     	_scroll.fullScroll(View.FOCUS_DOWN);
     	_chatText.setText(Html.fromHtml(_chatHistory));
    }
}
