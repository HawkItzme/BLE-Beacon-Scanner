package com.example.bluetoothscanner

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothscanner.databinding.ScanResultItemBinding

class BtAdapter(val btList : ArrayList<BluetoothModel>) :
  RecyclerView.Adapter<BtAdapter.MyViewHolder>(){

    fun setList(bts : List<BluetoothModel>){
        btList.clear()
        btList.addAll(bts)
    }

      inner class MyViewHolder(val binding : ScanResultItemBinding): RecyclerView.ViewHolder(binding.root){

          fun bind(bt : BluetoothModel){
              binding.macId.text = bt.mac
              binding.rssiId.text = bt.rssi.toString()
          }
      }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding : ScanResultItemBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.scan_result_item,
            parent,
            false
        )
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return btList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(btList[position])
    }
}