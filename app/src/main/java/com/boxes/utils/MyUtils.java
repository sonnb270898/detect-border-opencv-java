package com.boxes.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyUtils {
    public MyUtils() {
    }

    public static String stampToDate(String s) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
        long lt = new Long(s);
        Date date = new Date(lt);
        String res = simpleDateFormat.format(date);
        return res;
    }

    public static String dateToStamp(String s) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = simpleDateFormat.parse(s);
        long ts = date.getTime();
        String res = String.valueOf(ts);
        return res;
    }

    public static String getWeightUnit(String data) {
        if (data.indexOf("kg") != -1) {
            return "kg";
        } else if (data.indexOf("g") != -1) {
            return "g";
        } else if (data.indexOf("lb") != -1) {
            return "磅";
        } else if (data.indexOf("LB") != -1) {
            return "磅";
        } else if (data.indexOf("jl") != -1) {
            return "斤";
        } else {
            return data.indexOf("JL") != -1 ? "斤" : "?";
        }
    }
}
