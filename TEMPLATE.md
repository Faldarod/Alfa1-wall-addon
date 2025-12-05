# AlfaWall Conversation API - Developer Template

## Overview

This is a **template repository** for building AI-powered conversation APIs that integrate with Home Assistant. The system uses a multi-agent architecture to process natural language queries about employees and control WLED LED devices for visual feedback.

**What this template provides:**
- 🤖 Multi-agent OODA loop architecture (Observe → Orient → Decide → Act)
- 🔒 GDPR-compliant PII detection and sanitization
- 🎯 LLM-driven tool selection for complex queries
- 🏠 Home Assistant Conversation API integration
- 💡 WLED LED wall visualization
- ⚡ Reactive programming with Spring WebFlux
- 🧠 Embabel 0.2.0 + Spring AI framework

## What You Need to Customize

### 1. Employee Data
**File:** `src/main/resources/application.yaml`

Replace the example employees (Jane Developer, John Designer) with your organization's team:

```yaml
employee-data:
  employees:
    - id: your-employee-id
      name: "Employee Name"
      email: employee@yourcompany.com
      background: "Semantic search description..."
      skills: [Skill1, Skill2, ...]
      customers: [...]
      parking: {...}
      schedule: {...}
```

### 2. Home Assistant Connection
**Environment Variables:**
- `HOMEASSISTANT_BASE_URL`: Your Home Assistant URL (default: `http://home-assistant:8123`)
- `HOMEASSISTANT_TOKEN`: Long-lived access token from Home Assistant

**How to get a token:**
1. Open Home Assistant → Profile → Long-Lived Access Tokens
2. Click "Create Token"
3. Copy the token and set it as environment variable

### 3. WLED Segment Mappings
**File:** `src/main/resources/application.yaml`

Map employees to their LED segments:

```yaml
employee-led:
  mappings:
    "Employee Name":
      entity-id: light.wled_segment_0  # Your WLED entity ID
      color: "#00FF00"                  # Default color
      brightness: 255
```

### 4. Device Tracker Entities
**File:** `src/main/resources/application.yaml`

Map employees to their device trackers for presence detection:

```yaml
employee-device:
  use-mock-data: false  # Set to true for testing
  mappings:
    "Employee Name":
      - device_tracker.employee_phone
```

### 5. LLM API Configuration
**Environment Variables:**
- `OPENAI_API_KEY`: Your OpenAI or OpenRouter API key
- `SPRING_AI_OPENAI_BASE_URL`: API endpoint (default: OpenRouter)
- `SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL`: Model name (default: gpt-4o-mini)

**Supported Providers:**
- OpenRouter (default) - Aggregates multiple LLM providers
- OpenAI - Direct OpenAI API
- Any OpenAI-compatible API

---

## Quick Start

### Prerequisites
- Java 21 or higher
- Maven 3.6+
- Home Assistant instance (for production) or mock data (for testing)
- OpenAI/OpenRouter API key

### Step 1: Copy Configuration Files
```bash
# Copy example configuration
cp src/main/resources/application-example.yaml src/main/resources/application.yaml

# Copy environment variables template
cp .env.example .env
```

### Step 2: Configure Environment
Edit `.env` and set your values:
```bash
OPENAI_API_KEY=your-api-key-here
HOMEASSISTANT_TOKEN=your-token-here
HOMEASSISTANT_BASE_URL=http://localhost:8123
```

### Step 3: Update Employee Data
Edit `src/main/resources/application.yaml` and replace Jane/John with your team members.

### Step 4: Build and Run
```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/alfa-wall-addon-0.0.1-SNAPSHOT.jar
```

### Step 5: Test the API
```bash
curl -X POST http://localhost:8080/api/conversation/process \
  -H "Content-Type: application/json" \
  -d '{"text":"Who knows Java?"}'
```

**Expected Response:**
```json
{
  "response": {
    "speech": {
      "plain": {
        "speech": "1 employee matches your query: Jane Developer."
      }
    },
    "language": "en",
    "response_type": "action_done"
  },
  "conversation_id": "generated-uuid"
}
```

---

## Architecture

### Multi-Agent OODA Loop

The system implements an OODA (Observe-Orient-Decide-Act) decision-making loop:

```
User Query
    ↓
┌───────────────────────────────────────────────────┐
│ 1. PrivacyOfficerAgent (OBSERVE)                 │
│    - Detects PII (emails, phones, SSN, etc.)     │
│    - Blocks critical PII (credit cards)          │
│    - Sanitizes non-critical PII                  │
│    Output: SanitizedQuery                        │
└───────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────┐
│ 2. EmployeeCollectorAgent (ORIENT)               │
│    - LLM-driven tool selection                   │
│    - Calls appropriate @Tool methods             │
│    - Supports multi-criteria queries (AND/OR)    │
│    Output: EmployeeSearchResult + QueryType      │
└───────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────┐
│ 3. ActionAgent (DECIDE & ACT)                    │
│    - Determines LED colors by QueryType          │
│    - Turns matched employees ON                  │
│    - Turns unmatched employees OFF               │
│    Output: VisualizationResult                   │
└───────────────────────────────────────────────────┘
    ↓
WLED LED Wall + Spoken Response
```

### Request Flow

**File:** `src/main/java/nl/alfaone/infrastructure/ConversationApiController.java:41`

```
POST /api/conversation/process
  ↓
ConversationApiController.processConversation()
  ↓
PrivacyOfficerAgent.sanitizeQuery(QueryInput)
  ↓
EmployeeCollectorAgent.collectEmployees(SanitizedQuery)
  ↓
ActionAgent.visualizeEmployees(EmployeeSearchResult)
  ↓
HomeAssistantClient.callService() → WLED devices
  ↓
Return ConversationResponse
```

### Query Types and LED Colors

The system automatically determines query type and assigns context-appropriate colors:

| Query Type | Description | LED Color | Example Queries |
|-----------|-------------|-----------|-----------------|
| `PRESENCE` | Current presence | Green | "Who is here now?" |
| `SKILLS` | Skills/expertise | Purple | "Who knows Java?" |
| `CUSTOMER` | Customer assignments | Yellow | "Who works for ACME?" |
| `SCHEDULE` | Future presence | Blue | "Who is coming today?" |
| `PARKING` | Parking assignments | Orange | "Who has parking?" |
| `GENERAL` | Semantic search | Employee's default | "Backend experts" |

### LLM Tool Selection

**How it works:**
1. User query sent to LLM with available @Tool methods
2. LLM intelligently selects and calls tools (e.g., `filterBySkill("Java")`)
3. For multi-criteria queries, LLM uses `intersectEmployeeLists()` (AND) or `combineEmployeeLists()` (OR)
4. Falls back to keyword routing if LLM fails

**Example - Multi-Criteria Query:**
```
Query: "Java developers coming today"

LLM Tool Calls:
1. filterBySkill("Java")          → [Jane, Alice, Bob]
2. filterBySchedule("today")      → [Jane, John]
3. intersectEmployeeLists([...])  → [Jane]
```

---

## Extending the Template

### Adding a New Query Type

**1. Add enum value to QueryType:**
```java
// src/main/java/nl/alfaone/domain/QueryType.java
public enum QueryType {
    // ... existing types
    CERTIFICATION  // New type
}
```

**2. Add @Condition method to EmployeeCollectorAgent:**
```java
@Condition
public boolean isCertificationQuery(String query) {
    return query.toLowerCase().contains("certification") ||
           query.toLowerCase().contains("certified");
}
```

**3. Add switch case to determineQueryType():**
```java
private QueryType determineQueryType(String query) {
    if (isCertificationQuery(query)) return QueryType.CERTIFICATION;
    // ... other conditions
}
```

**4. Add color mapping to ActionAgent:**
```java
private String determineContextColor(QueryType queryType) {
    return switch (queryType) {
        case CERTIFICATION -> "#00FFFF";  // Cyan
        // ... other cases
    };
}
```

### Adding a New @Tool Method

Add a new tool to EmployeeCollectorAgent that the LLM can call:

```java
@Tool(description = "Find employees with specific certifications. " +
      "Use for queries about certified professionals. " +
      "Examples: 'AWS certified', 'Scrum Master certified'")
public List<Employee> filterByCertification(String certificationName) {
    log.info("Tool called: filterByCertification('{}')", certificationName);
    toolsUsedThreadLocal.get().add("filterByCertification");

    // Your implementation
    return employeeRepository.findByCertification(certificationName);
}
```

The LLM will automatically discover and use this tool when appropriate!

### Adding Custom Employee Fields

**1. Update Employee domain model:**
```java
// src/main/java/nl/alfaone/domain/Employee.java
@Data
@Builder
public class Employee {
    // ... existing fields
    private List<String> certifications;  // New field
}
```

**2. Update application.yaml:**
```yaml
employee-data:
  employees:
    - id: example-1
      name: "Jane Developer"
      # ... existing fields
      certifications:
        - "AWS Solutions Architect"
        - "Certified Scrum Master"
```

**3. Update EmployeeDataProperties:**
```java
// src/main/java/nl/alfaone/infrastructure/config/EmployeeDataProperties.java
@Data
public static class EmployeeData {
    // ... existing fields
    private List<String> certifications;
}
```

### Implementing a New Data Source

Replace the in-memory `EmployeeRepository` with a real database:

**1. Add database dependencies to pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

**2. Update Employee to be a JPA entity:**
```java
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    private String id;
    // ... fields with @Column annotations
}
```

**3. Create a Spring Data repository:**
```java
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    List<Employee> findBySkillsContaining(String skill);
    // ... custom queries
}
```

---

## Testing

### Unit Tests
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=AlfaWallApplicationTest
```

### Integration Testing

The template includes `ConversationApiIntegrationTest` for end-to-end testing. Update it with your employee data:

```java
@Test
void shouldHandleSkillQuery() {
    // Update to use your employee names
    String query = "Who knows Java?";
    // ... assertions
}
```

### Testing Without Home Assistant

Set `employee-device.use-mock-data: true` in application.yaml to simulate presence without real device trackers.

---

## Common Customization Points

### 1. Change LLM Provider

**Use OpenAI directly:**
```bash
SPRING_AI_OPENAI_BASE_URL=https://api.openai.com/v1
SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL=gpt-4
```

**Use a local LLM (Ollama):**
```bash
SPRING_AI_OPENAI_BASE_URL=http://localhost:11434/v1
SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL=llama2
```

### 2. Adjust LLM Tool Selection

**Disable LLM tool selection (keyword routing only):**
```yaml
alfawall:
  llm-tool-selection:
    enabled: false
```

**Increase timeout for slower LLMs:**
```yaml
alfawall:
  llm-tool-selection:
    timeout-ms: 10000  # 10 seconds
```

### 3. Customize PII Detection

Edit `PrivacyOfficerAgent.java` to add new PII patterns:

```java
private final Pattern[] piiPatterns = {
    // ... existing patterns
    Pattern.compile("\\b[A-Z]{2}\\d{6}\\b")  // Passport numbers
};
```

### 4. Modify LED Brightness/Colors

**Per-employee customization:**
```yaml
employee-led:
  mappings:
    "Jane Developer":
      color: "#FF0000"  # Red
      brightness: 128   # Dimmer
```

**Query-type customization:**
Edit `ActionAgent.determineContextColor()` method.

---

## Package Structure

```
nl.alfaone/
├── domain/                    # Core business logic (framework-independent)
│   ├── Employee.java
│   ├── QueryType.java
│   ├── SanitizedQuery.java
│   ├── EmployeeSearchResult.java
│   └── VisualizationResult.java
│
├── application/               # Agent orchestration
│   └── agents/
│       ├── PrivacyOfficerAgent.java
│       ├── EmployeeCollectorAgent.java
│       └── ActionAgent.java
│
└── infrastructure/            # External integrations
    ├── ConversationApiController.java
    ├── HomeAssistantClient.java
    ├── repository/
    │   └── EmployeeRepository.java
    └── config/
        ├── EmployeeDataProperties.java
        ├── EmployeeLedMappingProperties.java
        └── ChatClientConfig.java
```

---

## Troubleshooting

### "No ChatModel bean found"
**Solution:** Ensure `OPENAI_API_KEY` is set. The ChatModel bean is conditionally created when a valid API key is present.

### LLM tool selection failing
**Solution:** Enable fallback mode (default) to use keyword routing:
```yaml
alfawall:
  llm-tool-selection:
    fallback-on-error: true
```

### Home Assistant connection refused
**Solution:** Check `HOMEASSISTANT_BASE_URL` and `HOMEASSISTANT_TOKEN`. Test connectivity:
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://your-ha-url:8123/api/
```

### WLED lights not responding
**Solution:**
1. Verify entity IDs in `employee-led.mappings` match your Home Assistant setup
2. Check Home Assistant logs for WLED integration errors
3. Test manually: Developer Tools → Services → `light.turn_on`

---

## Package Naming

This template uses the package name `nl.alfaone` from the original implementation. You can refactor to your own package name:

```bash
# Example: Refactor to com.yourcompany.conversationapi
find src -name "*.java" -exec sed -i 's/nl.alfaone/com.yourcompany.conversationapi/g' {} +
```

Don't forget to update:
- Maven `groupId` in `pom.xml`
- Directory structure under `src/main/java/` and `src/test/java/`
- Import statements in all files

---

## Technology Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Runtime |
| Spring Boot | 3.5.7 | Application framework |
| Embabel Agent | 0.2.0 | AI agent orchestration |
| Spring AI | 1.0.0 | LLM integration & @Tool annotation |
| Project Reactor | (WebFlux) | Reactive async operations |
| Lombok | Latest | Code generation |
| Maven | 3.6+ | Build tool |

---

## License

[Specify your license here]

---

## Support

For issues and questions:
- Check the troubleshooting section above
- Review the inline code comments (especially in agent classes)
- See `GETTING_STARTED.md` for step-by-step setup guide

---

## Acknowledgments

Built with:
- [Embabel Agent Framework](https://github.com/embabel/embabel)
- [Spring AI](https://spring.io/projects/spring-ai)
- [Home Assistant](https://www.home-assistant.io/)
- [WLED](https://kno.wled.ge/)
