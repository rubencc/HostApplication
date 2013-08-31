package org.sunspotworld;

/**
 * Clase que atiende las peticiones de un cliente
 *
 * @author Rubén Carretero <rubencc@gmail.com>
 */
import Helpers.LogHelper;
import org.host.application.Entities.*;
import org.host.application.Network.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class Server implements Runnable {

    private ProcessSocket ps;
    private PeerConnection pCon;
    private BroadcastConnection bCon;
    private ArrayList<Command> cmdListToClient;
    private PeerDevices peerDevices;
    private final int QUEUE_ALERT = 0x20;
    private final int ADD_ADDRESS_MULTICAST = 0x70;
    private final int DELETE_ADDRES_MULTICAST = 0x71;
    private final int DELETE_MULTICAST = 0x72;
    private final int READ_CONFIGURATION_MULTICAST = 0x73;
    private final String CLASSNAME = getClass().getName();
    private LogHelper logger;

    public Server(Socket socket, BroadcastConnection bCon, PeerConnection pCon) {
        this.ps = new ProcessSocket(socket);
        this.bCon = bCon;
        this.pCon = pCon;
        this.cmdListToClient = new ArrayList<Command>();
        this.peerDevices = PeerDevices.getInstance();
        this.logger = LogHelper.getInstance();
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
            //Recibe dato desde el socket del cliente
            _temp = this.ps.recive();
            if (!peerDevices.isEmpty()) {
                if (!this.ps.isSocketClosed() && _temp != null) {
                    this.logger.logINFO(CLASSNAME, "run", "New message from client");
                    ParserFromJson _parserFromJson = new ParserFromJson();
                    //Obtiene el mensaje despues de parsearlo
                    Message _message = _parserFromJson.parse(_temp);
                    //this.logger.logINFO(CLASSNAME, "Message", _message.toString());
                    //Obtiene los comandos enviando en el mensaje
                    ArrayList<Command> _cmdlistFromClient = _message.getCommands();
                    //Procesa cada uno de los comandos contenidos en el mensaje
                    processReceiveCommands(_cmdlistFromClient);
                    //Se recogen las respuestas
                    processToSendToClient(_cmdlistFromClient);
                    //Se envian respuestas al cliente
                    sendToClient(this.cmdListToClient);
                    this.cmdListToClient.clear();
                }
            } else {
                sendErrorToClient("No devices", 0);
                _runCond = false;
            }
        }

        //System.out.println("Hilo terminado");
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
            try {
                this.bCon.SendBroadcast(command);
                result = true;
                this.logger.logINFO(CLASSNAME, "sendToSpot -- Broadcast", "Sendt to spot in broadcast");
            } catch (BroadcastConnectionException ex) {
                //Si no se puede enviar mediante la conexion broadcast
                //se crea un mensaje de error y se añade a la lista de respuestas
                ArrayList<String> _tempList = new ArrayList<String>();
                _tempList.add(ex.getMessage());
                this.cmdListToClient.add(new Command(command.getAddress(), command.getType(), _tempList, System.currentTimeMillis(), command.getGUID(), true));
                result = false;
                this.logger.logSEVERE(CLASSNAME, "sendToSpot -- BroadcastConnectionException", ex.getMessage());
            }
        } else {
            try {
                //Envia en modo peer
                this.pCon.Send(command);
                result = true;
                this.logger.logINFO(CLASSNAME, "sendToSpot -- Peer", "Sendt to spot in peer");
            } catch (PeerConnectionException ex) {
                //Si el dispositivo no esta conectado se ha generado la excepcion
                //y se envia una entidad comando en respuesta indicando el error.
                ArrayList<String> _tempList = new ArrayList<String>();
                _tempList.add(ex.getMessage());
                this.cmdListToClient.add(new Command(command.getAddress(), command.getType(), _tempList, System.currentTimeMillis(), command.getGUID(), false));
                result = false;
                this.logger.logSEVERE(CLASSNAME, "sendToSpot -- PeerConnectionException", ex.getMessage());
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
        this.logger.logINFO(CLASSNAME, "sendToClient", "Send replies to client");
    }

    /**
     * Envia una mensaje de error al cliente
     *
     * @param error -- Mensaje de error
     * @param type -- Tipo de error
     */
    private void sendErrorToClient(String error, int type) {
        //System.out.println(error);
        ArrayList<String> _tempList = new ArrayList<String>();
        _tempList.add(error);
        Command _cmd = new Command("ERROR", type, _tempList, System.currentTimeMillis());
        this.cmdListToClient.add(_cmd);
        sendToClient(this.cmdListToClient);
        this.logger.logINFO(CLASSNAME, "sendErrorToClient", "Send error to client");
    }

    /**
     * Recupera las respuestas a un mensaje broadcast
     *
     * @param command
     */
    private void getBroadcastReplies(Command command) {
        ArrayList<Command> _commandListFromSpot = null;
        do {
            _commandListFromSpot = this.pCon.getBroadcastCommandList(command.getGUID());
        } while (_commandListFromSpot == null);


        synchronized (_commandListFromSpot) {
            //Mientras que la lista este vacia o el numero de respuestas sea menor que el de dispositivos.
            while (_commandListFromSpot.isEmpty() || (_commandListFromSpot.size() < pCon.numberOfPeers() && !_commandListFromSpot.isEmpty())) {
                try {
                    _commandListFromSpot.wait();
                } catch (InterruptedException ex) {
                    this.logger.logSEVERE(CLASSNAME, "getBroadcastReplies -- InterruptedException", ex.getMessage());
                }
            }
            //Copiamos toda la lista de elementos para esa peticion
            this.cmdListToClient.addAll(_commandListFromSpot);
            //Eliminamos la lista y la entrada en el hashmap.
            this.pCon.deleteBroadcastCommandList(command.getGUID());
        }
        this.logger.logINFO(CLASSNAME, "getBroadcastReplies", "Getting broadcast replies");
    }

    /**
     * Recupera las respuestas correspondientes al campo GUID de este comando
     *
     * @param command
     */
    private void getNoBroadcastReplies(Command command) {
        //System.out.println("Esperando no broadcast");
        Command _tempCommand = null;
        //Espera hasta que la respuesta esta disponible ya que el hasmap devuelve
        //null mientras que la clave no este añadida a su mapa.
        //Podria producirse una espera infinita en caso de que el dispositivo nunca llegase a enviar respuesta.
        do {
            _tempCommand = this.pCon.getIndividualCommand(command.getGUID());
        } while (_tempCommand == null);
        this.cmdListToClient.add(_tempCommand);
        this.pCon.deleteIndividualCommamd(command.getGUID());
        this.logger.logINFO(CLASSNAME, "getNoBroadcastReplies", "Getting no broadcast replies");
    }

    /**
     * Procesa los mensajes para enviarselos a los spots
     *
     * @param cmdlistFromClient
     */
    private void processReceiveCommands(ArrayList<Command> cmdlistFromClient) {
        Iterator<Command> it = cmdlistFromClient.iterator();
        ArrayList<Command> _tempList = new ArrayList<Command>();
        //Buscamos si hay comandos en multicast
        while (it.hasNext()) {
            Command _command = it.next();
            if (_command.getType() == ADD_ADDRESS_MULTICAST || _command.getType() == DELETE_ADDRES_MULTICAST || _command.getType() == DELETE_MULTICAST || _command.getType() == READ_CONFIGURATION_MULTICAST) {
                manageMulticastConfiguration(_command);
                it.remove();
                this.logger.logINFO(CLASSNAME, "processReceiveCommands", "Multicast command");
            } else if (_command.getType() == QUEUE_ALERT) {
                if (_command.isBroadcast()) {
                    this.cmdListToClient.addAll(this.pCon.getAlertQueue());
                    this.logger.logINFO(CLASSNAME, "processReceiveCommands", "Alert Queue command");
                } else {
                    this.cmdListToClient.addAll(this.pCon.getAlertQueue(_command.getAddress()));
                    this.logger.logINFO(CLASSNAME, "processReceiveCommands", "Alert Queue command to " + _command.getAddress() + " spot");
                }
                it.remove();
            } else {
                ArrayList<String> _listAddress = this.peerDevices.isMulticastAddress(_command.getAddress());
                //Si la lista de direcciones multicas no es nula se crean nuevos comandos
                if (_listAddress != null) {
                    _tempList.addAll(createNewMessagesFromMulticast(_listAddress, _tempList, _command));
                    it.remove();
                }
            }
        }

        if (_tempList.size() > 0) {
            cmdlistFromClient.addAll(_tempList);
        }

        it = cmdlistFromClient.iterator();
        while (it.hasNext()) {
            Command _command = it.next();
            //Se envia el comando
            if (!sendToSpot(_command)) {
                it.remove();
            }


        }
    }

    /**
     * Procesa las respuestas para enviarselas al cliente
     *
     * @param cmdlistFromClient
     */
    private void processToSendToClient(ArrayList<Command> cmdlistFromClient) {
        Iterator<Command> it = cmdlistFromClient.iterator();
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

    /**
     * Gestiona los comandos de configuracion para multicast
     *
     * @param command Comando de gestion
     */
    private void manageMulticastConfiguration(Command command) {
        //Añadimos la nueva direccion multicast al grupo multicas correspondiente
        Command _command;
        switch (command.getType()) {
            case ADD_ADDRESS_MULTICAST:
                this.peerDevices.addToMulticast(command.getAddress(), command.getValue().get(0));
                _command = new Command(command.getAddress(), command.getType(), System.currentTimeMillis(), command.getGUID(), false);
                _command.addValue("Add " + command.getValue().get(0) + " to multicast " + command.getAddress());
                this.logger.logINFO(CLASSNAME, "manageMulticastConfiguration -- ADD_ADDRESS_MULTICAST", "Add " + command.getValue().get(0) + " to multicast " + command.getAddress());
                this.cmdListToClient.add(_command);
                break;
            case DELETE_ADDRES_MULTICAST:
                this.peerDevices.deleteFromMulticast(command.getAddress(), command.getValue().get(0));
                _command = new Command(command.getAddress(), command.getType(), System.currentTimeMillis(), command.getGUID(), false);
                _command.addValue("Delete " + command.getValue().get(0) + " from multicast " + command.getAddress());
                this.logger.logINFO(CLASSNAME, "manageMulticastConfiguration -- DELETE_ADDRES_MULTICAST", "Delete " + command.getValue().get(0) + " from multicast " + command.getAddress());
                this.cmdListToClient.add(_command);
                break;
            case DELETE_MULTICAST:
                this.peerDevices.deleteMulticastList(command.getAddress());
                _command = new Command(command.getAddress(), command.getType(), System.currentTimeMillis(), command.getGUID(), false);
                _command.addValue("Delete multicast list" + command.getValue().get(0));
                this.logger.logINFO(CLASSNAME, "manageMulticastConfiguration -- DELETE_MULTICAST", "Delete multicast list" + command.getValue().get(0));
                this.cmdListToClient.add(_command);
                break;
            case READ_CONFIGURATION_MULTICAST:
                _command = new Command(command.getAddress(), command.getType(), command.getTime(), command.getGUID(), false);
                if (!_command.getAddress().equals("ALL")) {
                    try {
                        _command.addValue(this.peerDevices.getMulticastList(_command.getAddress()));
                    } catch (PeerDevicesException ex) {
                        sendErrorToClient(ex.getMessage(), READ_CONFIGURATION_MULTICAST);
                    }
                } else {
                    _command.addValue(this.peerDevices.getMulticastList());
                }
                this.cmdListToClient.add(_command);
                break;

        }

    }

    /**
     * Crea una nueva lista de comandos para direcciones individuales a partir
     * de una direccion multicast
     *
     * @param listAddress Lista de direcciones contenida en la direccion
     * multicast
     * @param tempList Lista temporal
     * @param command Commando a duplicar para cada una de las direcciones
     * contenidas en la direccion muticast
     * @return
     */
    private ArrayList<Command> createNewMessagesFromMulticast(ArrayList<String> listAddress, ArrayList<Command> tempList, Command command) {
        Iterator<String> itAddress = listAddress.iterator();
        tempList = new ArrayList<Command>();
        while (itAddress.hasNext()) {
            //Para cada direccion de red de la lista multicast
            //creamos un nuevo comando con nuevo GUID
            Command _tempCommand = new Command(itAddress.next(), command.getType(), command.getValue(), System.currentTimeMillis(), UUID.randomUUID().toString(), command.isBroadcast());
            //Añadimos el comando temporal a la lista de los recibidos como si
            //hubiera sido realmente recibido
            tempList.add(_tempCommand);
        }
        return tempList;
    }
}
