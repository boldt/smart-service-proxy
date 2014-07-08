package eu.spitfire.ssp.backends.internal.se;

import eu.spitfire.ssp.backends.generic.Accessor;
import eu.spitfire.ssp.backends.generic.BackendComponentFactory;
import eu.spitfire.ssp.backends.generic.Observer;
import eu.spitfire.ssp.backends.generic.Registry;
import org.apache.commons.configuration.Configuration;
import org.jboss.netty.channel.local.LocalServerChannel;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by olli on 07.07.14.
 */
public class SemanticEntityBackendComponentFactory extends BackendComponentFactory<URI, SemanticEntity>{

    private SemanticEntityAccessor semanticEntityAccessor;

    /**
     * Creates a new instance of {@link eu.spitfire.ssp.backends.generic.BackendComponentFactory}.
     *
     * @param prefix                the prefix of the backend in the given config (without the ".")
     * @param config                the SSP config
     * @param localChannel
     * @param internalTasksExecutor the {@link java.util.concurrent.ScheduledExecutorService} for backend tasks,
     *                              e.g. translating and forwarding requests to data origins
     * @param ioExecutor            @throws java.lang.Exception if something went terribly wrong
     */
    protected SemanticEntityBackendComponentFactory(String prefix, Configuration config, LocalServerChannel localChannel,
                                                    ScheduledExecutorService internalTasksExecutor, ExecutorService ioExecutor)
            throws Exception {

        super(prefix, config, localChannel, internalTasksExecutor, ioExecutor);
    }


    @Override
    public void initialize() throws Exception {
        this.semanticEntityAccessor = new SemanticEntityAccessor(this);
    }

    /**
     * Returns <code>null</code> since instances of {@link eu.spitfire.ssp.backends.internal.se.SemanticEntity} are not
     * observable.
     *
     *
     * @param dataOrigin the {@link eu.spitfire.ssp.backends.generic.DataOrigin} to be observed
     *
     * @return <code>null</code> since instances of {@link eu.spitfire.ssp.backends.internal.se.SemanticEntity} are not
     * observable.
     */
    @Override
    public Observer<URI, ? extends SemanticEntity> getObserver(SemanticEntity semanticEntity) {
        return null;
    }


    @Override
    public SemanticEntityAccessor getAccessor(SemanticEntity dataOrigin) {
        return this.semanticEntityAccessor;
    }


    @Override
    public SemanticEntityRegistry createRegistry(Configuration config) throws Exception {
        return new SemanticEntityRegistry(this);
    }

    @Override
    public SemanticEntityRegistry getRegistry(){
        return (SemanticEntityRegistry) super.getRegistry();
    }


    @Override
    public void shutdown() {

    }
}
