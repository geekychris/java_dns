package com.example.javadns;

import com.example.javadns.plugin.impl.InMemoryDnsPlugin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DnsServerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InMemoryDnsPlugin inMemoryDnsPlugin;

    @Test
    public void testAddAndGetDnsRecord() {
        // Create a DNS record request
        var request = new InMemoryDnsPlugin.DnsRecord();
        request.setHostname("test.local");
        request.setAddresses(List.of("192.168.1.1", "192.168.1.2"));
        request.setStrategy(InMemoryDnsPlugin.LoadBalancingStrategy.ROUND_ROBIN);

        // Add the record via REST API
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/dns/records", request, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successfully"));

        // Verify the record was added
        ResponseEntity<List<String>> getResponse = restTemplate.exchange(
                "/api/dns/records/test.local",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {}
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertTrue(getResponse.getBody().contains("192.168.1.1") || getResponse.getBody().contains("192.168.1.2"));
    }

    @Test
    public void testGetAllRecords() {
        // Add some test records
        inMemoryDnsPlugin.addRecord("example1.local", List.of("10.0.0.1"));
        inMemoryDnsPlugin.addRecord("example2.local", List.of("10.0.0.2", "10.0.0.3"));

        // Get all records via REST API
        ResponseEntity<Map<String, List<String>>> response = restTemplate.exchange(
                "/api/dns/records",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, List<String>>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("example1.local"));
        assertTrue(response.getBody().containsKey("example2.local"));
    }

    @Test
    public void testUpdateDnsRecord() {
        // Add initial record
        inMemoryDnsPlugin.addRecord("update.test", List.of("1.1.1.1"));

        // Update the record
        List<String> newAddresses = List.of("2.2.2.2", "3.3.3.3");
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/dns/records/update.test",
                HttpMethod.PUT,
                null,
                String.class,
                newAddresses
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify the update
        List<String> updatedAddresses = inMemoryDnsPlugin.getRecord("update.test");
        assertNotNull(updatedAddresses);
        assertTrue(updatedAddresses.contains("2.2.2.2"));
        assertTrue(updatedAddresses.contains("3.3.3.3"));
        assertFalse(updatedAddresses.contains("1.1.1.1"));
    }

    @Test
    public void testDeleteDnsRecord() {
        // Add a record to delete
        inMemoryDnsPlugin.addRecord("delete.test", List.of("4.4.4.4"));

        // Delete the record
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/dns/records/delete.test",
                HttpMethod.DELETE,
                null,
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify deletion
        assertNull(inMemoryDnsPlugin.getRecord("delete.test"));
    }

    @Test
    public void testLoadBalancingStrategies() {
        String hostname = "lb.test";
        List<String> addresses = List.of("5.5.5.1", "5.5.5.2", "5.5.5.3");
        
        inMemoryDnsPlugin.addRecord(hostname, addresses);

        // Test ROUND_ROBIN strategy (default)
        String firstAddress = inMemoryDnsPlugin.resolve(hostname).get(0);
        String secondAddress = inMemoryDnsPlugin.resolve(hostname).get(0);
        String thirdAddress = inMemoryDnsPlugin.resolve(hostname).get(0);
        
        // All should be different due to round robin
        assertTrue(addresses.contains(firstAddress));
        assertTrue(addresses.contains(secondAddress));
        assertTrue(addresses.contains(thirdAddress));

        // Test FIRST strategy
        inMemoryDnsPlugin.setStrategy(hostname, InMemoryDnsPlugin.LoadBalancingStrategy.FIRST);
        String firstResult = inMemoryDnsPlugin.resolve(hostname).get(0);
        String secondResult = inMemoryDnsPlugin.resolve(hostname).get(0);
        
        assertEquals(firstResult, secondResult); // Should always return the same (first) address
        assertEquals(addresses.get(0), firstResult);
    }

    @Test
    public void testNonExistentRecord() {
        ResponseEntity<List<String>> response = restTemplate.exchange(
                "/api/dns/records/nonexistent.test",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {}
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testResolveEndpoint() {
        // Add a test record
        inMemoryDnsPlugin.addRecord("resolve.test", List.of("6.6.6.6"));

        // Test resolve endpoint
        ResponseEntity<List<String>> response = restTemplate.exchange(
                "/api/dns/resolve/resolve.test",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<String>>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("6.6.6.6", response.getBody().get(0));
    }

    @Test
    public void testClearAllRecords() {
        // Add some records
        inMemoryDnsPlugin.addRecord("clear1.test", List.of("7.7.7.1"));
        inMemoryDnsPlugin.addRecord("clear2.test", List.of("7.7.7.2"));

        // Clear all records
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/dns/records/clear", null, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify all records are cleared
        assertTrue(inMemoryDnsPlugin.getAllRecords().isEmpty());
    }
}