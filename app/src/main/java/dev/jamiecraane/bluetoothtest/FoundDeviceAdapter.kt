package dev.jamiecraane.bluetoothtest

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import dev.jamiecraane.bluetoothtest.databinding.ViewDeviceRowBinding

/**
 * User: jamiecraane
 * Date: 09/04/2021
 */
class FoundDeviceAdapter : ListAdapter<BluetoothDevice, BluetoothDeviceViewHolder>(DIFF_CALLBACK) {
    var onDeviceClickListener: (BluetoothDevice) -> Unit = {}

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BluetoothDevice>() {
            override fun areItemsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
                return oldItem.address == newItem.address
            }

            override fun areContentsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
                return oldItem.address == newItem.address
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothDeviceViewHolder {
        return BluetoothDeviceViewHolder(ViewDeviceRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: BluetoothDeviceViewHolder, position: Int) {
        holder.bind(getItem(position), onDeviceClickListener)
    }
}

class BluetoothDeviceViewHolder(binding: ViewDeviceRowBinding) : BoundHolder<ViewDeviceRowBinding>(binding) {
    fun bind(device: BluetoothDevice, onDeviceClickListener: (BluetoothDevice) -> Unit) {
        binding.name.text = device.name
        binding.address.text = device.address
        binding.root.setOnClickListener {
            onDeviceClickListener(device)
        }
    }
}


abstract class BoundHolder<T : ViewBinding>(protected val binding: T) : RecyclerView.ViewHolder(binding.root)