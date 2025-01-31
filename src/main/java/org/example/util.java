package org.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class util {
    public static int getDaysDifference(String dateString) {
        // Define a formatter matching the input date-time pattern.
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        // Parse the input string as a LocalDateTime, then extract the date portion.
        LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
        LocalDate inputDate = dateTime.toLocalDate();

        // Get today's date.
        LocalDate today = LocalDate.now();

        // Calculate the difference in days (today -> inputDate).
        long daysDifference = ChronoUnit.DAYS.between(today, inputDate);

        // Return as integer.
        return (int) daysDifference;
    }

    public static void main(String[] args) {
        // Example usage:
        String dateStr = "2025-02-01 00:00:00.000";
        int diff = getDaysDifference(dateStr);
        System.out.println("Days difference: " + diff);
    }

}
