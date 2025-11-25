# CLAUDE.md - AlfaWall Addon Development Guide

This file provides guidance to Claude Code (claude.ai/code) when working with the AlfaWall addon source code.

## Repository Purpose

This repository contains the **AlfaWall Addon** - a Spring Boot application that acts as an AI-powered presence assistant for Home Assistant. The addon processes natural language queries about employees and controls WLED LED devices.

## Quick Start Commands

### Building

```bash
# Build the application
mvn clean package

# Skip tests
mvn clean package -DskipTests
```

### Running Locally

```bash
# Run with Maven
mvn spring-boot:run

# Run compiled JAR
java -jar target/alfa-wall-addon-0.0.1-SNAPSHOT.jar
```

### Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=IntentAgentTest

# Run with coverage
mvn clean test jacoco:report
```

### Docker

```bash
# Build image
docker build -t alfa-wall-addon .

# Run container
docker run -p 8080:8080 \
  -e OPENAI_API_KEY=your-key \
  -e SPRING_AI_OPENAI_BASE_URL=https://openrouter.ai/api/v1 \
  alfa-wall-addon
```

## Directory Structure

```
alfa1-wall-addon-repo/
├── src/
│   ├── main/
│   │   ├── java/nl/alfaone/
│   │   │   ├── application/
│   │   │   │   └── agents/        # AI agent implementations
│   │   │   │       ├── PrivacyOfficerAgent.java
│   │   │   │       ├── EmployeeCollectorAgent.java
│   │   │   │       └── ActionAgent.java
│   │   │   ├── domain/             # Core business logic
│   │   │   │   ├── Employee.java
│   │   │   │   ├── Presence.java
│   │   │   │   ├── Intent.java
│   │   │   │   └── AgentContext.java
│   │   │   └── infrastructure/     # External integrations
│   │   │       ├── ConversationApiController.java
│   │   │       ├── HomeAssistantClient.java
│   │   │       └── EmployeeRepository.java
│   │   └── resources/
│   │       └── application.yaml    # Configuration
│   └── test/
├── pom.xml                         # Maven configuration
├── Dockerfile                      # Container build
├── config.yaml                     # Additional config
├── documentation/
│   ├── IMPLEMENTATION.md
│   ├── DATA-FLOW-ARCHITECTURE.md
│   ├── UPGRADE_PLAN_0.2.0.md
│   └── EMBABEL_0.2.0_UPGRADE_RESULTS.md
└── README.md
```

## Architecture

### Hexagonal Architecture (Ports & Adapters)

The codebase follows hexagonal architecture principles:

**Domain Layer** (`nl.alfaone.domain`)
- Core business logic, independent of frameworks
- Entities: Employee, Presence, Intent, AgentContext
- Value objects: QueryInput, SanitizedQuery, PrivacyViolation
- Domain services: EmployeeSearchResult, VisualizationResult

**Application Layer** (`nl.alfaone.application.agents`)
- Agent orchestration using Embabel framework
- PrivacyOfficerAgent: PII detection and sanitization
- EmployeeCollectorAgent: Data collection with @Tool methods
- ActionAgent: LED visualization logic

**Infrastructure Layer** (`nl.alfaone.infrastructure`)
- External system adapters
- ConversationApiController: REST API endpoint
- HomeAssistantClient: Home Assistant REST API integration
- EmployeeRepository: In-memory data storage
- Configuration properties

### Multi-Agent OODA Loop

The system implements an OODA (Observe-Orient-Decide-Act) loop:

1. **PrivacyOfficerAgent** (Observe)
   - Location: `src/main/java/nl/alfaone/application/agents/PrivacyOfficerAgent.java`
   - Detects 8 types of PII (email, phone, credit cards, SSN/BSN, addresses, IP, postcodes)
   - Blocks critical PII (credit cards, SSN/BSN)
   - Sanitizes non-critical PII before processing

2. **EmployeeCollectorAgent** (Orient)
   - Location: `src/main/java/nl/alfaone/application/agents/EmployeeCollectorAgent.java`
   - Multiple @Tool-annotated methods for different data sources
   - In-memory employee repository with semantic search
   - Tools: getCurrentPresence(), filterBySkill(), filterByCustomer(), filterBySchedule(), filterByParking()

3. **ActionAgent** (Decide & Act)
   - Location: `src/main/java/nl/alfaone/application/agents/ActionAgent.java`
   - Context-aware LED colors based on query type:
     - Green: Current presence queries
     - Blue: Scheduled presence
     - Purple: Skills-based queries
     - Yellow: Customer-based queries
     - Orange: Parking queries
   - Controls WLED devices via HomeAssistantClient

### Request Flow

```
User Voice → Home Assistant → POST /api/conversation/process
                                     ↓
                          ConversationApiController:41
                                     ↓
                            PrivacyOfficerAgent
                                     ↓
                          EmployeeCollectorAgent
                                     ↓
                              ActionAgent
                                     ↓
                          HomeAssistantClient → WLED devices
```

## Technology Stack

- **Java 21** - Required version (not Java 25 despite initial docs)
- **Spring Boot 3.5.7** - Main application framework
- **Embabel Agent 0.2.0** - AI agent orchestration with LLM integration
- **Spring AI 1.0.0** - Included as transitive dependency via Embabel, provides @Tool annotation
- **Project Reactor (WebFlux)** - Reactive programming for async Home Assistant API calls
- **Lombok** - Code generation (requires proper Maven annotation processor config)
- **Maven** - Build tool

## Configuration

### Main Configuration File

File: `src/main/resources/application.yaml`

**LLM Configuration** (via environment variables):
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${SPRING_AI_OPENAI_BASE_URL:https://openrouter.ai/api/v1}
      chat:
        options:
          model: ${SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL:openai/gpt-4o-mini}
          temperature: ${SPRING_AI_OPENAI_CHAT_OPTIONS_TEMPERATURE:0.7}

embabel:
  models:
    default-llm: ${EMBABEL_MODELS_DEFAULT_LLM:gpt-4.1-mini}
```

**Home Assistant Integration**:
```yaml
homeassistant:
  base-url: http://home-assistant:8123
  access-token: your-long-lived-access-token
```

**Employee Data Configuration**:
- `employee-led.mappings`: Maps employee names to WLED segment entity IDs
- `employee-device.mappings`: Maps employee names to device_tracker entities
- `employee-data.employees`: Full employee database (skills, customers, parking, schedules)

## Key Implementation Details

### Embabel 0.2.0 + Spring AI Integration

**Current State** (see `documentation/IMPLEMENTATION.md`):
- ✅ Upgraded to Embabel 0.2.0 + Spring AI 1.0.0
- ✅ All data collection methods annotated with Spring AI's @Tool
- ⏳ Using keyword routing temporarily while finalizing LLM tool integration
- ⏳ Need to implement PromptRunner.withToolObject(this) pattern

**Important Discovery**:
- `@Tool` annotation comes from Spring AI (`org.springframework.ai.tool.annotation.Tool`), NOT from Embabel
- Embabel 0.2.0 includes Spring AI 1.0.0 as transitive dependency
- Integration pattern: Use `PromptRunner.withToolObject(Object)` to register tool objects
- Collections (List<Employee>) fully supported as @Tool return types

### Home Assistant REST API Integration

The `HomeAssistantClient` uses Spring WebFlux for reactive HTTP:
- WebClient configured with base URL and bearer token auth
- Asynchronous calls to Home Assistant REST API
- Endpoints: `/api/states/{entity_id}`, `/api/services/light/turn_on`, etc.

Location: `src/main/java/nl/alfaone/infrastructure/HomeAssistantClient.java`

### Lombok Configuration

Lombok requires proper Maven annotation processor configuration:
- See `pom.xml:79-85` for annotation processor paths
- Use `@Data`, `@Builder`, `@Value` for domain objects
- All domain classes use Lombok annotations

## Testing the Application

### Unit Tests

Run unit tests with Maven:
```bash
mvn test
```

### Integration Testing

For full integration testing with Home Assistant and WLED simulator, use the separate testing environment:

**Repository**: [Alfa1-wall-running-local](https://github.com/yourorg/Alfa1-wall-running-local)

This provides:
- Docker Compose orchestration
- Home Assistant with HACS auto-installation
- WLED Simulator for visual feedback
- Mock device_tracker entities

### Testing Queries Directly

Test the conversation endpoint:
```bash
curl -X POST http://localhost:8080/api/conversation/process \
  -H "Content-Type: application/json" \
  -d '{"text":"Who knows Java?"}'
```

**Example queries**:
- "Who knows Java?" → Filters by skill
- "Who is here now?" → Checks current presence
- "Who has parking today?" → Filters by parking
- "Who works for ACME Corp?" → Filters by customer
- "Who is coming today?" → Filters by schedule

## Common Development Tasks

### Adding a New Agent

1. Create agent class in `src/main/java/nl/alfaone/application/agents/`
2. Annotate with `@Agent` from Embabel
3. Define actions with `@Action` annotation
4. Update request flow in ConversationApiController

### Adding a New @Tool Method

1. Add method to EmployeeCollectorAgent
2. Annotate with `@Tool(description="...")`
3. Method can return Employee, List<Employee>, or custom types
4. Tool will be automatically available to LLM

### Modifying Employee Data

Edit `src/main/resources/application.yaml`:
```yaml
employee-data:
  employees:
    - name: "John Doe"
      skills: ["Java", "Spring Boot"]
      customer: "ACME Corp"
      # ... etc
```

### Changing LED Colors

Modify ActionAgent color logic:
```java
private String determineColor(String queryType) {
    return switch (queryType) {
        case "PRESENCE" -> "00FF00";  // Green
        case "SKILLS" -> "800080";     // Purple
        // ... etc
    };
}
```

## Important Notes

- **No PII logging**: System must never log, persist, or return PII
- **Stateless processing**: No database, all state in memory
- **Privacy-first design**: PrivacyOfficerAgent is always first in pipeline
- **Async operations**: Use WebClient (not RestTemplate) for external calls
- **Lombok dependency**: Requires annotation processor in IDE and Maven
- **Java 21 required**: Despite initial docs mentioning Java 25

## Documentation References

- **[IMPLEMENTATION.md](documentation/IMPLEMENTATION.md)** - Detailed implementation including Embabel 0.2.0 upgrade
- **[DATA-FLOW-ARCHITECTURE.md](documentation/DATA-FLOW-ARCHITECTURE.md)** - System architecture and data flow diagrams
- **[UPGRADE_PLAN_0.2.0.md](documentation/UPGRADE_PLAN_0.2.0.md)** - Embabel upgrade plan from 0.1.4 to 0.2.0
- **[EMBABEL_0.2.0_UPGRADE_RESULTS.md](documentation/EMBABEL_0.2.0_UPGRADE_RESULTS.md)** - Upgrade execution results
- **[README.md](README.md)** - Project overview and getting started

## Related Repositories

- **[Alfa1-wall-running-local](https://github.com/yourorg/Alfa1-wall-running-local)** - Complete local testing environment with Docker Compose, Home Assistant, WLED simulator, and mock devices
- **Alfa1-wall-addon-hacs** - HACS distribution package for Home Assistant Community Store

## Support

For issues related to:
- **Application code**: File issues in this repository
- **Local testing environment**: See Alfa1-wall-running-local repository
- **Home Assistant integration**: Check Home Assistant documentation
- **Embabel framework**: See Embabel documentation
