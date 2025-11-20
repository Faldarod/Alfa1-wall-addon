# Home Assistant Documentation

## Goal

Home Assistant serves as the central hub for home automation and device management in the AlfaWall system. It acts as:

1. **Device Management Platform**: Manages and controls WLED LED strip devices
2. **Conversation Interface**: Provides user interaction through the built-in conversation integration
3. **Device Tracker Integration**: Integrates with UniFi Controller to track device presence
4. **API Gateway**: Exposes REST API for the alfa-wall-addon to control devices

**Key Objectives:**
- Host and manage WLED device integrations
- Provide conversation interface for natural language queries
- Forward conversation requests to alfa-wall-addon
- Track device presence via UniFi integration
- Control LED strips based on alfa-wall-addon commands

## Relevant Articles & Resources

### Official Home Assistant Documentation
- **Main Documentation**: https://www.home-assistant.io/docs/
- **Installation**: https://www.home-assistant.io/installation/
- **Configuration**: https://www.home-assistant.io/docs/configuration/

### Key Integrations Used

#### 1. Conversation Integration
- **Documentation**: https://www.home-assistant.io/integrations/conversation/
- **Purpose**: Process natural language voice/text commands
- **Custom Agent Setup**: https://www.home-assistant.io/integrations/conversation/#custom-agents

#### 2. WLED Integration
- **Documentation**: https://www.home-assistant.io/integrations/wled/
- **Purpose**: Control WLED LED strips
- **Features**: Segments, colors, brightness, effects
- **Discovery**: Automatic via mDNS/Zeroconf

#### 3. UniFi Network Integration
- **Documentation**: https://www.home-assistant.io/integrations/unifi/
- **Purpose**: Device tracking via UniFi Controller
- **Features**: Presence detection, device_tracker entities
- **Setup**: https://www.home-assistant.io/integrations/unifi/#configuration

#### 4. REST API
- **Documentation**: https://developers.home-assistant.io/docs/api/rest/
- **Authentication**: Long-lived access tokens
- **Endpoints**: `/api/services/light/turn_on`, `/api/states`, etc.

### Community Resources
- **Home Assistant Community**: https://community.home-assistant.io/
- **WLED Project**: https://kno.wled.ge/
- **UniFi Controller Setup**: https://help.ui.com/hc/en-us/categories/200320654-UniFi-Network

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Home Assistant Core | stable (latest) | Home automation platform |
| Python | 3.12+ | Home Assistant runtime |
| Docker | - | Containerization |
| SQLite | - | Default database for state/history |
| Jinja2 | - | Template engine |

### Docker Image
- **Image**: `ghcr.io/home-assistant/home-assistant:stable`
- **Registry**: GitHub Container Registry
- **Updates**: Automatic via `:stable` tag

### Port Mappings
- **8123**: Web UI and REST API (HTTP)
- **51827/tcp**: HomeKit integration (if enabled)

## Architecture

### System Integration Overview

```
User → Home Assistant UI (Conversation)
            ↓
       Conversation Integration
            ↓
     Custom Conversation Agent (alfawall_conversation)
            ↓
       HTTP POST to alfa-wall-addon:8080/api/conversation/process
            ↓
       Response ← alfa-wall-addon
            ↓
     Home Assistant receives control commands
            ↓
       WLED Integration executes light.turn_on service
            ↓
       WLED Device updates LED colors
```

### Configuration Architecture

Home Assistant uses YAML-based configuration:

```
home-assistant/
└── config/
    ├── configuration.yaml    # Main configuration
    ├── automations.yaml      # Automations (if any)
    ├── scripts.yaml          # Scripts (if any)
    ├── scenes.yaml           # Scenes (if any)
    └── .storage/             # Internal storage (JSON)
```

### Key Components in This Project

#### 1. **Conversation Integration**
- **Built-in**: Enabled by default in modern Home Assistant
- **Configuration**: Defined in `configuration.yaml`
- **Purpose**: Process natural language queries

#### 2. **Custom Conversation Agent (alfawall_conversation)**
- **Type**: Custom external conversation agent
- **Endpoint**: `http://alfa-wall-addon:8080/api/conversation/process`
- **Protocol**: HTTP POST with JSON payload
- **Configuration**:
  ```yaml
  alfawall_conversation:
    name: "Alfa1 AI Employee Presence Agent"
    url: "http://alfa-wall-addon:8080/api/conversation/process"
  ```

#### 3. **WLED Integration**
- **Discovery**: Automatic via mDNS (requires wled-simulator to broadcast)
- **Entities Created**:
  - `light.wled` (main device)
  - `light.wled_segment_0`, `light.wled_segment_1`, etc. (individual segments)
- **Control**: Via `light.turn_on` and `light.turn_off` services

#### 4. **UniFi Integration**
- **Setup**: Manual configuration required
- **Connection**: To `unifi-controller:8443` (HTTPS)
- **Entities**: `device_tracker.*` entities for tracked devices
- **Authentication**: UniFi admin credentials

## Functional Description

### Use Cases

#### 1. **Natural Language Query Processing**

**User Action**: User types "Who is at the office?" in Home Assistant conversation

**Flow**:
1. Home Assistant receives input via conversation UI
2. Routes to `alfawall_conversation` custom agent
3. Sends HTTP POST to `alfa-wall-addon:8080/api/conversation/process`:
   ```json
   {
     "text": "Who is at the office?",
     "language": "en",
     "conversation_id": "abc123"
   }
   ```
4. Alfa-wall-addon processes and returns:
   ```json
   {
     "speech": {
       "plain": {
         "speech": "3 employees are at the office: John, Jane, Alice"
       }
     }
   }
   ```
5. Home Assistant displays response to user

#### 2. **LED Control via REST API**

**Trigger**: Alfa-wall-addon determines which employees to visualize

**Flow**:
1. Alfa-wall-addon calls Home Assistant REST API:
   ```bash
   POST http://home-assistant:8123/api/services/light/turn_on
   Authorization: Bearer <access_token>

   {
     "entity_id": "light.wled_segment_0",
     "rgb_color": [0, 255, 0],
     "brightness": 255
   }
   ```
2. Home Assistant forwards command to WLED integration
3. WLED integration sends HTTP request to WLED device/simulator
4. LED strip updates to show green color at full brightness

#### 3. **Device Presence Tracking**

**Purpose**: Track employee devices to determine office presence

**Setup**:
1. UniFi Controller connected to network
2. UniFi integration configured in Home Assistant
3. Device MAC addresses mapped to employees in `application.yaml`

**Flow**:
1. UniFi Controller detects device on network (phone, laptop)
2. Home Assistant creates/updates `device_tracker.john_phone` entity
3. State becomes `home` or `not_home`
4. Alfa-wall-addon queries state via REST API:
   ```bash
   GET http://home-assistant:8123/api/states/device_tracker.john_phone
   ```
5. Uses state to determine employee presence

#### 4. **WLED Device Discovery**

**How it works**:
1. WLED simulator broadcasts mDNS service: `_wled._tcp.local`
2. Home Assistant automatically discovers WLED device
3. Creates WLED integration with all segments as separate entities
4. Entities become available for control:
   - `light.wled` (master)
   - `light.wled_segment_0` through `light.wled_segment_4`

### Configuration

**Location**: `home-assistant/config/configuration.yaml`

```yaml
# Enable built-in conversation integration
conversation:
assist_pipeline:

# AlfaWall AI Presence Agent - Custom Integration
alfawall_conversation:
  name: "AlfaWall AI Presence Agent"
  url: "http://alfa-wall-addon:8080/api/conversation/process"
```

**Docker Configuration** (in `docker-compose.yml`):
```yaml
home-assistant:
  image: ghcr.io/home-assistant/home-assistant:stable
  profiles:
    - local-testing
  volumes:
    - ./home-assistant/config:/config
  ports:
    - "8123:8123"
  depends_on:
    - unifi-controller
```

### REST API Usage

#### Authentication
All API calls require a Long-Lived Access Token:

**Generate Token**:
1. Open Home Assistant UI (http://localhost:8123)
2. Click profile (bottom left)
3. Scroll to "Long-Lived Access Tokens"
4. Click "Create Token"
5. Copy token and add to `application.yaml`

**Usage in requests**:
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8123/api/states
```

#### Common API Endpoints

**Get all states**:
```bash
GET /api/states
```

**Get specific entity state**:
```bash
GET /api/states/light.wled_segment_0
```

**Call a service (turn on light)**:
```bash
POST /api/services/light/turn_on
Content-Type: application/json
Authorization: Bearer <token>

{
  "entity_id": "light.wled_segment_0",
  "rgb_color": [0, 255, 0],
  "brightness": 255
}
```

**Turn off light**:
```bash
POST /api/services/light/turn_off
Content-Type: application/json

{
  "entity_id": "light.wled_segment_0"
}
```

## Developer Documentation

### Prerequisites

- **Docker & Docker Compose**
- **Network access** to UniFi Controller (if using device tracking)
- **WLED device or simulator** on same network

### Getting Started

#### 1. First-Time Setup

**Start Home Assistant**:
```bash
docker-compose --profile local-testing up home-assistant
```

**Access UI**:
- Open browser to http://localhost:8123
- Complete onboarding wizard:
  - Create admin account
  - Set up location and timezone
  - Skip mobile app setup (optional)

**Create Long-Lived Access Token**:
1. Go to Profile → Security
2. Scroll to "Long-Lived Access Tokens"
3. Create token named "alfa-wall-addon"
4. Copy token to `alfa-wall-addon/src/main/resources/application.yaml`:
   ```yaml
   homeassistant:
     access-token: "YOUR_TOKEN_HERE"
   ```

#### 2. Configure WLED Integration

**Automatic Discovery** (recommended):
1. Start wled-simulator: `docker-compose --profile local-testing up wled-simulator`
2. In Home Assistant UI, go to Settings → Devices & Services
3. WLED should appear in "Discovered" section
4. Click "Configure" and complete setup
5. Verify entities: `light.wled_segment_0`, `light.wled_segment_1`, etc.

**Manual Setup** (if auto-discovery fails):
1. Go to Settings → Devices & Services
2. Click "Add Integration"
3. Search for "WLED"
4. Enter host: `wled-simulator` or `192.168.x.x`
5. Complete setup

#### 3. Configure UniFi Integration

**Prerequisites**:
- UniFi Controller running and accessible
- Admin credentials for UniFi Controller

**Setup**:
1. Go to Settings → Devices & Services
2. Click "Add Integration"
3. Search for "UniFi Network"
4. Enter:
   - Host: `unifi-controller`
   - Port: `8443`
   - Username: `admin`
   - Password: (your UniFi password)
   - Verify SSL: `false` (self-signed cert)
5. Complete setup
6. Go to integration options:
   - Enable "Track network clients"
   - Set detection interval (default: 30 seconds)

#### 4. Verify Setup

**Check WLED entities**:
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8123/api/states/light.wled_segment_0
```

**Check device tracker entities**:
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8123/api/states/device_tracker.john_phone
```

**Test conversation agent**:
```bash
curl -X POST http://localhost:8123/api/conversation/process \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"text": "Who is at the office?", "language": "nl"}'
```

### Project Structure

```
home-assistant/
└── config/
    ├── configuration.yaml     # Main configuration file
    ├── automations.yaml       # Automation rules (if any)
    ├── scripts.yaml           # Custom scripts
    ├── .storage/              # Internal storage (auto-managed)
    │   ├── core.config_entries  # Integration configurations
    │   ├── core.entity_registry # Entity registry
    │   └── core.device_registry # Device registry
    └── home-assistant.log     # Log file
```

### Configuration Files

#### `configuration.yaml`
Main configuration file for Home Assistant. Defines:
- Enabled integrations
- Custom conversation agents
- Basic settings

**Minimal AlfaWall configuration**:
```yaml
# Default config (includes: conversation, zeroconf, etc.)
default_config:

# Custom conversation agent
alfawall_conversation:
  name: "AlfaWall AI Presence Agent"
  url: "http://alfa-wall-addon:8080/api/conversation/process"
```

#### `.storage/` Directory
**IMPORTANT**: Do not manually edit files in `.storage/`
- Auto-managed by Home Assistant
- Contains integration configs, entity registry, device registry
- Stored as JSON
- Backed up automatically by Home Assistant

### Debugging

#### View Logs

**In Docker**:
```bash
docker-compose logs -f home-assistant
```

**In UI**:
- Go to Settings → System → Logs
- Or directly: http://localhost:8123/config/logs

**Log file**:
```bash
cat home-assistant/config/home-assistant.log
```

#### Enable Debug Logging

Add to `configuration.yaml`:
```yaml
logger:
  default: info
  logs:
    homeassistant.components.wled: debug
    homeassistant.components.unifi: debug
    homeassistant.components.conversation: debug
```

Restart Home Assistant to apply.

#### Common Issues

**Issue**: WLED not discovered
- **Solution**:
  - Check wled-simulator is running: `docker ps`
  - Check mDNS broadcasting: Look for "mDNS service registered" in wled-simulator logs
  - Manually add WLED integration with host `wled-simulator`

**Issue**: UniFi integration fails to connect
- **Solution**:
  - Verify UniFi Controller is accessible: `curl -k https://localhost:8443`
  - Check credentials
  - Disable SSL verification in integration config

**Issue**: Conversation agent not responding
- **Solution**:
  - Check alfa-wall-addon is running: `docker ps`
  - Test endpoint directly: `curl http://localhost:8080/api/conversation/process`
  - Check logs for both Home Assistant and alfa-wall-addon

**Issue**: Device tracker entities not created
- **Solution**:
  - Check UniFi integration is configured to track clients
  - Ensure devices are connected to UniFi network
  - Check entity registry: Settings → Devices & Services → UniFi → Devices

### Testing

#### Test WLED Control

**Via Developer Tools** (in UI):
1. Go to Developer Tools → Services
2. Select service: `light.turn_on`
3. Target: `light.wled_segment_0`
4. Service data:
   ```yaml
   rgb_color: [255, 0, 0]
   brightness: 255
   ```
5. Click "Call Service"

**Via API**:
```bash
curl -X POST http://localhost:8123/api/services/light/turn_on \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "entity_id": "light.wled_segment_0",
       "rgb_color": [255, 0, 0],
       "brightness": 255
     }'
```

#### Test Conversation

**Via UI**:
1. Click conversation icon (bottom right)
2. Type: "Who is at the office?"
3. Verify response from alfa-wall-addon

**Via API**:
```bash
curl -X POST http://localhost:8123/api/conversation/process \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"text": "Who is at the office?", "language": "nl"}'
```

## Building from Scratch

### Setting Up Home Assistant

#### 1. Install Home Assistant

**Using Docker Compose**:
```yaml
version: '3.8'
services:
  home-assistant:
    image: ghcr.io/home-assistant/home-assistant:stable
    volumes:
      - ./home-assistant/config:/config
    ports:
      - "8123:8123"
    restart: unless-stopped
```

**Start**:
```bash
docker-compose up -d home-assistant
```

#### 2. Complete Onboarding

1. Navigate to http://localhost:8123
2. Create admin account
3. Set location and timezone
4. Complete setup wizard

#### 3. Configure Custom Conversation Agent

Create/edit `config/configuration.yaml`:
```yaml
# Enable built-in conversation
conversation:
assist_pipeline:

# Add custom conversation agent
alfawall_conversation:
  name: "AlfaWall AI Presence Agent"
  url: "http://alfa-wall-addon:8080/api/conversation/process"
```

Restart Home Assistant:
```bash
docker-compose restart home-assistant
```

#### 4. Set Up Integrations

**WLED**:
- Settings → Devices & Services → Add Integration → WLED
- Or wait for auto-discovery

**UniFi**:
- Settings → Devices & Services → Add Integration → UniFi Network
- Enter UniFi Controller details

#### 5. Create Access Token

1. Profile → Security → Long-Lived Access Tokens
2. Create token for "alfa-wall-addon"
3. Copy to alfa-wall-addon configuration

#### 6. Test Integration

Use Developer Tools or curl to test services and entities.

## Maintenance Notes

- **Backups**: Home Assistant creates automatic backups in Settings → System → Backups
- **Updates**: Pull latest `:stable` image and restart: `docker-compose pull && docker-compose up -d`
- **Integration Updates**: Check for integration updates in Settings → Devices & Services
- **Configuration Validation**: Use Configuration → Check Configuration before restarting
- **Logs**: Monitor logs regularly for warnings/errors
- **Access Token**: Store securely, rotate periodically for security
- **Port 8123**: Keep open for web UI and API access

## Additional Resources

- **Home Assistant Forums**: https://community.home-assistant.io/
- **Home Assistant Discord**: https://discord.gg/home-assistant
- **WLED Documentation**: https://kno.wled.ge/
- **UniFi Documentation**: https://help.ui.com/hc/en-us
