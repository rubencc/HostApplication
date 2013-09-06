/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.host.application.Entities;

import Helpers.DateHelper;
import java.util.ArrayList;

/**
 *
 * Clase que modela la entidad de comando para enviar o recibir a un spot.
 *
 * @author Rubén Carretero <rubencc@gmail.com>
 */
public class Command {

    private String address;
    private int Type;
    private ArrayList<String> values;
    private long time;
    private String formatedTime;
    private String GUID;
    private boolean broadcast;
    private final String EMPTY = " ";

    public Command(String address, int type, ArrayList<String> values, long time) {
        this.address = address.toUpperCase();
        this.Type = type;
        this.values = values;
        this.time = time;
        this.GUID = EMPTY;
        this.broadcast = false;
        this.formatedTime = DateHelper.convertLongToString(time);

    }

    public Command(String address, int type, ArrayList<String> values, long time, String GUID, boolean broadcast) {
        this.address = address.toUpperCase();
        this.Type = type;
        this.values = values;
        this.time = time;
        this.GUID = GUID;
        this.broadcast = broadcast;
        this.formatedTime = DateHelper.convertLongToString(time);

    }

    public Command(String address, int type, long time, String GUID, boolean broadcast) {
        this.address = address.toUpperCase();
        this.Type = type;
        this.values = new ArrayList<String>();
        this.time = time;
        this.GUID = GUID;
        this.broadcast = broadcast;
        this.formatedTime = DateHelper.convertLongToString(time);

    }

    public Command(int type) {
        this.address = EMPTY;
        this.Type = type;
        this.values = new ArrayList<String>();
        this.time = System.currentTimeMillis();
        this.GUID = EMPTY;
        this.broadcast = false;
        this.formatedTime = DateHelper.convertLongToString(time);

    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return the message
     */
    public int getType() {
        return Type;
    }

    /**
     * @return the time
     */
    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "Address: " + this.address + " Type: " + this.Type + "\nValue: " + this.values + "\nGUID: " + this.GUID + " Broadcast: " + this.broadcast;
    }

    /**
     * @return the value
     */
    public ArrayList<String> getValue() {
        return values;
    }

    /**
     * @return the formatedTime
     */
    public String getFormatedTime() {
        return formatedTime;
    }

    /**
     * @return the GUID
     */
    public String getGUID() {
        return GUID;
    }

    /**
     * @param GUID the GUID to set
     */
    public void setGUID(String GUID) {
        this.GUID = GUID;
    }

    /**
     * @return the individual
     */
    public boolean isBroadcast() {
        return broadcast;
    }

    /**
     * @param individual the individual to set
     */
    public void setBroadcast(boolean individual) {
        this.broadcast = individual;
    }

    public void addValue(String value) {
        this.values.add(value);
    }

    public int countValues() {
        return this.values.size();
    }
}
