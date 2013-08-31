package org.host.application.Entities;

import java.util.ArrayList;

/**
 * Clase que modela la entidad de mensaje que se usara al parsear mensajes desde
 * JSON o al recibir datos desde spot.
 *
 * @author Rubén Carretero <rubencc@gmail.com>
 */
public class Message {

    private long time;
    private String formatedTime;
    private ArrayList<Command> commands;

    public Message() {
        this.commands = new ArrayList<Command>();
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
     * @return the commands
     */
    public Command getCommand(int index) {
        return getCommands().get(index);
    }

    /**
     * @param commands the commands to set
     */
    public void setCommands(ArrayList<Command> commands) {
        this.commands = commands;
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
     * @return the commands
     */
    public ArrayList<Command> getCommands() {
        return commands;
    }

    @Override
    public String toString() {
        StringBuilder _sb = new StringBuilder();
        _sb.append(this.formatedTime);
        _sb.append("\n");
        for (int i = 0; i < this.commands.size(); i++) {
            _sb.append(this.commands.get(i).toString());
            _sb.append("\n");
        }
        return _sb.toString();

    }
}
