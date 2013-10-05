package Helpers;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rubén Carretero <rubencc@gmail.com>
 */
public class MeassureLogHelper {

    private static MeassureLogHelper INSTANCE = new MeassureLogHelper();
    private Logger logger;
    private boolean debug = false;

    private MeassureLogHelper() {
        this.logger = Logger.getAnonymousLogger();
        loadConfig();
    }

    private void loadConfig() {
        try {
            FileHandler fh = new FileHandler("%h/meassures_%g.log", 1048576, 1, false);
            fh.setLevel(Level.ALL);
            fh.setFormatter(new MeassureLogFormatter());
            this.logger.addHandler(fh);
            this.logger.setLevel(Level.ALL);
            this.logger.setUseParentHandlers(false);
        } catch (IOException ex) {
            Logger.getLogger(LogHelper.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(LogHelper.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static MeassureLogHelper getInstance() {
        return INSTANCE;
    }

    public void logINFO(String className, String method, String text) {
        this.logger.logp(Level.INFO, className, method, text);
    }

    public void logWARNING(String className, String method, String text) {
        this.logger.logp(Level.WARNING, className, method, text);
    }

    public void logSEVERE(String className, String method, String text) {
        this.logger.logp(Level.SEVERE, className, method, text);
    }

    public void logFINE(String className, String method, String text) {
        if (this.debug) {
            this.logger.logp(Level.FINE, className, method, text);
        }
    }

    /**
     * @param debug the debug to set
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
