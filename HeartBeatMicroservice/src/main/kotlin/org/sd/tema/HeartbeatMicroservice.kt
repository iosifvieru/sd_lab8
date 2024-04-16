package org.sd.tema

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Date
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class HeartbeatMicroservice {
    private lateinit var messageManagerSocket: Socket

    private val microservices: HashMap<Int, Date>

    companion object Constants {
        val MESSAGE_MANAGER_HOST = System.getenv("MESSAGE_MANANGER_HOST") ?: "localhost"
        const val MESSAGE_MANAGER_PORT = 1500
        const val SOCKET_TIMEOUT = 15000 // 15 sec.
    }

    init {
        microservices = hashMapOf()
    }

    private fun subscribeToMessageManager(){
        try {
            messageManagerSocket = Socket(MESSAGE_MANAGER_HOST, MESSAGE_MANAGER_PORT)
            println("[HEARTBEAT]: M-am conectat cu succes la MessageManager!")
        } catch (e: Exception){
            println("[HEARTBEAT]: Nu ma pot conecta la MessageManager!")
            exitProcess(1)
        }
    }

    private fun sendDummyMessage(){
        thread {
            while (true) {
                val message = "dummy ${messageManagerSocket.localPort} are you up?\n"
                try {
                    messageManagerSocket.getOutputStream().write(message.toByteArray())
                    messageManagerSocket.getOutputStream().flush()

                    println("Am trimis: $message")
                    Thread.sleep(5000)
                } catch(e: Exception){
                    println(e)
                    exitProcess(1)
                }
            }
        }
    }

    private fun recieveMessage(){
        messageManagerSocket.soTimeout = SOCKET_TIMEOUT;
        thread {
            val bufferReader = BufferedReader(InputStreamReader(messageManagerSocket.inputStream))
            while (true) {
                try {
                    val recievedMessage = bufferReader.readLine()

                    if (recievedMessage == null) {
                        println("S-a pierdut conexiunea catre MessageManager.")
                        bufferReader.close()
                        messageManagerSocket.close()
                        break;
                    }
                    println("Mesaj primit: $recievedMessage")
                } catch(e: SocketTimeoutException){
                    println("Timeout: N-am primit nimic de 15 secunde.")
                    bufferReader.close();
                    messageManagerSocket.close()
                }
            }
        }
    }

    //
    public fun run(){
        subscribeToMessageManager()

        sendDummyMessage()

        recieveMessage()
    }
}



fun main(args: Array<String>) {
    val heartbeatMicroservice = HeartbeatMicroservice()
    heartbeatMicroservice.run()
}