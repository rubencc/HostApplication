/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Helpers;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author rubencc
 */
public class DateHelper {

    /**
     * Convierte la marca de tiempo de tipo ticks en un string formateado.
     *
     * @param time
     * @return
     */
    public static String convertLongToString(long time) {
        Date _time = new Date(time);
        SimpleDateFormat _formatTime = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
        return _formatTime.format(_time);
    }
}
