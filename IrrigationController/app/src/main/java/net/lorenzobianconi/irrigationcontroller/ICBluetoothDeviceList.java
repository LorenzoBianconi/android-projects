package net.lorenzobianconi.irrigationcontroller;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;

public class ICBluetoothDeviceList extends ListActivity {
    private ICAdapter mAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<ICDevice> list = (ArrayList<ICDevice>)getIntent().getSerializableExtra("DevList");
        mAdapter = new ICAdapter(this, list);
        setListAdapter(mAdapter);
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        ICDevice device = (ICDevice)mAdapter.getItem(position);
        Intent intent = new Intent();
        intent.putExtra("position", position);
        setResult(RESULT_OK, intent);
        finish();
    }
}
