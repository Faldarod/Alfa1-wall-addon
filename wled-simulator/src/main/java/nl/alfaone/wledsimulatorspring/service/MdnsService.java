package nl.alfaone.wledsimulatorspring.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Service to broadcast WLED device via mDNS/Zeroconf for Home Assistant discovery
 */
@Service
@Slf4j
public class MdnsService {

    @Value("${server.port:8080}")
    private int serverPort;

    private JmDNS jmdns;
    private ServiceInfo serviceInfo;

    @PostConstruct
    public void registerService() {
        try {
            log.info("Starting mDNS service registration for WLED simulator...");

            // Create JmDNS instance
            jmdns = JmDNS.create(InetAddress.getLocalHost());

            // Register WLED service
            // Service type: _wled._tcp.local.
            // This is what Home Assistant looks for when discovering WLED devices
            serviceInfo = ServiceInfo.create(
                    "_wled._tcp.local.",
                    "AlfaWall-LED-Simulator",
                    serverPort,
                    "WLED Simulator for AlfaWall"
            );

            jmdns.registerService(serviceInfo);

            log.info("✓ mDNS service registered successfully!");
            log.info("  Service: _wled._tcp.local.");
            log.info("  Name: AlfaWall-LED-Simulator");
            log.info("  Port: {}", serverPort);
            log.info("  Home Assistant should now be able to auto-discover this WLED device");

        } catch (IOException e) {
            log.error("Failed to register mDNS service", e);
            log.warn("The WLED simulator will still work, but won't be auto-discovered by Home Assistant");
            log.warn("You can manually add it using the IP address and port");
        }
    }

    @PreDestroy
    public void unregisterService() {
        if (jmdns != null) {
            try {
                if (serviceInfo != null) {
                    jmdns.unregisterService(serviceInfo);
                    log.info("mDNS service unregistered");
                }
                jmdns.close();
                log.info("mDNS service closed");
            } catch (Exception e) {
                log.error("Error closing mDNS service", e);
            }
        }
    }
}
