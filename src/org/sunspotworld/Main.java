package org.sunspotworld;

import org.host.application.Network.BroadcastConnection;
import org.host.application.Network.PeerConnection;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Clase principal
 *
 * @author: rubencc
 */
public class Main {

    private static final int SOCKET_LISTEN_PORT = 2000;

    private void run() throws Exception {
        Server _serverTread = null;
        ServerSocket _ssocket = new ServerSocket(SOCKET_LISTEN_PORT, 10);
        Socket _socket = null;
        System.out.println("Escuchando el puerto: " + _ssocket.getLocalPort());
        //Lanza en hilo de comunicaciones broadcast
        BroadcastConnection _bCon = new BroadcastConnection();
        new Thread(_bCon).start();
        //Lanza el hilo de comunicaciones peer
        PeerConnection _pCon = new PeerConnection();
        new Thread(_pCon).start();
        //El ServerSocket atiente peticiones entrantes.
        while (true) {
            _socket = _ssocket.accept();
            if (_socket != null) {
                System.out.println("Conexion establecida en puerto: " + _socket.getLocalPort());
                //Se lanza un nuevo hilo para antender la peticion
                _serverTread = new Server(_socket, _bCon, _pCon);
                new Thread(_serverTread).start();
            }
        }

    }

    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) throws Exception {
        // register the application's name with the OTA Command server & start OTA running
        //OTACommandServer.start("TestHostApp");
        Main _app = new Main();
        _app.run();
    }
}
