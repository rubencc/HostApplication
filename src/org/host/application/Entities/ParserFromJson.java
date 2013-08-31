package org.host.application.Entities;

import Helpers.LogHelper;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Clase que parsea un mensaje JSON a la entidad Message.
 *
 * @author Rubén Carretero <rubencc@gmail.com>
 */
public class ParserFromJson {

    private final String CLASSNAME = getClass().getName();
    private LogHelper logger;

    /**
     * Parsea un string a una entidad Message
     *
     * @param input
     * @return
     */
    public Message parse(String input) {
        Message _message = new Message();
        this.logger = LogHelper.getInstance();
        try {

            JSONParser parser = new JSONParser();
            //Parsea la cadena de caracteres de entrada
            Object obj = parser.parse(input);
            //Crea el objeto JSON a partir del objeto parseado.
            JSONObject jsonObject = (JSONObject) obj;
            //Extrae la marca de tiempo.
            long _time = (Long) jsonObject.get("Time");
            _message.setTime(_time);
            //Crea una array de objetos, para cada uno de los objetos se crea una nueva entidad Command.
            JSONArray _commandList = (JSONArray) jsonObject.get("Commands");
            Iterator itr = _commandList.iterator();
            while (itr.hasNext()) {
                JSONObject _element = (JSONObject) itr.next();
                long _temp = (Long) _element.get("Type");
                Command _comm = new Command((String) _element.get("Address"), (int) _temp, _time, (String) _element.get("GUID"), (Boolean) _element.get("Broadcast"));
                JSONArray _valueList = (JSONArray) _element.get("Values");
                for (int i = 0; i < _valueList.size(); i++) {
                    _comm.addValue((String) _valueList.get(i));
                }

                _message.AddCommand(_comm);
            }
        } catch (ParseException ex) {
            this.logger.logSEVERE(CLASSNAME, "parse -- ParseException", ex.getMessage());
        }
        return _message;
    }
}
