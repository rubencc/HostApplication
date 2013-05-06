/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.host.application.Entities;

import Helpers.DateHelper;

/**
 *
 * Entidad de comando para enviar o recibir a un spot.
 *
 * @author rubencc
 */
public class Command {

    private String address;
    private int Type;
    private String value;
    private long time;
    private String formatedTime;
    private String GUID;
    private boolean broadcast;
    private final String EMPTY = " ";

    public Command(String address, int type, String value, long time) {
        this.address = address;
        this.Type = type;
        this.value = value;
        this.time = time;
        this.GUID = EMPTY;
        this.broadcast = false;
        this.formatedTime = DateHelper.convertLongToString(time);

    }

    public Command(String address, int type, String value, long time, String GUID, boolean broadcast) {
        this.address = address;
        this.Type = type;
        this.value = value;
        this.time = time;
        this.GUID = GUID;
        this.broadcast = broadcast;
        this.formatedTime = DateHelper.convertLongToString(time);

    }

    public Command(int type) {
        this.address = EMPTY;
        this.Type = type;
        this.value = EMPTY;
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
        return new String("Address: " + this.address + " Tipo: " + this.Type + "\nValue: " + this.value + " GUID: " + this.GUID + "\nBroadcast: " + this.broadcast);
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
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
}
