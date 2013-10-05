package org.sunspotworld;

import Helpers.LogHelper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Clase que gestiona la conexión mediante socket
 *
 * @author Rubén Carretero <rubencc@gmail.com>
 */
public class ProcessSocket {

    private Socket socket = null;
    private DataOutputStream oBuf;
    private DataInputStream iBuf;
    private final String CLASSNAME = getClass().getName();
    private LogHelper logger;

    protected ProcessSocket(Socket socket) {
        this.logger = LogHelper.getInstance();
        this.socket = socket;
        try {
            this.oBuf = new DataOutputStream(socket.getOutputStream());
            this.iBuf = new DataInputStream(socket.getInputStream());
            this.socket.setKeepAlive(true);
        } catch (IOException ex) {
            this.logger.logSEVERE(CLASSNAME, "ProcessSocket -- IOException", ex.getMessage());
        }

    }

    /**
     * Envia al cliente una respuesta
     *
     * @param message
     */
    protected synchronized void send(String message) {
        try {
            this.oBuf.write(message.getBytes("UTF-8"));
            this.oBuf.flush();
        } catch (IOException ex) {
            this.close();
            this.logger.logSEVERE(CLASSNAME, "send -- IOException", ex.getMessage());
        }
    }

    /**
     * Recibe datos desde el cliente
     *
     * @return
     */
    protected synchronized String recive() {
        byte[] _message;
        String _temp = null;
        if (socket.isConnected()) {
            try {
                _message = new byte[iBuf.available()];
                this.iBuf.readFully(_message);
                if (_message.length > 0) {
                    _temp = new String(_message, "UTF-8");
                    if (_temp.equals("END")) {
                        this.close();
                    }
                }
            } catch (SocketException ex) {
                this.logger.logSEVERE(CLASSNAME, "recive -- SocketException", ex.getMessage());
                close();
            } catch (IOException ex) {
                this.logger.logSEVERE(CLASSNAME, "recive -- IOException", ex.getMessage());
            }
        }
        return _temp;
    }

    /**
     * Cierra el socket usado por este hilo.
     *
     *
     */
    protected void close() {
        try {
            //System.out.println("Socket Cerrado");
            this.iBuf.close();
            this.oBuf.close();
            this.socket.close();
            this.logger.logINFO(CLASSNAME, "close", "Socket Closed");
        } catch (IOException ex) {
            this.logger.logSEVERE(CLASSNAME, "close -- IOException", ex.getMessage());
        }
    }

    /**
     * Comprueba la disponibilidad del socket para lectura/escritura.
     *
     * @return boolean
     *
     */
    protected boolean isSocketClosed() {
        return socket.isClosed();
    }

    public void destroy() {
        this.close();
    }
}
