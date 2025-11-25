# AlfaWall Addon - AI-Powered Presence Assistant

Spring Boot application that processes natural language queries about employee presence and controls WLED LED devices via Home Assistant.

## Overview

AlfaWall is an AI-powered employee presence assistant that integrates with Home Assistant to provide visual feedback on LED strips. The system uses a multi-agent architecture built with the Embabel Agent framework to process natural language queries, identify employees, and control WLED devices.

**Example flow**: User asks "Who knows Java?" → AlfaWall processes query → Identifies matching employees → Lights up their LED segments → Returns spoken response.

## Features

- **Multi-agent AI architecture** using Embabel Agent 0.2.0
  - PrivacyOfficerAgent: PII detection and sanitization
  - EmployeeCollectorAgent: Data collection with multiple @Tool methods
  - ActionAgent: LED visualization and control
- **Natural language processing** for employee queries
- **Home Assistant REST API integration** for device control
- **WLED LED control** with context-aware colors
- **Privacy-first design** - no PII logged or persisted
- **Stateless processing** - all data in memory only

## Technology Stack

- **Java 21** - Required Java version
- **Spring Boot 3.5.7** - Main application framework
- **Embabel Agent 0.2.0** - AI agent orchestration framework
- **Spring AI 1.0.0** - Provides @Tool annotation (transitive via Embabel)
- **Project Reactor (WebFlux)** - Reactive programming for async API calls
- **Lombok** - Code generation for domain objects
- **Maven** - Build tool

## Architecture

### Multi-Agent OODA Loop

The system implements an OODA (Observe-Orient-Decide-Act) loop:

1. **PrivacyOfficerAgent** (Observe)
   - Detects 8 types of PII
   - Blocks critical PII (credit cards, SSN/BSN)
   - Sanitizes non-critical PII

2. **EmployeeCollectorAgent** (Orient)
   - Multiple @Tool-annotated methods for data sources
   - In-memory employee repository with semantic search
   - Tools: getCurrentPresence(), filterBySkill(), filterByCustomer(), filterBySchedule(), filterByParking()

3. **ActionAgent** (Decide & Act)
   - Context-aware LED colors based on query type
   - Controls WLED devices via Home Assistant

### Request Flow

```
User Voice → Home Assistant → POST /api/conversation/process
                                     ↓
                          ConversationApiController
                                     ↓
                            PrivacyOfficerAgent
                                     ↓
                          EmployeeCollectorAgent
                                     ↓
                              ActionAgent
                                     ↓
                          HomeAssistantClient → WLED devices
```

## Building

```bash
mvn clean package
```

## Running

### Standalone (requires Home Assistant)

```bash
java -jar target/alfa-wall-addon-0.0.1-SNAPSHOT.jar
```

### With Docker

```bash
docker build -t alfa-wall-addon .
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=your-key \
  alfa-wall-addon
```

## Configuration

Configuration file: `src/main/resources/application.yaml`

### LLM Configuration

Set via environment variables:

```bash
OPENAI_API_KEY=your-key-here
SPRING_AI_OPENAI_BASE_URL=https://openrouter.ai/api/v1
SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL=openai/gpt-4o-mini
SPRING_AI_OPENAI_CHAT_OPTIONS_TEMPERATURE=0.7
EMBABEL_MODELS_DEFAULT_LLM=gpt-4.1-mini
```

### Home Assistant Integration

```yaml
homeassistant:
  base-url: http://home-assistant:8123
  access-token: your-long-lived-access-token
```

### Employee Data

Configured in `application.yaml`:
- `employee-led.mappings`: Maps names to WLED segment entity IDs
- `employee-device.mappings`: Maps names to device_tracker entities
- `employee-data.employees`: Full employee database (skills, customers, parking, schedules)

## API

### Process Conversation

**Endpoint**: `POST /api/conversation/process`

**Request**:
```json
{
  "text": "Who knows Java?"
}
```

**Response**:
```json
{
  "response": {
    "speech": {
      "plain": {
        "speech": "3 employees match your query: John Doe, Sarah Chen, Mike Johnson."
      }
    },
    "language": "en",
    "response_type": "action_done"
  },
  "conversation_id": "generated-id"
}
```

### Example Queries

- "Who knows Java?" → Filters by skill (purple LEDs)
- "Who is here now?" → Current presence (green LEDs)
- "Who has parking today?" → Parking assignments (orange LEDs)
- "Who works for ACME Corp?" → Customer filter (yellow LEDs)
- "Who is coming today?" → Schedule check (blue LEDs)

## Testing

Run all tests:
```bash
mvn test
```

Run specific test:
```bash
mvn test -Dtest=IntentAgentTest
```

## Testing Locally

For a complete local testing environment with Home Assistant, WLED simulator, and mock devices, see:

**[Alfa1-wall-running-local](https://github.com/yourorg/Alfa1-wall-running-local)** - Local testing environment repository

## Project Goals

- Build a Spring Boot conversation agent for Home Assistant
- Implement agentic architecture with privacy-first design
- Integrate with external REST APIs (future: Kantoordagen-app)
- Integrate with UniFi via Home Assistant for presence detection
- Control WLED LEDs via Home Assistant REST API
- No persistent database - stateless processing only
- No PII should be logged, persisted, or returned

## Constraints

- Java 21 (not Java 25 despite initial docs)
- Maven, Spring Boot 3.5.7
- Hexagonal Architecture (Domain, Application, Infrastructure)
- Embabel Agent 0.2.0
- Stateless processing only
- Privacy-first: no PII logging or persistence
- Async WebClient calls where possible

## Future Extensions

- Support for different LLMs (Ollama for local execution)
- React to Home Assistant events/sensor changes
- Multi-lingual support

## Documentation

- **[IMPLEMENTATION.md](documentation/IMPLEMENTATION.md)** - Detailed implementation guide including Embabel 0.2.0 integration
- **[DATA-FLOW-ARCHITECTURE.md](documentation/DATA-FLOW-ARCHITECTURE.md)** - System architecture and data flow
- **[UPGRADE_PLAN_0.2.0.md](documentation/UPGRADE_PLAN_0.2.0.md)** - Embabel upgrade plan
- **[CLAUDE.md](CLAUDE.md)** - Development guide for Claude Code

## Related Repositories

- **[Alfa1-wall-running-local](https://github.com/yourorg/Alfa1-wall-running-local)** - Complete local testing environment with Docker Compose
- **Alfa1-wall-addon-hacs** - HACS distribution package

## License

See LICENSE file.
