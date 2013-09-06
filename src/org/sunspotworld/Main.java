package org.sunspotworld;

import Helpers.LogHelper;
import org.host.application.Network.BroadcastConnection;
import org.host.application.Network.PeerConnection;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Clase principal
 *
 * @author: Rubén Carretero <rubencc@gmail.com>
 */
public class Main {

    private static final int SOCKET_LISTEN_PORT = 2000;
    private final String CLASSNAME = getClass().getName();

    private void run(boolean debug) throws Exception {
        LogHelper logger = LogHelper.getInstance();
        if (debug) {
            logger.setDebug(debug);
        }
        Server _serverTread = null;
        ServerSocket _ssocket = new ServerSocket(SOCKET_LISTEN_PORT, 10);
        Socket _socket = null;
        logger.logINFO(CLASSNAME, "run", "HostApllication running on port " + SOCKET_LISTEN_PORT);
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
                logger.logINFO(CLASSNAME, "run", "Client in port: " + _socket.getLocalPort());
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
        boolean _debug = false;
        if (args.length > 0) {
            String _temp = args[0];
            if (_temp.equals("-v")) {
                _debug = true;
            }
        }
        _app.run(_debug);
    }
}
