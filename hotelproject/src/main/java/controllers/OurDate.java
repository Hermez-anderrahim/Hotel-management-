package controllers;

import java.time.LocalDate;

public class OurDate {
    private int year ;
    private int month;
    private int day;
    final int  currentYear = LocalDate.now().getYear();
    /**
     *
     * @param day the day of the month
     * @param month the month of the year
     * @param year the year
     * */
    public OurDate(int day ,int month ,int  year ) {
        this.day = day;
        this.month = month;
        this.year = year;
        try {
            this.validate();
        }catch (InvalidDateException e) {
            System.out.println("error : " + e.getMessage());
        }
    }
    /**
     * if no date is provided then the current date is used (Calendar class is used to get the current date)
     * */
    public OurDate(){
        this.MakeTodaydate();
    }

    void MakeTodaydate(){

        LocalDate today = LocalDate.now();
        this.year = today.getYear();
        this.month = today.getMonthValue();
        this.day = today.getDayOfMonth();

    }


    void validate() throws InvalidDateException{
        if (String.valueOf(this.year).length()!=4) {
            throw new InvalidDateException("Invalid year");
        }

        if (this.month < 1 || this.month > 12) {
            throw new InvalidDateException("Invalid day: Day must be between 1 and  12 ");
        }

        int  daysInMonth = this.getDaysInMonth();
        if (this.day < 1 || this.day > daysInMonth) {
            throw new InvalidDateException("Invalid day: Day must be between 1 and " + daysInMonth + " for this month.");
        }
    }
   /**
       validate the input date , year and day and month should be in specified ranges
       consider leap years , if leap year or not then verify  the month if it is february
       if it is check the day (28, 29)
    */
    int getDaysInMonth() {
        int[] daysInMonthLookup = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (this.isLeapYear() && this.month == 2) {
            return 29; // February has 29 days in a leap year
        } else {
            return daysInMonthLookup[this.month - 1];
        }
    }
    /**
    *  verify the input year if it is leap or not <s
    *  to treat the all cases
    */
    boolean  isLeapYear() {
        return (this.year % 4 == 0 && this.year % 100 != 0) || (this.year % 400 == 0);
    }


    public int getDay(){
        return this.day;
    }
    public int getMonth(){
        return this.month;
    }
    public int getYear(){
        return this.year;
    }

    public void setDay(int day) throws  InvalidDateException{
        if (day < 1 || day > 31) {
            throw new InvalidDateException("day is invalid");
        }
        this.day = day;
    }

    public void setMonth(int month) throws InvalidDateException {
        if (month < 1 || month > 12) {
            throw new InvalidDateException("month no valid number");
        }
        this.month = month;
    }

    public void setYear(int year) {
        this.year = year;
    }

    String tostring(){
        return  this.day + "/" + this.month + "/" + this.year;
    }

    /**
     * Compare if date1 is before date2
     * */
    public static  boolean Compare(OurDate date1 , OurDate date2){

        // Compare years
        if (date1.year < date2.year) {
            return true;
        } else if (date1.year > date2.year) {
            return false;
        }

        // Years are equal, compare months
        if (date1.month < date2.month) {
            return true;
        } else if (date1.month > date2.month) {
            return false;
        }

        return date1.day < date2.day;
    }

    public boolean equals(OurDate date) {
        if (date == null) return false;
        if (this == date) return true;
        if (this.getClass() != date.getClass()) return false;
        return this.day == date.day && this.month == date.month && this.year == date.year;
    }

    public static long getDaysBetweenDates(OurDate date1 , OurDate date2 ) throws  InvalidDateException {

        if (date1.getMonth() < 1 || date1.getMonth() > 12 || date2.getMonth() < 1 || date2.getMonth() > 12) {
            throw new InvalidDateException("Invalid date") ;
        }

        // Ensure date1 is before date2 (swap if needed)
        if (!Compare(date1 ,date2)) {
            int tempDay = date1.getDay();
            int tempMonth = date1.getMonth();
            int tempYear = date1.getYear();

            date1.setDay(date2.getDay());
            date1.setMonth(date2.getMonth());
            date1.setYear(date2.getYear());

            date2.setDay(tempDay);
            date2.setMonth(tempMonth);
            date2.setYear(tempYear);
        }

        long totalDays = 0;

        // Handle year difference (including leap years)
        for (int y = date1.getYear(); y < date2.getYear(); y++) {
            totalDays += (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0) ? 366 : 365;
        }

        // Handle month difference
        for (int m = date1.getMonth(); m < date2.getMonth(); m++) {
            int daysInMonth;
            switch (m) {
                case 1:  // January
                case 3:  // March
                case 5:  // May
                case 7:  // July
                case 8:  // August
                case 10: // October
                case 12: // December
                    daysInMonth = 31;
                    break;
                case 4:  // April
                case 6:  // June
                case 9:  // September
                case 11: // November
                    daysInMonth = 30;
                    break;
                case 2:  // February
                    daysInMonth = (date2.year % 4 == 0 && date2.year % 100 != 0) || (date2.year % 400 == 0) ? 29 : 28;  // Use year from date2
                    break;
                default:
                    daysInMonth = 0;
            }
            totalDays += daysInMonth;
        }

        // Handle day difference within the same month
        totalDays += date2.getDay() - date1.getDay();

        return totalDays;
    }


    /**
     * Parse a date string in the format day/month/year
     * @param dateString the date string to parse
     * @return a new OurDate object
     * @throws InvalidDateException if the date string is not in the expected format
     * */
    public static OurDate parse(String dateString) throws InvalidDateException {
        if (dateString != null) {
            String[] parts = dateString.split("/");
            if (parts.length != 3) {
                throw new InvalidDateException("Invalid date format. Expected format is day/month/year");
            }
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            return new OurDate(day, month, year);
        }
        return null;
    }

    @Override
    public String toString() {
        return day + "/" + month + "/" + year;
    }
}