# Getting Started with AlfaWall Conversation API

> 🎯 **New to this template?** Follow this checklist to get your conversation API up and running!

This guide provides a step-by-step process for setting up your own implementation of the AlfaWall Conversation API.

---

## Prerequisites Checklist

Before you begin, ensure you have:

- [ ] **Java 21 or higher** installed ([Download](https://adoptium.net/))
- [ ] **Maven 3.6+** installed ([Download](https://maven.apache.org/download.cgi))
- [ ] **Git** installed ([Download](https://git-scm.com/))
- [ ] **OpenAI or OpenRouter API key** ([OpenRouter](https://openrouter.ai) | [OpenAI](https://platform.openai.com))
- [ ] **(Optional) Home Assistant instance** with WLED devices

**Verify installations:**
```bash
java -version  # Should show Java 21+
mvn --version  # Should show Maven 3.6+
git --version  # Should show Git 2.x+
```

---

## Step 1: Clone or Fork the Repository

### Option A: Fork (Recommended for your own project)
1. Fork this repository on GitHub
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Alfa1-wall-addon.git
   cd Alfa1-wall-addon
   ```

### Option B: Clone directly
```bash
git clone <repository-url>
cd Alfa1-wall-addon
```

---

## Step 2: Set Up Configuration Files

### 2.1 Create `.env` from template
```bash
cp .env.example .env
```

### 2.2 Edit `.env` with your values
Open `.env` in your favorite editor and set:

**Required:**
```bash
OPENAI_API_KEY=sk-...  # Your actual API key
HOMEASSISTANT_TOKEN=eyJ...  # Your HA token (or leave default for testing)
```

**Optional** (use defaults if unsure):
```bash
SPRING_AI_OPENAI_BASE_URL=https://openrouter.ai/api/v1
HOMEASSISTANT_BASE_URL=http://localhost:8123
```

### 2.3 Review `application.yaml`
Open `src/main/resources/application.yaml` and review the example employee data (Jane Developer, John Designer).

For now, you can leave these examples - you'll customize them later.

---

## Step 3: Test the Build

Verify everything compiles:

```bash
# Clean build
mvn clean package

# If successful, you should see:
# [INFO] BUILD SUCCESS
```

**If build fails:**
- Check Java version: `java -version` (must be 21+)
- Clear Maven cache: `rm -rf ~/.m2/repository/com/embabel`
- Try again: `mvn clean package -U`

---

## Step 4: Run the Application

### Option A: Run with Maven (recommended for development)
```bash
mvn spring-boot:run
```

### Option B: Run the JAR directly
```bash
java -jar target/alfa-wall-addon-0.0.1-SNAPSHOT.jar
```

**Application should start on port 8080.**

Look for this in the logs:
```
Started AlfaWallApplication in X.XXX seconds
```

---

## Step 5: Test the API

### 5.1 Test with example employee data

Open a new terminal and run:

```bash
curl -X POST http://localhost:8080/api/conversation/process \
  -H "Content-Type: application/json" \
  -d '{"text":"Who knows Java?"}'
```

**Expected response:**
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
  "conversation_id": "uuid-here"
}
```

### 5.2 Try more queries

```bash
# UX Design query
curl ... -d '{"text":"Who knows Figma?"}'
# Response: "1 employee matches: John Designer."

# Multi-criteria (LLM-powered)
curl ... -d '{"text":"Java developers coming today"}'
# Response depends on schedule data
```

---

## Step 6: Customize Employee Data

Now that everything works with example data, customize it for your organization.

### 6.1 Edit `application.yaml`

Open `src/main/resources/application.yaml` and replace Jane/John with your team:

```yaml
employee-data:
  employees:
    - id: employee-1
      name: "Your Employee Name"
      email: employee@yourcompany.com
      background: "Description for semantic search..."
      skills:
        - Skill1
        - Skill2
      customers:
        - customer-name: "Customer Name"
          role: "Role"
          percentage: 100
      parking:
        spot-number: "A-1"
        recurring-days:
          - MONDAY
          - WEDNESDAY
      schedule:
        monday: true
        tuesday: true
        # ... etc
```

### 6.2 Update LED mappings

```yaml
employee-led:
  mappings:
    "Your Employee Name":
      entity-id: light.wled_segment_0  # Your WLED entity ID
      color: "#00FF00"
      brightness: 255
```

### 6.3 Update device trackers

```yaml
employee-device:
  use-mock-data: false  # Set to true if testing without Home Assistant
  mappings:
    "Your Employee Name":
      - device_tracker.employee_phone
```

### 6.4 Restart the application

```bash
# Stop with Ctrl+C, then restart
mvn spring-boot:run
```

---

## Step 7: Connect to Home Assistant (Optional)

If you have a Home Assistant instance:

### 7.1 Get a long-lived access token

1. Open Home Assistant → Your Profile (bottom left)
2. Scroll to "Long-Lived Access Tokens"
3. Click "Create Token"
4. Give it a name like "AlfaWall API"
5. Copy the token

### 7.2 Update `.env`

```bash
HOMEASSISTANT_TOKEN=eyJhbGciOiJIUzI1NiI...  # Your actual token
HOMEASSISTANT_BASE_URL=http://your-ha-ip:8123
```

### 7.3 Verify connection

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://your-ha-ip:8123/api/
```

Should return: `{"message": "API running."}`

### 7.4 Restart application with new settings

```bash
mvn spring-boot:run
```

---

## Step 8: Run Tests

```bash
# Run all tests
mvn test

# Should see: Tests run: X, Failures: 0, Errors: 0
```

**Note:** You may need to update test assertions in `ConversationApiIntegrationTest.java` to match your employee data.

---

## Common Issues & Solutions

### Issue: "No ChatModel bean found"

**Cause:** Missing or invalid `OPENAI_API_KEY`

**Solution:**
1. Check `.env` file has valid API key
2. Restart application to pick up new env vars
3. Verify key works:
   ```bash
   curl https://openrouter.ai/api/v1/models \
     -H "Authorization: Bearer $OPENAI_API_KEY"
   ```

### Issue: Build fails with "package com.embabel does not exist"

**Cause:** Maven can't download Embabel dependency

**Solution:**
```bash
# Clear Maven cache
rm -rf ~/.m2/repository/com/embabel

# Retry with force update
mvn clean package -U
```

### Issue: "Connection refused" to Home Assistant

**Cause:** Wrong URL or token

**Solution:**
1. Verify Home Assistant is running: Open http://your-ha-ip:8123 in browser
2. Check token is valid (see Step 7.3)
3. If testing without HA, set `employee-device.use-mock-data: true` in application.yaml

### Issue: LEDs not responding

**Cause:** WLED entity IDs don't match or wrong format

**Solution:**
1. Open Home Assistant → Developer Tools → States
2. Search for `light.wled`
3. Copy exact entity IDs to `employee-led.mappings` in application.yaml
4. Format example:
   ```yaml
   entity-id: light.wled_segment_0  # Exact ID from HA
   ```

---

## Next Steps

✅ **Congratulations!** You have a working conversation API!

### Learn More:
- **[TEMPLATE.md](TEMPLATE.md)** - Comprehensive customization guide
  - Adding new query types
  - Creating custom @Tool methods
  - Extending the employee model
  - Changing LED visualization logic

- **[README.md](README.md)** - Architecture overview and feature list

- **[CLAUDE.md](CLAUDE.md)** - Developer guide for Claude Code (AI assistant)

### Common Customizations:
1. **Add a new query type** (e.g., "certifications") → See TEMPLATE.md "Adding a New Query Type"
2. **Add custom employee fields** (e.g., "office location") → See TEMPLATE.md "Adding Custom Employee Fields"
3. **Connect to a database** instead of YAML → See TEMPLATE.md "Implementing a New Data Source"
4. **Change LLM provider** (e.g., use Ollama locally) → Update `SPRING_AI_OPENAI_BASE_URL` in .env

### Join the Community:
- Star this repository if you found it helpful! ⭐
- Report issues or suggest improvements
- Share your implementation!

---

## Quick Reference: Essential Commands

```bash
# Build
mvn clean package

# Run
mvn spring-boot:run

# Test
mvn test

# Test API
curl -X POST http://localhost:8080/api/conversation/process \
  -H "Content-Type: application/json" \
  -d '{"text":"Who knows Java?"}'
```

---

**Need help?** Check the troubleshooting section above or review [TEMPLATE.md](TEMPLATE.md) for detailed guidance.

**Ready to customize?** Dive into [TEMPLATE.md](TEMPLATE.md) for comprehensive extension examples!
