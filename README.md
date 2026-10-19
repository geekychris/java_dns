<p align="center">
  <img src="assets/logo.png" alt="Java DNS logo" width="180">
</p>

# JavaDNS Server

A high-performance, extensible DNS server built with Spring Boot and dnsjava, featuring a plugin-based architecture for custom hostname resolution.

## 🚀 Features

- **High-Performance DNS Server**: UDP-based DNS server with concurrent request handling
- **Plugin Architecture**: Extensible plugin system for custom hostname resolution
- **Multiple Load Balancing Strategies**: Round-robin, random, and first-match algorithms
- **REST API**: Complete management interface for dynamic DNS record management
- **Caching**: Configurable DNS response caching for improved performance
- **File-based Configuration**: Support for DNS records from configuration files
- **In-Memory Management**: Runtime DNS record management via REST API
- **Health Checks**: Built-in health monitoring and metrics
- **Container Ready**: Docker and Kubernetes deployment support
- **Monitoring**: Prometheus metrics integration
- **Scalability**: Horizontal pod autoscaling support

## 🏗️ Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────────────┐
│                         JavaDNS Server                         │
├─────────────────────────────────────────────────────────────────┤
│  REST API (Port 8080)           │  DNS Server (Port 5353)      │
│  ├─ DNS Management              │  ├─ UDP Request Handler      │
│  ├─ Health Checks               │  ├─ Query Parser             │
│  └─ Metrics                     │  └─ Response Builder         │
├─────────────────────────────────────────────────────────────────┤
│                        Plugin System                           │
│  ├─ InMemoryDnsPlugin (REST managed)                          │
│  ├─ FileDnsPlugin (Configuration file)                        │
│  └─ Custom Plugins (Extensible)                               │
├─────────────────────────────────────────────────────────────────┤
│                      Upstream DNS                              │
│  └─ Fallback to configured DNS server (e.g., 8.8.8.8)        │
└─────────────────────────────────────────────────────────────────┘
```

### DNS Resolution Flow

1. **DNS Query Received**: Client sends DNS query to port 5353
2. **Plugin Chain**: Query processed through registered plugins
3. **Local Resolution**: If plugin can handle, return local result
4. **Upstream Fallback**: Otherwise, forward to upstream DNS server
5. **Caching**: Response cached based on configuration
6. **Response**: DNS response sent back to client

### Plugin System

The server implements a plugin-based architecture allowing custom hostname resolution:

- **DnsPlugin Interface**: All plugins implement this interface
- **Priority Order**: Plugins processed in registration order
- **Extensible**: Easy to add new resolution strategies

## 🛠️ Building and Development

### Prerequisites

- **Java 21+** (OpenJDK or Amazon Corretto 23 recommended)
- **Maven 3.6+**
- **Docker** (for containerization)
- **Kubernetes** (for orchestration)

### Build Commands

```bash
# Build the application
mvn clean compile

# Run tests
mvn test

# Package the application
mvn clean package

# Run locally
mvn spring-boot:run

# Run with custom profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Development Setup

1. **Clone and build**:
   ```bash
   git clone <repository-url>
   cd javadns
   mvn clean package
   ```

2. **Run with development profile**:
   ```bash
   java -jar target/javadns-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
   ```

3. **Test DNS server**:
   ```bash
   dig @localhost -p 5353 google.com
   java -cp target/javadns-0.0.1-SNAPSHOT.jar com.example.javadns.client.DnsTestClient
   ```

## 🐳 Docker Deployment

### Build Docker Image

```bash
# Build the Docker image
docker build -t javadns:latest .

# Run with Docker
docker run -d \
  --name javadns-server \
  -p 8080:8080 \
  -p 5353:5353/udp \
  -v $(pwd)/dns-records.txt:/app/dns-records.txt:ro \
  javadns:latest
```

### Docker Compose

For local development with monitoring:

```bash
# Start basic service
docker-compose up -d

# Start with testing
docker-compose --profile testing up -d

# Start with monitoring (Prometheus + Grafana)
docker-compose --profile monitoring up -d

# View logs
docker-compose logs -f javadns

# Cleanup
docker-compose down -v
```

**Service URLs:**
- DNS Server: `localhost:5353`
- REST API: `http://localhost:8080`
- Health Check: `http://localhost:8080/actuator/health`
- Prometheus: `http://localhost:9090` (monitoring profile)
- Grafana: `http://localhost:3000` (monitoring profile, admin/admin)

## ☸️ Kubernetes Deployment

### Prerequisites

- Kubernetes cluster (1.19+)
- kubectl configured
- Container registry access (for custom images)

### Quick Deployment

```bash
# Navigate to k8s directory
cd k8s

# Deploy to default namespace
./deploy.sh

# Deploy to custom namespace
NAMESPACE=javadns-prod IMAGE_TAG=v1.0.0 ./deploy.sh

# Check deployment status
kubectl get pods,svc -l app=javadns

# Access services
kubectl port-forward svc/javadns-http-service 8080:8080
kubectl port-forward svc/javadns-dns-service 5353:53
```

### Manual Deployment

```bash
# Create resources in order
kubectl apply -f rbac.yaml
kubectl apply -f configmap.yaml
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml
kubectl apply -f hpa.yaml

# Verify deployment
kubectl rollout status deployment/javadns-deployment
```

### Configuration

The Kubernetes deployment uses ConfigMaps for configuration. Modify `k8s/configmap.yaml` to customize:

- DNS server settings
- Plugin configurations
- Application properties

## 📚 API Documentation

### DNS Management REST API

Base URL: `http://localhost:8080/api/dns`

#### DNS Records

```bash
# Get all records
curl http://localhost:8080/api/dns/records

# Get specific record
curl http://localhost:8080/api/dns/records/example.com

# Add new record
curl -X POST http://localhost:8080/api/dns/records \
  -H "Content-Type: application/json" \
  -d '{
    "hostname": "api.example.com",
    "addresses": ["192.168.1.10", "192.168.1.11"],
    "strategy": "ROUND_ROBIN"
  }'

# Update record
curl -X PUT http://localhost:8080/api/dns/records/api.example.com \
  -H "Content-Type: application/json" \
  -d '["192.168.1.20", "192.168.1.21"]'

# Delete record
curl -X DELETE http://localhost:8080/api/dns/records/api.example.com

# Test resolution
curl http://localhost:8080/api/dns/resolve/api.example.com
```

#### Load Balancing Strategies

```bash
# Get current strategy
curl http://localhost:8080/api/dns/records/api.example.com/strategy

# Update strategy
curl -X PUT http://localhost:8080/api/dns/records/api.example.com/strategy \
  -H "Content-Type: application/json" \
  -d '{"strategy": "RANDOM"}'
```

Available strategies:
- `ROUND_ROBIN`: Cycle through addresses sequentially
- `RANDOM`: Return random address from list
- `FIRST`: Always return first address

#### Bulk Operations

```bash
# Clear all records
curl -X POST http://localhost:8080/api/dns/records/clear
```

### Health and Monitoring

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics (Prometheus format)
curl http://localhost:8080/actuator/metrics

# Application info
curl http://localhost:8080/actuator/info
```

## 🔧 Configuration

### Application Properties

Key configuration options in `application.properties`:

```properties
# DNS Server Configuration
dns.upstream-dns-server=8.8.8.8
dns.cache-expiration-seconds=300
dns.server-port=5353

# File Plugin Configuration
dns.plugin.file.file-path=dns-records.txt

# Spring Boot Configuration
server.port=8080
logging.level.com.example.javadns=INFO

# Actuator endpoints
management.endpoints.web.exposure.include=health,info,metrics,prometheus
```

### DNS Records File Format

The file plugin supports a simple text format:

```
# Comments start with #
# Format: hostname address1 address2 address3 ...

# Direct mappings
api.example.com 192.168.1.10 192.168.1.11
db.example.com 192.168.1.20

# Wildcard patterns
*.dev.local 192.168.100.100
*.staging.local 192.168.200.100

# Load balancing
web.example.com 10.0.0.10 10.0.0.11 10.0.0.12
```

## 🔌 Plugin Development

### Creating Custom Plugins

1. **Implement the DnsPlugin interface**:

```java
@Component
public class CustomDnsPlugin implements DnsPlugin {
    
    @Override
    public List<String> resolve(String hostname) {
        // Custom resolution logic
        if (hostname.endsWith(".custom")) {
            return List.of("192.168.1.100");
        }
        return List.of();
    }
    
    @Override
    public boolean canHandle(String hostname) {
        return hostname.endsWith(".custom");
    }
}
```

2. **Add to classpath**: Spring Boot will automatically register the plugin

3. **Configuration**: Use `@ConfigurationProperties` for plugin-specific settings

### Plugin Examples

#### Database Plugin
```java
@Component
public class DatabaseDnsPlugin implements DnsPlugin {
    
    @Autowired
    private DnsRecordRepository repository;
    
    @Override
    public List<String> resolve(String hostname) {
        Optional<DnsRecord> record = repository.findByHostname(hostname);
        return record.map(DnsRecord::getAddresses).orElse(List.of());
    }
    
    @Override
    public boolean canHandle(String hostname) {
        return repository.existsByHostname(hostname);
    }
}
```

#### Service Discovery Plugin
```java
@Component
public class ServiceDiscoveryPlugin implements DnsPlugin {
    
    @Override
    public List<String> resolve(String hostname) {
        if (hostname.endsWith(".service.local")) {
            // Integrate with service registry
            return serviceRegistry.getEndpoints(hostname);
        }
        return List.of();
    }
    
    // ... implementation
}
```

## 🧪 Testing

### Unit Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DnsServerIntegrationTest

# Run with coverage
mvn test jacoco:report
```

### Integration Testing

```bash
# Start server in test mode
java -jar target/javadns-0.0.1-SNAPSHOT.jar --spring.profiles.active=test

# Run DNS client tests
java -cp target/javadns-0.0.1-SNAPSHOT.jar com.example.javadns.client.DnsTestClient

# Test with dig
dig @localhost -p 5353 google.com
dig @localhost -p 5353 test.example.com
```

### Performance Testing

```bash
# Performance test with custom client
java -cp target/javadns-0.0.1-SNAPSHOT.jar \
  com.example.javadns.client.DnsTestClient localhost 5353

# Load testing with drill (if available)
drill -s localhost -p 5353 -Q 1000 google.com
```

## 📊 Monitoring and Observability

### Metrics

The server exports metrics in Prometheus format at `/actuator/prometheus`:

- **DNS Query Metrics**: Request count, response times, error rates
- **JVM Metrics**: Memory usage, GC statistics, thread counts
- **Spring Boot Metrics**: HTTP requests, cache statistics
- **Custom Metrics**: Plugin performance, cache hit rates

### Logging

Configurable log levels:

```properties
# Root logging level
logging.level.root=INFO

# DNS server specific logging
logging.level.com.example.javadns=DEBUG

# Third-party libraries
logging.level.org.xbill.DNS=WARN
```

### Health Checks

Built-in health indicators:

- **Liveness**: Server is running and responsive
- **Readiness**: Server is ready to handle requests
- **Custom**: Plugin health, upstream DNS connectivity

## 🚀 Production Deployment

### Performance Tuning

**JVM Options**:
```bash
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=70.0
-XX:+UseG1GC
-XX:+UseStringDeduplication
-Djava.security.egd=file:/dev/./urandom
```

**Spring Boot Configuration**:
```properties
# Connection pool tuning
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10

# Cache configuration
spring.cache.type=caffeine
spring.cache.caffeine.spec=maximumSize=10000,expireAfterWrite=300s
```

### Security Considerations

- **Non-root user**: Container runs as non-root user (UID 1001)
- **Network policies**: Restrict network access in Kubernetes
- **Resource limits**: Set appropriate CPU and memory limits
- **Health checks**: Configure proper liveness and readiness probes

### High Availability

- **Multiple replicas**: Deploy 3+ replicas for redundancy
- **Pod anti-affinity**: Spread pods across nodes
- **Load balancing**: Use Kubernetes service load balancing
- **Health monitoring**: Monitor with Prometheus and Grafana

## 🐛 Troubleshooting

### Common Issues

**Server won't start**:
```bash
# Check port availability
sudo netstat -tulpn | grep 5353
sudo netstat -tulpn | grep 8080

# Check Java version
java -version

# Check application logs
kubectl logs deployment/javadns-deployment
```

**DNS queries not working**:
```bash
# Test with dig
dig @localhost -p 5353 google.com

# Check server logs
curl http://localhost:8080/actuator/loggers/com.example.javadns

# Verify plugins are loaded
curl http://localhost:8080/actuator/beans | grep DnsPlugin
```

**Performance issues**:
```bash
# Check metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/http.server.requests

# Monitor GC
curl http://localhost:8080/actuator/metrics/jvm.gc.pause
```

### Debugging

Enable debug logging:
```properties
logging.level.com.example.javadns=DEBUG
logging.level.org.xbill.DNS=DEBUG
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

- **Documentation**: Check this README and inline code documentation
- **Issues**: Use GitHub issues for bug reports and feature requests
- **Discussions**: Use GitHub discussions for questions and community support

---

**Happy DNS serving! 🎉**