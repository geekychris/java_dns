package com.example.javadns.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "dns")
public class DnsConfig {

    private String upstreamDnsServer = "8.8.8.8"; // Default Google DNS
    private int cacheExpirationSeconds = 300; // Default 5 minutes
    private int serverPort = 5353; // DNS server port

    public String getUpstreamDnsServer() { return upstreamDnsServer; }
    public void setUpstreamDnsServer(String upstreamDnsServer) { this.upstreamDnsServer = upstreamDnsServer; }
    public int getCacheExpirationSeconds() { return cacheExpirationSeconds; }
    public void setCacheExpirationSeconds(int cacheExpirationSeconds) { this.cacheExpirationSeconds = cacheExpirationSeconds; }
    public int getServerPort() { return serverPort; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }
}
