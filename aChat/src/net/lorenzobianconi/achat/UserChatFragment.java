package net.lorenzobianconi.achat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

public class UserChatFragment extends Fragment
	implements OnClickListener {
	/**
	 * AChatActivity interface
	 */
	public interface UserChatListener {
		public String getNick();
        public void sendText(String text);
	}
		
	private EditText _msgEdit;
	private TextView _chatText;
	private ScrollView _scroll;
	private Button _sndButton;
	private int _historyDepth;
	private String _chatHistory = "";

	private UserChatListener _uChatListener;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle state) {
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

		SharedPreferences sharedPrefs;
		
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
		_historyDepth = Integer.parseInt(sharedPrefs.getString("DEPTH", "45"));
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
	
	public void setDepth(int depth) {
		_historyDepth = depth;
	}

    public void onStart() {
    	super.onStart();

    	try {
    		String line;
			FileInputStream iS = getActivity().openFileInput("CHAT_HISTORY");
			BufferedReader br = new BufferedReader(new InputStreamReader(iS));

			while ((line = br.readLine()) != null)
				_chatHistory += line; 
			iS.close();
			
    		String [] historyArray = _chatHistory.split("<br>");
    		if (historyArray.length >= _historyDepth) {
    			_chatHistory = "";
        		for (int i = historyArray.length - _historyDepth;
        			 i < historyArray.length; i++)
        			_chatHistory += historyArray[i] + "<br>";
    		}
	     	_scroll.fullScroll(View.FOCUS_DOWN);
	     	_chatText.setText(Html.fromHtml(_chatHistory));	     	
		} catch (FileNotFoundException e) {
		} catch (IOException e) {}
     }
    
    public void onStop() {
    	super.onStop();

    	try {
    		/* save AChat History */
    		FileOutputStream oS;
    		
    		oS = getActivity().openFileOutput("CHAT_HISTORY", Context.MODE_PRIVATE);
    		oS.write(_chatHistory.getBytes());
    		oS.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {}
    }

    public void appendText(String user, String text, int type) {
    	switch (type) {
    	case AChatMessage.ACHAT_DATA: {
    		String msg = "&lt;" + user + "&gt; " + text;
    		if (user.equals(_uChatListener.getNick()) == true)
    			msg = "<font color='#00FF00'>" + msg + "</font>";
    		_chatHistory += (msg + "<br>");
    		break;
    	}
    	default: /* control message */
    		_chatHistory += "<font color='#FF00FF'><i>* " + text +
					"</i></font><br>";
    		break;
    	}
     	_scroll.fullScroll(View.FOCUS_DOWN);
     	_chatText.setText(Html.fromHtml(_chatHistory));
    }
}
