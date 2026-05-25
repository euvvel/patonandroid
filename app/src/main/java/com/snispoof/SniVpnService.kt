package com.snispoof

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import snispoof.Snispoof
import kotlin.concurrent.thread

class SniVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    
    override fun onCreate() {
        super.onCreate()
        setupVpn()
        startSpoofing()
    }
    
    private fun setupVpn() {
        val builder = Builder()
        builder.setSession("SNI Spoof VPN")
        builder.addAddress("10.0.0.2", 32)
        builder.addRoute("0.0.0.0", 0)
        builder.addDnsServer("8.8.8.8")
        builder.addDnsServer("1.1.1.1")
        builder.setBlocking(true)
        
        vpnInterface = builder.establish()
        Log.d("SniVpn", "VPN interface established")
    }
    
    private fun startSpoofing() {
        thread {
            try {
                // Your exact tested pattern
                val targetIP = "104.19.229.21"
                val fakeSNI = "www.hcaptcha.com"
                
                Log.d("SniVpn", "Phase 1: Sending fake ClientHello with SNI: $fakeSNI")
                val result = Snispoof.spoofAndWhitelist(targetIP, fakeSNI)
                Log.d("SniVpn", result)
                
                // Start local proxy on port 8443
                Log.d("SniVpn", "Starting local proxy on port 8443")
                Snispoof.startProxy(8443, targetIP, fakeSNI)
                
            } catch (e: Exception) {
                Log.e("SniVpn", "SNI spoof failed", e)
            }
        }
    }
    
    override fun onDestroy() {
        vpnInterface?.close()
        isRunning = false
        super.onDestroy()
    }
}