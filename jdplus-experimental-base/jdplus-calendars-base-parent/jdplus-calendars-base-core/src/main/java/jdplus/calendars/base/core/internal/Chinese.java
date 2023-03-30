package jdplus.calendars.base.core.internal;

@lombok.Value
public class Chinese implements Date {

    //
    // fields
    //
    private long cycle;
    private int year;
    private int month;
    private boolean leapMonth;
    private int day;

    public Chinese(long cycle, int year, int month, boolean leapMonth, int day) {
        this.cycle = cycle;
        this.year = year;
        this.month = month;
        this.leapMonth = leapMonth;
        this.day = day;
    }

    //
    // constants
    //
    /*- chinese-epoch -*/
    // TYPE fixed-date
    // Fixed date of start of the Chinese calendar.
    public static final long EPOCH = Gregorian.toFixed(-2636, DateUtility.FEBRUARY, 15);

    /*- chinese-day-name-epoch -*/
    // TYPE integer
    // Index of Chinese sexagesimal name of RD 1.
    public static final int DAY_NAME_EPOCH = 15;

    /*- chinese-month-name-epoch -*/
    // TYPE integer
    // Index of Chinese sexagesimal name of month 1 of Chinese
    // year 1.
    public static final int MONTH_NAME_EPOCH = 3;

    //
    // date conversion methods
    //
    /*- fixed-from-chinese -*/
    // TYPE chinese-date -> fixed-date
    // Fixed date of Chinese date (cycle year month leap day).
    public static long toFixed(long cycle, int year, int month, boolean leapMonth, int day) {
        long midYear = (long) Math.floor(EPOCH + ((cycle - 1) * 60 + (year - 1) + .5) * DateUtility.MEAN_TROPICAL_YEAR);
        long theNewYear = newYearOnOrBefore(midYear);
        long p = newMoonOnOrAfter(theNewYear + 29 * (month - 1));
        Chinese d = Chinese.of(p);
        long priorNewMoon = month == d.month && leapMonth == d.leapMonth
                ? p
                : newMoonOnOrAfter(p + 1);
        return priorNewMoon + day - 1;
    }

    @Override
    public long toFixed() {
        return toFixed(cycle, year, month, leapMonth, day);
    }

    /*- chinese-from-fixed -*/
    // TYPE fixed-date -> chinese-date
    // Chinese date (cycle year month leap day) of fixed
    // $date$.
    public static Chinese of(long date) {
        long s1 = winterSolsticeOnOrBefore(date);
        long s2 = winterSolsticeOnOrBefore(s1 + 370);
        long m12 = newMoonOnOrAfter(s1 + 1);
        long nextM11 = newMoonBefore(s2 + 1);
        long m = newMoonBefore(date + 1);
        boolean leapYear = Math.round((nextM11 - m12) / DateUtility.MEAN_SYNODIC_MONTH) == 12;
        int month = (int) DateUtility.adjustedMod(
                (long) Math.round((m - m12) / DateUtility.MEAN_SYNODIC_MONTH)
                - (leapYear && hasPriorLeapMonth(m12, m) ? 1 : 0),
                12);
        boolean leapMonth = leapYear
                && hasNoMajorSolarTerm(m)
                && !hasPriorLeapMonth(m12, newMoonBefore(m));
        long elapsedYears = (long) Math.floor(1.5 - (month / 12d) + (date - EPOCH) / DateUtility.MEAN_TROPICAL_YEAR);
        long cycle = DateUtility.quotient(elapsedYears - 1, 60) + 1;
        int year = (int) DateUtility.adjustedMod(elapsedYears, 60);
        int day = (int) (date - m + 1);
        return new Chinese(cycle, year, month, leapMonth, day);
    }

    //
    // support methods
    //
    /*- chinese-solar-longitude-on-or-after -*/
    // TYPE (moment season) -> moment
    // Moment (Beijing time) of the first date on or after
    // fixed $date$ (Beijing time) when the solar longitude
    // will be $theta$ degrees.
    public static double solarLongitudeOnOrAfter(long date, double theta) {
        Location beijing = chineseLocation(date);
        double tee = DateUtility.solarLongitudeAfter(DateUtility.universalFromStandard(date, beijing), theta);
        return DateUtility.standardFromUniversal(tee, beijing);
    }

    /*- midnight-in-china -*/
    // TYPE fixed-date -> moment
    // Universal time of (clock) midnight at start of fixed
    // $date$ in China.
    public static double midnightInChina(long date) {
        return DateUtility.universalFromStandard(date, chineseLocation(date));
    }

    /*- chinese-winter-solstice-on-or-before -*/
    // TYPE fixed-date -> fixed-date
    // Fixed date, in the Chinese zone, of winter solstice
    // on or before fixed $date$.
    public static long winterSolsticeOnOrBefore(long date) {
        double approx = DateUtility.estimatePriorSolarLongitude(midnightInChina(date + 1), DateUtility.WINTER);
        long i;
        for (i = (long) (Math.floor(approx) - 1); !(DateUtility.WINTER <= DateUtility.solarLongitude(midnightInChina(i + 1))); ++i);
        return i;
    }

    /*- chinese-new-year-in-sui -*/
    // TYPE (fixed-date) -> fixed-date
    // Fixed date of Chinese New Year in sui (period from
    // solstice to solstice) containing $date$.
    public static long newYearInSui(long date) {
        long s1 = winterSolsticeOnOrBefore(date);
        long s2 = winterSolsticeOnOrBefore(s1 + 370);
        long m12 = newMoonOnOrAfter(s1 + 1);
        long m13 = newMoonOnOrAfter(m12 + 1);
        long nextM11 = newMoonBefore(s2 + 1);
        if ((Math.round((nextM11 - m12) / DateUtility.MEAN_SYNODIC_MONTH) == 12) && (hasNoMajorSolarTerm(m12) || hasNoMajorSolarTerm(m13))) {
            return newMoonOnOrAfter(m13 + 1);
        } else {
            return m13;
        }
    }

    /*- chinese-new-year-on-or-before -*/
    // TYPE fixed-date -> fixed-date
    // Fixed date of Chinese New Year on or before fixed $date$.
    public static long newYearOnOrBefore(long date) {
        long newYear = newYearInSui(date);
        return date >= newYear ? newYear : newYearInSui(date - 180);
    }

    /*- current-major-solar-term -*/
    // TYPE fixed-date -> integer
    // Last Chinese major solar term (zhongqi) before fixed
    // $date$.
    public static int currentMajorSolarTerm(long date) {
        double s = DateUtility.solarLongitude(DateUtility.universalFromStandard(date, chineseLocation(date)));
        return (int) DateUtility.adjustedMod(2 + DateUtility.quotient(s, DateUtility.deg(30)), 12);
    }


    /*- major-solar-term-on-or-after -*/
    // TYPE fixed-date -> moment
    // Fixed date (in Beijing) of the first Chinese major
    // solar term (zhongqi) on or after fixed $date$.  The
    // major terms begin when the sun's longitude is a
    // multiple of 30 degrees.
    public static double majorSolarTermOnOrAfter(long date) {
        double l = DateUtility.mod(30 * Math.ceil(DateUtility.solarLongitude(midnightInChina(date)) / 30), 360);
        return solarLongitudeOnOrAfter(date, l);
    }

    /*- current-minor-solar-term -*/
    // TYPE fixed-date -> integer
    // Last Chinese minor solar term (jieqi) before $date$.
    public static int currentMinorSolarTerm(long date) {
        double s = DateUtility.solarLongitude(midnightInChina(date));
        return (int) DateUtility.adjustedMod(3 + DateUtility.quotient(s - DateUtility.deg(15), DateUtility.deg(30)), 12);
    }


    /*- minor-solar-term-on-or-after -*/
    // TYPE fixed-date -> moment
    // Moment (in Beijing) of the first Chinese minor solar
    // term (jieqi) on or after fixed $date$.  The minor terms
    // begin when the sun's longitude is an odd multiple of 15
    // degrees.
    public static double minorSolarTermOnOrAfter(long date) {
        double l = DateUtility.mod(30 * Math.ceil((DateUtility.solarLongitude(midnightInChina(date)) - DateUtility.deg(15)) / 30) + DateUtility.deg(15), 360);
        return solarLongitudeOnOrAfter(date, l);
    }


    /*- chinese-new-moon-before -*/
    // TYPE fixed-date -> fixed-date
    // Fixed date (Beijing) of first new moon before
    // fixed $date$.
    public static long newMoonBefore(long date) {
        double tee = DateUtility.newMoonBefore(midnightInChina(date));
        return (long) Math.floor(DateUtility.standardFromUniversal(tee, chineseLocation(tee)));
    }

    /*- chinese-new-moon-on-or-after -*/
    // TYPE fixed-date -> fixed-date
    // Fixed date (Beijing) of first new moon on or after
    // fixed $date$.
    public static long newMoonOnOrAfter(long date) {
        double tee = DateUtility.newMoonAfter(midnightInChina(date));
        return (long) Math.floor(DateUtility.standardFromUniversal(tee, chineseLocation(tee)));
    }


    /*- no-major-solar-term? -*/
    // TYPE fixed-date -> boolean
    // True if Chinese lunar month starting on $date$
    // has no major solar term.
    public static boolean hasNoMajorSolarTerm(long date) {
        return currentMajorSolarTerm(date)
                == currentMajorSolarTerm(newMoonOnOrAfter(date + 1));
    }


    /*- prior-leap-month? -*/
    // TYPE (fixed-date fixed-date) -> boolean
    // True if there is a Chinese leap month on or after lunar
    // month starting on fixed day $m-prime$ and at or before
    // lunar month starting at fixed date $m$.
    /* RECURSIVE */
    public static boolean hasPriorLeapMonth(long mPrime, long m) {
        return m >= mPrime && (hasNoMajorSolarTerm(m) || hasPriorLeapMonth(mPrime, newMoonBefore(m)));
    }

    /*- chinese-location -*/
    // TYPE moment -> location
    // Location of Beijing; time zone varies with $tee$.
    public static final Location beijing(double tee) {
        long year = Gregorian.yearFromFixed((long) Math.floor(tee));
        return new Location("Beijing, China", DateUtility.deg(39.55), DateUtility.angle(116, 25, 0), DateUtility.mt(43.5), year < 1929 ? 1397d / 180 : 8);
    }

    public static final Location chineseLocation(double tee) {
        return beijing(tee);
    }

    /*- japanese-location -*/
    // TYPE moment -> location
    // Location for Japanese calendar; varies with $date$.
    public static final Location tokyo(double date) {
        long year = Gregorian.yearFromFixed((long) Math.floor(date));
        if (year < 1888) {
            return new Location("Tokyo, Japan", DateUtility.deg(35.7), DateUtility.angle(139, 46, 0), DateUtility.mt(24), 9 + 143d / 450);
        } else {
            return new Location("Tokyo, Japan", DateUtility.deg(35), DateUtility.deg(135), DateUtility.mt(0), 9);
        }
    }

    public static final Location japaneseLocation(double date) {
        return tokyo(date);
    }

    //
    // auxiliary methods
    //
    /*- chinese-new-year -*/
    // TYPE (fixed-date) -> fixed-date
    // Fixed date of Chinese New Year in sui (period from
    // solstice to solstice) containing $date$.
    public static long newYear(long gYear) {
        return newYearOnOrBefore(Gregorian.toFixed(gYear, DateUtility.JULY, 1));
    }


    /*- dragon-festival -*/
    // TYPE gregorian-year -> fixed-date
    // Fixed date of the Dragon Festival occurring in
    // Gregorian year.
    public static long dragonFestival(long gYear) {
        long elapsedYears = gYear - Gregorian.yearFromFixed(EPOCH) + 1;
        long cycle = DateUtility.quotient(elapsedYears - 1, 60) + 1;
        int year = (int) DateUtility.adjustedMod(elapsedYears, 60);
        return toFixed(cycle, year, 5, false, 5);
    }

    /*- qing-ming -*/
    // TYPE gregorian-year -> fixed-date
    // Fixed date of Qingming occurring in Gregorian year.
    public static long qingMing(long gYear) {
        return (long) Math.floor(minorSolarTermOnOrAfter(Gregorian.toFixed(gYear, DateUtility.MARCH, 30)));
    }

//    /*- chinese-age -*/
//    // TYPE (chinese-date fixed-date) -> non-negative-integer
//    // Age at fixed $date$, given Chinese $birthdate$,
//    // according to the Chinese custom.
//    public static long age(Chinese birthdate, long date)
//            throws BogusDateException {
//        Chinese today = new Chinese(date);
//        if (date >= birthdate.toFixed()) {
//            return 60 * (today.cycle - birthdate.cycle) + (today.year - birthdate.year) + 1;
//        } else {
//            throw new BogusDateException();
//        }
//    }
//
}
