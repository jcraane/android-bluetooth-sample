package dev.jamiecraane.bluetoothtest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

/**
 * User: jamiecraane
 * Date: 16/04/2021
 */
class BlueToothCommunication(private val socket: BluetoothSocket) {
    private val inputStream = socket.inputStream
    private val outputStream = socket.outputStream
    private val buffer: ByteArray = ByteArray(1024)

    suspend fun start() = flow {
        try {
            while (true) {
                val bytes = inputStream.read(buffer)
                emit(String(buffer.copyOfRange(0, bytes)))
            }
        } catch (e: CancellationException) {
            socket.close()
            throw e
        }
    }.flowOn(Dispatchers.IO)

    fun write(msg: String) {
        try {
            outputStream.write(msg.toByteArray(Charset.forName("UTF-8")))
        } catch (e: IOException) {

        }
    }

    fun stop() {
        socket.close()
    }

    companion object {
        suspend fun openServerSocketAndListenerForIncomingConnections(bluetoothAdapter: BluetoothAdapter, uuid: UUID): BluetoothSocket? = withContext(
            Dispatchers.IO
        ) {
            val serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Hably", uuid)
            var shouldLoop = true
            var socket: BluetoothSocket? = null
            while (shouldLoop && isActive) {
                socket = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    throw IllegalStateException("ServerSocket accept failed")
                }
                println("Got socket: $socket")
                socket?.also { socket ->
                    /*startListeningForIncomingMessages(socket)
                    this@MainActivity.blueToothSocket = socket*/
                    serverSocket?.close()
                    shouldLoop = false
                }
            }

            socket
        }

        suspend fun connect(bluetoothAdapter: BluetoothAdapter, bluetoothDevice: BluetoothDevice, uuid: UUID): BluetoothSocket? = withContext(Dispatchers.IO) {
            val socket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid)
            bluetoothAdapter.cancelDiscovery()
            try {
                @Suppress("BlockingMethodInNonBlockingContext")
                socket.connect()
            } catch (e: IOException) {
                throw IllegalStateException("Unable to connect")
            }

            socket
        }
    }
}