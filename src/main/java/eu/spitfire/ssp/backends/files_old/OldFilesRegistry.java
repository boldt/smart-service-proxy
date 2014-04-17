//package eu.spitfire.ssp.backends.files_old;
//
//import com.hp.hpl.jena.rdf.model.Model;
//import eu.spitfire.ssp.backends.generic.registration.DataOriginRegistry;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.FileNotFoundException;
//import java.net.URI;
//import java.nio.file.Path;
//import java.util.Map;
//
//
///**
//* Created with IntelliJ IDEA.
//* User: olli
//* Date: 04.10.13
//* Time: 17:01
//* To change this template use File | Settings | File Templates.
//*/
//public class OldFilesRegistry extends DataOriginRegistry<Path> {
//
//    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
//
//
//    protected OldFilesRegistry(OldFilesBackendComponentFactory componentFactory) {
//        super(componentFactory);
//    }
//
//
//    public void handleN3FileCreation(Path file){
//        log.info("File created: {}", file);
//
//        try {
//            if(file.toString().endsWith(".n3") ){
//                Model model = OldFilesResourceToolBox.readModelFromFile(file);
//                Map<URI, Model> modelsFromFile = OldFilesObserver.getModelsPerSubject(model);
//
//                log.info("Register {} new resources.", modelsFromFile.size());
//
//                for(URI resourceUri : modelsFromFile.keySet())
//                    registerDataOrigin(file, modelsFromFile.get(resourceUri));
//            }
//        }
//        catch (FileNotFoundException e) {
//            log.error("This should never happen.", e);
//        }
//    }
//
//    public void registerDataOrigin(Model model, Path file){
//        super.registerDataOrigin(file, model);
//    }
//}
