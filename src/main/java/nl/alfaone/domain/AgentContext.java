package nl.alfaone.domain;

import java.util.List;

public record AgentContext(String query, Intent intent, List<String> employeeNames, List<Presence> presences) {
}
