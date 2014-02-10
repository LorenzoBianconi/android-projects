package net.lorenzobianconi.achat;

import java.util.ArrayList;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class UserListFragment extends Fragment {
	private AchatActivity _parent = null;
	
	private ListView _userListView = null;
	private ArrayList<String> _userList = new ArrayList<String>();
	private ArrayAdapter<String> _listAdapter = null;
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    						 Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_list_fragment, container, false);
        
        _userListView = (ListView)view.findViewById(R.id.userListView);
        _listAdapter = new ArrayAdapter<String>(getActivity(),
        					android.R.layout.simple_list_item_1, _userList);        
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