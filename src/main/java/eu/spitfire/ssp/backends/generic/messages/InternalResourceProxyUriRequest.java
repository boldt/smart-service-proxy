//package eu.spitfire.ssp.backends.generic.messages;
//
//import com.google.common.util.concurrent.SettableFuture;
//
//import java.net.URI;
//
///**
// * This is an internal message to request the resource-backends-{@link URI} for given resource-{@link URI}. The
// * resource-URI is the URI identifying the resource. The resource-backends-URI is the URI to make the resource
// * accessable via the backends.
// */
//public class InternalResourceProxyUriRequest {
//
//    private final SettableFuture<URI> resourceProxyUriFuture;
//    private final String gatewayPrefix;
//    private final URI resourceUri;
//
//    /**
//     * @param resourceProxyUriFuture The {@link SettableFuture} to contain the resource backends URI after processing
//     *                               this request
//     * @param gatewayPrefix the prefix of the gateway initiating this request
//     * @param resourceUri   the {@link URI} identifying the resource
//     */
//    public InternalResourceProxyUriRequest(SettableFuture<URI> resourceProxyUriFuture, String gatewayPrefix, URI resourceUri){
//        this.resourceProxyUriFuture = resourceProxyUriFuture;
//        this.gatewayPrefix = gatewayPrefix;
//        this.resourceUri = resourceUri;
//    }
//
//    /**
//     * Returns the {@link SettableFuture} to contain the resource backends URI after processing this request
//     * @return the {@link SettableFuture} to contain the resource backends URI after processing this request
//     */
//    public SettableFuture<URI> getResourceProxyUriFuture() {
//        return resourceProxyUriFuture;
//    }
//
//    /**
//     * Returns the prefix of the gateway initiating this request
//     * @return the prefix of the gateway initiating this request
//     */
//    public String getGatewayPrefix() {
//        return gatewayPrefix;
//    }
//
//    /**
//     * Returns the resource-URI which a resource-backends-URI is requested for
//     * @return the resource-URI which a resource-backends-URI is requested for
//     */
//    public URI getResourceUri() {
//        return resourceUri;
//    }
//
//    @Override
//    public String toString(){
//        return "Resource-proxy-URI request from " + getGatewayPrefix() + " (gateway name) for: " + getResourceUri();
//    }
//}
