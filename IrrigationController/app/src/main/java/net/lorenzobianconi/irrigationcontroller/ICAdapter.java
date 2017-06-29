package net.lorenzobianconi.irrigationcontroller;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;

class ICDevice implements Serializable {
    private static final long serialVersionUID = 1L;
    private String mName;
    private String mAddress;

    public ICDevice(String name, String address) {
        mName = name;
        mAddress = address;
    }

    public String getName() {
        return mName;
    }
    public String getAddress() {
        return mAddress;
    }

    static ArrayList<ICDevice> getICDeviceList(ArrayList<BluetoothDevice> deviceList) {
        ArrayList<ICDevice> list = new ArrayList<ICDevice>();
        for (BluetoothDevice bd: deviceList) {
            list.add(new ICDevice(bd.getName(), bd.getAddress()));
        }
        return list;
    }
}

public class ICAdapter extends BaseAdapter {
    private static class ViewHolder {
        public TextView mDeviceName;
        public TextView mDeviceAddress;
    }

    private ArrayList<ICDevice> mDevList;
    private LayoutInflater mInflater;

    public ICAdapter(Context context) {
        mDevList = new ArrayList<ICDevice>();
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public ICAdapter(Context context, ArrayList<ICDevice> devList) {
        mDevList = devList;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return mDevList.size();
    }

    public Object getItem(int position) {
        return mDevList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.bluetooth_row, parent, false);
            holder = new ViewHolder();
            holder.mDeviceName = (TextView)convertView.findViewById(R.id.dev_name);
            holder.mDeviceAddress = (TextView)convertView.findViewById(R.id.dev_addr);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        ICDevice device = mDevList.get(position);
        holder.mDeviceName.setText(device.getName());
        holder.mDeviceAddress.setText(device.getAddress());

        return convertView;
    }

    public void clear() {
        mDevList.clear();
    }

    public void addDevice(ICDevice device) {
        if (mDevList.contains(device) == false)
            mDevList.add(device);
    }
}
