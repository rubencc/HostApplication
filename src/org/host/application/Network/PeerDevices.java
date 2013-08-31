package org.host.application.Network;

import Helpers.LogHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Clase de instancia unica para la gestión de los dispostivos
 *
 * @author Rubén Carretero <rubencc@gmail.com>
 */
public class PeerDevices {

    //Colección para almacenar la dirección de los dispositivos y el numero de veces que no han respondido
    private HashMap<String, Integer> devices;
    //Colección para almacenar las listas multicast
    private HashMap<String, ArrayList<String>> multicastList;
    //Instancia de la clase
    private static PeerDevices INSTANCE = new PeerDevices();
    private final String CLASSNAME = getClass().getName();
    private LogHelper logger;

    private PeerDevices() {
        this.devices = new HashMap<String, Integer>();
        this.multicastList = new HashMap<String, ArrayList<String>>();
        this.logger = LogHelper.getInstance();
    }

    public static PeerDevices getInstance() {
        return INSTANCE;
    }

    /**
     * Añade una nueva dirección de red a la colección
     *
     * @param address -- Dirección del dispostivio
     */
    public synchronized void addDevice(String address) {
        synchronized (this.devices) {
            this.devices.put(address, 0);
        }
        this.logger.logINFO(CLASSNAME, "addDevice", "New spot " + address);
    }

    /**
     * Elimina una direccion de red de la colección
     *
     * @param address -- Dirección del dispostivio
     */
    public synchronized void removeDevice(String address) {
        synchronized (this.devices) {
            this.devices.remove(address);
        }
        this.logger.logINFO(CLASSNAME, "removeDevice", "Delete spot " + address);
    }

    /**
     * Decrementa el contador para una direccion de red
     *
     * @param address
     */
    public synchronized void checkDevice(String address) {
        synchronized (this.devices) {
            int _count = this.devices.get(address);
            if (_count > 0) {
                _count--;
                this.devices.put(address, _count);
            }
        }
    }

    /**
     * Si un elemento del mapa tiene un valor superior en su contador a 5 es
     * eliminado.
     */
    public synchronized void checkAllDevices() {
        ArrayList<String> _addressList = new ArrayList<String>();
        synchronized (this.devices) {
            for (Map.Entry<String, Integer> item : this.devices.entrySet()) {
                int _count = item.getValue();

                switch (_count) {
                    case 5:
                        _addressList.add(item.getKey());
                        break;
                    case 0:
                        break;
                    default:
                        break;
                }
                _count++;
                item.setValue(_count);
            }
            if (_addressList.size() > 0) {
                for (int i = 0; i < _addressList.size(); i++) {
                    this.devices.remove(_addressList.get(i));
                }
            }
        }
    }

    /**
     * Comprueba si una direccion de dispositivo existe en la colección
     *
     * @param address -- Dirección del dispostivio
     * @return
     */
    public synchronized boolean existDevices(String address) {
        synchronized (this.devices) {
            return this.devices.containsKey(address);
        }
    }

    /**
     * Comprueba si el mapa de dispositivos esta vacio
     *
     * @return
     */
    public synchronized boolean isEmpty() {
        synchronized (this.devices) {
            return this.devices.isEmpty();
        }
    }

    /**
     * Devuelve el numero de dispositivos conectados NOTA: No anidar con otras
     * llamadas synchronized ya que se bloqueara si el mapa esta vacio
     *
     * @return
     */
    public synchronized int numberOfPeers() {
        int size = 0;
        synchronized (this.devices) {
            if (!this.devices.isEmpty()) {
                size = this.devices.size();
            }
        }
        return size;
    }

    /**
     * Añade la direccion de un host a una lista de multicast
     *
     * @param multicasAddress -- Lista de multicast
     * @param deviceAddress -- Dirección del dispostivio
     */
    public synchronized void addToMulticast(String multicasAddress, String deviceAddress) {
        if (this.multicastList.containsKey(multicasAddress)) {
            ArrayList<String> _temp = this.multicastList.get(multicasAddress);
            if (!_temp.contains(deviceAddress) && !this.multicastList.containsKey(deviceAddress)) {
                _temp.add(deviceAddress);
            } else {
                this.logger.logINFO(CLASSNAME, "addToMulticast", "Address " + deviceAddress + " already exists in multicast " + multicasAddress);
            }
        } else {
            //System.out.println("Añadiendo: " + deviceAddress + " a la lista multicast: " + multicasAddress);
            ArrayList<String> _temp = new ArrayList<String>();
            _temp.add(deviceAddress);
            this.multicastList.put(multicasAddress, _temp);
        }

    }

    /**
     * Elimina una direccion de dispostivo de una lista multicast
     *
     * @param multicasAddress -- Lista de multicast
     * @param deviceAddress -- Dirección del dispostivio
     */
    public synchronized void deleteFromMulticast(String multicasAddress, String deviceAddress) {
        if (this.multicastList.containsKey(multicasAddress)) {
            ArrayList<String> _temp = this.multicastList.get(multicasAddress);
            _temp.remove(deviceAddress);
        } else {
            this.logger.logINFO(CLASSNAME, "deleteFromMulticast", "Address " + deviceAddress + " is not into multicast " + multicasAddress);
        }
    }

    /**
     * Comprueba si una direccion es multicast
     *
     * @param deviceAddress -- Dirección del dispostivio
     * @return
     */
    public synchronized ArrayList<String> isMulticastAddress(String deviceAddress) {
        if (this.multicastList.containsKey(deviceAddress)) {
            ArrayList<String> _temp = this.multicastList.get(deviceAddress);
            return _temp;
        } else {
            return null;
        }
    }

    /**
     * Elimina una lista de multicast
     *
     * @param multicasAddress -- Lista multicast
     */
    public synchronized void deleteMulticastList(String multicasAddress) {
        if (this.multicastList.containsKey(multicasAddress)) {
            this.multicastList.remove(multicasAddress);
        }
    }

    /**
     * Devuelve la configuración de direcciones multicast
     *
     * @return
     */
    public synchronized String getMulticastList() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ArrayList<String>> item : this.multicastList.entrySet()) {
            sb.append("\n\n");
            sb.append("\tLista: \t").append(item.getKey());
            Iterator<String> it = item.getValue().iterator();
            while (it.hasNext()) {
                sb.append("\n");
                sb.append("\t\t").append(it.next());
            }
            sb.append("\n");
        }
        //System.out.println(sb.toString());
        return sb.toString();
    }

    /**
     * Devuelve la configuración de una lista multicast
     *
     * @param address
     * @return
     */
    public synchronized String getMulticastList(String address) throws PeerDevicesException {
        HashMap<String, ArrayList<String>> _returnList = new HashMap<String, ArrayList<String>>();
        synchronized (this.multicastList) {
            _returnList.putAll(this.multicastList);
        }
        StringBuilder sb = new StringBuilder();

        if (_returnList.containsKey(address)) {
            sb.append("\n\n");
            sb.append("\tLista: \t").append(address);
            Iterator<String> it = _returnList.get(address).iterator();
            while (it.hasNext()) {
                sb.append("\n");
                sb.append("\t\t").append(it.next());
            }
            sb.append("\n");
        } else {
            throw new PeerDevicesException("La dirección " + address + " no es una dirección multicast valida");
        }
        return sb.toString();
    }
}
