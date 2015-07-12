package net.lorenzobianconi.wwatcher;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class BTScanList extends ListActivity {
	private BTScanAdapter adapter;

	@SuppressWarnings("unchecked")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ArrayList<WWDevice> list = (ArrayList<WWDevice>)getIntent().getSerializableExtra("dev_list");
		adapter = new BTScanAdapter(this, list);
		setListAdapter(adapter);
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		WWDevice device = (WWDevice)adapter.getItem(position);
		Intent intent = new Intent();
		intent.putExtra("device", device.getAddress());
		setResult(RESULT_OK, intent);
		finish();
	}
}
