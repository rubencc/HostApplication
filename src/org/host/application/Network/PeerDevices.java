/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.host.application.Network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author rubencc
 */
public class PeerDevices {

    private HashMap<String, Integer> devices;
    private HashMap<String, ArrayList<String>> multicastList;
    //Patron singleton para garantizar que solo hay un objeto de este tipo.
    private static PeerDevices INSTANCE = new PeerDevices();

    private PeerDevices() {
        this.devices = new HashMap<String, Integer>();
        this.multicastList = new HashMap<String, ArrayList<String>>();
    }

    public static PeerDevices getInstance() {
        return INSTANCE;
    }

    /**
     * Añade una nueva direcion de red al mapa
     *
     * @param address
     */
    public synchronized void add(String address) {
        synchronized (this.devices) {
            this.devices.put(address, 0);
            System.out.println("Nuevo dispositivo: " + address);
        }
    }

    /**
     * Elimina una direccion de red del mapa
     *
     * @param address
     */
    public synchronized void remove(String address) {
        synchronized (this.devices) {
            this.devices.remove(address);
        }
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
     * Si un elemento del mapa tiene un valor superior en su contador a 2 es
     * eliminado.
     */
    public synchronized void checkAllDevices() {
        ArrayList<String> _addressList = new ArrayList<String>();
        synchronized (this.devices) {
            for (Map.Entry<String, Integer> item : this.devices.entrySet()) {
                int _count = item.getValue();

                switch (_count) {
                    case 5:
                        System.out.println("[" + item.getKey() + "] Eliminando dispositivo");
                        _addressList.add(item.getKey());
                        break;
                    case 0:
                        //System.out.println("[" + item.getKey() + "] Esta conectado");
                        break;
                    default:
                        //System.out.println("[" + item.getKey() + "] No ha respondido a " + _count + " ping request");
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
     * Comprueba si una direccion de dispositivo existe en el mapa.
     *
     * @param address
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
     * @param multicasAddress
     * @param hostAddress
     */
    public synchronized void addToMulticast(String multicasAddress, String hostAddress) {
        if (this.multicastList.containsKey(multicasAddress)) {
            ArrayList<String> _temp = this.multicastList.get(multicasAddress);
            if (!_temp.contains(hostAddress) && !this.multicastList.containsKey(hostAddress)) {
                _temp.add(hostAddress);
                //System.out.println("Creando nueva entrada en multicast " + multicasAddress + " para " + hostAddress);
            } else {
                System.out.println("Entrada ya contenida multicast ");
            }
        } else {
            //System.out.println("Añadiendo: " + hostAddress + " a la lista multicast: " + multicasAddress);
            ArrayList<String> _temp = new ArrayList<String>();
            _temp.add(hostAddress);
            this.multicastList.put(multicasAddress, _temp);
        }

    }

    /**
     * Elimina una direccion de host de una lista multicast
     *
     * @param multicasAddress
     * @param hostAddress
     */
    public synchronized void deleteFromMulticast(String multicasAddress, String hostAddress) {
        if (this.multicastList.containsKey(multicasAddress)) {
            //System.out.println("Eliminando entrada multicast para: " + multicasAddress + " -- " + hostAddress);
            ArrayList<String> _temp = this.multicastList.get(multicasAddress);
            _temp.remove(hostAddress);
        } else {
            //System.out.println("La direccion: " + hostAddress + " no pertenece al multicast " + multicasAddress);
        }
    }

    /**
     * Comprueba si una direccion es multicast
     *
     * @param hostAddress
     * @return
     */
    public synchronized ArrayList<String> isMulticastAddress(String hostAddress) {
        if (this.multicastList.containsKey(hostAddress)) {
            //System.out.println(hostAddress + " es multicast");
            ArrayList<String> _temp = this.multicastList.get(hostAddress);
            return _temp;
        } else {
            //System.out.println(hostAddress + " no es multicast");
            return null;
        }
    }

    /**
     * Elimina una lista de multicast
     *
     * @param multicasAddress
     */
    public synchronized void deleteMulticastList(String multicasAddress) {
        if (this.multicastList.containsKey(multicasAddress)) {
            System.out.println("Lista multicast " + multicasAddress + " eliminada");
            this.multicastList.remove(multicasAddress);
        }
    }

    /**
     * Devuelve una copia de la lista que contiene las direcciones multicast.
     *
     * @return
     */
    public synchronized HashMap<String, ArrayList<String>> getMulticastList() {
        HashMap<String, ArrayList<String>> _returnList = new HashMap<String, ArrayList<String>>();
        synchronized (this.multicastList) {
            _returnList.putAll(this.multicastList);
        }
        return _returnList;
    }
}
