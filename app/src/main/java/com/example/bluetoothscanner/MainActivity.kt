package com.example.bluetoothscanner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothscanner.databinding.ActivityMainBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var btScanner: BluetoothLeScanner? = null
    private var isScanning = false
    private val rssiArray = ArrayList<String>()
    private val deviceList = ArrayList<BluetoothModel>()
    private val scannedDevices = HashMap<String, ArrayList<Int>>()
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var recyclerViewAdapter: BtAdapter

    //Firebase
    private lateinit var database: DatabaseReference

    lateinit var binding: ActivityMainBinding

    private lateinit var locationManager: LocationManager

    companion object {
        const val PERMISSIONS_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        //Firebase
        database = Firebase.database.reference

        // Get the BluetoothAdapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        btScanner = bluetoothAdapter?.bluetoothLeScanner


        // Set up the RecyclerView adapter
        recyclerViewAdapter = BtAdapter(deviceList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = recyclerViewAdapter

        //GPS
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Set up the handler and runnable for periodic updates
        handler = Handler()
        runnable = Runnable {
            if (isScanning) {
                //scanForDevices()
                if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    // ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    // public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.BLUETOOTH_SCAN
                        ),
                        PERMISSIONS_REQUEST_CODE
                    )
                    //return
                }
                btScanner!!.startScan(leScanCallback)
                handler.postDelayed(runnable, 2000) // Update every 2 seconds
            }
        }

        // Set up the button click listeners
        binding.startButton.setOnClickListener {
            if (checkPer()) {
                if (bluetoothAdapter?.isEnabled == true) {
                    startScanning()
                } else {
                    Toast.makeText(this, "TURN ON YOUR BLUETOOTH & LOCATION", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    ),
                    PERMISSIONS_REQUEST_CODE
                )
            }
        }
        binding.stopButton.setOnClickListener {
            Log.d("Taggy", "Stop button working")
            stopScanning()
        }
    }

    private fun startScanning() {
        isScanning = true
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            // ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            // public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ),
                PERMISSIONS_REQUEST_CODE
            )
        }
        //scanForDevices()
        btScanner!!.startScan(leScanCallback)
        handler.postDelayed(runnable, 2000) // Update every 2 seconds
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        isScanning = false
        handler.removeCallbacks(runnable)
        btScanner!!.stopScan(leScanCallback)
        for (device in deviceList) {
            val devicesRef = database.child("Device")
            // Save a new user to the database
            val deviceRef = devicesRef.child(device.mac)
            deviceRef.setValue(device)
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val scanRecord = result.scanRecord
            if (scannedDevices.containsKey(result.device.address)) {
                // Add the new RSSI value to the existing list
                scannedDevices[result.device.address]!!.add(result.rssi)

                // Create a new BluetoothDevice object with the updated RSSI list
                val btDevice =
                    BluetoothModel(result.device.address, scannedDevices[result.device.address]!!)

                // Check if the device is already in the device list
                if (!deviceList.contains(btDevice)) {
                    // Add the new device to the device list
                    deviceList.add(btDevice)
                }
            } else {
                // Create a new RSSI list for the new device
                val rssiList = arrayListOf(result.rssi)

                // Add the new device to the scanned devices list
                scannedDevices[result.device.address] = rssiList

                // Create a new BluetoothDevice object with the RSSI list
                val btDevice = BluetoothModel(result.device.address, rssiList)

                // Add the new device to the device list
                deviceList.add(btDevice)
            }
            binding.recyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun checkPer(): Boolean {
        var resultLoc: Int = ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        var resultBtConnect: Int = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.BLUETOOTH_CONNECT
        )
        var resultBtScan: Int = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.BLUETOOTH_SCAN
        )
        return resultLoc == PackageManager.PERMISSION_GRANTED && resultBtConnect == PackageManager.PERMISSION_GRANTED && resultBtScan == PackageManager.PERMISSION_GRANTED

    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            var allPermissionsGranted = true

            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }

            if (allPermissionsGranted) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
                if (bluetoothAdapter?.isEnabled == true) {
                    startScanning()
                } else {
                    Toast.makeText(
                        this,
                        "TURN ON YOUR BLUETOOTH & LOCATION",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}