package eu.spitfire.ssp.server.messages;

import eu.spitfire.ssp.server.handler.cache.ExpiringGraph;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * Created by olli on 17.04.14.
 */
public class ExpiringGraphStatusMessage extends GraphStatusMessage{

    private ExpiringGraph expiringGraph;


    public ExpiringGraphStatusMessage(HttpResponseStatus statusCode, ExpiringGraph expiringGraph) {
        super(statusCode);
        this.expiringGraph = expiringGraph;
    }


    public ExpiringGraph getExpiringGraph() {
        return this.expiringGraph;
    }
}
