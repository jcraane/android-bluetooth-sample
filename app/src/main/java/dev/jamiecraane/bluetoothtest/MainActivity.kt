package dev.jamiecraane.bluetoothtest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.jamiecraane.bluetoothtest.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private val scanModeReceiver = ScanModeBroadcastReceiver()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val foundDeviceAdapter = FoundDeviceAdapter()
    private val discoverDeviceReceiver = DiscoverDeviceReceiver(foundDeviceAdapter)
    private var serverSocket: BluetoothSocket? = null
    private var clientSocket: BluetoothSocket? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        Toast.makeText(this, "Location Permission Granted", Toast.LENGTH_SHORT).show()
//        todo show rationale for multiplayer.
        bluetoothAdapter?.startDiscovery()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        foundDeviceAdapter.onDeviceClickListener = { bluetoothDevice ->
            Toast.makeText(this, "Tapped device", Toast.LENGTH_SHORT).show()
            val socket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID)
            bluetoothAdapter?.cancelDiscovery()
            lifecycleScope.launchWhenStarted {
                withContext(Dispatchers.IO) {
                    try {
                        socket.connect()
                        clientSocket = socket
                    } catch (e: IOException) {
                        println("Unable to connecto")
                    }
                }
            }
        }
        registerReceiver(scanModeReceiver, IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        })

        registerReceiver(discoverDeviceReceiver, IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        })

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        scanModeReceiver.statusView = binding.discoverStatus

        binding.foundDevices.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.foundDevices.adapter = foundDeviceAdapter

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth is enabled ${bluetoothAdapter?.isEnabled}", Toast.LENGTH_SHORT).show()

            binding.enableDiscovery.setOnClickListener {
                val serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("Hably", MY_UUID)
                lifecycleScope.launchWhenStarted {
                    withContext(Dispatchers.IO) {
                        var shouldLoop = true
                        while (shouldLoop && isActive) {
                            val socket: BluetoothSocket? = try {
                                serverSocket?.accept()
                            } catch (e: IOException) {
                                println("ServerSocket accept failed")
                                shouldLoop = false
                                null
                            }
                            println("Got socket: $socket")
                            socket?.also {
                                this@MainActivity.serverSocket = socket
                                serverSocket?.close()
                                shouldLoop = false
                            }

                        }
                    }
                }

                val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 20)
                }
                startActivity(discoverableIntent)
            }

            binding.scanForBondedDevices.setOnClickListener {
                val pairedDevices = bluetoothAdapter?.bondedDevices
                if (pairedDevices?.isNotEmpty() == true) {
                    val names = pairedDevices.map { it.name }.joinToString(", ")
                    binding.bondedDevices.text = names
                } else {
                    binding.bondedDevices.text = "No bonded devices found"
                }
            }

            binding.discoverDevices.setOnClickListener {
                println("action: start discovery")
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    bluetoothAdapter?.startDiscovery()
                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(scanModeReceiver)
        unregisterReceiver(discoverDeviceReceiver)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 105
        private val MY_UUID = UUID.fromString("fa0a016f-e392-420d-8c0f-26c33053cb9d")
    }
}

class ScanModeBroadcastReceiver : BroadcastReceiver() {
    var statusView: TextView? = null
    override fun onReceive(context: Context, intent: Intent) {
        println("action: ScanModeChanged")
        if (intent.action == BluetoothAdapter.ACTION_SCAN_MODE_CHANGED) {
            val mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR)
            when (mode) {
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> statusView?.text = "Discoverable"
                BluetoothAdapter.SCAN_MODE_CONNECTABLE -> statusView?.text = "Connectable"
                BluetoothAdapter.SCAN_MODE_NONE -> statusView?.text = "None"
                else -> statusView?.text = "Error"
            }
        }
    }
}

class DiscoverDeviceReceiver(private val foundDeviceAdapter: FoundDeviceAdapter) : BroadcastReceiver() {
    private val devices = mutableListOf<BluetoothDevice>()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        println("action: in discovery receiver $action")
        when (action) {
            ACTION_DISCOVERY_STARTED -> {
                devices.clear()
            }
            BluetoothDevice.ACTION_FOUND -> {
                // Discovery has found a device. Get the BluetoothDevice object and its info from the Intent.
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let { blueToothDevice ->
                    if (devices.find { it.address == blueToothDevice.address } == null) {
                        devices.add(blueToothDevice)
                        println("action: devices = $devices")
                        foundDeviceAdapter.submitList(mutableListOf<BluetoothDevice>().apply {
                            addAll(devices)
                        })
                    }
                }
                val deviceName = device?.name
                println("action: Found bluetooth device $device")
                val deviceHardwareAddress = device?.address
//                todo to something with device
            }
        }
    }
}