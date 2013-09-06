package org.host.application.Network;

import Helpers.LogHelper;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import org.host.application.Entities.Command;

/**
 * Clase de instancia unica que gestiona la conexiñon broadcast
 *
 * @author Rubén Carretero <rubencc@gmail.com>
 */
public class BroadcastConnection implements Runnable {

    //Puerto de la conexión broadcast
    private static final int BROADCAST_PORT = 66;
    private static final int PING_PACKET_REQUEST = 0x30;
    //Datagrama
    private Datagram sendDg;
    //Conexión broadcast
    private DatagramConnection bCon;
    //Condición para terminar
    private boolean finish;
    //Condición de disponibilidad
    private boolean ready;
    //Instnacia del gestor de dispostivios
    private PeerDevices peerDevices = PeerDevices.getInstance();
    private Command cmdPing;
    private final String CLASSNAME = getClass().getName();
    private LogHelper logger;

    public void run() {
        finish = true;
        try {
            //Abre la conexion en modo broadcast
            this.bCon = (DatagramConnection) Connector.open("radiogram://broadcast:" + this.BROADCAST_PORT);
            this.sendDg = this.bCon.newDatagram(bCon.getMaximumLength());
            this.cmdPing = new Command(PING_PACKET_REQUEST);
            this.logger = LogHelper.getInstance();
        } catch (IOException ex) {
            this.logger.logSEVERE(CLASSNAME, "run -- IOException", ex.getMessage());
        }

        //Mientras no se indique terminacion del hilo: 
        while (this.finish) {
            try {
                //Cada 5s se envia un paquete de ping
                Thread.sleep(3000);
                SendBroadcast(this.cmdPing);
                this.peerDevices.checkAllDevices();
                //Pasado el primer ciclo de espera la conexion esta completamente disposible
                if (!this.ready) {
                    this.ready = true;
                }
            } catch (InterruptedException ex) {
                this.logger.logSEVERE(CLASSNAME, "run -- InterruptedException", ex.getMessage());
            } catch (BroadcastConnectionException ex) {
                this.logger.logSEVERE(CLASSNAME, "run -- BroadcastConnectionException", ex.getMessage());
            }
        }
        this.CloseBroadcastConnection();
    }

    /**
     * Termina la ejecucion del bucle para terminar la ejecion de hilo
     */
    public synchronized void FinishThread() {
        this.finish = false;
    }

    /**
     * Cierra la conexion de broadcast.
     */
    public synchronized void CloseBroadcastConnection() {
        try {
            this.bCon.close();
        } catch (IOException ex) {
            this.logger.logSEVERE(CLASSNAME, "CloseBroadcastConnection -- IOException", "closing peer connection " + ex.getMessage());
        }
    }

    /**
     * Envia un mensaje a traves de la conexion broadcast
     *
     * @param command
     * @return
     */
    public synchronized void SendBroadcast(Command command) throws BroadcastConnectionException {
        boolean _sendCond = true;
        try {
            //Si la conexion esta completamente disponible
            if (this.ready) {
                this.sendDg.reset();
                /*El formato de la PDU es {tipo,tamaño, valor, guid}*/
                this.sendDg.writeInt(command.getType());
                if ((command.getValue() != null) && (command.getGUID() != null)) {
                    this.sendDg.writeInt(command.countValues());
                    Iterator it = command.getValue().iterator();
                    while (it.hasNext()) {
                        this.sendDg.writeUTF((String) it.next());
                    }
                    this.sendDg.writeUTF(command.getGUID());
                } else {
                    _sendCond = false;
                }
                if (_sendCond) {
                    this.bCon.send(this.sendDg);
                } else {
                    throw new BroadcastConnectionException("Los campos de la PDU no son correctos");
                }
                if (command.getType() != PING_PACKET_REQUEST) {
                    this.logger.logFINE(CLASSNAME, "SendBroadcast -- debug command", command.toString());
                }
            }
        } catch (IOException ex) {
            this.logger.logSEVERE(CLASSNAME, "SendBroadcast -- IOException", ex.getMessage());
            throw new BroadcastConnectionException("Error al enviar en broadcast");
        }
    }
}
