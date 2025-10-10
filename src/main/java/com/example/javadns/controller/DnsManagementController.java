package com.example.javadns.controller;

import com.example.javadns.plugin.impl.InMemoryDnsPlugin;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dns")
public class DnsManagementController {

    private final InMemoryDnsPlugin inMemoryDnsPlugin;

    public DnsManagementController(InMemoryDnsPlugin inMemoryDnsPlugin) {
        this.inMemoryDnsPlugin = inMemoryDnsPlugin;
    }

    public static class DnsRecordRequest {
        private String hostname;
        private List<String> addresses;
        private InMemoryDnsPlugin.LoadBalancingStrategy strategy = InMemoryDnsPlugin.LoadBalancingStrategy.ROUND_ROBIN;

        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        public List<String> getAddresses() { return addresses; }
        public void setAddresses(List<String> addresses) { this.addresses = addresses; }
        public InMemoryDnsPlugin.LoadBalancingStrategy getStrategy() { return strategy; }
        public void setStrategy(InMemoryDnsPlugin.LoadBalancingStrategy strategy) { this.strategy = strategy; }
    }

    public static class StrategyUpdateRequest {
        private InMemoryDnsPlugin.LoadBalancingStrategy strategy;

        public InMemoryDnsPlugin.LoadBalancingStrategy getStrategy() { return strategy; }
        public void setStrategy(InMemoryDnsPlugin.LoadBalancingStrategy strategy) { this.strategy = strategy; }
    }

    @GetMapping("/records")
    public ResponseEntity<Map<String, List<String>>> getAllRecords() {
        return ResponseEntity.ok(inMemoryDnsPlugin.getAllRecords());
    }

    @GetMapping("/records/{hostname}")
    public ResponseEntity<List<String>> getRecord(@PathVariable String hostname) {
        List<String> addresses = inMemoryDnsPlugin.getRecord(hostname);
        if (addresses != null) {
            return ResponseEntity.ok(addresses);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/records")
    public ResponseEntity<String> addRecord(@RequestBody DnsRecordRequest request) {
        if (request.getHostname() == null || request.getAddresses() == null || request.getAddresses().isEmpty()) {
            return ResponseEntity.badRequest().body("Hostname and addresses are required");
        }

        inMemoryDnsPlugin.addRecord(request.getHostname(), request.getAddresses(), request.getStrategy());
        return ResponseEntity.ok("DNS record added successfully");
    }

    @PutMapping("/records/{hostname}")
    public ResponseEntity<String> updateRecord(@PathVariable String hostname, @RequestBody List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return ResponseEntity.badRequest().body("Addresses are required");
        }

        List<String> existing = inMemoryDnsPlugin.getRecord(hostname);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        inMemoryDnsPlugin.updateRecord(hostname, addresses);
        return ResponseEntity.ok("DNS record updated successfully");
    }

    @DeleteMapping("/records/{hostname}")
    public ResponseEntity<String> deleteRecord(@PathVariable String hostname) {
        List<String> existing = inMemoryDnsPlugin.getRecord(hostname);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        inMemoryDnsPlugin.removeRecord(hostname);
        return ResponseEntity.ok("DNS record deleted successfully");
    }

    @GetMapping("/records/{hostname}/strategy")
    public ResponseEntity<InMemoryDnsPlugin.LoadBalancingStrategy> getStrategy(@PathVariable String hostname) {
        List<String> existing = inMemoryDnsPlugin.getRecord(hostname);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        InMemoryDnsPlugin.LoadBalancingStrategy strategy = inMemoryDnsPlugin.getStrategy(hostname);
        return ResponseEntity.ok(strategy);
    }

    @PutMapping("/records/{hostname}/strategy")
    public ResponseEntity<String> updateStrategy(@PathVariable String hostname, @RequestBody StrategyUpdateRequest request) {
        List<String> existing = inMemoryDnsPlugin.getRecord(hostname);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        inMemoryDnsPlugin.setStrategy(hostname, request.getStrategy());
        return ResponseEntity.ok("Load balancing strategy updated successfully");
    }

    @PostMapping("/records/clear")
    public ResponseEntity<String> clearAllRecords() {
        inMemoryDnsPlugin.clear();
        return ResponseEntity.ok("All DNS records cleared successfully");
    }

    @GetMapping("/resolve/{hostname}")
    public ResponseEntity<List<String>> resolveHostname(@PathVariable String hostname) {
        if (inMemoryDnsPlugin.canHandle(hostname)) {
            List<String> addresses = inMemoryDnsPlugin.resolve(hostname);
            return ResponseEntity.ok(addresses);
        }
        return ResponseEntity.notFound().build();
    }
}