package com.example.javadns.server;

import com.example.javadns.config.DnsConfig;
import com.example.javadns.plugin.DnsPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class DnsServer {

    private static final Logger log = LoggerFactory.getLogger(DnsServer.class);

    private final DnsConfig dnsConfig;
    private final List<DnsPlugin> plugins;
    private final Map<String, List<String>> cache = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private DatagramSocket socket;

    @Autowired
    public DnsServer(DnsConfig dnsConfig, List<DnsPlugin> plugins) {
        this.dnsConfig = dnsConfig;
        this.plugins = plugins;
    }

    @Bean
    public ApplicationRunner startDnsServer() {
        return args -> {
            try {
                start();
            } catch (Exception e) {
                log.error("Failed to start DNS server", e);
            }
        };
    }

    public void start() throws IOException {
        socket = new DatagramSocket(5353); // Use port 5353 to avoid conflicts with system DNS
        log.info("DNS Server started on port 5353");

        while (!socket.isClosed()) {
            try {
                byte[] buffer = new byte[512];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                executorService.submit(() -> handleDnsQuery(packet));
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    log.error("Error receiving DNS query", e);
                }
            }
        }
    }

    private void handleDnsQuery(DatagramPacket packet) {
        try {
            Message query = new Message(packet.getData());
            Message response = new Message(query.getHeader().getID());
            response.getHeader().setFlag(Flags.QR);

            Record questionRecord = (Record) query.getSection(Section.QUESTION).get(0);
            String hostname = questionRecord.getName().toString();

            log.debug("Received DNS query for: {}", hostname);

            List<String> addresses = resolveHostname(hostname);

            if (!addresses.isEmpty()) {
                response.getHeader().setFlag(Flags.AA);
                for (String address : addresses) {
                    Record record = new ARecord(questionRecord.getName(), DClass.IN, 300,
                            InetAddress.getByName(address));
                    response.addRecord(record, Section.ANSWER);
                }
            } else {
                response.getHeader().setRcode(Rcode.NXDOMAIN);
            }

            response.addRecord(questionRecord, Section.QUESTION);

            byte[] responseData = response.toWire();
            DatagramPacket responsePacket = new DatagramPacket(
                    responseData, responseData.length, packet.getAddress(), packet.getPort());
            socket.send(responsePacket);

        } catch (Exception e) {
            log.error("Error handling DNS query", e);
        }
    }

    private List<String> resolveHostname(String hostname) {
        // First check plugins
        for (DnsPlugin plugin : plugins) {
            if (plugin.canHandle(hostname)) {
                List<String> result = plugin.resolve(hostname);
                if (!result.isEmpty()) {
                    log.debug("Resolved {} via plugin: {}", hostname, result);
                    return result;
                }
            }
        }

        // Fall back to upstream DNS
        try {
            Lookup lookup = new Lookup(hostname, Type.A);
            lookup.setResolver(new SimpleResolver(dnsConfig.getUpstreamDnsServer()));
            Record[] records = lookup.run();

            List<String> addresses = new java.util.ArrayList<>();
            if (records != null) {
                for (Record record : records) {
                    if (record instanceof ARecord) {
                        ARecord aRecord = (ARecord) record;
                        addresses.add(aRecord.getAddress().getHostAddress());
                    }
                }
            }
            log.debug("Resolved {} via upstream DNS: {}", hostname, addresses);
            return addresses;
        } catch (Exception e) {
            log.error("Error resolving {} via upstream DNS", hostname, e);
            return List.of();
        }
    }

    public void stop() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        executorService.shutdown();
    }
}