package com.example.javadns.plugin.impl;

import com.example.javadns.plugin.DnsPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@Component
@ConfigurationProperties(prefix = "dns.plugin.file")
public class FileDnsPlugin implements DnsPlugin {

    private static final Logger log = LoggerFactory.getLogger(FileDnsPlugin.class);

    private String filePath = "dns-records.txt";
    private final Map<String, List<String>> hostnameMap = new ConcurrentHashMap<>();
    private final Map<String, Pattern> patternMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    public void setFilePath(String filePath) {
        this.filePath = filePath;
        loadFromFile();
    }

    private void loadFromFile() {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    if (line.trim().isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    parseLine(line);
                }
                log.info("Loaded {} DNS records from file: {}", hostnameMap.size(), filePath);
            } else {
                log.warn("DNS records file not found: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Error loading DNS records from file: {}", filePath, e);
        }
    }

    private void parseLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            String hostname = parts[0];
            List<String> addresses = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                addresses.add(parts[i]);
            }
            hostnameMap.put(hostname, addresses);
            
            // Create pattern if hostname contains wildcards
            if (hostname.contains("*")) {
                String regex = hostname.replace(".", "\\.").replace("*", ".*");
                patternMap.put(hostname, Pattern.compile(regex));
            }
        }
    }

    @Override
    public List<String> resolve(String hostname) {
        // Direct match first
        List<String> addresses = hostnameMap.get(hostname);
        if (addresses != null && !addresses.isEmpty()) {
            return roundRobin(hostname, addresses);
        }

        // Pattern match for wildcards
        for (Map.Entry<String, Pattern> entry : patternMap.entrySet()) {
            if (entry.getValue().matcher(hostname).matches()) {
                List<String> patternAddresses = hostnameMap.get(entry.getKey());
                if (patternAddresses != null && !patternAddresses.isEmpty()) {
                    return roundRobin(hostname, patternAddresses);
                }
            }
        }

        return List.of();
    }

    private List<String> roundRobin(String hostname, List<String> addresses) {
        if (addresses.size() == 1) {
            return addresses;
        }

        AtomicInteger counter = roundRobinCounters.computeIfAbsent(hostname, k -> new AtomicInteger(0));
        int index = counter.getAndIncrement() % addresses.size();
        return List.of(addresses.get(index));
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

    public String getFilePath() {
        return filePath;
    }
}