package net.lorenzobianconi.achat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public class UserListFragment extends Fragment {
	private static final String USER_LIST_KEY = "USER_LIST";
	
	private ListView _userList;
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    						 Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_list_fragment, container, false);
        
        _userList = (ListView)view.findViewById(R.id.userListView);
        return view;
	}
    
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        
        
    }
    
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    }
}
