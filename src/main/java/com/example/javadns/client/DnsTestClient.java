package com.example.javadns.client;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;

public class DnsTestClient {

    private static final Logger log = LoggerFactory.getLogger(DnsTestClient.class);

    private final String dnsServerHost;
    private final int dnsServerPort;

    public DnsTestClient(String dnsServerHost, int dnsServerPort) {
        this.dnsServerHost = dnsServerHost;
        this.dnsServerPort = dnsServerPort;
    }

    public static void main(String[] args) throws Exception {
        String serverHost = args.length > 0 ? args[0] : "localhost";
        int serverPort = args.length > 1 ? Integer.parseInt(args[1]) : 5353;
        
        DnsTestClient client = new DnsTestClient(serverHost, serverPort);
        client.runTests();
    }

    public void runTests() throws Exception {
        log.info("Starting DNS client tests against {}:{}", dnsServerHost, dnsServerPort);

        // Test cases
        testQuery("google.com");
        testQuery("github.com");
        testQuery("localhost");
        testQuery("test.example.com");
        testQuery("*.example.com");
        testQuery("nonexistent.domain.com");

        // Performance test
        performanceTest("google.com", 100);

        log.info("DNS client tests completed");
    }

    public void testQuery(String hostname) {
        try {
            log.info("Testing query for: {}", hostname);
            Instant start = Instant.now();
            
            Lookup lookup = new Lookup(hostname, Type.A);
            SimpleResolver resolver = new SimpleResolver(dnsServerHost);
            resolver.setPort(dnsServerPort);
            lookup.setResolver(resolver);
            
            Record[] records = lookup.run();
            Duration duration = Duration.between(start, Instant.now());
            
            if (records != null && records.length > 0) {
                log.info("Query successful for {} ({}ms):", hostname, duration.toMillis());
                for (Record record : records) {
                    if (record instanceof ARecord) {
                        ARecord aRecord = (ARecord) record;
                        log.info("  -> {}", aRecord.getAddress().getHostAddress());
                    }
                }
            } else {
                log.warn("No records found for {} ({}ms)", hostname, duration.toMillis());
            }
        } catch (Exception e) {
            log.error("Error querying {}: {}", hostname, e.getMessage());
        }
    }

    public void performanceTest(String hostname, int iterations) {
        log.info("Running performance test for {} with {} iterations", hostname, iterations);
        
        try {
            SimpleResolver resolver = new SimpleResolver(dnsServerHost);
            resolver.setPort(dnsServerPort);
            
            Instant overallStart = Instant.now();
            int successCount = 0;
            long totalTime = 0;
            
            for (int i = 0; i < iterations; i++) {
                Instant start = Instant.now();
                
                try {
                    Lookup lookup = new Lookup(hostname, Type.A);
                    lookup.setResolver(resolver);
                    Record[] records = lookup.run();
                    
                    Duration queryTime = Duration.between(start, Instant.now());
                    totalTime += queryTime.toMillis();
                    
                    if (records != null && records.length > 0) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.debug("Query {} failed: {}", i, e.getMessage());
                }
            }
            
            Duration overallTime = Duration.between(overallStart, Instant.now());
            
            log.info("Performance test results:");
            log.info("  Total queries: {}", iterations);
            log.info("  Successful queries: {}", successCount);
            log.info("  Success rate: {:.2f}%", (successCount * 100.0) / iterations);
            log.info("  Average query time: {:.2f}ms", (double) totalTime / iterations);
            log.info("  Total test time: {}ms", overallTime.toMillis());
            log.info("  Queries per second: {:.2f}", iterations * 1000.0 / overallTime.toMillis());
            
        } catch (Exception e) {
            log.error("Performance test failed", e);
        }
    }
    
    public static class Builder {
        private String dnsServerHost = "localhost";
        private int dnsServerPort = 5353;
        
        public Builder host(String host) {
            this.dnsServerHost = host;
            return this;
        }
        
        public Builder port(int port) {
            this.dnsServerPort = port;
            return this;
        }
        
        public DnsTestClient build() {
            return new DnsTestClient(dnsServerHost, dnsServerPort);
        }
    }
}
