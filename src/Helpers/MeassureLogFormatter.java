/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Helpers;

import java.util.logging.LogRecord;

/**
 *
 * @author rubencc
 */
public class MeassureLogFormatter extends LogFormatter {

    @Override
    public String format(LogRecord record) {
        return "[" + DateHelper.convertLongToString(record.getMillis()) + "] TYPE:" + record.getSourceClassName() + " FROM: " + record.getSourceMethodName() + "\nLEVEL: " + record.getLevel() + " " + record.getMessage() + "\n\n";
    }
}
