package com.example.javadns.plugin;

import java.util.List;

public interface DnsPlugin {
    /**
     * Resolves the given hostname to a list of IP addresses.
     *
     * @param hostname The hostname to resolve.
     * @return A list of IP addresses, or an empty list if the hostname cannot be resolved.
     */
    List<String> resolve(String hostname);

    /**
     * Checks if this plugin can handle the given hostname.
     *
     * @param hostname The hostname to check.
     * @return True if this plugin can handle the hostname, false otherwise.
     */
    boolean canHandle(String hostname);
}

