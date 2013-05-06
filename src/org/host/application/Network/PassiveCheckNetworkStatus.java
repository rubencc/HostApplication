/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.host.application.Network;

import java.util.TimerTask;
import java.io.IOException;
import com.googlecode.jsendnsca.Level;
import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosException;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.googlecode.jsendnsca.builders.NagiosSettingsBuilder;
import com.googlecode.jsendnsca.encryption.Encryption;

/**
 * Informa del estado de la red a Nagios
 *
 * @author rubencc
 */
public class PassiveCheckNetworkStatus extends TimerTask {

    private PeerDevices peerDevices = PeerDevices.getInstance();
    NagiosPassiveCheckSender sender;
    MessagePayload payload;
    NagiosSettings settings;

    public PassiveCheckNetworkStatus() {
    }

    @Override
    public void run() {

        this.settings = new NagiosSettingsBuilder()
                .withNagiosHost("localhost")
                .withPort(5667)
                .withEncryption(Encryption.XOR)
                .withNoPassword()
                .create();
        this.payload = new MessagePayloadBuilder()
                .withHostname("sunspot")
                .withLevel(getStatusLevel())
                .withServiceName("Network Status")
                .withMessage(getMessage())
                .create();
        this.sender = new NagiosPassiveCheckSender(settings);

        try {
            //setMessage();
            sender.send(payload);
        } catch (NagiosException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mensaje que se enviara a Nagios
     */
    private String getMessage() {
        StringBuilder sb = new StringBuilder();
        if (this.peerDevices.isEmpty()) {
            sb.append("Ningun dispositivo conectado");
        } else {
            sb.append("Dispositivos conectados: ").append(this.peerDevices.numberOfPeers());
        }
        //System.out.println(sb.toString());
        return sb.toString();
    }

    /**
     * Nivel de alerta para Nagios
     */
    private Level getStatusLevel() {
        Level _level;
        if (this.peerDevices.isEmpty()) {
            _level = Level.UNKNOWN;
        } else {
            _level = Level.OK;
        }
        return _level;
    }
}
