package tp1.impl.servers.rest;

import org.glassfish.jersey.server.ResourceConfig;
import tp1.api.service.java.Files;
import tp1.impl.servers.rest.util.GenericExceptionMapper;
import util.Debug;
import util.Token;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DropboxFilesRestServer extends AbstractRestServer {
    public static final int PORT = 8080;

    private static Logger Log = Logger.getLogger(DropboxFilesRestServer.class.getName());

    private final String apiKey;
    private final String apiSecret;
    private final String accessTokenStr;

    private final boolean flag;

    DropboxFilesRestServer( int port, String apiKey, String apiSecret, String apiToken, boolean flag ) {
        super(Log, Files.SERVICE_NAME, port);

        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.accessTokenStr = apiToken;
        this.flag = flag;
    }

    @Override
    void registerResources(ResourceConfig config) {
        config.register( new DropboxFilesResources(apiKey, apiSecret, accessTokenStr, flag) );
        config.register( GenericExceptionMapper.class );
    }

    public static void main(String[] args) {
        Debug.setLogLevel(Level.INFO, Debug.TP1);

        boolean flag = Boolean.parseBoolean(args[0]);

        Token.set(args.length == 0 ? "" : args[1]);

        new DropboxFilesRestServer(PORT, args[2], args[3], args[4], flag).start();
    }
}
