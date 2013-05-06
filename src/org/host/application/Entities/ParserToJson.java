/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.host.application.Entities;

import java.util.ArrayList;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author rubencc
 */
public class ParserToJson {

    /**
     * Parsea una lista de comando para formar un mensaje en formato JSON
     *
     * @param commandList
     * @return
     */
    public String parse(ArrayList<Command> commandList) {

        JSONObject _objJSON = new JSONObject();
        //Añade el tiempo del mensaje
        _objJSON.put("Time", new Long(System.currentTimeMillis()));
        //Añade la lista e comandos
        JSONArray _listJSON = new JSONArray();
        Iterator itr = commandList.iterator();
        while (itr.hasNext()) {
            JSONObject _temp = new JSONObject();
            Command _cmd = (Command) itr.next();
            _temp.put("Address", _cmd.getAddress());
            _temp.put("Type", _cmd.getType());
            _temp.put("Value", _cmd.getValue());
            _temp.put("Time", new Long(_cmd.getTime()));
            _temp.put("GUID", _cmd.getGUID());
            _temp.put("Broadcast", _cmd.isBroadcast());
            _listJSON.add(_temp);
        }
        _objJSON.put("Commands", _listJSON);
        return _objJSON.toJSONString();
    }
}
