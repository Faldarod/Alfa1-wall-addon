# AlfaWall LLM-Driven Implementation Documentation

## Overview

This document describes the implementation of the LLM-driven employee query system using the Embabel Agent framework (v0.2.0) with Spring AI tool support.

## Architecture

### Curren[README.md](../../Alfa1-wall-addon/README.md)t Implementation (Embabel 0.2.0 + Spring AI)

```
User Voice Query → Home Assistant → /api/conversation/process
    ↓
ConversationApiController
    ↓
OrchestratorAgent
    ├→ PrivacyOfficerAgent (PII detection)
    ├→ EmployeeCollectorAgent (keyword-based routing + data collection)
    └→ ActionAgent (LED visualization with context colors)
```

### Agent Responsibilities

1. **PrivacyOfficerAgent**: First line of defense
   - Detects 8 types of PII (email, phone, credit cards, SSN/BSN, addresses, IP, postcodes)
   - Blocks critical PII (credit cards, SSN/BSN)
   - Sanitizes non-critical PII

2. **EmployeeCollectorAgent**: Data collection
   - Keyword-based query routing
   - Multiple @Action methods for different data sources
   - In-memory employee repository with semantic search

3. **ActionAgent**: LED visualization
   - Context-aware LED colors:
     - Green: Current presence
     - Blue: Scheduled presence
     - Purple: Skills-based queries
     - Yellow: Customer-based queries
     - Orange: Parking queries
   - Turns matched employees ON, unmatched OFF

## @Action vs @Tool: Current Implementation

### What We Learned

**Spring AI @Tool Annotation**:
- `@Tool` annotation comes from Spring AI (org.springframework.ai.tool.annotation.Tool)
- NOT from Embabel - this was a key discovery!
- Methods annotated with @Tool are automatically exposed to LLMs
- Supports all standard return types including collections (List<Employee>)
- Integrated with Embabel 0.2.0 through Spring AI 1.0.0

**Embabel 0.2.0**:
- Includes Spring AI 1.0.0 as transitive dependency
- Provides `PromptRunner.withToolObject(Object)` method to register tool objects
- Integration with Spring AI tool calling mechanism

### Current Hybrid Approach

We've upgraded to Embabel 0.2.0 and added Spring AI `@Tool` annotations to all data collection methods. However, the main action method still uses **keyword-based routing** temporarily while we finalize the LLM tool selection integration:

```java
@Component
@Agent(description = "Collects employees using LLM-driven tool selection")
public class EmployeeCollectorAgent {

    /**
     * Main entry point - currently using keyword-based routing
     * TODO: Implement proper LLM tool selection when integration pattern is clarified
     */
    @Action
    public List<Employee> collectEmployees(String query, OperationContext context) {
        String lowerQuery = query.toLowerCase();

        // Temporary keyword-based routing
        if (lowerQuery.contains("here") || lowerQuery.contains("present")) {
            return getCurrentPresence();
        }
        // ... more routing logic

        return searchEmployees(query); // Default: semantic search
    }

    /**
     * Tool methods - annotated with Spring AI @Tool
     * Ready for LLM tool selection once integration is complete
     */
    @Tool(description = "Get employees currently in the office...")
    public List<Employee> getCurrentPresence() { ... }

    @Tool(description = "Filter employees by exact skill name...")
    public List<Employee> filterBySkill(String skill) { ... }

    @Tool(description = "Find employees working for a specific customer...")
    public List<Employee> filterByCustomer(String customerName) { ... }

    @Tool(description = "Find employees scheduled to be in the office...")
    public List<Employee> filterBySchedule(String date) { ... }

    @Tool(description = "Find employees who have a parking spot...")
    public List<Employee> filterByParking(String date) { ... }

    @Tool(description = "Intersect employee lists for AND logic...")
    public List<Employee> intersectEmployeeLists(List<List<Employee>> lists) { ... }

    @Tool(description = "Combine employee lists for OR logic...")
    public List<Employee> combineEmployeeLists(List<List<Employee>> lists) { ... }
}
```

### Next Steps for LLM Tool Selection

The infrastructure is ready:
- ✅ Upgraded to Embabel 0.2.0
- ✅ All methods annotated with Spring AI @Tool
- ✅ Build compiles successfully
- ⏳ Need to finalize integration pattern

Potential approaches being investigated:
1. Use `PromptRunner.withToolObject(this)` to register tools
2. Explore Embabel's documentation for recommended patterns
3. Test with simple queries to validate tool calling mechanism

## Query Examples

### Simple Queries

**"Who knows Java?"**
```
→ EmployeeCollectorAgent.collectEmployees()
→ Detects "java" keyword
→ Calls filterBySkill("Java")
→ ActionAgent visualizes with PURPLE color
→ Response: "3 employees match your query: John Doe, Sarah Chen, Mike Johnson"
```

**"Who is here now?"**
```
→ Detects "here" keyword
→ Calls getCurrentPresence()
→ Checks device_tracker entities in Home Assistant
→ ActionAgent visualizes with GREEN color
```

**"Who has parking today?"**
```
→ Detects "parking" keyword
→ Calls filterByParking("today")
→ Checks parking assignments for today
→ ActionAgent visualizes with ORANGE color
```

### Complex Queries (Future with 0.2.0)

**"Who with Python skills is coming today?"**

With Embabel 0.2.0:
```
→ LLM analyzes query
→ Calls filterBySkill("Python")
→ Calls filterBySchedule("today")
→ Calls intersectEmployeeLists([pythonDevs, comingToday])
→ Returns intersection
```

Current implementation (0.1.4):
- Not supported yet
- Would need additional keyword logic

## Data Sources

### In-Memory Employee Repository

```yaml
employee-data:
  employees:
    - id: john-doe
      name: John Doe
      background: "Senior software engineer with 10+ years..."
      skills: [Java, Spring Boot, Docker, Kubernetes]
      customers:
        - customer-name: "ACME Corp"
          role: "Senior Backend Developer"
      parking:
        spot-number: "A-12"
        recurring-days: [MONDAY, WEDNESDAY, FRIDAY]
      schedule:
        monday: true
        tuesday: true
        ...
```

### Search Capabilities

1. **Semantic Search** (in-memory text matching)
   - Searches employee name + background + skills
   - Simple contains() matching
   - Future: Can be upgraded to vector embeddings

2. **Exact Filters**
   - Skills: Case-insensitive exact match
   - Customers: Case-insensitive exact match
   - Parking: Date-based with recurring days
   - Schedule: Day-of-week based

3. **Current Presence**
   - Queries Home Assistant device_tracker entities
   - Checks if ANY employee device is "home"

## Home Assistant Integration

### Conversation API Endpoint

**Endpoint**: `POST /api/conversation/process`

**Request**:
```json
{
  "text": "Who knows Java?",
  "conversation_id": "optional-id",
  "language": "en"
}
```

**Response**:
```json
{
  "response": {
    "speech": {
      "plain": {
        "speech": "3 employees match your query: John Doe, Sarah Chen, Mike Johnson. I've highlighted them on the LED wall."
      }
    },
    "language": "en",
    "response_type": "action_done"
  },
  "conversation_id": "01AB23CD45EF67GH89IJ01KL23MN45OP"
}
```

### Voice Flow

1. User speaks to Home Assistant: *"Hey Google, who knows Java?"*
2. Home Assistant processes speech-to-text
3. HA sends query to AlfaWall: `POST /api/conversation/process`
4. AlfaWall processes:
   - Privacy check
   - Employee collection
   - LED visualization
5. AlfaWall returns text response
6. Home Assistant speaks response via TTS

## LED Visualization

### Color Schemes

| Query Type | Color | Hex Code |
|------------|-------|----------|
| Current Presence | Green | #00FF00 |
| Scheduled Presence | Blue | #0000FF |
| Skills | Purple | #9400D3 |
| Customer | Yellow | #FFD700 |
| Parking | Orange | #FFA500 |

### Behavior

- **Matched employees**: LED turns ON with context color
- **Unmatched employees**: LED turns OFF
- **No matches**: All LEDs turn OFF

## Testing

### Manual Testing

**Running Docker Compose Services**

To run only the `alfa-wall-addon` service (default):

```bash
docker-compose up
```

To run all services, including those in the `local-testing` profile:

```bash
docker-compose --profile local-testing up
```

To run specific services within the `local-testing` profile (e.g., home-assistant and wled-simulator):

```bash
docker-compose --profile local-testing up home-assistant wled-simulator
```

**Test 1: Skills Query**
```bash
curl -X POST http://localhost:8080/api/conversation/process \
  -H "Content-Type: application/json" \
  -d '{"text":"Who knows Java?"}'
```

Expected:
- Returns employees with Java skill
- LEDs light up in PURPLE

**Test 2: Current Presence**
```bash
curl -X POST http://localhost:8080/conversation \
  -H "Content-Type: text/plain" \
  -d "Who is here now?"
```

Expected:
- Queries Home Assistant device trackers
- LEDs light up in GREEN for present employees

**Test 3: Customer Query**
```bash
curl -X POST http://localhost:8080/conversation \
  -H "Content-Type: text/plain" \
  -d "Who works for ACME Corp?"
```

Expected:
- Returns employees assigned to ACME Corp
- LEDs light up in YELLOW

### Privacy Testing

**Test 4: PII Detection**
```bash
curl -X POST http://localhost:8080/conversation \
  -H "Content-Type: text/plain" \
  -d "Find john.doe@example.com"
```

Expected:
- Privacy violation detected
- Email redacted
- Query continues with sanitized version

**Test 5: Critical PII Block**
```bash
curl -X POST http://localhost:8080/conversation \
  -H "Content-Type: text/plain" \
  -d "Employee with card 4111-1111-1111-1111"
```

Expected:
- Request BLOCKED
- Response: "Request blocked due to privacy violation"

## Future Enhancements

### Phase 1: Upgrade to Embabel 0.2.0
- [ ] Update pom.xml to embabel-agent-starter:0.2.0
- [ ] Refactor EmployeeCollectorAgent to use `@Tool` annotations
- [ ] Remove keyword-based routing
- [ ] Let LLM decide which tools to call
- [ ] Support complex multi-tool queries

### Phase 2: Real Vector Search
- [ ] Integrate ChromaDB or Pinecone
- [ ] Generate embeddings for employee profiles
- [ ] True semantic search instead of text matching
- [ ] Better matching for "backend expert" → "Java developer"

### Phase 3: External Data Sources
- [ ] Calendar integration (Google Calendar, Outlook)
- [ ] Real parking API integration
- [ ] CRM system for customer assignments
- [ ] Real-time presence from WiFi/UniFi

### Phase 4: Advanced Features
- [ ] Multi-criteria queries: "Java developers coming today with parking"
- [ ] Conversation memory: "Show me their projects"
- [ ] Confidence scoring: "Probably matches: ..."
- [ ] Admin UI for employee data management

## Summary

**What We Built:**
✅ Multi-agent architecture with Embabel 0.2.0
✅ Privacy-first design with PII detection
✅ Spring AI @Tool annotations on all data collection methods
✅ In-memory employee database with semantic search
✅ Context-aware LED visualization
✅ Home Assistant Conversation API integration
✅ Supports 5+ query types

**Current Status:**
✅ Successfully upgraded to Embabel 0.2.0 + Spring AI 1.0.0
✅ All @Tool annotations in place and compiling
⏳ Using keyword routing temporarily while finalizing LLM tool integration
⏳ Need to clarify Embabel + Spring AI integration pattern

**Technical Discoveries:**
- `@Tool` annotation is from Spring AI, not Embabel
- Spring AI 1.0.0 included as transitive dependency in Embabel 0.2.0
- `PromptRunner.withToolObject(Object)` available for tool registration
- Collections (List<Employee>) fully supported as @Tool return types

**Next Steps:**
1. Research and implement proper Embabel + Spring AI tool calling pattern
2. Test LLM tool selection with simple queries
3. Add support for complex multi-criteria queries using tool composition
4. Test with Home Assistant voice integration
5. Add Wim de Kok's CV data to employee database
