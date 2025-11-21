project:
name: "AI Presence Assistant for Home Assistant"
description:
A Java 25 (Spring Boot) Home Assistant add-on that answers natural language questions
about employees and controls WLED LED segments accordingly.
Runs locally via Docker Compose with Home Assistant Core and a WLED simulator.

goals:
- Build a Spring Boot application (Java 25) that acts as a conversation agent for Home Assistant.
- Implement agentic architecture (InfoAgent, PresenceCollector, PrivacyOfficerAgent, ActionAgent).
- Integrate with external REST APIs (Kantoordagen-app)
  - To gather the following information
    - Who is registered to be at the office on a certain date?
    - Who is registered to have a parking spot?
- Integrate with Unifi via Home Assistant to detect presence
  - Presence can be detected via MAC-addresses
- Control WLED LEDs via Home Assistant REST API.
- Provide full local testing environment using Docker Compose.
- No persistent database; state is passed in memory only.

constraints:
- Use Java 25, Maven, Spring Boot 3.2+
- Use Hexagonal Architecture
- Use Embabel: https://docs.embabel.com/embabel-agent/guide/0.1.2-SNAPSHOT/
- Stateless processing only.
- No personally identifiable information (PII) should be logged or persisted or answered.
- Docker Compose must start all components with one command.
- Use async WebClient calls where possible.

To see more about the phases to setup this project. See the `documentation/Phases.md` file.

Later extensions will be:
- Run against different kind of LLM's
  - Can run with ollama locally
- React on Home-assistant events/changes of sensors
- Multi lingual support 