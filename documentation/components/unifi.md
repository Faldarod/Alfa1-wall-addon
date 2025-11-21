# UniFi Network Controller Documentation

## Goal

The UniFi Network Controller serves as the network management platform for device tracking and presence detection in the AlfaWall system. It provides:

1. **Network Device Management**: Manages UniFi access points, switches, and network devices
2. **Client Tracking**: Tracks connected devices (phones, laptops) via MAC addresses
3. **Presence Detection**: Determines if employees are on-site based on device connectivity
4. **Integration with Home Assistant**: Exposes device presence data through Home Assistant's UniFi integration

**Key Objectives:**
- Track employee devices (phones, laptops) connected to the office network
- Provide real-time presence data to Home Assistant
- Enable presence-based queries (e.g., "Who is at the office?")
- Support device_tracker entities in Home Assistant

## Relevant Articles & Resources

### Official UniFi Documentation
- **UniFi Network Application**: https://help.ui.com/hc/en-us/categories/200320654-UniFi-Network
- **Controller Installation**: https://help.ui.com/hc/en-us/articles/220066768-UniFi-Network-Setting-up-UniFi-Network-Application
- **Device Tracking**: https://help.ui.com/hc/en-us/articles/204959834-UniFi-Network-Device-Tracking

### Home Assistant Integration
- **UniFi Integration**: https://www.home-assistant.io/integrations/unifi/
- **Device Tracker Platform**: https://www.home-assistant.io/integrations/device_tracker/
- **Configuration**: https://www.home-assistant.io/integrations/unifi/#configuration

### Docker Image
- **LinuxServer.io UniFi Controller**: https://docs.linuxserver.io/images/docker-unifi-network-application
- **Docker Hub**: https://hub.docker.com/r/linuxserver/unifi-network-application
- **GitHub**: https://github.com/linuxserver/docker-unifi-network-application

### Community Resources
- **Ubiquiti Community**: https://community.ui.com/
- **UniFi Subreddit**: https://www.reddit.com/r/Ubiquiti/
- **Home Assistant Community - UniFi**: https://community.home-assistant.io/tag/unifi

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| UniFi Network Application | latest | Network controller software |
| MongoDB | 4.4 | Database backend for UniFi data |
| Java | 17 (bundled) | Runtime for UniFi Controller |
| Docker | - | Containerization |
| Linux (Alpine) | - | Base OS in container |

### Docker Images Used

#### UniFi Controller
- **Image**: `lscr.io/linuxserver/unifi-network-application:latest`
- **Provider**: LinuxServer.io
- **Base**: Alpine Linux
- **Java Version**: OpenJDK 17

#### MongoDB
- **Image**: `mongo:4.4`
- **Provider**: Official MongoDB
- **Purpose**: Backend database for UniFi Controller
- **Data Storage**: Persistent volume

### Port Mappings

| Port | Protocol | Purpose |
|------|----------|---------|
| 8443 | TCP | Web UI (HTTPS) - Main interface |
| 8082 | TCP | Device communication (mapped from 8080 to avoid conflicts) |
| 3478 | UDP | STUN port for NAT traversal |
| 10001 | UDP | Device discovery |
| 1900 | UDP | L2 network discovery |
| 8843 | TCP | Guest portal HTTPS |
| 8880 | TCP | Guest portal HTTP |
| 6789 | TCP | Mobile throughput test |

## Architecture

### System Integration

```
Employee Devices (phones, laptops)
       ↓
UniFi Access Point/Network
       ↓
UniFi Controller (tracks MAC addresses)
       ↓
MongoDB (stores client data)
       ↓
Home Assistant UniFi Integration
       ↓
device_tracker entities (device_tracker.john_phone)
       ↓
Alfa-Wall-Addon (queries device states)
       ↓
LED Visualization
```

### Component Architecture

```
Docker Environment:
┌─────────────────────────────────────────┐
│ unifi-controller container              │
│ ┌─────────────────────────────────────┐ │
│ │ UniFi Network Application           │ │
│ │ - Web UI (port 8443)                │ │
│ │ - Device management                 │ │
│ │ - Client tracking                   │ │
│ └─────────────────────────────────────┘ │
│              ↓ ↑                        │
│ ┌─────────────────────────────────────┐ │
│ │ MongoDB Client                      │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
              ↓ ↑
┌─────────────────────────────────────────┐
│ unifi-db container (MongoDB)            │
│ - Port: 27017                           │
│ - Database: unifi                       │
│ - Persistent storage                    │
└─────────────────────────────────────────┘
              ↓ ↑
┌─────────────────────────────────────────┐
│ Home Assistant                          │
│ - UniFi Integration                     │
│ - Creates device_tracker entities       │
└─────────────────────────────────────────┘
```

### Data Flow

1. **Device Connection**:
   - Employee's phone/laptop connects to office WiFi
   - UniFi access point detects device via MAC address
   - UniFi Controller records connection in MongoDB

2. **Home Assistant Sync**:
   - Home Assistant UniFi integration polls controller (every 30 seconds default)
   - Creates/updates `device_tracker.device_name` entity
   - State: `home` (connected) or `not_home` (disconnected)

3. **AlfaWall Query**:
   - User asks "Who is at the office?"
   - Alfa-wall-addon queries device_tracker states via Home Assistant API
   - Maps device_tracker entities to employee names
   - Returns list of present employees

## Functional Description

### Use Cases

#### 1. **Device Presence Tracking**

**Scenario**: Track if John Doe is at the office

**Setup**:
1. John's devices configured in UniFi Controller
2. MAC addresses noted: `AA:BB:CC:DD:EE:01` (phone), `AA:BB:CC:DD:EE:02` (laptop)
3. Home Assistant UniFi integration creates entities:
   - `device_tracker.john_phone`
   - `device_tracker.john_laptop`
4. Mapping in `application.yaml`:
   ```yaml
   employee-device:
     mappings:
       "John Doe":
         - device_tracker.john_phone
         - device_tracker.john_laptop
   ```

**Flow**:
1. John arrives at office, phone connects to WiFi
2. UniFi Controller detects connection
3. Home Assistant updates `device_tracker.john_phone` to `home`
4. Alfa-wall-addon queries state and determines John is present
5. LED segment for John turns green

#### 2. **Office Presence Query**

**User Query**: "Who is at the office today?"

**Processing**:
1. Alfa-wall-addon queries all mapped device_tracker entities
2. Checks state of each:
   ```bash
   GET /api/states/device_tracker.john_phone → state: "home"
   GET /api/states/device_tracker.jane_phone → state: "not_home"
   GET /api/states/device_tracker.alice_phone → state: "home"
   ```
3. Determines present employees: John, Alice
4. Returns: "2 employees are at the office: John Doe, Alice Smith"

#### 3. **Device Discovery and Management**

**Initial Setup**:
1. Access UniFi Controller at https://localhost:8443
2. Complete first-time setup wizard
3. Adopt UniFi devices (access points, switches)
4. Devices appear in "Devices" section
5. Connected clients visible in "Clients" section

**Ongoing Management**:
- Monitor connected devices in real-time
- View client statistics (bandwidth, signal strength)
- Assign static IPs or aliases to devices
- Create device groups for organization

### Configuration

#### Docker Compose Configuration

**Location**: `docker-compose.yml`

```yaml
unifi-controller:
  image: lscr.io/linuxserver/unifi-network-application:latest
  container_name: unifi-controller
  profiles:
    - local-testing
  environment:
    - PUID=1000              # User ID for file permissions
    - PGID=1000              # Group ID for file permissions
    - TZ=Europe/Amsterdam    # Timezone
    - MONGO_USER=unifi       # MongoDB username
    - MONGO_PASS=unifi       # MongoDB password
    - MONGO_HOST=unifi-db    # MongoDB container hostname
    - MONGO_PORT=27017       # MongoDB port
    - MONGO_DBNAME=unifi     # Database name
  volumes:
    - ./unifi/unifi-data:/config  # Persistent config/data
  ports:
    - "8443:8443"            # Web UI (HTTPS)
    - "3478:3478/udp"        # STUN
    - "10001:10001/udp"      # Device discovery
    - "8082:8080"            # Device communication (remapped)
    - "1900:1900/udp"        # L2 discovery
    - "8843:8843"            # Guest portal HTTPS
    - "8880:8880"            # Guest portal HTTP
    - "6789:6789"            # Throughput test
  restart: unless-stopped
  depends_on:
    - unifi-db

unifi-db:
  image: mongo:4.4
  container_name: unifi-db
  profiles:
    - local-testing
  environment:
    - MONGO_INITDB_ROOT_USERNAME=unifi
    - MONGO_INITDB_ROOT_PASSWORD=unifi
    - MONGO_INITDB_DATABASE=unifi
  volumes:
    - ./unifi/unifi-db-data:/data/db  # Persistent MongoDB data
  restart: unless-stopped
```

#### Home Assistant Integration Configuration

**Setup in Home Assistant UI**:
1. Go to Settings → Devices & Services
2. Click "Add Integration"
3. Search for "UniFi Network"
4. Enter details:
   - **Host**: `unifi-controller` (container name)
   - **Port**: `8443`
   - **Username**: UniFi admin username (created during setup)
   - **Password**: UniFi admin password
   - **Verify SSL**: Disabled (self-signed certificate)
5. Options:
   - **Track network clients**: Enabled
   - **Detection time**: 300 seconds (default)
   - **Consider home**: 180 seconds (default)

#### Employee-to-Device Mapping

**Location**: `alfa-wall-addon/src/main/resources/application.yaml`

```yaml
employee-device:
  use-mock-data: false  # Set to false to use real UniFi data
  mappings:
    "John Doe":
      - device_tracker.john_phone
      - device_tracker.john_laptop
    "Jane Doe":
      - device_tracker.jane_phone
    "Alice Smith":
      - device_tracker.alice_phone
      - device_tracker.alice_laptop
```

**Note**: Device tracker entity names are auto-generated by Home Assistant based on device hostname or MAC address. You may need to adjust these after checking Home Assistant's entity registry.

## Developer Documentation

### Prerequisites

- **Docker & Docker Compose**
- **4GB RAM minimum** (UniFi Controller is memory-intensive)
- **UniFi network device** (optional - for production use)
  - UniFi Access Point, Switch, or Gateway
  - Or run in standalone mode for testing

### Getting Started

#### 1. Initial Setup

**Start UniFi Controller and Database**:
```bash
docker-compose --profile local-testing up -d unifi-db unifi-controller
```

**Wait for startup** (can take 2-5 minutes):
```bash
docker-compose logs -f unifi-controller
```

Look for: `[services.d] done.`

#### 2. Access Web Interface

**URL**: https://localhost:8443

**Note**: You'll see a security warning due to self-signed certificate. This is normal - click "Advanced" and proceed.

#### 3. Complete Setup Wizard

**First-Time Setup**:
1. **Country**: Select your country
2. **Terms**: Accept terms and conditions
3. **Admin Account**:
   - Name: `admin`
   - Email: `admin@example.com`
   - Password: Choose strong password (save this!)
4. **Network Name**: `AlfaWall Office`
5. **WiFi Setup** (optional for testing):
   - SSID: `AlfaWall-WiFi`
   - Password: Set WiFi password
6. **Device Discovery**: Skip (unless you have real UniFi devices)
7. **Review**: Complete setup

#### 4. Configure for Testing

**Without Physical UniFi Devices**:
- Controller runs in "standalone mode"
- Can still track devices connected to your existing network
- Useful for testing Home Assistant integration

**With Physical UniFi Devices**:
1. Connect UniFi access point to network
2. Access point should auto-discover controller
3. In UniFi UI: Devices → Adopt → Approve
4. Wait for provisioning to complete

#### 5. Find Device MAC Addresses

**View Connected Clients**:
1. Go to "Clients" tab in UniFi UI
2. See all connected devices
3. Click device → Details → MAC address
4. Note MAC address for employee device mapping

**Assign Friendly Names**:
1. Click client in UniFi UI
2. Name: `John's iPhone`
3. This helps identify devices in Home Assistant

#### 6. Configure Home Assistant Integration

Follow steps in Home Assistant Integration Configuration section above.

#### 7. Verify Device Tracker Entities

**In Home Assistant**:
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8123/api/states | grep device_tracker
```

**Expected Output**:
```json
{
  "entity_id": "device_tracker.johns_iphone",
  "state": "home",
  "attributes": {
    "source_type": "router",
    "ip": "192.168.1.100",
    "mac": "AA:BB:CC:DD:EE:01"
  }
}
```

### Project Structure

```
unifi/
├── unifi-data/              # UniFi Controller data (persistent)
│   ├── data/
│   │   ├── db/              # Local database files
│   │   ├── sites/           # Site configurations
│   │   ├── backup/          # Auto backups
│   │   └── logs/            # Controller logs
│   └── logs/                # Application logs
└── unifi-db-data/           # MongoDB data (persistent)
    ├── db/                  # MongoDB collections
    ├── configdb/
    └── journal/
```

**Important**: These directories are git-ignored (should be in `.gitignore`) as they contain runtime data and potentially sensitive information.

### Debugging

#### View Logs

**UniFi Controller logs**:
```bash
docker-compose logs -f unifi-controller
```

**MongoDB logs**:
```bash
docker-compose logs -f unifi-db
```

**Specific log files**:
```bash
# Controller application log
cat unifi/unifi-data/logs/server.log

# MongoDB log (in container)
docker exec -it unifi-db cat /var/log/mongodb/mongod.log
```

#### Common Issues

**Issue**: Web interface not accessible at https://localhost:8443
- **Solution**:
  - Check container is running: `docker ps | grep unifi-controller`
  - Check logs: `docker-compose logs unifi-controller`
  - Wait 2-5 minutes for initial startup
  - Verify port 8443 is not in use: `netstat -an | grep 8443`

**Issue**: Home Assistant cannot connect to UniFi Controller
- **Solution**:
  - Verify controller is accessible from Home Assistant container:
    ```bash
    docker exec -it home-assistant curl -k https://unifi-controller:8443
    ```
  - Check firewall rules
  - Verify credentials are correct
  - Disable SSL verification in Home Assistant integration

**Issue**: Device tracker entities not created
- **Solution**:
  - Verify "Track network clients" is enabled in UniFi integration options
  - Check that devices are actually connected (visible in UniFi Clients tab)
  - Check Home Assistant logs: `docker-compose logs home-assistant | grep unifi`
  - Reload UniFi integration: Settings → Devices & Services → UniFi → ⋮ → Reload

**Issue**: MongoDB connection errors
- **Solution**:
  - Verify unifi-db container is running: `docker ps | grep unifi-db`
  - Check MongoDB logs: `docker-compose logs unifi-db`
  - Verify MONGO_* environment variables match in both containers
  - Ensure MongoDB has sufficient disk space

**Issue**: Device tracker shows "not_home" but device is connected
- **Solution**:
  - Check "Consider home" timeout in UniFi integration options
  - Verify device is actually connected in UniFi Clients tab
  - Check device settings in Home Assistant: may be marked as "away"
  - Reload integration or restart Home Assistant

### Testing

#### Test Device Tracking

**Simulate Device Connection**:
1. Connect your phone to the office WiFi network
2. Check UniFi Clients tab - should see device appear
3. Note MAC address
4. Check Home Assistant for device_tracker entity:
   ```bash
   curl -H "Authorization: Bearer YOUR_TOKEN" \
        http://localhost:8123/api/states | grep device_tracker
   ```

**Test Mock Data Mode**:
If you don't have real UniFi devices, use mock data:
```yaml
# In application.yaml
employee-device:
  use-mock-data: true  # Use simulated presence data
```

This bypasses actual device tracking for testing.

#### Test Integration with Alfa-Wall-Addon

**Query Presence**:
```bash
curl -X POST http://localhost:8080/api/conversation/process \
     -H "Content-Type: application/json" \
     -d '{"text": "Who is at the office?", "language": "nl"}'
```

**Expected Response** (with mock data):
```json
{
  "speech": {
    "plain": {
      "speech": "2 employees are at the office: John Doe, Alice Smith. I've highlighted them on the LED wall."
    }
  }
}
```

### Performance Considerations

**Resource Usage**:
- **RAM**: 1-2GB for UniFi Controller
- **RAM**: 256-512MB for MongoDB
- **CPU**: Low (unless processing many clients)
- **Disk**: ~500MB for applications, grows with client history

**Optimization**:
- Limit client history retention in UniFi settings
- Disable unused features (Guest Portal, DPI, etc.)
- Schedule database backups during off-hours

## Building from Scratch

### Setting Up UniFi Controller

#### 1. Create Docker Compose Configuration

```yaml
version: '3.8'
services:
  unifi-controller:
    image: lscr.io/linuxserver/unifi-network-application:latest
    container_name: unifi-controller
    environment:
      - PUID=1000
      - PGID=1000
      - TZ=Europe/Amsterdam
      - MONGO_USER=unifi
      - MONGO_PASS=unifi
      - MONGO_HOST=unifi-db
      - MONGO_PORT=27017
      - MONGO_DBNAME=unifi
    volumes:
      - ./unifi/unifi-data:/config
    ports:
      - "8443:8443"
      - "3478:3478/udp"
      - "10001:10001/udp"
      - "8082:8080"
    restart: unless-stopped
    depends_on:
      - unifi-db

  unifi-db:
    image: mongo:4.4
    container_name: unifi-db
    environment:
      - MONGO_INITDB_ROOT_USERNAME=unifi
      - MONGO_INITDB_ROOT_PASSWORD=unifi
      - MONGO_INITDB_DATABASE=unifi
    volumes:
      - ./unifi/unifi-db-data:/data/db
    restart: unless-stopped
```

#### 2. Create Directory Structure

```bash
mkdir -p unifi/unifi-data unifi/unifi-db-data
```

#### 3. Add to .gitignore

```bash
echo "unifi/unifi-data/" >> .gitignore
echo "unifi/unifi-db-data/" >> .gitignore
```

#### 4. Start Services

```bash
docker-compose up -d unifi-controller unifi-db
```

#### 5. Complete Setup

Follow "Getting Started" section above for initial configuration.

#### 6. Configure Home Assistant

Add UniFi integration as described in "Home Assistant Integration Configuration" section.

#### 7. Map Devices to Employees

Update `application.yaml` with device_tracker entity names.

## Maintenance Notes

- **Backups**: UniFi creates automatic backups in `unifi-data/data/backup/`
- **Manual Backup**: Settings → System → Backup → Download Backup
- **Updates**: Controller auto-updates within the Docker container
- **Database**: MongoDB data persists in `unifi-db-data/`
- **Logs**: Rotate logs periodically to prevent disk space issues
- **Performance**: Monitor RAM usage - UniFi Controller can be memory-intensive
- **Security**: Change default admin password immediately after setup
- **SSL Certificate**: For production, configure proper SSL certificate in UniFi settings

## Additional Resources

- **UniFi Controller Documentation**: https://help.ui.com/hc/en-us
- **LinuxServer.io Docs**: https://docs.linuxserver.io/images/docker-unifi-network-application
- **Home Assistant UniFi Integration**: https://www.home-assistant.io/integrations/unifi/
- **Ubiquiti Community Forums**: https://community.ui.com/
- **UniFi Subreddit**: https://www.reddit.com/r/Ubiquiti/
