package nl.alfaone.domain;

import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class ParkingAssignment {
    private String spotNumber;
    private List<DayOfWeek> recurringDays = new ArrayList<>();
    private List<LocalDate> specificDates = new ArrayList<>();
}
