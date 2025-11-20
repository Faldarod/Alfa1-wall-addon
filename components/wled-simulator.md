# WLED Simulator Documentation

## Goal

The WLED Simulator is a Spring Boot application that emulates a real WLED LED controller for testing and development purposes. It provides:

1. **WLED API Compatibility**: Implements the WLED JSON API that Home Assistant expects
2. **Visual LED Simulation**: Web-based interface showing real-time LED colors and states
3. **WebSocket Support**: Real-time updates to the web interface when LED states change
4. **mDNS Service Discovery**: Broadcasts mDNS service for automatic discovery by Home Assistant
5. **Segment Support**: Simulates 5 LED segments (380 LEDs total) for employee visualization

**Key Objectives:**
- Eliminate need for physical WLED hardware during development
- Provide visual feedback for LED control commands
- Enable full testing of AlfaWall system without hardware dependencies
- Simulate real WLED behavior and API responses

## Relevant Articles & Resources

### WLED Project
- **Official WLED**: https://kno.wled.ge/
- **WLED GitHub**: https://github.com/Aircoookie/WLED
- **WLED JSON API**: https://kno.wled.ge/interfaces/json-api/
- **WLED Segments**: https://kno.wled.ge/features/segments/

### Home Assistant WLED Integration
- **Integration Documentation**: https://www.home-assistant.io/integrations/wled/
- **Setup Guide**: https://www.home-assistant.io/integrations/wled/#configuration
- **Discovery**: Uses mDNS/Zeroconf for automatic discovery

### Spring Boot
- **Spring Boot Documentation**: https://docs.spring.io/spring-boot/docs/3.5.7/reference/html/
- **WebSocket Support**: https://docs.spring.io/spring-framework/reference/web/websocket.html
- **Thymeleaf Templates**: https://www.thymeleaf.org/documentation.html

### mDNS/Service Discovery
- **JmDNS Library**: https://github.com/jmdns/jmdns
- **mDNS Protocol**: https://datatracker.ietf.org/doc/html/rfc6762
- **Service Type**: `_wled._tcp.local` (WLED standard)

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Primary programming language |
| Spring Boot | 3.5.7 | Application framework |
| Spring Web | 3.5.7 | REST API endpoints |
| Spring WebSocket | 3.5.7 | Real-time updates to UI |
| Thymeleaf | 3.x | Template engine for HTML views |
| JmDNS | 3.5.9 | mDNS service broadcasting |
| Lombok | - | Code generation |
| Maven | - | Build tool |
| Docker | - | Containerization |

### Dependencies (from pom.xml)
- `spring-boot-starter-web`: REST API endpoints
- `spring-boot-starter-thymeleaf`: HTML templates
- `spring-boot-starter-websocket`: Real-time communication
- `jmdns`: mDNS service discovery broadcasting
- `lombok`: Annotation-based code generation

### Port Mapping
- **8081** (host) → **80** (container): HTTP web server and API

## Architecture

### System Integration

```
Home Assistant WLED Integration
       ↓ (mDNS discovery: _wled._tcp.local)
WLED Simulator (port 80/8081)
       ↓
┌─────────────────────────────────────────┐
│ REST API (/json/*)                      │
│ - GET /json → Full state + info         │
│ - POST /json/state → Update LEDs        │
│ - GET /json/info → Device info          │
└─────────────────────────────────────────┘
       ↓ ↑
┌─────────────────────────────────────────┐
│ WledService (State Management)          │
│ - 5 LED segments (76 LEDs each)         │
│ - Color, brightness, effects            │
└─────────────────────────────────────────┘
       ↓ (broadcasts updates)
┌─────────────────────────────────────────┐
│ WebSocket (/ws)                         │
│ - Real-time state updates               │
└─────────────────────────────────────────┘
       ↓
┌─────────────────────────────────────────┐
│ Web UI (/)                              │
│ - Visual LED strip representation       │
│ - Shows colors, brightness, status      │
└─────────────────────────────────────────┘
```

### Component Architecture

#### 1. **REST Controllers**

**WledController** (`WledController.java`):
- Implements WLED JSON API endpoints
- Handles GET/POST requests from Home Assistant
- Endpoints:
  - `GET /json`: Full device state + info
  - `POST /json/state`: Update LED state
  - `GET /json/info`: Device information
  - `GET /json/eff`: Available effects
  - `GET /json/pal`: Available palettes

**ViewController** (`ViewController.java`):
- Serves HTML page with LED visualization
- `GET /`: Main visualization page

**WledRootController** (`WledRootController.java`):
- Root endpoint compatibility with real WLED
- Additional metadata endpoints

**ConfigApiController** (`ConfigApiController.java`):
- Configuration endpoints (if needed)

#### 2. **Service Layer**

**WledService** (`WledService.java`):
- Manages LED state (segments, colors, brightness)
- Initializes 5 segments with 76 LEDs each (380 total)
- Updates state when Home Assistant sends commands
- Broadcasts updates via WebSocket
- Provides effects and palettes lists

**MdnsService** (`MdnsService.java`):
- Broadcasts mDNS service: `_wled._tcp.local`
- Enables Home Assistant auto-discovery
- Registers on startup, unregisters on shutdown

#### 3. **Domain Models**

**WledState** (`WledState.java`):
- Overall device state (on/off, brightness)
- List of Segment objects
- Transition settings

**Segment** (`Segment.java`):
- Individual LED segment (0-4)
- LED range (start, stop, length)
- Colors (primary, secondary, tertiary)
- Effect, speed, intensity, palette

**WledInfo** (`WledInfo.java`):
- Device metadata (version, name, IP)
- LED configuration (count, type, pins)
- Hardware info (architecture, memory)

**Led** (`Led.java`):
- Individual LED representation for visualization

#### 4. **WebSocket**

**WledWebSocketHandler** (`WledWebSocketHandler.java`):
- Handles WebSocket connections at `/ws`
- Broadcasts state updates to all connected clients
- Used by web UI for real-time visualization

**WebSocketConfig** (`WebSocketConfig.java`):
- Configures WebSocket endpoint and handlers

### Data Flow

#### Home Assistant Controlling LEDs

1. **Home Assistant** sends `light.turn_on` service call:
   ```json
   POST /json/state
   {
     "seg": [{
       "id": 0,
       "on": true,
       "col": [[0, 255, 0]],
       "bri": 255
     }]
   }
   ```

2. **WledController** receives request, calls `WledService.updateState()`

3. **WledService** updates segment 0:
   - Color: Green (0, 255, 0)
   - Brightness: 255 (max)
   - State: On

4. **WebSocket broadcast** sends update to all connected browsers

5. **Web UI** updates visualization showing green LED segment

#### Home Assistant Discovery

1. **MdnsService** broadcasts on startup:
   ```
   Service Type: _wled._tcp.local
   Name: AlfaWall LED Simulator
   Port: 80
   ```

2. **Home Assistant** discovers service via Zeroconf

3. **WLED Integration** queries `/json/info` for device details

4. **Integration setup** creates entities:
   - `light.wled` (main device)
   - `light.wled_segment_0` through `light.wled_segment_4`

## Functional Description

### Use Cases

#### 1. **Visual LED Testing**

**Scenario**: Developer wants to see LED changes in real-time

**Steps**:
1. Start WLED simulator: `docker-compose --profile local-testing up wled-simulator`
2. Open browser to http://localhost:8081
3. See 5 LED segments displayed as colored bars
4. Send command from Home Assistant or alfa-wall-addon
5. Watch LED colors update in real-time

**Example**: When John Doe is detected at office:
- Alfa-wall-addon sends command to turn segment 0 green
- Web UI instantly shows segment 0 turning green
- Brightness and color visually represented

#### 2. **Home Assistant Integration Testing**

**Scenario**: Test WLED integration without physical hardware

**Steps**:
1. WLED simulator broadcasts mDNS service
2. Home Assistant auto-discovers "AlfaWall LED Simulator"
3. Configure WLED integration
4. Test `light.turn_on` service calls
5. Verify LED states via `/json/state` endpoint

#### 3. **API Development and Debugging**

**Scenario**: Develop alfa-wall-addon LED control logic

**Steps**:
1. Query device info: `curl http://localhost:8081/json/info`
2. Get current state: `curl http://localhost:8081/json/state`
3. Update segment color:
   ```bash
   curl -X POST http://localhost:8081/json/state \
        -H "Content-Type: application/json" \
        -d '{"seg":[{"id":0,"col":[[255,0,0]],"bri":255}]}'
   ```
4. Verify change in web UI and API response

#### 4. **Multi-Segment Visualization**

**Scenario**: Visualize multiple employees simultaneously

**Configuration**: 5 segments mapped to 5 employees

**Example**:
- Segment 0 (John): Green
- Segment 1 (Jane): Blue
- Segment 2 (Alice): Magenta
- Segment 3 (Bob): Yellow
- Segment 4 (Wim): Orange

All segments update independently when employees arrive/leave.

### Configuration

#### Docker Compose Configuration

**Location**: `docker-compose.yml`

```yaml
wled-simulator:
  build: ./wled-simulator
  profiles:
    - local-testing
  environment:
    - SERVER_PORT=80  # Internal port
  ports:
    - "8081:80"       # Host:Container mapping
```

#### Application Configuration

**Location**: `wled-simulator/src/main/resources/application.properties` (or .yaml)

```properties
# Server port
server.port=${SERVER_PORT:80}

# Logging
logging.level.nl.alfaone.wledsimulator=DEBUG
logging.level.javax.jmdns=INFO

# Thymeleaf (for templates)
spring.thymeleaf.cache=false  # Disable cache in development
```

#### LED Segment Initialization

**In WledService.java:70-152**:
- 5 segments initialized at startup
- Each segment: 76 LEDs
- Total: 380 LEDs
- Default colors: Red, Green, Blue, Yellow, Magenta

### API Endpoints

#### GET `/json`
**Purpose**: Get full device state and info

**Response**:
```json
{
  "state": {
    "on": true,
    "bri": 128,
    "transition": 7,
    "seg": [
      {
        "id": 0,
        "start": 0,
        "stop": 76,
        "len": 76,
        "on": true,
        "bri": 255,
        "col": [[255, 0, 0], [0, 0, 0], [0, 0, 0]],
        "fx": 0,
        "sx": 128,
        "ix": 128,
        "pal": 0
      }
      // ... segments 1-4
    ]
  },
  "info": {
    "ver": "0.14.1-simulator",
    "name": "AlfaWall LED Simulator",
    "leds": { "count": 380, "rgbw": false }
  },
  "effects": { "0": "Solid", "1": "Blink", ... },
  "palettes": { "0": "Default", "1": "Random Cycle", ... }
}
```

#### POST `/json/state`
**Purpose**: Update LED state

**Request**:
```json
{
  "on": true,
  "bri": 255,
  "seg": [
    {
      "id": 0,
      "col": [[0, 255, 0]],
      "bri": 255,
      "on": true
    }
  ]
}
```

**Response**: Updated state (same format as GET)

#### GET `/json/info`
**Purpose**: Get device information

**Response**:
```json
{
  "ver": "0.14.1-simulator",
  "vid": 2309200,
  "name": "AlfaWall LED Simulator",
  "udpport": 21324,
  "live": false,
  "lm": "WS281x",
  "lip": "192.168.1.100",
  "ws": 0,
  "fxcount": 118,
  "palcount": 67,
  "arch": "esp32",
  "core": "3_3_6",
  "freeheap": 150000,
  "uptime": 3600,
  "opt": 127,
  "brand": "WLED",
  "product": "AlfaWall Simulator",
  "btype": "simulator",
  "mac": "AA:BB:CC:DD:EE:FF",
  "leds": {
    "count": 380,
    "rgbw": false,
    "wv": false,
    "cct": false,
    "pin": [2],
    "pwr": 0,
    "fps": 42,
    "maxpwr": 850,
    "maxseg": 16,
    "seglc": [1, 1, 1, 1, 1]
  }
}
```

#### GET `/json/eff`
**Purpose**: List available effects

**Response**:
```json
{
  "0": "Solid",
  "1": "Blink",
  "2": "Breathe",
  ...
}
```

#### GET `/json/pal`
**Purpose**: List available palettes

**Response**:
```json
{
  "0": "Default",
  "1": "Random Cycle",
  "2": "Primary Color",
  ...
}
```

#### GET `/`
**Purpose**: Web UI visualization

**Response**: HTML page with:
- LED strip visualization
- Real-time WebSocket updates
- Color and brightness display

#### WS `/ws`
**Purpose**: WebSocket for real-time updates

**Message Format**: JSON state update sent on every change

## Developer Documentation

### Prerequisites

- **Java 21** (required)
- **Maven 3.6+**
- **Docker & Docker Compose**
- **Modern web browser** (for visualization UI)

### Getting Started

#### 1. Build the Application

```bash
cd wled-simulator
mvn clean package
```

#### 2. Run Standalone (for development)

```bash
java -jar target/wled-simulator-0.0.1-SNAPSHOT.jar
```

Access at: http://localhost:80

#### 3. Run via Docker Compose

```bash
cd ..
docker-compose --profile local-testing up --build wled-simulator
```

Access at: http://localhost:8081

#### 4. Verify mDNS Broadcasting

**Check logs**:
```bash
docker-compose logs wled-simulator | grep mDNS
```

**Expected output**:
```
mDNS service registered: AlfaWall LED Simulator at port 80
Service type: _wled._tcp.local
```

#### 5. Test API Endpoints

**Get device info**:
```bash
curl http://localhost:8081/json/info
```

**Get current state**:
```bash
curl http://localhost:8081/json/state
```

**Update segment color** (turn segment 0 green):
```bash
curl -X POST http://localhost:8081/json/state \
     -H "Content-Type: application/json" \
     -d '{
       "seg": [{
         "id": 0,
         "col": [[0, 255, 0]],
         "bri": 255,
         "on": true
       }]
     }'
```

#### 6. Open Web UI

Navigate to http://localhost:8081 in browser to see LED visualization.

### Project Structure

```
wled-simulator/
├── src/
│   ├── main/
│   │   ├── java/nl/alfaone/wledsimulator/
│   │   │   ├── controller/
│   │   │   │   ├── WledController.java          # Main WLED API
│   │   │   │   ├── WledRootController.java      # Root endpoints
│   │   │   │   ├── ViewController.java          # Web UI
│   │   │   │   └── ConfigApiController.java     # Config endpoints
│   │   │   ├── service/
│   │   │   │   ├── WledService.java             # State management
│   │   │   │   └── MdnsService.java             # mDNS broadcasting
│   │   │   ├── websocket/
│   │   │   │   └── WledWebSocketHandler.java    # WebSocket handler
│   │   │   ├── config/
│   │   │   │   └── WebSocketConfig.java         # WebSocket config
│   │   │   ├── domain/
│   │   │   │   ├── WledState.java               # State model
│   │   │   │   ├── WledInfo.java                # Device info model
│   │   │   │   ├── Segment.java                 # Segment model
│   │   │   │   ├── Led.java                     # LED model
│   │   │   │   └── LedState.java                # LED state
│   │   │   └── WledSimulatorSpringApplication.java  # Main class
│   │   └── resources/
│   │       ├── templates/
│   │       │   └── index.html                   # Web UI template
│   │       ├── static/                          # Static assets (if any)
│   │       └── application.properties           # Configuration
│   └── test/                                    # Unit tests
├── Dockerfile
└── pom.xml                                      # Maven dependencies
```

### Key Code Locations

#### State Management
- **WledService.java:70-152**: Segment initialization
- **WledService.java:162-179**: State update logic
- **WledService.java:181-205**: Segment update logic

#### API Endpoints
- **WledController.java:29-37**: GET /json (full state)
- **WledController.java:51-55**: POST /json/state (update)
- **WledController.java:78-82**: GET /json/info

#### mDNS Broadcasting
- **MdnsService.java**: Service registration and broadcasting

#### WebSocket
- **WledWebSocketHandler.java**: Real-time update broadcasting
- **WebSocketConfig.java**: WebSocket endpoint configuration

### Testing

#### Unit Tests

**Run all tests**:
```bash
cd wled-simulator
mvn test
```

**Run specific test**:
```bash
mvn test -Dtest=WledServiceTest
```

#### Integration Testing

**Test with Home Assistant**:
1. Start simulator: `docker-compose --profile local-testing up wled-simulator`
2. Start Home Assistant: `docker-compose --profile local-testing up home-assistant`
3. Check for auto-discovery in Home Assistant UI
4. Configure WLED integration
5. Test `light.turn_on` service in Developer Tools

**Test with curl**:
```bash
# Turn segment 0 red
curl -X POST http://localhost:8081/json/state \
     -H "Content-Type: application/json" \
     -d '{"seg":[{"id":0,"col":[[255,0,0]],"bri":255}]}'

# Turn all segments off
curl -X POST http://localhost:8081/json/state \
     -H "Content-Type: application/json" \
     -d '{"on":false}'

# Set brightness to 50%
curl -X POST http://localhost:8081/json/state \
     -H "Content-Type: application/json" \
     -d '{"bri":128}'
```

**Test WebSocket updates**:
1. Open http://localhost:8081 in browser
2. Open browser console (F12)
3. Send API request to update LED state
4. Observe real-time update in browser without refresh

### Debugging

#### Enable Debug Logging

**Add to application.properties**:
```properties
logging.level.nl.alfaone.wledsimulator=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.javax.jmdns=DEBUG
```

#### View Logs in Docker

```bash
docker-compose logs -f wled-simulator
```

#### Common Issues

**Issue**: Home Assistant doesn't discover WLED simulator
- **Solution**:
  - Check mDNS logs: `docker-compose logs wled-simulator | grep mDNS`
  - Verify service is broadcasting: Look for "mDNS service registered"
  - Manually add integration with host `wled-simulator`
  - Ensure containers are on same Docker network

**Issue**: API returns 404
- **Solution**:
  - Verify endpoint: `curl http://localhost:8081/json`
  - Check application is running: `docker ps`
  - Review logs for startup errors

**Issue**: WebSocket not updating UI
- **Solution**:
  - Check WebSocket connection in browser console
  - Verify `/ws` endpoint is accessible
  - Check for CORS or connection errors
  - Reload page

**Issue**: LED colors not displaying correctly
- **Solution**:
  - Verify RGB values are 0-255
  - Check segment ID is 0-4
  - Review WledService state update logic
  - Check browser console for JavaScript errors

### Customization

#### Change Number of Segments

**In WledService.java:70-152**:
```java
// Change loop from 5 to desired number
for (int i = 0; i < 5; i++) {  // Change to: i < 10
    int startLed = i * 76;     // Adjust LED count per segment
    int stopLed = startLed + 76;
    // ...
}
```

**Update device info**:
```java
.leds(WledInfo.LedInfo.builder()
    .count(760)  // Update total: 10 segments x 76 LEDs
    // ...
)
```

#### Add Custom Effects

**In WledService.java:31-52**:
```java
private final List<String> effects = Arrays.asList(
    "Solid", "Blink", "Breathe",
    "Custom Effect 1",  // Add your custom effect
    "Custom Effect 2"
    // ...
);
```

#### Customize Web UI

**Edit**: `src/main/resources/templates/index.html`
- Add CSS styling
- Modify LED visualization
- Add control buttons

## Building from Scratch

### Setting Up WLED Simulator

#### 1. Create Spring Boot Project

**Using Spring Initializr** (https://start.spring.io/):
- **Project**: Maven
- **Language**: Java
- **Spring Boot**: 3.5.7
- **Java**: 21
- **Dependencies**:
  - Spring Web
  - Thymeleaf
  - WebSocket
  - Lombok

#### 2. Add JmDNS Dependency

**In pom.xml**:
```xml
<dependency>
    <groupId>org.jmdns</groupId>
    <artifactId>jmdns</artifactId>
    <version>3.5.9</version>
</dependency>
```

#### 3. Create Domain Models

**WledState.java**, **Segment.java**, **WledInfo.java**, **Led.java**
- Use Lombok @Data, @Builder annotations
- Follow WLED JSON API structure

#### 4. Implement WledService

- Initialize segments with default colors
- Implement `updateState()` logic
- Add WebSocket broadcasting

#### 5. Create REST Controllers

**WledController.java**:
- `GET /json`: Return full state + info
- `POST /json/state`: Update state
- `GET /json/info`: Device info

#### 6. Implement mDNS Broadcasting

**MdnsService.java**:
```java
@Service
public class MdnsService {
    private JmDNS jmdns;

    @PostConstruct
    public void init() throws IOException {
        jmdns = JmDNS.create(InetAddress.getLocalHost());
        ServiceInfo serviceInfo = ServiceInfo.create(
            "_wled._tcp.local.",
            "AlfaWall LED Simulator",
            80,
            "WLED Simulator"
        );
        jmdns.registerService(serviceInfo);
    }
}
```

#### 7. Create WebSocket Handler

**WledWebSocketHandler.java**:
- Handle connections
- Broadcast state updates

**WebSocketConfig.java**:
- Configure `/ws` endpoint

#### 8. Create Web UI

**templates/index.html**:
- Display LED segments as colored bars
- Connect to WebSocket
- Update colors in real-time

#### 9. Create Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 80
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 10. Add to docker-compose.yml

```yaml
wled-simulator:
  build: ./wled-simulator
  environment:
    - SERVER_PORT=80
  ports:
    - "8081:80"
```

#### 11. Test

```bash
docker-compose up --build wled-simulator
curl http://localhost:8081/json/info
```

## Maintenance Notes

- **WLED API Compatibility**: Keep updated with WLED API changes (https://kno.wled.ge/interfaces/json-api/)
- **Spring Boot Updates**: Test compatibility when upgrading Spring Boot
- **mDNS Reliability**: JmDNS can be flaky - ensure proper cleanup on shutdown
- **WebSocket Connections**: Limit concurrent connections if performance degrades
- **Memory Usage**: Monitor if running many simultaneous updates
- **LED Count**: Adjust total LED count if scaling segments
- **Effects/Palettes**: Sync with latest WLED effects list for accurate simulation

## Additional Resources

- **WLED Documentation**: https://kno.wled.ge/
- **WLED GitHub**: https://github.com/Aircoookie/WLED
- **Spring Boot WebSocket**: https://spring.io/guides/gs/messaging-stomp-websocket/
- **JmDNS Documentation**: https://github.com/jmdns/jmdns
- **Home Assistant WLED Integration**: https://www.home-assistant.io/integrations/wled/
- **Thymeleaf Documentation**: https://www.thymeleaf.org/
