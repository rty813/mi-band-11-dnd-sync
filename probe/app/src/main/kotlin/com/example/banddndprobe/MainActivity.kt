package com.example.banddndprobe

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import java.util.UUID

class MainActivity : Activity() {
  private lateinit var statusView: TextView
  @Volatile private var socket: BluetoothSocket? = null
  private var gatt: BluetoothGatt? = null
  private var scanning = false
  private var band: BluetoothDevice? = null
  private var advertisedBand: BluetoothDevice? = null
  private val seenCandidates = mutableSetOf<String>()
  private var scanCount = 0
  private val logLock = Any()
  private val handler = Handler(Looper.getMainLooper())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    statusView = TextView(this).apply {
      textSize = 16f
      setPadding(32, 48, 32, 32)
      text = "Band 11 connection probe\n"
    }
    setContentView(ScrollView(this).apply { addView(statusView) })

    val requiredPermissions = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    if (requiredPermissions.any { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
      requestPermissions(requiredPermissions, REQUEST_BLUETOOTH)
    } else {
      connectToBand()
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_BLUETOOTH && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
      connectToBand()
    } else {
      append("Bluetooth permission denied")
    }
  }

  private fun connectToBand() {
    val manager = getSystemService(BluetoothManager::class.java)
    val existingConnections = manager.getConnectedDevices(BluetoothProfile.GATT)
    append("Existing system GATT connections: ${existingConnections.size}")

    band = manager.adapter.bondedDevices.firstOrNull {
      it.name?.contains("Smart Band 11", ignoreCase = true) == true
    }
    val device = band
    if (device == null) {
      append("No paired Xiaomi Smart Band 11 found")
      return
    }

    append("Found paired Band 11")
    append("Test label=" + intent.getStringExtra("label") + " mode=" + intent.getStringExtra("mode"))
    if (intent.getStringExtra("mode") == "ble") {
      scanBle()
      return
    }
    append("Opening RFCOMM/SPP socket without sending data")
    Thread({ connectSpp(device) }, "spp-probe").start()
  }

  private fun connectSpp(device: BluetoothDevice) {
    var candidate: BluetoothSocket? = null
    try {
      candidate = device.createRfcommSocketToServiceRecord(SPP_UUID)
      socket = candidate
      val timeoutSocket = candidate
      handler.postDelayed({
        if (socket === timeoutSocket) {
          append("SPP probe timeout, closing its socket")
          closeSocket()
        }
      }, 12_000L)
      candidate.connect()
      append("SPP connected successfully. No protocol data was sent")
      Thread.sleep(RESULT_DISPLAY_MS)
    } catch (error: Exception) {
      append("SPP connect failed: ${error.javaClass.simpleName}")
    } finally {
      try {
        candidate?.close()
      } catch (_: Exception) {
      }
      socket = null
      append("SPP probe socket closed")
    }
  }

  private fun scanBle() {
    val scanner = getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner
    if (scanner == null) {
      append("BLE scanner unavailable")
      return
    }
    scanning = true
    append("Starting 15-second BLE scan")
    scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback)
    handler.postDelayed({
      stopScan()
      append("BLE scan complete, observations=" + scanCount + " candidates=" + seenCandidates.size)
      val target = advertisedBand
      if (target == null) {
        append("No advertisement identified as the paired Band 11")
      } else {
        append("Opening GATT using the address observed in advertising")
        gatt = target.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        handler.postDelayed({ closeGatt() }, 15_000L)
      }
    }, 15_000L)
  }

  private val scanCallback = object : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult) {
      scanCount++
      val record = result.scanRecord
      val name = record?.deviceName ?: result.device.name.orEmpty()
      val exactBand = result.device.address == band?.address || name.contains("Band 11", ignoreCase = true)
      val xiaomiService = record?.serviceUuids.orEmpty().any { it.toString().startsWith("0000fe95-") }
      if (exactBand) advertisedBand = result.device
      if ((exactBand || xiaomiService) && seenCandidates.add(result.device.address)) {
        append("BLE candidate: band=$exactBand rssi=${result.rssi} services=${record?.serviceUuids}")
      }
    }

    override fun onScanFailed(errorCode: Int) {
      append("BLE scan failed code=" + errorCode)
    }
  }

  private val gattCallback = object : BluetoothGattCallback() {
    override fun onConnectionStateChange(connection: BluetoothGatt, status: Int, newState: Int) {
      append("GATT status=" + status + " state=" + newState)
      if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
        connection.discoverServices()
      }
    }

    override fun onServicesDiscovered(connection: BluetoothGatt, status: Int) {
      append("GATT discovery status=" + status)
      connection.services.forEach { service ->
        append("GATT service " + service.uuid)
        service.characteristics.forEach { characteristic ->
          append("GATT characteristic " + characteristic.uuid + " properties=" + characteristic.properties)
        }
      }
      handler.postDelayed({ closeGatt() }, 1_000L)
    }
  }

  private fun stopScan() {
    if (scanning) {
      getSystemService(BluetoothManager::class.java).adapter.bluetoothLeScanner?.stopScan(scanCallback)
      scanning = false
    }
  }

  private fun closeGatt() {
    gatt?.let {
      it.disconnect()
      it.close()
      append("GATT probe closed")
    }
    gatt = null
  }

  private fun closeSocket() {
    socket?.let {
      try {
        it.close()
      } catch (_: Exception) {
      }
    }
    socket = null
  }

  private fun append(message: String) {
    Log.i(TAG, message)
    synchronized(logLock) {
      openFileOutput("probe.log", MODE_APPEND).bufferedWriter().use {
        it.appendLine(System.currentTimeMillis().toString() + " " + message)
      }
    }
    runOnUiThread { statusView.append("$message\n") }
  }

  override fun onDestroy() {
    handler.removeCallbacksAndMessages(null)
    stopScan()
    closeGatt()
    closeSocket()
    super.onDestroy()
  }

  companion object {
    private const val TAG = "BandDndProbe"
    private const val REQUEST_BLUETOOTH = 1
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val RESULT_DISPLAY_MS = 3_000L
  }
}
