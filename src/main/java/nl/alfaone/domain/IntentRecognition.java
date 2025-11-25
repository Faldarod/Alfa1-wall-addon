package nl.alfaone.domain;

import java.util.List;

public record IntentRecognition(Intent intent, List<String> employeeNames) {
}
