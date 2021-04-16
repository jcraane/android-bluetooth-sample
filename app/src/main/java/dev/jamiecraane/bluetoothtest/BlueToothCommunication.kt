package dev.jamiecraane.bluetoothtest

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.IOException
import java.nio.charset.Charset

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
}