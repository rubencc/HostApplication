/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.host.application.Network;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import org.host.application.Entities.Command;

/**
 * Establece una conexion de broadcast en el puerto 66 para enviar informacio a
 * todos los dispositivos escuchando en dicho puerto.
 *
 * @author rubencc
 */
public class BroadcastConnection implements Runnable {

    private static final int BROADCAST_PORT = 66;
    private static final int PING_PACKET_REQUEST = 0x30;
    private Datagram sendDg;
    private DatagramConnection bCon;
    private boolean finish;
    private boolean ready;
    private PeerDevices peerDevices = PeerDevices.getInstance();
    private Command cmdPing;

    public void run() {
        finish = true;
        try {
            //Abre la conexion en modo broadcast
            bCon = (DatagramConnection) Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
            sendDg = bCon.newDatagram(bCon.getMaximumLength());
            cmdPing = new Command(PING_PACKET_REQUEST);
        } catch (IOException ex) {
            Logger.getLogger(BroadcastConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Mientras no se indique terminacion del hilo: 
        while (finish) {
            try {
                //Cada 2.5s se envia un paquete con el texto "Hello".
                Thread.sleep(5000);
                SendBroadcast(cmdPing);
                peerDevices.checkAllDevices();
                //Pasado el primer ciclo de espera la conexion esta completamente disposible
                if (!ready) {
                    ready = true;
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(BroadcastConnection.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BroadcastConnectionException ex) {
                Logger.getLogger(BroadcastConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.CloseBroadcastConnection();
    }

    /**
     * Termina la ejecucion del bucle para terminar la ejecion de hilo.
     */
    public synchronized void FinishThread() {
        this.finish = false;
    }

    /**
     * Cierra la conexion de broadcast.
     */
    public synchronized void CloseBroadcastConnection() {
        try {
            bCon.close();
        } catch (IOException ex) {
            Logger.getLogger(BroadcastConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Envia un mensaje a traves de la conexion broadcast.
     *
     * @param command
     * @return
     */
    public synchronized void SendBroadcast(Command command) throws BroadcastConnectionException {
        boolean _sendCond = true;
        try {
            //Si la conexion esta completamente disponible

            if (ready) {
                //System.out.println("Enviando [" + command.getType() + " " + command.getValue() + "] en broadcast" + "[" + command.getGUID() + "]");

                sendDg.reset();
                /*El formato de la PDU es {tipo, valor, guid}*/
                sendDg.writeInt(command.getType());
                if (command.getValue() != null) {
                    sendDg.writeUTF(command.getValue());
                } else {
                    _sendCond = false;
                }
                if (command.getGUID() != null) {
                    sendDg.writeUTF(command.getGUID());
                } else {
                    _sendCond = false;
                }
                if (_sendCond) {
                    bCon.send(sendDg);

                } else {
                    throw new BroadcastConnectionException("Los campos de la PDU no son correctos");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(BroadcastConnection.class.getName()).log(Level.SEVERE, null, ex);
            throw new BroadcastConnectionException("Error al enviar en broadcast");
        }
    }
}
