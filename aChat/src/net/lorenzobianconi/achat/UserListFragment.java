package net.lorenzobianconi.achat;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class UserListFragment extends Fragment {
	private class UserListArrayAdapter extends ArrayAdapter<String> {
		private Context _context = null;
		private ArrayList<String> _list;
		private int _layoutId;
		
		public UserListArrayAdapter(Context context, int resource,
									ArrayList<String> objects) {
			super(context, resource, objects);
			_context = context;
			_layoutId = resource;
			_list = objects;
		}
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView;
			LayoutInflater inflater = ((Activity)_context).getLayoutInflater();
			rowView = inflater.inflate(_layoutId, parent, false);
			
			TextView textView = (TextView)rowView.findViewById(R.id.txtTitle);
			String user = _list.get(position);
			textView.setText(user);
			if (user.equals(_parent._nick) == true)
				textView.setTextColor(Color.GREEN);
			return rowView;
		}
	}
	
	private AchatActivity _parent = null;
	
	private ListView _userListView = null;
	private ArrayList<String> _userList = new ArrayList<String>();
	private UserListArrayAdapter _listAdapter = null;
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    						 Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_list_fragment, container, false);
        
        _userListView = (ListView)view.findViewById(R.id.userListView);
        _listAdapter = new UserListArrayAdapter(getActivity(), R.layout.listview_item_row,
        										_userList);
        LayoutInflater inflat = getActivity().getLayoutInflater();
        View header = (View)inflat.inflate(R.layout.listview_header_row, null);
        _userListView.addHeaderView(header);
        _userListView.setAdapter(_listAdapter);
        return view;
	}
    
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		_parent = (AchatActivity)activity;
	}
    
    public void updateUserList(ArrayList<String> userList) {
       	for (int i = 0; i < _userList.size(); i++) {
       		boolean found = false;
       		String user = _userList.get(i);
       	  	for (String nuser : userList) {
       	  		if (user.equals(nuser) == true) {
       	  			found = true;
       	  			break;
       	  		}
       	  	}
       	  	if (found == false) {
       	  		_parent.displayText(user, " has left",
       	  							AChatMessage.ACHAT_USER_SUMMARY);
       	  		_listAdapter.remove(user);
       	  	}
       	}
   	  	for (String nuser : userList) {
   	  		boolean found = false;
   	  		for (int i = 0; i < _userList.size(); i++) {
   	  			if (nuser.equals(_userList.get(i))) {
   	  				found = true;
   	  				break;
   	  			}
   	  		}
   	  		if (found == false) {
       	  		_parent.displayText(nuser, " has joined",
 							AChatMessage.ACHAT_USER_SUMMARY);
   	  			_listAdapter.add(nuser);
   	  		}
   	  	}
    }
}