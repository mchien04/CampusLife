package vn.campuslife.model.activity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalCalendarResponse {
    private LocalDate from;
    private LocalDate to;
    private List<CalendarMarkedDate> markedDates = new ArrayList<>();
    private List<PersonalCalendarEventItem> events = new ArrayList<>();
}
