package com.snispoof

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private val VPN_REQUEST_CODE = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple UI
        statusText = TextView(this).apply {
            text = "Status: Not started\nTarget: 104.19.229.21\nFake SNI: www.hcaptcha.com"
            textSize = 16f
            setPadding(50, 50, 50, 20)
        }
        
        startButton = Button(this).apply {
            text = "Start SNI Spoof"
            setOnClickListener { startVpn() }
        }
        
        setContentView(startButton)
        addContentView(statusText, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        ))
    }
    
    private fun startVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startSniSpoofService()
        }
    }
    
    private fun startSniSpoofService() {
        val intent = Intent(this, SniVpnService::class.java)
        startService(intent)
        statusText.text = "Status: Running - Performing SNI spoof..."
        Toast.makeText(this, "SNI Spoof Service Started", Toast.LENGTH_SHORT).show()
    }
}