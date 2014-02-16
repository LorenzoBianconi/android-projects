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
	}
	
	private EditText _msgEdit = null;
	private TextView _chatText = null;
	private ScrollView _scroll = null;
	private String _chatHistory = "";

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
     	if (type == AChatMessage.ACHAT_DATA) {
    		String msg = "&lt;" + user + "&gt; " + text;
    		if (user.equals(_uChatListener.getNick()) == true)
    			msg = "<font color='#00FF00'>" + msg + "</font>";
    		_chatHistory += msg + "<br>";
    	} else
    		_chatHistory += "<font color='#FF00FF'><i>" + "* " + user +
    						text + "</i></font><br>";
     	_scroll.fullScroll(View.FOCUS_DOWN);
     	_chatText.setText(Html.fromHtml(_chatHistory));
    }
}
