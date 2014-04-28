package eu.spitfire.ssp.server.messages;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * A {@link eu.spitfire.ssp.server.messages.GraphStatusMessage} is the result of access to a
 * {@link eu.spitfire.ssp.backends.generic.DataOrigin}. Create instances of this class to represent access results
 * that were successful but do not contain payload, i.e. upon UPDATE or DELETE operations.
 *
 * If the access caused an error, use {@link eu.spitfire.ssp.server.messages.GraphStatusErrorMessage} instead. If the
 * access result is supposed to contain payload (e.g. GET requests) use
 * {@link eu.spitfire.ssp.server.messages.ExpiringNamedGraphStatusMessage}.
 *
 * @author Oliver Kleine
 */
public abstract class GraphStatusMessage {

//   public static enum StatusCode {
//        OK, CHANGED, DELETED, ERROR
//   }

    private HttpResponseStatus statusCode;

    public GraphStatusMessage(HttpResponseStatus statusCode){
        this.statusCode = statusCode;
    }

    public HttpResponseStatus getStatusCode() {
        return statusCode;
    }

}
