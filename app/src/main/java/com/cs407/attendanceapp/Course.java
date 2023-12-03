package com.cs407.attendanceapp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Course {
    private String courseName;
    private String timeRange;
    private String classDocumentId;

    private List<String> daysOfWeek;

    public Course(String courseName, String timeRange, String classDocumentId) {
        this.courseName = courseName;
        this.timeRange = timeRange;
        this.classDocumentId = classDocumentId;
    }

    public Course(String courseName, String timeRange, String classDocumentId, List<String> daysOfWeek) {
        this.courseName = courseName;
        this.timeRange = timeRange;
        this.classDocumentId = classDocumentId;
        this.daysOfWeek = daysOfWeek;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public String getId() { return classDocumentId; }

    public List<String> getDaysOfWeek() { return daysOfWeek; }

    private String[] getStartAndEndTimes()
    {
        // timeRange = startTime + " - " + endTime
        String[] times = timeRange.split("-");
        String startTime = times[0].substring(0, times[0].length()-2);
        String endTime = times[1].substring(0, times[0].length()-2);
        times[0] = startTime;
        times[1] = endTime;
        return times;
    }

    private Date[] getStartAndEndDates()
    {
        String[] times = timeRange.split("-");
        String startTime = times[0].substring(0, times[0].length()-2);
        String endTime = times[1].substring(0, times[0].length()-2);
        times[0] = startTime;
        times[1] = endTime;
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        try {
            Date startTimeAsDate = dateFormat.parse(times[0]);
            Date endTimeAsDate = dateFormat.parse(times[1]);
            Date[] dates = new Date[2];
            dates[0] = startTimeAsDate;
            dates[1] = endTimeAsDate;
            return dates;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isClassHappeningNow()
    {
        Date[] dates = getStartAndEndDates();
        LocalTime startLocalTime = dates[0].toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
        LocalTime endLocalTime = dates[1].toInstant().atZone(ZoneId.systemDefault()).toLocalTime();

        return (startLocalTime.isBefore(LocalTime.now()) && endLocalTime.isAfter(LocalTime.now()));
    }

    private String getDayOfWeek(int dayOfWeek) {
        String[] days = new String[]{"", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        return days[dayOfWeek];
    }

    public boolean isCourseScheduledToday() {
        Calendar calendar = Calendar.getInstance();
        Date currentDate = Calendar.getInstance().getTime();
        calendar.setTime(currentDate);
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String currentDay = getDayOfWeek(currentDayOfWeek);

        if (daysOfWeek != null && daysOfWeek.contains(currentDay)) {
            Date[] dates = getStartAndEndDates();

            return currentDate.after(dates[0]) && currentDate.before(dates[1]);
        }

        return false;
    }
}
