package com.example.javadns.plugin.impl;

import com.example.javadns.plugin.DnsPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Component
public class InMemoryDnsPlugin implements DnsPlugin {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDnsPlugin.class);

    private final Map<String, List<String>> hostnameMap = new ConcurrentHashMap<>();
    private final Map<String, Pattern> patternMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final Map<String, LoadBalancingStrategy> strategies = new ConcurrentHashMap<>();

    public enum LoadBalancingStrategy {
        ROUND_ROBIN,
        RANDOM,
        FIRST
    }

    public static class DnsRecord {
        private String hostname;
        private List<String> addresses;
        private LoadBalancingStrategy strategy = LoadBalancingStrategy.ROUND_ROBIN;

        public String getHostname() { return hostname; }
        public void setHostname(String hostname) { this.hostname = hostname; }
        public List<String> getAddresses() { return addresses; }
        public void setAddresses(List<String> addresses) { this.addresses = addresses; }
        public LoadBalancingStrategy getStrategy() { return strategy; }
        public void setStrategy(LoadBalancingStrategy strategy) { this.strategy = strategy; }
    }

    @Override
    public List<String> resolve(String hostname) {
        // Direct match first
        List<String> addresses = hostnameMap.get(hostname);
        if (addresses != null && !addresses.isEmpty()) {
            return applyLoadBalancing(hostname, addresses);
        }

        // Pattern match for wildcards
        for (Map.Entry<String, Pattern> entry : patternMap.entrySet()) {
            if (entry.getValue().matcher(hostname).matches()) {
                List<String> patternAddresses = hostnameMap.get(entry.getKey());
                if (patternAddresses != null && !patternAddresses.isEmpty()) {
                    return applyLoadBalancing(hostname, patternAddresses);
                }
            }
        }

        return List.of();
    }

    private List<String> applyLoadBalancing(String hostname, List<String> addresses) {
        if (addresses.size() == 1) {
            return addresses;
        }

        LoadBalancingStrategy strategy = strategies.getOrDefault(hostname, LoadBalancingStrategy.ROUND_ROBIN);

        return switch (strategy) {
            case ROUND_ROBIN -> {
                AtomicInteger counter = roundRobinCounters.computeIfAbsent(hostname, k -> new AtomicInteger(0));
                int index = counter.getAndIncrement() % addresses.size();
                yield List.of(addresses.get(index));
            }
            case RANDOM -> {
                Random random = new Random();
                int index = random.nextInt(addresses.size());
                yield List.of(addresses.get(index));
            }
            case FIRST -> List.of(addresses.get(0));
        };
    }

    @Override
    public boolean canHandle(String hostname) {
        // Check direct match
        if (hostnameMap.containsKey(hostname)) {
            return true;
        }

        // Check pattern match
        for (Pattern pattern : patternMap.values()) {
            if (pattern.matcher(hostname).matches()) {
                return true;
            }
        }

        return false;
    }

    // Management methods for REST API

    public void addRecord(String hostname, List<String> addresses) {
        addRecord(hostname, addresses, LoadBalancingStrategy.ROUND_ROBIN);
    }

    public void addRecord(String hostname, List<String> addresses, LoadBalancingStrategy strategy) {
        hostnameMap.put(hostname, new ArrayList<>(addresses));
        strategies.put(hostname, strategy);

        // Create pattern if hostname contains wildcards
        if (hostname.contains("*")) {
            String regex = hostname.replace(".", "\\.").replace("*", ".*");
            patternMap.put(hostname, Pattern.compile(regex));
        }

        log.info("Added DNS record: {} -> {} (strategy: {})", hostname, addresses, strategy);
    }

    public void removeRecord(String hostname) {
        hostnameMap.remove(hostname);
        strategies.remove(hostname);
        patternMap.remove(hostname);
        roundRobinCounters.remove(hostname);
        log.info("Removed DNS record: {}", hostname);
    }

    public void updateRecord(String hostname, List<String> addresses) {
        if (hostnameMap.containsKey(hostname)) {
            LoadBalancingStrategy strategy = strategies.getOrDefault(hostname, LoadBalancingStrategy.ROUND_ROBIN);
            addRecord(hostname, addresses, strategy);
            log.info("Updated DNS record: {} -> {}", hostname, addresses);
        }
    }

    public Map<String, List<String>> getAllRecords() {
        return new HashMap<>(hostnameMap);
    }

    public List<String> getRecord(String hostname) {
        return hostnameMap.get(hostname);
    }

    public LoadBalancingStrategy getStrategy(String hostname) {
        return strategies.getOrDefault(hostname, LoadBalancingStrategy.ROUND_ROBIN);
    }

    public void setStrategy(String hostname, LoadBalancingStrategy strategy) {
        if (hostnameMap.containsKey(hostname)) {
            strategies.put(hostname, strategy);
            log.info("Updated load balancing strategy for {}: {}", hostname, strategy);
        }
    }

    public void clear() {
        hostnameMap.clear();
        patternMap.clear();
        strategies.clear();
        roundRobinCounters.clear();
        log.info("Cleared all DNS records");
    }
}