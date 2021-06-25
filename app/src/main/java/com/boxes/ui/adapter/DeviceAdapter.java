package com.boxes.ui.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.boxes.ui.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {


    private List<BluetoothDevice> mList;
    private Context context;
    private IOnClickListener listener;

    public DeviceAdapter(Context context, IOnClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setListDevice(List<BluetoothDevice> mList){
        this.mList = mList;
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_device,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull DeviceAdapter.ViewHolder holder, int position) {
        BluetoothDevice device = mList.get(position);

        if (device.getAddress().equals("00:15:87:00:8F:D0")){
            holder.name.setText("Cái cân cần tìm");
        }else {
            holder.name.setText(device.getName() == null ? "null" : device.getName());
        }

        holder.address.setText(device.getAddress());

        holder.itemView.setOnClickListener(v -> listener.listener(device));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView name;
        private TextView address;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.itemName);
            address = itemView.findViewById(R.id.itemAddress);
        }
    }

    public interface IOnClickListener{
        void listener(BluetoothDevice device);
    }
}
