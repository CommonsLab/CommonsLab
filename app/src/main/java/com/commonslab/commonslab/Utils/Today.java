package com.commonslab.commonslab.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Valdio Veliu on 31/01/2017.
 */

public class Today {
    public String date() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat month = new SimpleDateFormat("MMMM");
        SimpleDateFormat day = new SimpleDateFormat("d");
        String formattedMonth = month.format(c.getTime());
        String formattedDay = day.format(c.getTime());
        return formattedMonth + " " + formattedDay;
    }
}
