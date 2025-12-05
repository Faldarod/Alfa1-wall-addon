# AlfaWall Conversation API - Developer Template

> 🎯 **This is a template repository** for building AI-powered conversation APIs that integrate with Home Assistant. Clone it, customize it, and build your own implementation!

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-green.svg)](https://spring.io/projects/spring-boot)
[![Embabel](https://img.shields.io/badge/Embabel-0.2.0-blue.svg)](https://github.com/embabel/embabel)

---

## What is AlfaWall?

AlfaWall is a Spring Boot application that processes natural language queries about employees and provides visual feedback via WLED LED devices. It demonstrates a **multi-agent AI architecture** for building intelligent conversation APIs.

**Example Flow:**
```
User asks: "Who knows Java?"
    ↓
AlfaWall processes with AI agents
    ↓
Identifies: Jane Developer
    ↓
Lights up Jane's LED segment in PURPLE
    ↓
Returns: "1 employee matches your query: Jane Developer"
```

---

## ✨ Key Features

- 🤖 **Multi-Agent OODA Loop** - Observe (Privacy) → Orient (Search) → Decide (Color) → Act (LEDs)
- 🔒 **GDPR Compliant** - Automatic PII detection and sanitization
- 🎯 **LLM-Driven Tool Selection** - Intelligently handles multi-criteria queries
- 🏠 **Home Assistant Integration** - REST API for device control
- 💡 **Context-Aware LED Colors** - Different colors for presence, skills, customers, etc.
- ⚡ **Reactive & Async** - Built with Spring WebFlux
- 🧠 **Embabel 0.2.0 + Spring AI** - Modern AI agent framework

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.6+
- OpenAI or OpenRouter API key
- (Optional) Home Assistant instance with WLED devices

### 1. Clone & Configure
```bash
# Clone this template
git clone <your-fork-url>
cd Alfa1-wall-addon

# Copy example configuration
cp src/main/resources/application-example.yaml src/main/resources/application.yaml

# Set your API keys
export OPENAI_API_KEY="your-api-key"
export HOMEASSISTANT_TOKEN="your-ha-token"
```

### 2. Customize Employee Data
Edit `src/main/resources/application.yaml`:
```yaml
employee-data:
  employees:
    - name: "Your Employee"
      skills: [Java, Python, ...]
      # ... customize fields
```

### 3. Build & Run
```bash
# Build
mvn clean package

# Run
mvn spring-boot:run
```

### 4. Test It
```bash
curl -X POST http://localhost:8080/api/conversation/process \
  -H "Content-Type: application/json" \
  -d '{"text":"Who knows Java?"}'
```

📖 **For detailed setup instructions, see [GETTING_STARTED.md](GETTING_STARTED.md)**
📚 **For customization guide, see [TEMPLATE.md](TEMPLATE.md)**

---

## 🏗️ Architecture

### Multi-Agent OODA Loop

```
┌─────────────────────────────────────────────────┐
│  1. PrivacyOfficerAgent (OBSERVE)              │
│     • Detects PII (emails, phones, SSN, etc.)  │
│     • Blocks critical PII                       │
│     • Sanitizes query for processing            │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│  2. EmployeeCollectorAgent (ORIENT)            │
│     • LLM selects appropriate tools             │
│     • Calls @Tool methods (filterBySkill, etc.) │
│     • Handles multi-criteria with AND/OR logic  │
└─────────────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────┐
│  3. ActionAgent (DECIDE & ACT)                 │
│     • Determines context color (green/purple)   │
│     • Turns matched employees ON                │
│     • Turns unmatched employees OFF             │
│     • Sends commands to WLED via Home Assistant │
└─────────────────────────────────────────────────┘
```

### Query Types & LED Colors

| Query | Type | Color | Example |
|-------|------|-------|---------|
| "Who is here?" | PRESENCE | 🟢 Green | Current office presence |
| "Who knows Java?" | SKILLS | 🟣 Purple | Skills & expertise |
| "Who works for ACME?" | CUSTOMER | 🟡 Yellow | Customer assignments |
| "Who is coming today?" | SCHEDULE | 🔵 Blue | Future schedules |
| "Who has parking?" | PARKING | 🟠 Orange | Parking spots |
| "Backend experts" | GENERAL | Default | Semantic search |

---

## 🧩 Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Runtime |
| Spring Boot | 3.5.7 | Application framework |
| Embabel Agent | 0.2.0 | AI agent orchestration |
| Spring AI | 1.0.0 | LLM integration (@Tool) |
| Spring WebFlux | 3.5.7 | Reactive HTTP client |
| Lombok | Latest | Code generation |
| Maven | 3.6+ | Build tool |

---

## 📂 Project Structure

```
src/main/java/nl/alfaone/
├── domain/                      # Core business logic
│   ├── Employee.java
│   ├── QueryType.java
│   ├── SanitizedQuery.java
│   └── EmployeeSearchResult.java
│
├── application/agents/          # AI agents
│   ├── PrivacyOfficerAgent.java    # PII detection
│   ├── EmployeeCollectorAgent.java # Data collection
│   └── ActionAgent.java            # LED control
│
└── infrastructure/              # External integrations
    ├── ConversationApiController.java
    ├── HomeAssistantClient.java
    └── repository/
        └── EmployeeRepository.java
```

---

## 🔧 Configuration

### Environment Variables

**Required:**
```bash
OPENAI_API_KEY=your-api-key           # OpenAI or OpenRouter key
HOMEASSISTANT_TOKEN=your-ha-token     # Home Assistant access token
```

**Optional:**
```bash
SPRING_AI_OPENAI_BASE_URL=https://openrouter.ai/api/v1  # LLM API endpoint
HOMEASSISTANT_BASE_URL=http://localhost:8123            # HA URL
ALFAWALL_LLM_TOOL_SELECTION_ENABLED=true               # Enable LLM tools
```

### Configuration Files

- `application.yaml` - Main config (customize this with your data)
- `application-example.yaml` - Template reference (don't edit)

---

## 🎓 Example Queries

### Simple Queries
```bash
# Presence
curl ... -d '{"text":"Who is here now?"}'

# Skills
curl ... -d '{"text":"Who knows Python?"}'

# Customer
curl ... -d '{"text":"Who works for ACME Corp?"}'

# Schedule
curl ... -d '{"text":"Who is coming today?"}'

# Parking
curl ... -d '{"text":"Who has parking tomorrow?"}'
```

### Multi-Criteria Queries (LLM-Powered)
```bash
# AND logic
curl ... -d '{"text":"Java developers coming today"}'
# → LLM calls: filterBySkill + filterBySchedule + intersect

# OR logic
curl ... -d '{"text":"Java or Python developers"}'
# → LLM calls: filterBySkill + filterBySkill + combine
```

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=AlfaWallApplicationTest

# Build and test
mvn clean verify
```

**Note:** Update test assertions in `ConversationApiIntegrationTest.java` to match your employee data.

---

## 🎨 Customization Guide

### Add a New Query Type

1. **Add enum** to `QueryType.java`:
   ```java
   LOCATION  // New type
   ```

2. **Add @Condition** to `EmployeeCollectorAgent.java`:
   ```java
   @Condition
   public boolean isLocationQuery(String query) {
       return query.contains("location") || query.contains("office");
   }
   ```

3. **Add color** to `ActionAgent.java`:
   ```java
   case LOCATION -> "#FF00FF";  // Magenta
   ```

### Add a New @Tool Method

Add to `EmployeeCollectorAgent.java`:

```java
@Tool(description = "Find employees by office location")
public List<Employee> filterByLocation(String office) {
    log.info("Tool called: filterByLocation('{}')", office);
    toolsUsedThreadLocal.get().add("filterByLocation");
    return employeeRepository.findByLocation(office);
}
```

The LLM automatically discovers and uses new tools! 🎉

---

## 🐛 Troubleshooting

### "No ChatModel bean found"
**Solution:** Set `OPENAI_API_KEY` environment variable.

### LLM tool selection failing
**Solution:** Check API key and model name. Enable fallback:
```yaml
alfawall:
  llm-tool-selection:
    fallback-on-error: true
```

### Home Assistant connection refused
**Solution:** Verify `HOMEASSISTANT_BASE_URL` and `HOMEASSISTANT_TOKEN`. Test:
```bash
curl -H "Authorization: Bearer TOKEN" http://your-ha:8123/api/
```

### LEDs not responding
**Solution:**
1. Verify `entity-id` in `employee-led.mappings` matches Home Assistant
2. Test manually in HA: Developer Tools → Services → `light.turn_on`

---

## 📖 Documentation

- **[TEMPLATE.md](TEMPLATE.md)** - Comprehensive customization guide
- **[GETTING_STARTED.md](GETTING_STARTED.md)** - Step-by-step setup checklist
- **[CLAUDE.md](CLAUDE.md)** - Instructions for Claude Code (AI assistant)

---

## 🤝 Contributing

This is a template repository. Feel free to:
- Fork it for your own implementation
- Submit issues for bugs
- Propose improvements via pull requests

---

## 📄 License

[Specify your license here]

---

## 🙏 Acknowledgments

Built with:
- [Embabel Agent Framework](https://github.com/embabel/embabel)
- [Spring AI](https://spring.io/projects/spring-ai)
- [Home Assistant](https://www.home-assistant.io/)
- [WLED](https://kno.wled.ge/)

---

## 🔗 Related Repositories

- **Alfa1-wall-running-local** - Complete Docker Compose testing environment with Home Assistant, WLED simulator, and mock devices

---

**Ready to build your own conversation API?** 🚀

Start with the [Quick Start](#-quick-start) above, then dive into [TEMPLATE.md](TEMPLATE.md) for detailed customization!
