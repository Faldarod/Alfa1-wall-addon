package nl.alfaone.domain;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerAssignment {
    private String customerName;
    private String role;
    private Integer percentage;  // 50%, 100%, etc.
    private LocalDate startDate;
    private LocalDate endDate;  // null = ongoing

    public boolean isActive() {
        LocalDate now = LocalDate.now();

        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }

        if (endDate != null && now.isAfter(endDate)) {
            return false;
        }

        return true;
    }
}
