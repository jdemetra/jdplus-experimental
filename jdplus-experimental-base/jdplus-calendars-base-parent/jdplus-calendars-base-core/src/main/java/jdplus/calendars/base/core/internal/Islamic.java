package jdplus.calendars.base.core.internal;

@lombok.Value
public class Islamic implements Date {

    public long year;
    public int month;
    public int day;

    //
    // constants
    //
    /*- islamic-epoch -*/
    // TYPE fixed-date
    // Fixed date of start of the Islamic calendar.
    public static final long EPOCH = Julian.toFixed(Julian.CE(622), DateUtility.JULY, 16);

    /*- mecca -*/
    // TYPE location
    // Location of Mecca.
    public static final Location MECCA = new Location("Mecca, Saudi Arabia", DateUtility.angle(21, 25, 24), DateUtility.angle(39, 49, 24), DateUtility.mt(1000), 2);

    //
    // date conversion methods
    //
    /*- fixed-from-islamic -*/
    // TYPE islamic-date -> fixed-date
    // Fixed date equivalent to Islamic date.
    public static long toFixed(long year, int month, int day) {
        return day
                + 29 * (month - 1)
                + DateUtility.quotient((6 * month) - 1, 11)
                + (year - 1) * 354
                + DateUtility.quotient(3 + 11 * year, 30)
                + EPOCH - 1;
    }

    @Override
    public long toFixed() {
        return toFixed(year, month, day);
    }

    /*- islamic-from-fixed -*/
    // TYPE fixed-date -> islamic-date
    // Islamic date (year month day) corresponding to fixed
    // $date$.
    public static Islamic fromFixed(long date) {
        long year = DateUtility.quotient(30 * (date - EPOCH) + 10646, 10631);
        long priorDays = date - toFixed(year, 1, 1);
        int month = (int) DateUtility.quotient(11 * priorDays + 330, 325);
        int day = (int) (1 + date - toFixed(year, month, 1));
        return new Islamic(year, month, day);
    }

    //
    // support methods
    //
    /*- islamic-leap-year? -*/
    // TYPE islamic-year -> boolean
    // True if $i-year$ is an Islamic leap year.
    public static boolean isLeapYear(long iYear) {
        return DateUtility.mod(11 * iYear + 14, 30) < 11;
    }

    /*- asr -*/
    public static double asr(long date, Location locale)
            throws BogusTimeException {
        double noon = DateUtility.universalFromStandard(DateUtility.midday(date, locale), locale);
        double phi = locale.getLatitude();
        double delta = DateUtility.arcSinDegrees(DateUtility.sinDegrees(DateUtility.obliquity(noon)) * DateUtility.sinDegrees(DateUtility.solarLongitude(noon)));
        double altitude = DateUtility.arcSinDegrees(DateUtility.sinDegrees(phi) * DateUtility.sinDegrees(delta) + DateUtility.cosDegrees(phi) * DateUtility.cosDegrees(delta));
        double h = DateUtility.arcTanDegrees(DateUtility.tanDegrees(altitude) / (1 + 2 * DateUtility.tanDegrees(altitude)), 1);
        return DateUtility.dusk(date, locale, -h);
    }

    //
    // auxiliary methods
    //
    /*- islamic-in-gregorian -*/
    // List of the fixed dates of Islamic month, day
    // that occur in Gregorian year.
    public static long[] inGregorian(int iMonth, int iDay, long gYear) {
        long jan1 = Gregorian.toFixed(gYear, DateUtility.JANUARY, 1);
        long dec31 = Gregorian.toFixed(gYear, DateUtility.DECEMBER, 31);
        long y = fromFixed(jan1).year;
        long date1 = toFixed(y, iMonth, iDay);
        long date2 = toFixed(y + 1, iMonth, iDay);
        long date3 = toFixed(y + 2, iMonth, iDay);
        long[] ll = new long[2];
        int lpos=0;
        if (jan1 <= date1 && date1 <= dec31) {
            ll[lpos++]=date1;
        }
        if (jan1 <= date2 && date2 <= dec31) {
            ll[lpos++]=date2;
        }
        if (jan1 <= date3 && date3 <= dec31) {
            ll[lpos++]=date3;
        }
        switch (lpos){
            case 0:
                return DateUtility.LEMPTY;
            case 1:
                return new long[]{ll[0]};
            default:
                return ll;
        }
    }


    /*- Ras-El-Am -*/
    // TYPE gregorian-year -> list-of-fixed-dates
    // List of fixed dates of Ras-El-Am (New Year) occurring in
    // Gregorian year.
    public static long[] rasElAm(long gYear) {
        return inGregorian(1, 1, gYear);
    }

    /*- Achoura -*/
    // TYPE gregorian-year -> list-of-fixed-dates
    // List of fixed dates of Ras-El-Am (New Year) occurring in
    // Gregorian year.
    public static long[] achoura(long gYear) {
        return inGregorian(1, 10, gYear);
    }

    /*- Magal de Touba -*/
    // TYPE gregorian-year -> list-of-fixed-dates
    // List of fixed dates of Ras-El-Am (New Year) occurring in
    // Gregorian year.
    public static long[] magaldeTouba(long gYear) {
        return inGregorian(2, 18, gYear);
    }

    /*- mawlid-an-nabi -*/
    // TYPE gregorian-year -> list-of-fixed-dates
    // List of fixed dates of Mawlid-an-Nabi occurring in
    // Gregorian year.
    public static long[] mawlidAnNabi(long gYear) {
        return inGregorian(3, 12, gYear);
    }

    /*- Ramadan -*/
    // TYPE gregorian-year -> list-of-fixed-dates
    // List of fixed dates of Ras-El-Am (New Year) occurring in
    // Gregorian year.
    public static long[] startRamadan(long gYear) {
        return inGregorian(9, 1, gYear);
    }

    /*- Ramadan -*/
    // TYPE gregorian-year -> list-of-fixed-dates
    // List of fixed dates of Ras-El-Am (New Year) occurring in
    // Gregorian year.
    public static long[] aidelFitr(long gYear) {
        return inGregorian(10, 1, gYear);
    }

    /*- Ramadan -*/
    // TYPE gregorian-year -> list-of-fixed-dates
    // List of fixed dates of Ras-El-Am (New Year) occurring in
    // Gregorian year.
    public static long[] aidelAdha(long gYear) {
        return inGregorian(12, 10, gYear);
    }

}
