package com.ar.arstoken.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.annotation.RequiresPermission
import java.io.OutputStream
import java.util.*

class ThermalPrinterHelper {

    private val adapter = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var output: OutputStream? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            val uuid =
                device.uuids.firstOrNull()?.uuid
                    ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()
            output = socket?.outputStream
            true
        } catch (e: Exception) {
            false
        }
    }

    fun print(text: String) {
        output?.write(text.toByteArray())
        output?.write(byteArrayOf(0x0A)) // line feed
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun printToPairedPrinter(text: String): Boolean {
        return try {
            val device = adapter?.bondedDevices?.firstOrNull() ?: return false
            if (!connect(device)) return false
            print(text)
            true
        } catch (_: Exception) {
            false
        } finally {
            disconnect()
        }
    }

    fun disconnect() {
        output?.close()
        socket?.close()
    }
}
