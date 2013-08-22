package org.host.application.Network;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import org.host.application.Entities.Command;

/**
 * Clase de instancia unica que gestion la conexión peer. Mantiene una lista de
 * dispositivos activos en la clase PeerDevices.
 *
 * @author rubencc
 */
public class PeerConnection implements Runnable {

    private final int PING_PACKET_REPLY = 0x33;
    private final int QUEUE_ALERT = 0x20;
    //Puerto para la conexion peer
    private final int HOST_PORT = 100;
    //Conexión peer
    private RadiogramConnection rCon;
    //Condición para terminar
    private boolean finish;
    //Datagrama de envío
    private Datagram sendDg = null;
    //Datagrama de recepción
    private Datagram receiveDg = null;
    //Colección para almacenar respuestas individuales
    private HashMap<String, Command> individualCommandList;
    //Colección para almacenar respuestas a mensajes de tipo broadcast
    private HashMap<String, ArrayList<Command>> broadcastCommandList;
    //Colección para almacenar alertas
    private ArrayList<Command> alertQueue;
    //Instancia del gestor de dispostivios
    private PeerDevices peerDevices;

    public PeerConnection() {
        this.broadcastCommandList = new HashMap<String, ArrayList<Command>>();
        this.individualCommandList = new HashMap<String, Command>();
        this.alertQueue = new ArrayList<Command>();
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
                    peerDevices.addDevice(_command.getAddress());
                } else {
                    peerDevices.checkDevice(_command.getAddress());
                }

            } else if (_command.getType() == QUEUE_ALERT) {
                //La PDU se almacena en la cola de alertas
                _command.setGUID(UUID.randomUUID().toString());
                this.alertQueue.add(_command);
            } else {
                if (_command.isBroadcast()) {
                    //Se trata de una mensaje de respuesta a un mensaje enviado por la conexión broadcast
                    addBroadcastResponse(_command);
                } else {
                    //Se trata de una mensaje de respuesta a un mensaje enviado por la conexión peer
                    synchronized (this.individualCommandList) {
                        this.individualCommandList.put(_command.getGUID(), _command);
                        this.individualCommandList.notify();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage() + " mientras se leia la conexion peer");
        }
    }

    /**
     * Lee una PDU de la conexion peer para procesarla.
     *
     * @param receiveDg
     * @return
     */
    private Command readDatagram(Datagram receiveDg) {
        Command _command = null;
        try {
            //El formato de la PDU es {direccion, tipo, valor, tiempo, guid, broadcast}
            String _address = receiveDg.readUTF();
            int _type = receiveDg.readInt();
            int _count = receiveDg.readInt();
            ArrayList<String> _values = new ArrayList<String>();
            for (int i = 0; i < _count; i++) {
                _values.add(receiveDg.readUTF());
            }
            long _time = receiveDg.readLong();
            String _GUID = receiveDg.readUTF();
            boolean _individual = receiveDg.readBoolean();
            //System.out.println(PeerConnection.class.getName() + "[readDatagram]" + " " + _address + " " + _type + " " + _value + " " + _time + " " + _GUID + " " + _individual);
            _command = new Command(_address, _type, _values, _time, _GUID, _individual);

        } catch (IOException ex) {
            Logger.getLogger(PeerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return _command;
    }

    /**
     * Añade una respuesta a la collección correspondiente
     *
     * @param _command
     */
    private void addBroadcastResponse(Command _command) {
        //System.out.println("Hilo peer -- Recibiendo respuestas broadcast" + _command);
        synchronized (this.broadcastCommandList) {
            if (this.broadcastCommandList.containsKey(_command.getGUID())) {
                /*Si ya existen respuestas de otros dispostivios a este mensaje broadcast se almacenan en la colección 
                 correspondiente, si no se crea y se almacena*/
                //System.out.println("Elemento añadido a la lista " + _command.getGUID() + " " + _command.getAddress());
                ArrayList<Command> _tempList = this.broadcastCommandList.get(_command.getGUID());
                synchronized (_tempList) {
                    _tempList.add(_command);
                    _tempList.notify();
                }
            } else {
                //System.out.println("Elemento añadido a la nueva lista " + _command.getGUID() + " " + _command.getAddress());
                ArrayList<Command> _tempList = new ArrayList<Command>();
                _tempList.add(_command);
                this.broadcastCommandList.put(_command.getGUID(), _tempList);
            }

            this.broadcastCommandList.notify();
        }
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
            if ((command.getAddress() != null) && (command.getValue() != null) && (command.getGUID() != null)) {
                writeDatagram(command);
            } else {
                _sendCond = false;
            }
        } catch (IOException e) {
            throw new PeerConnectionException("Error al formar la PDU --" + e.getMessage());
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
        } catch (IOException ex) {
            Logger.getLogger(PeerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        return _temp;
    }

    /**
     * Escribe los campos de un comando en el datagram
     *
     * @param command -- Comando a enviar
     * @throws IOException
     */
    private void writeDatagram(Command command) throws IOException {
        this.sendDg.setAddress(command.getAddress());
        this.sendDg.writeInt(command.getType());
        this.sendDg.writeInt(command.countValues());
        Iterator it = command.getValue().iterator();
        while (it.hasNext()) {
            this.sendDg.writeUTF((String) it.next());
        }
        this.sendDg.writeUTF(command.getGUID());
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
     * @return Comando
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
     * @return Lista de comandos
     */
    public synchronized ArrayList<Command> getBroadcastCommandList(String GUID) {
        synchronized (this.broadcastCommandList) {
            return this.broadcastCommandList.get(GUID);
        }
    }

    /**
     * Elimina una lista de comandos de la colección en funcion de su GUID
     *
     * @param GUID
     */
    public synchronized void deleteBroadcastCommandList(String GUID) {
        synchronized (this.broadcastCommandList) {
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
        synchronized (this.broadcastCommandList) {
            //System.out.println("Nueva lista en addguid");
            this.broadcastCommandList.put(GUID, new ArrayList<Command>());
            this.broadcastCommandList.notifyAll();
        }
    }

    /**
     * Obtiene todas las alertas contenidas en la cola de alertas
     *
     * @return Lista de alertas
     */
    public synchronized ArrayList<Command> getAlertQueue() {
        ArrayList<Command> _temp = new ArrayList<Command>();
        synchronized (this.alertQueue) {
            _temp.addAll(this.alertQueue);
            this.alertQueue.clear();
        }
        return _temp;
    }

    /**
     * Obtiene la lista de alertas de una dirección en concreto
     *
     * @param address -- Dirección a consultar
     * @return Lista de alertas
     */
    public synchronized ArrayList<Command> getAlertQueue(String address) {
        ArrayList<Command> _temp = new ArrayList<Command>();
        synchronized (this.alertQueue) {
            Iterator<Command> it = this.alertQueue.iterator();
            while (it.hasNext()) {
                Command _command = it.next();
                if (_command.getAddress().equals(address)) {
                    _temp.add(_command);
                } else {
                    it.remove();
                }
            }
        }
        return _temp;
    }
}
