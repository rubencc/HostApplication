/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 *
 * @author rubencc
 */
public class ProcessSocket {

    private Socket socket = null;
    private DataOutputStream oBuf;
    private DataInputStream iBuf;

    /**
     * Constructor para el conjunto de funciones que realiza el el hilo que
     * atiende la peticion.
     *
     * @param socket - Socket conectado con el cliente
     *
     */
    protected ProcessSocket(Socket socket) {
        this.socket = socket;
        try {
            this.oBuf = new DataOutputStream(socket.getOutputStream());
            this.iBuf = new DataInputStream(socket.getInputStream());
            this.socket.setKeepAlive(true);
        } catch (IOException e) {
            e.getMessage();
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
        } catch (IOException e) {
            this.close();
            e.getMessage();
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
            } catch (SocketException e) {
                e.getMessage();
                close();
            } catch (IOException e) {
                e.getMessage();
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
            System.out.println("Socket Cerrado");
            this.iBuf.close();
            this.oBuf.close();
            this.socket.close();
        } catch (IOException e) {
            e.getMessage();
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
