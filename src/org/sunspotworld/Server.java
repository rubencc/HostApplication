/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld;

/**
 *
 * @author rubencc
 */
import org.host.application.Entities.*;
import org.host.application.Network.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Runnable {

    private ProcessSocket ps;
    private PeerConnection pCon;
    private BroadcastConnection bCon;
    private ArrayList<Command> cmdListToClient;
    private PeerDevices peerDevices;
    private final int ADD_ADDRESS_MULTICAST = 0x70;
    private final int DELETE_ADDRES_MULTICAST = 0x71;
    private final int DELETE_MULTICAST = 0x72;

    /**
     * Constructor para el hilo que atendera las peticiones de un cliente.
     *
     * @param socket - Socket de atencion a la nueva peticion.
     *
     */
    public Server(Socket socket, BroadcastConnection bCon, PeerConnection pCon) {
        this.ps = new ProcessSocket(socket);
        this.bCon = bCon;
        this.pCon = pCon;
        this.cmdListToClient = new ArrayList<Command>();
        this.peerDevices = PeerDevices.getInstance();

    }

    /**
     * Operaciones que realiza el hilo: 1º Recibe una peticion a traves del
     * socket. 2º Procesa la petición. 3º Envia el resultado al cliente. 4º
     * Cierra el socket y termina su ejecucion cuando el cliente se desconecta.
     *
     */
    @Override
    public void run() {
        String _temp;
        boolean _runCond = true;
        while (!this.ps.isSocketClosed() && _runCond) {
            //Recibir dato desde el socket del cliente
            _temp = this.ps.recive();
            //System.out.println("[Recibido socket] " + _temp);
            if (!peerDevices.isEmpty()) {
                if (!this.ps.isSocketClosed() && _temp != null) {
                    System.out.println("[Recibido por socket] " + _temp);
                    ParserFromJson _parserFromJson = new ParserFromJson();
                    Message _message = _parserFromJson.parse(_temp);
                    ArrayList<Command> _cmdlistFromClient = _message.getCommands();
                    //Procesa cada uno de los comandos contenidos en el mensaje
                    processToSendToSpots(_cmdlistFromClient);
                    //Se recogen las respuestas
                    processToSendToClient(_cmdlistFromClient);
                    //Se envian respuestas al cliente
                    sendToClient(this.cmdListToClient);
                }
            } else {
                System.out.println("Enviando error al cliente");
                sendErrorToClient("No devices", 0);
                _runCond = false;
            }
        }

        System.out.println("Hilo terminado");
    }

    /**
     * Envia por la conexion correspondiente los mensajes individuales y de
     * broadcast.
     *
     * @param _message
     */
    private boolean sendToSpot(Command command) {
        boolean result = false;
        if (command.isBroadcast()) {
            //Envia en modo broadcast         
            try {
                this.bCon.SendBroadcast(command);
                //Se añade al mapa el GUID de esta peticion para esperar sus resultados
                this.pCon.addGUIDforBroadcastReply(command.getGUID());
                result = true;
            } catch (BroadcastConnectionException ex) {
                //Si no se puede enviar mediante la conexion broadcast
                //se crea un mensaje de error y se añade a la lista de respuestas
                this.cmdListToClient.add(new Command(command.getAddress(), command.getType(), ex.getMessage(), System.currentTimeMillis(), command.getGUID(), true));
                result = false;
            }
        } else {
            try {
                //Envia en modo peer
                this.pCon.Send(command);
                result = true;
            } catch (PeerConnectionException ex) {
                //Si el dispositivo no esta conectado se ha generado la excepcion
                //y se envia una entidad comando en respuesta indicando el error.
                this.cmdListToClient.add(new Command(command.getAddress(), command.getType(), ex.getMessage(), System.currentTimeMillis(), command.getGUID(), false));
                result = false;
            }
        }
        return result;

    }

    /**
     * Realiza el envio del mensaje ya formado al cliente mediante el socket
     * tcp.
     *
     * @param CommandList
     */
    private void sendToClient(ArrayList<Command> CommandList) {
        String _temp;
        //Parseo a JSON de las respuestas
        ParserToJson _parserToJson = new ParserToJson();
        _temp = _parserToJson.parse(CommandList);
        //Envia respues al cliente a traves del socket
        //System.out.println("Enviando a cliente:" + _temp);
        this.ps.send(_temp);
    }

    /**
     * Envia una mensaje de error al cliente
     *
     * @param error
     * @param type
     */
    private void sendErrorToClient(String error, int type) {
        System.out.println(error);
        Command _cmd = new Command("ERROR", type, error, System.currentTimeMillis());
        this.cmdListToClient.add(_cmd);
        sendToClient(this.cmdListToClient);
    }

    private void getBroadcastReplies(Command _command) {
        //System.out.println("Esperando broadcast\n" + _command);
        ArrayList<Command> _commandListFromSpot = this.pCon.getBroadcastCommandList(_command.getGUID());
        synchronized (_commandListFromSpot) {
            //Mientras que la lista este vacia o el numero de respuestas sea menor que el de dispositivos.
            while (_commandListFromSpot.isEmpty() || (_commandListFromSpot.size() < pCon.numberOfPeers() && !_commandListFromSpot.isEmpty())) {
                try {
                    _commandListFromSpot.wait();
                } catch (InterruptedException ex) {
                    Logger.getLogger(Server.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            }
            //Copiamos toda la lista de elementos para esa peticion
            this.cmdListToClient.addAll(_commandListFromSpot);
            //Eliminamos la lista y la entrada en el hashmap.
            this.pCon.deleteBroadcastCommandList(_command.getGUID());
        }

    }

    /**
     * Recupera las respuestas correspondientes al campo GUID de este comando
     *
     * @param _command
     */
    private void getNoBroadcastReplies(Command _command) {
        //System.out.println("Esperando no broadcast");
        Command _tempCommand = null;
        //Espera hasta que la respuesta esta disponible ya que el hasmap devuelve
        //null mientras que la clave no este añadida a su mapa.
        //Podria producirse una espera infinita en caso de que el dispositivo nunca llegase a enviar respuesta.
        do {
            _tempCommand = this.pCon.getIndividualCommand(_command.getGUID());
        } while (_tempCommand == null);
        this.cmdListToClient.add(_tempCommand);
        this.pCon.deleteIndividualCommamd(_command.getGUID());
    }

    /**
     * Procesa los mensajes para enviarselos a los spots
     *
     * @param _cmdlistFromClient
     */
    private void processToSendToSpots(ArrayList<Command> _cmdlistFromClient) {
        Iterator<Command> it = _cmdlistFromClient.iterator();
        ArrayList<Command> _tempList = new ArrayList<Command>();
        //Buscamos is hay comandos en multicast
        while (it.hasNext()) {
            Command _command = it.next();
            if (_command.getType() == ADD_ADDRESS_MULTICAST || _command.getType() == DELETE_ADDRES_MULTICAST || _command.getType() == DELETE_MULTICAST) {
                manageMulticastConfiguration(_command);
                it.remove();
            } else {
                ArrayList<String> _listAddress = this.peerDevices.isMulticastAddress(_command.getAddress());
                //Si la lista de direcciones multicas no es nula se crean nuevos comandos
                if (_listAddress != null) {
                    _tempList = createNewMessagesFromMulticast(_listAddress, _tempList, _command);
                    it.remove();
                }
            }
        }

        if (_tempList.size() > 0) {
            _cmdlistFromClient.addAll(_tempList);
        }

        it = _cmdlistFromClient.iterator();
        while (it.hasNext()) {
            Command _command = it.next();
            //Se envia el comando
            //System.out.println(_command.toString());
            if (!sendToSpot(_command)) {
                it.remove();
            }


        }
        //System.out.println(_cmdlistFromClient.toString());
    }

    /**
     * Procesa las respuestas para enviarselas al cliente
     *
     * @param _cmdlistFromClient
     */
    private void processToSendToClient(ArrayList<Command> _cmdlistFromClient) {
        //System.out.println(_cmdlistFromClient.toString());
        Iterator<Command> it = _cmdlistFromClient.iterator();
        while (it.hasNext()) {
            Command _command = it.next();
            //Busqueda en lista de mensajes broadcasr
            if (_command.isBroadcast()) {
                getBroadcastReplies(_command);
            } else {
                getNoBroadcastReplies(_command);
            }
        }
    }

    private void manageMulticastConfiguration(Command _command) {
        //Añadimos la nueva direccion multicast al grupo multicas correspondiente
        System.out.println("Mensaje de gestion de multicast");
        switch (_command.getType()) {
            case ADD_ADDRESS_MULTICAST:
                this.peerDevices.addToMulticast(_command.getAddress(), _command.getValue());
                this.cmdListToClient.add(new Command(_command.getAddress(), _command.getType(), "Añadido " + _command.getValue() + " a multicast " + _command.getAddress(), System.currentTimeMillis(), _command.getGUID(), false));
                break;
            case DELETE_ADDRES_MULTICAST:
                this.peerDevices.deleteFromMulticast(_command.getAddress(), _command.getValue());
                this.cmdListToClient.add(new Command(_command.getAddress(), _command.getType(), "Eliminado " + _command.getValue() + " de multicast " + _command.getAddress(), System.currentTimeMillis(), _command.getGUID(), false));
                break;
            case DELETE_MULTICAST:
                this.peerDevices.deleteMulticastList(_command.getAddress());
                this.cmdListToClient.add(new Command(_command.getAddress(), _command.getType(), "Eliminada lista multicast " + _command.getValue(), System.currentTimeMillis(), _command.getGUID(), false));
                break;

        }

    }

    private ArrayList<Command> createNewMessagesFromMulticast(ArrayList<String> _listAddress, ArrayList<Command> _tempList, Command _command) {
        Iterator<String> itAddress = _listAddress.iterator();
        _tempList = new ArrayList<Command>();
        while (itAddress.hasNext()) {
            //Para cada direccion de red de la lista multicast
            //creamos un nuevo comando con nuevo GUID
            Command _tempCommand = new Command(itAddress.next(), _command.getType(), _command.getValue(), System.currentTimeMillis(), UUID.randomUUID().toString(), _command.isBroadcast());
            //Añadimos el comando temporal a la lista de los recibidos como si
            //hubiera sido realmente recibido
            _tempList.add(_tempCommand);
        }
        return _tempList;
    }
}
