package org.host.application.Entities;

import java.util.ArrayList;

/**
 * Clase que modela la entidad de mensaje que se usara al parsear mensajes desde
 * JSON o al recibir datos desde spot.
 *
 * @author rubencc
 */
public class Message {

    private long time;
    private String formatedTime;
    private ArrayList<Command> _commands;

    public Message() {
        this._commands = new ArrayList<Command>();
    }

    /**
     * @return the time
     */
    public long getTime() {
        return time;
    }

    /**
     * @param time the time to set
     */
    public void setTime(long time) {
        this.time = time;
        this.formatedTime = Helpers.DateHelper.convertLongToString(time);
    }

    /**
     * @return the _commands
     */
    public Command getCommand(int index) {
        return getCommands().get(index);
    }

    /**
     * @param commands the _commands to set
     */
    public void setCommands(ArrayList<Command> commands) {
        this._commands = commands;
    }

    /**
     * Añade un comando a la lista.
     *
     * @param command
     */
    public void AddCommand(Command command) {
        this.getCommands().add(command);
    }

    /**
     * Devuelve el numero de comandos que contiene el mensaje
     *
     * @return
     */
    public int numberOfCommands() {
        return getCommands().size();
    }

    /**
     * @return the formatedTime
     */
    public String getFormatedTime() {
        return formatedTime;
    }

    /**
     * Devuelve la lista de comandos.
     *
     * @return the _commands
     */
    public ArrayList<Command> getCommands() {
        return _commands;
    }
}
