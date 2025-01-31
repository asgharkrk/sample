package org.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class util {
    public static int getDaysDifference(String dateString) {
        // 1. Use the correct formatter for "yyyy-MM-dd"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 2. Parse directly into a LocalDate
        LocalDate inputDate = LocalDate.parse(dateString, formatter);

        // 3. Get today's date
        LocalDate today = LocalDate.now();

        // 4. Calculate difference in days
        long daysDifference = ChronoUnit.DAYS.between(today, inputDate);

        return (int) daysDifference;
    }

    public static void main(String[] args) {
        // Example usage:
        String dateStr = "2024-02-01";
        int diff = getDaysDifference(dateStr);
        System.out.println("Days difference: " + diff);
    }

}
