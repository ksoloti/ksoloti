package axoloti.utils;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class Timestamp {

    public static String getCurrentTimestamp() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        DateFormat f = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
        return f.format(c.getTime());
    }

}
