package com.ar.arstoken.print

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.annotation.RequiresPermission
import java.io.OutputStream
import java.util.*

class BluetoothPrinterManager {

    private val adapter = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun getPairedPrinters(): List<BluetoothDevice> {
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            val uuid = device.uuids.first().uuid
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()
            outputStream = socket?.outputStream
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun print(text: String) {
        outputStream?.write(text.toByteArray())
    }

    fun cut() {
        outputStream?.write(byteArrayOf(0x1D, 0x56, 0x41, 0x10))
    }

    fun close() {
        outputStream?.close()
        socket?.close()
    }
}
