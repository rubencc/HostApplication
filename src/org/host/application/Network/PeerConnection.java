/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.host.application.Network;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import org.host.application.Entities.Command;

/**
 * Envia o recibe mediante la conexion de tipo peer. Mantiene una lista de
 * dispositivos activos en la clase PeerDevices.
 *
 * @author rubencc
 */
public class PeerConnection implements Runnable {

    private final int PING_PACKET_REPLY = 0x33;
    private final int HOST_PORT = 100;
    private RadiogramConnection rCon;
    private boolean finish;
    private Datagram sendDg = null;
    private Datagram receiveDg = null;
    //private ArrayList<Command> broadcastCommandList;
    private HashMap<String, Command> individualCommandList;
    private HashMap<String, ArrayList<Command>> broadcastCommandList;
    private PeerDevices peerDevices;

    public PeerConnection() {
        broadcastCommandList = new HashMap<String, ArrayList<Command>>();
        individualCommandList = new HashMap<String, Command>();
        this.peerDevices = PeerDevices.getInstance();
    }

    @Override
    public void run() {
        finish = true;
        try {
            //Abre conexion en modo servidor a la espera de dispositivos
            rCon = (RadiogramConnection) Connector.open("radiogram://:" + HOST_PORT);
            this.setRadiogramConnection();
        } catch (IOException ex) {
            Logger.getLogger(PeerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Escucha respuestas de los dispositivos a los mensajes de broadcast
        while (finish) {
            try {
                this.receive();
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(PeerConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Activa la condicion para terminar el hilo de ejecucion
     */
    public void FinishThread() {
        this.finish = false;
    }

    /**
     * Espera una nueva conexion y guarda sus parametros.
     */
    public void setRadiogramConnection() {

        try {
            if (receiveDg == null) {
                receiveDg = rCon.newDatagram(rCon.getMaximumLength());
            }
            if (sendDg == null) {
                sendDg = rCon.newDatagram(rCon.getMaximumLength());
            }

        } catch (IOException ex) {
            Logger.getLogger(PeerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Cierra las conexiones.
     */
    public void CloseRadiogramConnection() {
        try {
            rCon.close();
            System.out.println("Cerrando conexiones de radio");
        } catch (IOException ex) {
            Logger.getLogger(PeerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Recibe un datagrama desde la direccion indicada y la procesa segun
     * corresponda. En caso de ser un mensaje en respuesta a una peticicion de
     * broadcast se almacenara en la lista de mensajes de broadcast. En caso de
     * ser un mensaje en respuesta a una peticion individual se almacenara en la
     * lista de mensajes individuales.
     *
     * @return
     */
    public synchronized void receive() {
        try {
            this.rCon.receive(receiveDg);
            //Lectura del datagrama
            Command _command = this.readDatagram(this.receiveDg);

            if (_command.getType() == PING_PACKET_REPLY) {
                //Si el dispositivo no existe se añade a la lista
                //En caso de existir se decrementa su contador
                if (!peerDevices.existDevices(_command.getAddress())) {
                    peerDevices.add(_command.getAddress());
                } else {
                    peerDevices.checkDevice(_command.getAddress());
                }

            } else {
                if (_command.isBroadcast()) {
                    synchronized (this.broadcastCommandList) {
                        if (this.broadcastCommandList.containsKey(_command.getGUID())) {
                            ArrayList<Command> _tempList = this.broadcastCommandList.get(_command.getGUID());
                            synchronized (_tempList) {
                                _tempList.add(_command);
                                _tempList.notify();
                            }
                        } else {
                            ArrayList<Command> _tempList = new ArrayList<Command>();
                            synchronized (_tempList) {
                                _tempList.add(_command);
                                this.broadcastCommandList.put(_command.getGUID(), _tempList);
                                _tempList.notify();
                            }
                        }
                        this.broadcastCommandList.notify();
                    }
                } else {
                    synchronized (this.individualCommandList) {
                        this.individualCommandList.put(_command.getGUID(), _command);
                        this.individualCommandList.notify();
                    }
                }
            }


        } catch (com.sun.spot.peripheral.TimeoutException te) {
            System.err.println("Ningun dispositivo conectado");
        } catch (Exception e) {
            System.err.println(e + " mientras se leia la conexion peer");
        }
    }

    /**
     * Lee una PDU de la conexion peer para procesarla.
     *
     * @param receiveDg
     * @return
     */
    private Command readDatagram(Datagram receiveDg) {
        Command _commandList = null;
        try {
            /*El formato de la PDU es {direccion, tipo, valor, tiempo, guid, broadcast}*/
            String _address = receiveDg.readUTF();
            int _type = receiveDg.readInt();
            String _value = receiveDg.readUTF();
            long _time = receiveDg.readLong();
            String _GUID = receiveDg.readUTF();
            boolean _individual = receiveDg.readBoolean();
            //System.out.println(PeerConnection.class.getName() + "[readDatagram]" + " " + _address + " " + _type + " " + _value + " " + _time + " " + _GUID + " " + _individual);
            _commandList = new Command(_address, _type, _value, _time, _GUID, _individual);

        } catch (IOException ex) {
            Logger.getLogger(PeerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return _commandList;
    }

    /**
     * Envia un datagrama a la direccion indicada
     *
     * @param message
     * @return
     */
    public synchronized boolean Send(Command command) throws PeerConnectionException {
        boolean _sendCond = true;
        boolean _temp = false;
        try {
            this.sendDg.reset();
            //Se establece la direccion del dispostivo de destino
            if (command.getAddress() != null) {
                this.sendDg.setAddress(command.getAddress());
            } else {
                _sendCond = false;
            }
            /*El formato de la PDU es {tipo, valor, guid}*/
            this.sendDg.writeInt(command.getType());
            if (command.getValue() != null) {
                this.sendDg.writeUTF(command.getValue());
            } else {
                _sendCond = false;
            }
            if (command.getGUID() != null) {
                this.sendDg.writeUTF(command.getGUID());
            } else {
                _sendCond = false;
            }
        } catch (Exception e) {
            System.err.println("setUp caught " + e.getMessage() + this.getClass());
        }

        try {
            if (peerDevices.existDevices(command.getAddress()) && _sendCond) {
                _temp = true;
                synchronized (this.individualCommandList) {
                    this.individualCommandList.put(command.getGUID(), null);
                }
                this.rCon.send(sendDg);
            } else {
                if (_sendCond) {
                    throw new PeerConnectionException("El dispositivo no esta conectado");
                } else {
                    throw new PeerConnectionException("Los campos de la PDU no son correctos");
                }

            }
        } catch (com.sun.spot.peripheral.TimeoutException te) {
            System.err.println("Ningun dispositivo conectado");
        } catch (IOException ex) {
            Logger.getLogger(PeerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return _temp;
    }

    /**
     * Devuelve el numero de dispostivos NOTA: No utilizar para comprobrar si
     * hay dispositivos conectados
     *
     * @return
     */
    public synchronized int numberOfPeers() {
        return this.peerDevices.numberOfPeers();
    }

    /**
     * Devuelve el comando asociado a ese GUID o null si no esta en la lista o
     * aun no se ha recibido su respuesta.
     *
     * @param GUID
     * @return
     */
    public synchronized Command getIndividualCommand(String GUID) {
        synchronized (this.individualCommandList) {
            return this.individualCommandList.get(GUID);
        }
    }

    /**
     * Elimina un comando del hashmap en funcion de su GUID
     *
     * @param GUID
     */
    public synchronized void deleteIndividualCommamd(String GUID) {
        synchronized (this.individualCommandList) {
            this.individualCommandList.remove(GUID);
            this.individualCommandList.notify();
        }
    }

    /**
     * Devuelve la lista de comandos asociados a ese GUID
     *
     * @param GUID
     * @return
     */
    public synchronized ArrayList<Command> getBroadcastCommandList(String GUID) {
        synchronized (broadcastCommandList) {
            return this.broadcastCommandList.get(GUID);
        }
    }

    /**
     * Elimina una lista de comandos del hashmap en funcion de su GUID
     *
     * @param GUID
     */
    public synchronized void deleteBroadcastCommandList(String GUID) {
        synchronized (broadcastCommandList) {
            this.broadcastCommandList.remove(GUID);
            this.broadcastCommandList.notify();
        }
    }

    /**
     * Añade un GUID al mapa de broadcast para esperar su respuesta
     *
     * @param GUID
     */
    public synchronized void addGUIDforBroadcastReply(String GUID) {
        synchronized (broadcastCommandList) {
            this.broadcastCommandList.put(GUID, new ArrayList<Command>());
            this.broadcastCommandList.notify();
        }
    }
}
