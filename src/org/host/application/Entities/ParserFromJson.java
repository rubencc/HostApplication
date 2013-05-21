package org.host.application.Entities;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Parsea un mensaje JSON a la entidad Message.
 *
 * @author rubencc
 */
public class ParserFromJson {

    /**
     * Parsea un string a una entidad Message
     *
     * @param input
     * @return
     */
    public Message parse(String input) {
        Message _message = new Message();
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
                //Command _comm = new Command((String) _element.get("Address"), (int) _temp, (String) _element.get("Value"), _time);
                Command _comm = new Command((String) _element.get("Address"), (int) _temp, _time, (String) _element.get("GUID"), (Boolean) _element.get("Broadcast"));
                JSONArray _valueList = (JSONArray) jsonObject.get("Values");
                Iterator vIt = _valueList.iterator();
                while (vIt.hasNext()) {
                    _comm.addValue((String) itr.next());
                }
                _message.AddCommand(_comm);
            }
        } catch (ParseException ex) {
            Logger.getLogger(ParserFromJson.class.getName()).log(Level.SEVERE, null, ex);
        }
        return _message;
    }
}
