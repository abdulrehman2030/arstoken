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
    private val sppUuid: UUID = UUID.fromString(
        "00001101-0000-1000-8000-00805F9B34FB"
    )

    data class PrintResult(
        val success: Boolean,
        val message: String
    )

    data class PrintOptions(
        val bottomPaddingLines: Int = 1,
        val spacingFix: Boolean = false
    )

    private fun isLikelyPrinter(device: BluetoothDevice): Boolean {
        val name = device.name?.lowercase(Locale.getDefault()) ?: return false
        return name.contains("pos") ||
            name.contains("printer") ||
            name.contains("thermal") ||
            name.contains("58") ||
            name.contains("80")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectWithFallbacks(device: BluetoothDevice): Boolean {
        // Standard SPP socket first.
        runCatching {
            socket = device.createRfcommSocketToServiceRecord(sppUuid)
            socket?.connect()
            output = socket?.outputStream
            return true
        }.onFailure {
            disconnect()
        }

        // Some POS80 variants connect only via insecure RFCOMM.
        runCatching {
            socket = device.createInsecureRfcommSocketToServiceRecord(sppUuid)
            socket?.connect()
            output = socket?.outputStream
            return true
        }.onFailure {
            disconnect()
        }

        // Final legacy fallback.
        return runCatching {
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            socket = method.invoke(device, 1) as BluetoothSocket
            socket?.connect()
            output = socket?.outputStream
            true
        }.getOrElse {
            disconnect()
            false
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice): Boolean {
        return connectWithFallbacks(device)
    }

    fun print(
        text: String,
        options: PrintOptions = PrintOptions()
    ) {
        val out = output ?: return
        out.write(byteArrayOf(0x1B, 0x40)) // ESC @ initialize
        if (options.spacingFix) {
            out.write(byteArrayOf(0x1B, 0x32)) // normalize line spacing
        }

        text.split('\n').forEach { raw ->
            var line = raw

            val align = when {
                line.contains("{C}") -> 1
                line.contains("{R}") -> 2
                else -> 0
            }
            val bold = line.contains("{B}")
            val doubleSize = line.contains("{W2}")

            line = line
                .replace("{L}", "")
                .replace("{C}", "")
                .replace("{R}", "")
                .replace("{B}", "")
                .replace("{/B}", "")
                .replace("{W2}", "")
                .replace("{W1}", "")

            // ESC a n -> alignment (0 left, 1 center, 2 right)
            out.write(byteArrayOf(0x1B, 0x61, align.toByte()))
            // ESC E n -> bold
            out.write(byteArrayOf(0x1B, 0x45, if (bold) 1 else 0))
            // GS ! n -> character size
            out.write(byteArrayOf(0x1D, 0x21, if (doubleSize) 0x11 else 0x00))

            out.write(line.toByteArray(Charsets.UTF_8))
            out.write(byteArrayOf(0x0A))
        }

        // Reset to defaults and keep feed tight.
        out.write(byteArrayOf(0x1B, 0x61, 0x00))
        out.write(byteArrayOf(0x1B, 0x45, 0x00))
        out.write(byteArrayOf(0x1D, 0x21, 0x00))
        repeat(options.bottomPaddingLines.coerceIn(0, 10)) {
            out.write(byteArrayOf(0x0A))
        }
        output?.flush()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun printToPairedPrinter(
        text: String,
        preferredAddress: String? = null,
        options: PrintOptions = PrintOptions()
    ): PrintResult {
        return try {
            val btAdapter = adapter ?: return PrintResult(false, "Bluetooth not supported")
            if (!btAdapter.isEnabled) {
                return PrintResult(false, "Bluetooth is OFF")
            }

            val bonded = btAdapter.bondedDevices
            if (bonded.isEmpty()) {
                return PrintResult(false, "No paired Bluetooth devices")
            }

            val preferred = preferredAddress?.let { address ->
                bonded.firstOrNull { it.address == address }
            }
            val device = preferred
                ?: bonded.firstOrNull(::isLikelyPrinter)
                ?: bonded.first()
            if (!connect(device)) {
                return PrintResult(
                    false,
                    "Connection failed: ${device.name ?: device.address}"
                )
            }
            print(text, options)
            PrintResult(
                true,
                "Printed on ${device.name ?: device.address}"
            )
        } catch (e: Exception) {
            PrintResult(false, e.message ?: "Unknown print error")
        } finally {
            disconnect()
        }
    }

    fun disconnect() {
        output?.close()
        socket?.close()
    }
}
