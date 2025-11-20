This document describes the phases to setup this project.

phases:
Phase 1:
- name: "1. Architecture & Setup"
  description: "Define project structure and core agent architecture."
  tasks:
  - Create Maven project skeleton `alfa-wall-addon`.
  - Implement package structure:
    - Use a hexagonal architecture
    - Have different agents with separation of concern. 
      - IntentAgent: Checks the intent and which other agents are needed. 
      - EmployeeInformationCollectorAgent: collects data about the employee from a vector database. 
      - PresentCollectorAgent: collects data if needed about devices/people that are on the present in home-assistant, kantoordagen app or on the local-network using ubiquiti
      - PrivacyOfficerAgent: Checks if the question that was asked contains GDPR related data and if that is the case. We dont act on that question, but return a response why we dont act.
      - ActionAgent: Is the only one allowed to turn on led-segments corresponding the the employees.
  - Add `pom.xml` dependencies (Spring Boot starter web, reactor, lombok, junit5).
  - Define application.yaml with environment-based configuration keys.
    deliverables:
  - Compilable Maven project with empty class stubs and build passing (`mvn clean package`).

Phase 2:
- name: "2. Implement Agents"
  description: "Implement logic for all agents with in-memory context passing."
  tasks:
    - Implement `AgentContext` record to carry conversation state.
    - `IntendAgent`: parse natural-language query, extract employee names and determines the intent.
    - `EmployeeInformationCollectorAgent`: parse natural-language query, call vector context.
    - `PresenceCollector`: call Kantoordagen and home-assistant UniFi rest-api for live presence.
    - `PrivacyOfficerAgent`: filter or redact sensitive information from results.
    - `ActionAgent`: send commands via Home Assistant API to control WLED segments.
    - `OrchestratorAgent`: manage sequential calls and return combined response.
      deliverables:
    - Fully working Java logic, returning mocked responses if APIs unavailable.

Phase 3: 
  - name: "3. API Integration"
    description: "Implement REST endpoints and Home Assistant integration."
    tasks:
      - See: https://developers.home-assistant.io/docs/intent_conversation_api/
      - Test via `curl` or Postman locally.
        deliverables:
      - Local API working independently of HA container.
      - Verified logs showing agent execution order.

Phase 4:
  - name: "4. Containerization"
    description: "Containerize Java service and define full local stack."
    tasks:
      - Write `Dockerfile` for add-on:
        ```Dockerfile
        FROM eclipse-temurin:25-jre
        WORKDIR /app
        COPY target/ai-presence.jar /app/ai-presence.jar
        EXPOSE 8080
        CMD ["java", "-jar", "/app/ai-presence.jar"]
        ```
      - Create `docker-compose.yml` with:
          - Home Assistant Core
          - Alfa-Wall-addon
          - WLED simulator
      - Ensure environment variables are properly passed.
      - Validate communication between services.
        deliverables:
      - `docker compose up -d --build` starts all containers without errors.
    
Phase 5:
  - name: "5. Home Assistant Integration"
    description: "Link the addon to Home Assistant conversation system."
    tasks:
      - Add configuration snippet in HA config:
        ```yaml
        conversation:
          language: nl
          agents:
            - name: AI Presence Agent
              type: http
              url: http://ai-presence-addon:8080/conversation
        ```
      - Restart Home Assistant.
      - Test by asking: “Wie is er vandaag op kantoor?”
      - Confirm that WLED simulator lights up appropriate segments.
        deliverables:
      - Full HA conversation loop working end-to-end.

Phase 6:
  - name: "6. Testing & Validation"
    description: "Run automated and manual verification."
    tasks:
      - Unit tests for all agents using mock data.
      - End-to-end integration test with Docker Compose.
      - Confirm privacy filtering (no PII in logs).
      - Confirm that add-on scales correctly with multiple queries.
        deliverables:
      - JUnit tests passing.
      - Manual functional test successful.

Phase 7:
  - name: "7. Documentation"
    description: "Create comprehensive README and developer guide."
    tasks:
      - Document setup steps for local testing.
      - Include API reference, environment variable list, example queries.
      - Include Docker Compose commands.
        deliverables:
      - `README.md` explaining:
          - System overview
          - Installation steps
          - Usage examples
          - Privacy considerations
          - Troubleshooting guide

Phase 8:
 - name: "8. Load csv data into vector database"
   description: "Load csv data into vector database"
   tasks:
   - Load csv data into vector database
   - Connect the vector database with the `EmployeeInformationCollectorAgent`  

outputs:
- Full Maven project (`alfa-wall-addon/`)
- Dockerfile
- Docker Compose file
- Home Assistant config (`configuration.yaml`)
- README.md[java](../src/main/java)

success_criteria:
- Project builds successfully.
- Local Docker stack runs (`docker compose up -d`).
- Home Assistant can query the add-on successfully.
- WLED simulator reflects presence visually.
- Privacy filters verified.
- All agents operate in correct order with stateless context.

notes:
- No persistent data storage.
- Focus on clean, well-documented agent flow.
- Use async API calls where appropriate.
- Support extension for LLM integration (Embabel or local model) later.
