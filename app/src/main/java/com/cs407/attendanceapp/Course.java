package com.cs407.attendanceapp;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Course {
    private String courseName;
    private String timeRange;
    private String classDocumentId;
    private Date startDate;
    private Date endDate;

    private List<String> daysOfWeek;

    public Course(String courseName, String timeRange, String classDocumentId, List<String> daysOfWeek, Date startDate, Date endDate) {
        this.courseName = courseName;
        this.timeRange = timeRange;
        this.classDocumentId = classDocumentId;
        this.daysOfWeek = daysOfWeek;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public String getId() { return classDocumentId; }

    public List<String> getDaysOfWeek() { return daysOfWeek; }

    private Date[] getStartAndEndTimes()
    {
        Log.i("INFO", "timeRange: " + timeRange);
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
            Log.i("INFO", "start time: " + dates[0].toString());
            Log.i("INFO", "end time: " + dates[1].toString());
            return dates;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isClassHappeningNow()
    {
        Date[] dates = getStartAndEndTimes();
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
        Log.i("INFO", "currentDate = " + currentDate.toString());
        calendar.setTime(currentDate);
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String currentDay = getDayOfWeek(currentDayOfWeek);
        Log.i("INFO", "current day of the week: " + currentDay);

        Log.i("INFO","daysOfWeek != null: " + (daysOfWeek != null) + "\tdaysOfWeek.contains(currentDay): " + daysOfWeek.contains(currentDay));
        if (daysOfWeek != null && daysOfWeek.contains(currentDay)) {
            // set time to 12am
            LocalDateTime localDateTime = this.startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime updatedLocalDateTime = localDateTime.withHour(0).withMinute(0).withSecond(0);
            Date startDateTwelveAm = Date.from(updatedLocalDateTime.atZone(ZoneId.systemDefault()).toInstant());
            Log.i("INFO", "startDateTwelveAm: " + startDateTwelveAm.toString());
            Log.i("INFO", "currentDate.after(startDateTwelveAm) && currentDate.before(this.endDate): " + (currentDate.after(startDateTwelveAm) && currentDate.before(this.endDate)));
            return currentDate.after(startDateTwelveAm) && currentDate.before(this.endDate);
        }

        return false;
    }
}
