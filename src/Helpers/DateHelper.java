package Helpers;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Clase que convierte el valor de ticks en formato de fecha estandar
 *
 * @author Rubén Carretero <rubencc@gmail.com>
 */
public class DateHelper {

    /**
     * Convierte la marca de tiempo de tipo ticks en un string formateado.
     *
     * @param time
     * @return Fecha formateada
     */
    public static String convertLongToString(long time) {
        Date _time = new Date(time);
        SimpleDateFormat _formatTime = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
        return _formatTime.format(_time);
    }
}
