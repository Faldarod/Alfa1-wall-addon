# Alfa-Wall-Addon Documentation

## Goal

The Alfa-Wall-Addon is the main AI-powered presence assistant application that processes natural language queries about employee presence and availability. It serves as a conversation agent for Home Assistant, using a multi-agent orchestration pattern to intelligently respond to user queries and control WLED LED strips to visualize employee presence.

**Key Objectives:**
- Process natural language queries about employees (e.g., "Who is at the office today?", "Show me Java developers")
- Use AI agents to determine intent, collect employee data, and perform actions
- Enforce GDPR compliance and privacy protection
- Control WLED LED strips via Home Assistant REST API to visualize results
- Provide a REST API endpoint for Home Assistant integration

## Relevant Articles & Resources

### Embabel Agent Framework
- **Official Documentation**: https://docs.embabel.com/embabel-agent/guide/0.2.0/
- **Version Used**: 0.2.0
- **Key Concepts**:
  - @Agent annotation for defining AI agents
  - @Action annotation for tool methods that agents can call
  - Multi-agent orchestration patterns
  - LLM-driven tool selection

### Spring Boot
- **Official Documentation**: https://docs.spring.io/spring-boot/docs/3.5.7/reference/html/
- **Version Used**: 3.5.7
- **Key Features Used**:
  - Spring WebFlux for reactive HTTP communication
  - WebClient for async REST API calls
  - Spring Boot configuration with application.yaml

### Home Assistant Integration
- **REST API Documentation**: https://developers.home-assistant.io/docs/api/rest/
- **Conversation Agent**: https://www.home-assistant.io/integrations/conversation/
- **WLED Integration**: https://www.home-assistant.io/integrations/wled/

### AI & LLM
- **OpenRouter API**: https://openrouter.ai/ (Used for LLM inference)
- **Supported Models**: GPT-4o-mini, GPT-4.1-mini, and others via OpenRouter

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Primary programming language |
| Spring Boot | 3.5.7 | Application framework |
| Embabel Agent | 0.2.0 | AI agent orchestration framework |
| Spring WebFlux | 3.5.7 | Reactive web framework for async HTTP |
| Project Reactor | - | Reactive programming library |
| Lombok | - | Code generation (reduces boilerplate) |
| Maven | - | Build tool and dependency management |
| Docker | - | Containerization |
| OpenRouter API | - | LLM inference provider |

### Dependencies (from pom.xml)
- `spring-boot-starter-web`: REST API endpoints
- `spring-boot-starter-webflux`: Reactive HTTP client (WebClient)
- `reactor-core`: Reactive programming support
- `embabel-agent-starter`: Core Embabel Agent framework
- `embabel-agent-starter-openai`: OpenAI/OpenRouter LLM integration
- `lombok`: Annotation-based code generation

## Architecture

### Multi-Agent System

The application uses a **multi-agent orchestration pattern** where specialized agents collaborate to process user queries:

```
User Query → ConversationController → OrchestratorAgent
                                            ↓
                                     PrivacyOfficerAgent (GDPR check)
                                            ↓
                                     EmployeeCollectorAgent (LLM-driven)
                                            ↓
                                     ActionAgent (LED control)
                                            ↓
                                       Response
```

### Core Agents

#### 1. **OrchestratorAgent** (`OrchestratorAgent.java`)
- **Purpose**: Coordinates all other agents and orchestrates the query processing pipeline
- **Flow**:
  1. Validates input
  2. Invokes PrivacyOfficerAgent for GDPR compliance
  3. Calls EmployeeCollectorAgent to find matching employees
  4. Executes ActionAgent to visualize results on LED strips
  5. Returns ProcessResult with response
- **Key Method**: `process(String query, OperationContext context)`

#### 2. **PrivacyOfficerAgent** (`PrivacyOfficerAgent.java`)
- **Purpose**: Ensures GDPR compliance and detects PII in queries
- **Features**:
  - Pattern-based PII detection (emails, phone numbers, addresses)
  - LLM-based GDPR concern analysis
  - Query sanitization
  - Request blocking for critical violations
- **Key Methods**:
  - `checkPrivacy(String query)`: Pattern-based PII detection
  - `analyzeGDPRConcerns(String query, OperationContext context)`: LLM-based analysis

#### 3. **EmployeeCollectorAgent** (`EmployeeCollectorAgent.java`)
- **Purpose**: Uses LLM to intelligently collect employees matching the query
- **How it works**: The agent is equipped with @Action-annotated tools:
  - `getAllEmployees()`: Get all employee data
  - `getEmployeesBySkill(String skill)`: Filter by skill
  - `getEmployeesByCustomer(String customer)`: Filter by customer
  - `getEmployeesByPresence(boolean present)`: Filter by presence status
  - The **LLM decides which tools to call** based on the natural language query
- **Data Sources**:
  - Employee data from `application.yaml` (employee-data section)
  - Device tracker integration via Home Assistant
  - Mock data mode for testing (configurable via `employee-device.use-mock-data`)
  - Enhancements:
    - Vector database containing employee data
    - Api towards the Kantoordagen-app backend to fetch employee registered for a specific date
      - Can include allowed to parc at the office
    - Ubiquiti controller exposed with Home Assisten REST API
      - Contains presence data about devices on the office LAN

#### 4. **ActionAgent** (`ActionAgent.java`)
- **Purpose**: Controls WLED LED strips via Home Assistant REST API
- **Features**:
  - Turns on LED segments for present employees
  - Sets colors and brightness per employee
  - Uses reactive WebClient for async HTTP calls
- **Integration**: Communicates with Home Assistant via `HomeAssistantClient`

### Data Flow

1. **ConversationController** (`/api/conversation/process`) receives POST request from Home Assistant
2. **OrchestratorAgent** validates and processes the query through the agent pipeline
3. **EmployeeCollectorAgent** uses LLM to determine which employee data tools to call
4. **ActionAgent** sends REST API calls to Home Assistant to control WLED devices
5. **Response** is returned to Home Assistant and displayed to the user

## Functional Description

### Use Cases

#### 1. **Employee Presence Query**
**Example**: "Who is at the office today?"

**Flow**:
1. User asks via Home Assistant conversation interface
2. Query sent to `/api/conversation/process` endpoint
3. PrivacyOfficerAgent checks for PII/GDPR concerns
4. EmployeeCollectorAgent:
   - LLM analyzes query intent
   - Calls `getEmployeesByPresence(true)` tool
   - Returns list of present employees
5. ActionAgent:
   - Turns on LED segments for present employees
   - Turns of LED segments for employees not present
   - Sets appropriate colors
6. Response: "3 employees match your query: John Doe, Jane Doe, Alice Smith. I've highlighted them on the LED wall."

#### 2. **Skills-Based Query**
**Example**: "Show me all Java developers"

**Flow**:
1. User asks via Home Assistant conversation interface
2. Query sent to `/api/conversation/process` endpoint
3. PrivacyOfficerAgent checks for PII/GDPR concerns 
4. EmployeeCollectorAgent:
   - LLM recognizes skills-based query
   - Calls `getEmployeesBySkill("Java")` tool
5. ActionAgent:
   - Turns on LED segments for matching employees
   - Turns off LED segments for non-matching employees
6. Response with matching employee names

#### 3. **Customer-Based Query**
**Example**: "Who is currently working for ACDC Corp?"

**Flow**:
1. User asks via Home Assistant conversation interface
2. Query sent to `/api/conversation/process` endpoint
3. PrivacyOfficerAgent checks for PII/GDPR concerns
4. EmployeeCollectorAgent calls `getEmployeesByCustomer("ACME Corp")`
5. ActionAgent:
   - Turns on LED segments for matching employees
   - Turns off LED segments for non-matching employees3. Response with employee names and roles
6. Response with matching employee names

### Configuration

**Location**: `alfa-wall-addon/src/main/resources/application.yaml`

**Key Sections**:

```yaml
# LLM Configuration
spring.ai.openai:
  api-key: ${OPENAI_API_KEY}
  base-url: https://openrouter.ai/api/v1
  chat.options:
    model: openai/gpt-4o-mini
    temperature: 0.7

embabel.models:
  default-llm: gpt-4.1-mini

# Home Assistant Connection
homeassistant:
  base-url: http://home-assistant:8123
  access-token: <JWT_TOKEN>

# Employee to LED Mapping
employee-led.mappings:
  "John Doe":
    entity-id: light.wled_segment_0
    color: "#00FF00"
    brightness: 255

# Employee to Device Tracker Mapping
employee-device:
  use-mock-data: true  # Set to false for real UniFi tracking
  mappings:
    "John Doe":
      - device_tracker.john_phone
      - device_tracker.john_laptop

# Employee Data (skills, customers, parking, schedules)
employee-data:
  employees:
    - id: john-doe
      name: John Doe
      email: john.doe@alfaone.nl
      skills: [Java, Spring Boot, Docker]
      customers:
        - customer-name: "ACME Corp"
          role: "Senior Backend Developer"
          percentage: 80
```

### API Endpoints

#### POST `/api/conversation/process`
**Purpose**: Main conversation endpoint called by Home Assistant

**Request**:
```json
{
  "text": "Who is at the office today?",
  "language": "nl",
  "conversation_id": "abc123"
}
```

**Response**:
```json
{
  "speech": {
    "plain": {
      "speech": "3 employees match your query: John Doe, Jane Doe, Alice Smith. I've highlighted them on the LED wall.",
      "extra_data": null
    }
  },
  "card": {},
  "language": "nl",
  "response_type": "action_done",
  "data": {
    "targets": [],
    "success": [],
    "failed": []
  }
}
```

## Developer Documentation

### Prerequisites

- **Java 21** (required)
- **Maven 3.6+**
- **Docker & Docker Compose**
- **OpenRouter API Key** (for LLM access)

### Getting Started

1. **Clone the repository**:
   ```bash
   git clone git@github.com:Faldarod/Alfa1-wall-addon.git
   cd AlfaWall
   ```

2. **Set up environment variables**:
   Create a `.env` file in the project root:
   ```bash
   OPENAI_API_KEY=your_openrouter_api_key_here
   ```

3. **Build the application**:
   ```bash
   cd alfa-wall-addon
   mvn clean package
   ```

4. **Run via Docker Compose**:
   ```bash
   cd ..
   docker-compose up --build
   ```

5. **Access the application**:
   - Alfa-Wall-Addon API: http://localhost:8080
   - Home Assistant: http://localhost:8123

### Project Structure

```
alfa-wall-addon/
├── src/
│   ├── main/
│   │   ├── java/nl/alfaone/
│   │   │   ├── application/
│   │   │   │   ├── agents/           # AI Agent implementations
│   │   │   │   │   ├── OrchestratorAgent.java
│   │   │   │   │   ├── PrivacyOfficerAgent.java
│   │   │   │   │   ├── EmployeeCollectorAgent.java
│   │   │   │   │   └── ActionAgent.java
│   │   │   │   └── controller/       # REST controllers
│   │   │   │       └── ConversationController.java
│   │   │   ├── domain/               # Domain models
│   │   │   │   ├── Employee.java
│   │   │   │   ├── AgentContext.java
│   │   │   │   └── PrivacyViolation.java
│   │   │   ├── infrastructure/       # External integrations
│   │   │   │   └── HomeAssistantClient.java
│   │   │   └── AlfaWallApplication.java
│   │   └── resources/
│   │       └── application.yaml      # Configuration
│   └── test/                         # Unit tests
├── Dockerfile
└── pom.xml                           # Maven dependencies
```

### Testing

**Run all tests**:
```bash
cd alfa-wall-addon
mvn test
```

**Run specific test**:
```bash
mvn test -Dtest=IntentAgentTest
```

**Test with mock data**:
Set `employee-device.use-mock-data: true` in `application.yaml` to use simulated presence data instead of real UniFi device tracking.

### Adding a New Agent

1. Create a new Java class annotated with `@Component`
2. Define actions using `@Action` annotation:
   ```java
   @Component
   public class MyNewAgent {

       @Action(description = "Does something useful")
       public String performAction(String input) {
           // Implementation
           return "Result";
       }
   }
   ```
3. Inject into `OrchestratorAgent` and call in the processing pipeline

### Debugging

**Enable debug logging**:
Add to `application.yaml`:
```yaml
logging:
  level:
    nl.alfaone: DEBUG
    com.embabel: DEBUG
```

**View logs in Docker**:
```bash
docker-compose logs -f alfa-wall-addon
```

### Common Issues

**Issue**: Agent not calling the right tools
- **Solution**: Check @Action descriptions - they guide the LLM's tool selection

**Issue**: Home Assistant connection fails
- **Solution**: Verify `homeassistant.base-url` and `access-token` in application.yaml

**Issue**: LLM API errors
- **Solution**: Check OPENAI_API_KEY environment variable and OpenRouter account credits

## Building from Scratch

To set up the alfa-wall-addon from scratch:

1. **Initialize Spring Boot project**:
   - Use Spring Initializr or Maven archetype
   - Add dependencies: spring-boot-starter-web, spring-boot-starter-webflux, lombok

2. **Add Embabel Agent framework**:
   - Add Embabel repository to pom.xml
   - Add embabel-agent-starter and embabel-agent-starter-openai dependencies

3. **Create agent classes**:
   - Implement OrchestratorAgent (main coordinator)
   - Implement specialized agents (PrivacyOfficerAgent, EmployeeCollectorAgent, ActionAgent)
   - Annotate with @Component and @Action

4. **Configure application.yaml**:
   - Set up LLM configuration (OpenRouter API)
   - Configure Home Assistant connection
   - Define employee-to-LED mappings

5. **Create Dockerfile**:
   ```dockerfile
   FROM eclipse-temurin:21-jre
   COPY target/*.jar app.jar
   ENTRYPOINT ["java", "-jar", "/app.jar"]
   ```

6. **Add to docker-compose.yml**:
   ```yaml
   alfa-wall-addon:
     build: ./alfa-wall-addon
     ports:
       - "8080:8080"
     environment:
       - OPENAI_API_KEY=${OPENAI_API_KEY}
   ```

7. **Test the setup**:
   - Start with `docker-compose up --build`
   - Test endpoint: `curl http://localhost:8080/api/conversation/process`

## Maintenance Notes

- **Embabel Agent version**: Currently 0.2.0 - check for updates at https://docs.embabel.com
- **Spring Boot version**: 3.5.7 - ensure compatibility when upgrading
- **Access tokens**: The Home Assistant access token in application.yaml is for development only
- **Privacy compliance**: Regularly review PrivacyOfficerAgent rules to ensure GDPR compliance
