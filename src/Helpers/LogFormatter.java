package Helpers;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Formato para los mensajes de log
 *
 * @author Rubén Carretero <rubencc@gmail.com>
 */
public class LogFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        return "[" + DateHelper.convertLongToString(record.getMillis()) + "] on CLASS:" + record.getSourceClassName() + " METHOD: " + record.getSourceMethodName() + "\nLEVEL: " + record.getLevel() + " " + record.getMessage() + "\n\n";
    }
}
