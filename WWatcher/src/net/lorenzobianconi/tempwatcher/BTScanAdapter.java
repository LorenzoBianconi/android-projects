package net.lorenzobianconi.tempwatcher;

import java.io.Serializable;
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

class WWDevice implements Serializable {
	private static final long serialVersionUID = 1L;
	private String name;
	private String addr;

	public WWDevice(String name, String addr) {
		this.name = name;
		this.addr = addr;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return addr;
	}
}

public class BTScanAdapter extends BaseAdapter {
	private static class ViewHolder {
        public TextView devName;
        public TextView devAddr;
    }

	private ArrayList<WWDevice> devList;
	private LayoutInflater inflater;

	public BTScanAdapter(Context context) {
		devList = new ArrayList<WWDevice>();
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public BTScanAdapter(Context context, ArrayList<WWDevice> devList) {
		this.devList = devList;
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return devList.size();
	}

	public Object getItem(int position) {
		return devList.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.bt_row, parent, false);
            holder = new ViewHolder();
            holder.devName = (TextView)convertView.findViewById(R.id.dev_name);
            holder.devAddr = (TextView)convertView.findViewById(R.id.dev_addr);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        WWDevice device = devList.get(position);
        holder.devName.setText(device.getName());
        holder.devAddr.setText(device.getAddress());

        return convertView;
	}

	public void clear() {
		devList.clear();
	}

	public void addDevice(WWDevice device) {
		if (devList.contains(device) == false)
			devList.add(device);
	}
}
