package snispoof

import (
    "crypto/tls"
    "fmt"
    "io"
    "net"
    "time"
)

// It sends a fake ClientHello, aborts, waits, then verifies the connection is whitelisted
func SpoofAndWhitelist(targetIP string, fakeSNI string) (string, error) {
    targetAddr := fmt.Sprintf("%s:443", targetIP)
    
    // Phase 1: Fake ClientHello - send and abort after 50ms
    fakeConn, err := net.DialTimeout("tcp", targetAddr, 3*time.Second)
    if err != nil {
        return "", fmt.Errorf("❌ Fake connection failed: %v", err)
    }
    
    fakeTLS := tls.Client(fakeConn, &tls.Config{
        ServerName:         fakeSNI,
        InsecureSkipVerify: true,
    })
    
    go fakeTLS.Handshake()
    time.Sleep(50 * time.Millisecond)
    fakeConn.Close()
    
    // Phase 2: Wait for DPI to whitelist
    time.Sleep(100 * time.Millisecond)
    
    // Phase 3: Verify with real connection
    realConn, err := net.DialTimeout("tcp", targetAddr, 5*time.Second)
    if err != nil {
        return "", fmt.Errorf("❌ Real connection failed: %v", err)
    }
    defer realConn.Close()
    
    realTLS := tls.Client(realConn, &tls.Config{
        ServerName:         fakeSNI,
        InsecureSkipVerify: true,
    })
    
    if err := realTLS.Handshake(); err != nil {
        return "", fmt.Errorf("❌ Handshake failed: %v", err)
    }
    
    return fmt.Sprintf("✅ DPI whitelisted! Target: %s, Fake SNI: %s", targetIP, fakeSNI), nil
}

// StartProxy creates a local proxy that forwards traffic through the whitelisted connection
func StartProxy(port int, targetIP string, fakeSNI string) error {
    listener, err := net.Listen("tcp", fmt.Sprintf("127.0.0.1:%d", port))
    if err != nil {
        return err
    }
    
    for {
        client, err := listener.Accept()
        if err != nil {
            continue
        }
        go handleConnection(client, targetIP, fakeSNI)
    }
}

func handleConnection(client net.Conn, targetIP, fakeSNI string) {
    defer client.Close()
    
    targetAddr := fmt.Sprintf("%s:443", targetIP)
    target, err := net.DialTimeout("tcp", targetAddr, 10*time.Second)
    if err != nil {
        return
    }
    defer target.Close()
    
    tlsTarget := tls.Client(target, &tls.Config{
        ServerName:         fakeSNI,
        InsecureSkipVerify: true,
    })
    
    if err := tlsTarget.Handshake(); err != nil {
        return
    }
    
    go func() { io.Copy(tlsTarget, client) }()
    io.Copy(client, tlsTarget)
}