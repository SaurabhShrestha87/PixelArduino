package com.example.wifi3

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.NetworkOnMainThreadException
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.snackbar.Snackbar
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.net.UnknownHostException

class MainActivity : AppCompatActivity() {

    private lateinit var view: View
    private lateinit var wifiStateReceiver: BroadcastReceiver
    private lateinit var progressBar: ProgressBar
    private lateinit var sendDataEditText: EditText
    private lateinit var sendDataButton: Button
    private lateinit var wifiConnectionSwitch: SwitchCompat
    private lateinit var arduinoConnectionSwitch: SwitchCompat

    companion object {
        const val IP = "10.1.10.134"
        const val PORT = 80
    }

    var socket: Socket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        view = findViewById(R.id.container) // Assuming you have a container in your layout

        wifiConnectionSwitch = findViewById(R.id.wifiConnectionSwitch)
        arduinoConnectionSwitch = findViewById(R.id.arduinoConnectionSwitch)
        progressBar = findViewById(R.id.progressBar)
        sendDataEditText = findViewById(R.id.sendDataEditText)
        sendDataButton = findViewById(R.id.sendDataButton)

        wifiStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
                    val networkInfo: NetworkInfo? =
                        intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)
                    if (networkInfo?.isConnected == true && networkInfo.typeName.contains("WIFI") ) {
                        // Wifi is connected
                        Snackbar.make(view, "WiFi is connected", Snackbar.LENGTH_LONG)
                            .setAction("DISMISS") {}.show()
                        wifiConnectionSwitch.isChecked = true
                    } else {
                        Snackbar.make(view, "WiFi is dis-Connected", Snackbar.LENGTH_LONG)
                            .setAction("DISMISS") {}.show()
                        wifiConnectionSwitch.isChecked = false
                    }
                    Log.e("TAG", "onReceive: ${networkInfo.toString()}", )
                }
            }
        }

        arduinoConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if(!connectToArduino()) {
                    arduinoConnectionSwitch.isChecked = false
                }
            }else {
                disconnectArduino()
            }
        }

        sendDataButton.setOnClickListener {
            val message = sendDataEditText.text.toString()
            showProgressBar()
            try {
                if (socket != null) {
                    val out = PrintWriter(
                        BufferedWriter(OutputStreamWriter(socket!!.getOutputStream())), true
                    )
                    out.println(message)
                    out.close()
                } else {
                    if(arduinoConnectionSwitch.isEnabled)
                        Snackbar.make(view, "Socket was not connected properly", Snackbar.LENGTH_LONG).show()
                    else
                        Snackbar.make(view, "Connect To Arduino First", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                hideProgressBar()
            }
        }
    }

    private fun connectToArduino(): Boolean {
        return try {
            socket = Socket(IP, PORT)
            Snackbar.make(view, "Connected to $IP:$PORT", Snackbar.LENGTH_LONG).show()
            return  true
        } catch (e : Exception) {
            Snackbar.make(view, "ERROR : $e", Snackbar.LENGTH_LONG).show()
            false
        }
    }

    private fun disconnectArduino() {
        try {
            if(socket != null) {
                socket!!.close()
                Snackbar.make(view, "Disconnected Socket", Snackbar.LENGTH_LONG).show()
            } else {
                Snackbar.make(view, "Socket was not connected properly", Snackbar.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Snackbar.make(view, "ERROR : $e", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.INVISIBLE
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(wifiStateReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(wifiStateReceiver)
    }
}
