package nl.alfaone.domain;

import lombok.Data;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;

@Data
public class OfficeSchedule {
    private Map<DayOfWeek, Boolean> weeklySchedule = new HashMap<>();

    public OfficeSchedule() {
        // Default: Monday-Friday
        weeklySchedule.put(DayOfWeek.MONDAY, true);
        weeklySchedule.put(DayOfWeek.TUESDAY, true);
        weeklySchedule.put(DayOfWeek.WEDNESDAY, true);
        weeklySchedule.put(DayOfWeek.THURSDAY, true);
        weeklySchedule.put(DayOfWeek.FRIDAY, true);
        weeklySchedule.put(DayOfWeek.SATURDAY, false);
        weeklySchedule.put(DayOfWeek.SUNDAY, false);
    }
}
