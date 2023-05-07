package com.example.bluetoothscanner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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

class MainActivity : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isScanning = false
    private val rssiArray = ArrayList<String>()
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val deviceList = ArrayList<BluetoothModel>()
    private lateinit var recyclerViewAdapter: BtAdapter

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestEnableBtLauncher: ActivityResultLauncher<Intent>

    lateinit var binding: ActivityMainBinding

    private lateinit var locationManager: LocationManager

    companion object {
        const val PERMISSIONS_REQUEST_CODE = 123
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // Get the BluetoothAdapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

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
                scanForDevices()
                handler.postDelayed(runnable, 2000) // Update every 2 seconds
            }
        }

        // Set up the button click listeners
        binding.startButton.setOnClickListener {
            if (checkPer()){
                if (bluetoothAdapter?.isEnabled == true){
                    startScanning()
                }else {
                    Toast.makeText(this, "TURN ON YOUR BLUETOOTH & LOCATION", Toast.LENGTH_SHORT)
                        .show()
                }
            }else{
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN),
                    PERMISSIONS_REQUEST_CODE
                    )
            }

        }
        binding.stopButton.setOnClickListener {
            stopScanning()
        }
    }

    private fun startScanning() {
        isScanning = true
        scanForDevices()
        handler.postDelayed(runnable, 2000) // Update every 2 seconds
    }

    private fun stopScanning() {
        isScanning = false
        handler.removeCallbacks(runnable)
    }

    @SuppressLint("MissingPermission")
    private fun scanForDevices() {
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
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN),
                PERMISSIONS_REQUEST_CODE
            )
            return
        }
        bluetoothAdapter?.startDiscovery()
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val rssi = device.bluetoothClass.majorDeviceClass.toString()
            val macAddress = device.address
            val btDevice = BluetoothModel(macAddress, rssi)
            //val deviceInfo = "RSSI: $rssi MAC: $macAddress"
            if (!deviceList.contains(btDevice)){
                deviceList.add(btDevice)
            }
            /*if (!rssiArray.contains(deviceInfo)) {
                rssiArray.add(deviceInfo)
            }*/
        }
        binding.recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun checkPer() : Boolean{
        var resultLoc : Int = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        var resultBtConnect : Int = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
        var resultBtScan : Int = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN)
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
                if (bluetoothAdapter?.isEnabled == true){
                    startScanning()
                }else{
                    Toast.makeText(this, "TURN ON YOUR BLUETOOTH & LOCATION", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}