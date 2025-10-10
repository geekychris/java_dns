package com.example.javadns.resolver;

import com.example.javadns.config.DnsConfig;

import org.springframework.stereotype.Service;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;

@Service
public class DnsResolver {

    private DnsConfig dnsConfig;

    public void setDnsConfig(DnsConfig dnsConfig) {
        this.dnsConfig = dnsConfig;
    }

    @Cacheable("dnsCache")
    public List<String> resolve(String hostname) {
        List<String> addresses = new ArrayList<>();
        try {
            Lookup lookup = new Lookup(hostname, Type.A);
            lookup.setResolver(new SimpleResolver(dnsConfig.getUpstreamDnsServer()));
            Record[] records = lookup.run();

            if (records != null) {
                for (Record record : records) {
                    if (record instanceof ARecord) {
                        ARecord aRecord = (ARecord) record;
                        addresses.add(aRecord.getAddress().getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            // Handle exception appropriately (log, etc.)
            System.err.println("Error resolving " + hostname + ": " + e.getMessage());
        }
        return addresses;
    }
}

