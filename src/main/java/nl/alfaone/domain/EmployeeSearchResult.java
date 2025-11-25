package nl.alfaone.domain;

import java.util.List;

/**
 * Result of employee collection/search operation.
 * This object is produced by EmployeeCollectorAgent and consumed by ActionAgent.
 * Represents the OODA "Orient" step - understanding who matches the query and why.
 */
public record EmployeeSearchResult(
    String query,
    List<Employee> employees,
    QueryType queryType
) {
    /**
     * Check if any employees were found
     */
    public boolean hasEmployees() {
        return employees != null && !employees.isEmpty();
    }

    /**
     * Get count of employees found
     */
    public int getCount() {
        return employees != null ? employees.size() : 0;
    }
}
