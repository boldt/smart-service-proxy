package eu.spitfire.ssp.server.handler.cache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.sun.org.apache.xpath.internal.operations.Bool;
import eu.spitfire.ssp.server.messages.ExpiringGraphStatusMessage;
import eu.spitfire.ssp.server.messages.ExpiringNamedGraphStatusMessage;
import eu.spitfire.ssp.server.messages.GraphStatusMessage;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.SettableFuture;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;


/**
* Created with IntelliJ IDEA.
* User: olli
* Date: 05.09.13
* Time: 09:43
* To change this template use File | Settings | File Templates.
*/
public class JenaTdbSemanticCache extends SemanticCache {

	private static final String SPT_SOURCE = "http://spitfire-project.eu/ontology.rdf";
	private static final String SPTSN_SOURCE = "http://spitfire-project.eu/sn.rdf";
	private static final String RULE_FILE_PROPERTY_KEY = "RULE_FILE";

	private static String ruleFile = null;

	private static OntModel ontologyBaseModel = null;

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private Dataset dataset;
	private Reasoner reasoner;


	public JenaTdbSemanticCache(ExecutorService ioExecutorService,
                                ScheduledExecutorService internalTasksExecutorService, Path dbDirectory){

		super(ioExecutorService, internalTasksExecutorService);

		File directory = dbDirectory.toFile();
		File[] oldFiles = directory.listFiles();

        assert oldFiles != null;
        for (File dbFile : oldFiles) {
                dbFile.delete();
        }


		dataset = TDBFactory.createDataset(dbDirectory.toString());
		TDB.getContext().set(TDB.symUnionDefaultGraph, true);

		//Collect the SPITFIRE vocabularies
//		if (ontologyBaseModel == null) {
//			ontologyBaseModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
//			if (isUriAccessible(SPT_SOURCE)) {
//				ontologyBaseModel.read(SPT_SOURCE, "RDF/XML");
//			}
//			if (isUriAccessible(SPTSN_SOURCE)) {
//				ontologyBaseModel.read(SPTSN_SOURCE, "RDF/XML");
//			}
//		}

//		reasoner = ReasonerRegistry.getRDFSSimpleReasoner().bindSchema(ontologyBaseModel);


//		if (ruleFile == null) {
//			ruleFile = getRuleFilePath();
//		}

//		Model m = ModelFactory.createDefaultModel();
//		Resource configuration = m.createResource();
//		configuration.addProperty(ReasonerVocabulary.PROPruleMode, "hybrid");
//		if (ruleFile != null) {
//			configuration.addProperty(ReasonerVocabulary.PROPruleSet, ruleFile);
//		}
//
//		reasoner= GenericRuleReasonerFactory.theInstance().create(configuration).bindSchema(ontologyBaseModel);


	}

	private static String getRuleFilePath() {
		String ret = null;
		Configuration config;
		try {
			config = new PropertiesConfiguration("ssp.properties");

			ret = config.getString(RULE_FILE_PROPERTY_KEY);

		} catch (ConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}


		return ret;
	}

	private Reasoner getCustomReasoner() {
		return reasoner;
	}

	private static boolean isUriAccessible(String uri) {
		HttpURLConnection connection = null;
		int code = -1;
		URL myurl;
		try {
			myurl = new URL(uri);

			connection = (HttpURLConnection) myurl.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(1000);
			code = connection.getResponseCode();
		}
        catch (IOException e) {
			System.err.println(uri + " is not accessible.");
		}

		return (code == 200);
	}


	@Override
	public ListenableFuture<ExpiringGraphStatusMessage> getNamedGraph(URI graphName) {

        SettableFuture<ExpiringGraphStatusMessage> resultFuture = SettableFuture.create();

        try {
            dataset.begin(ReadWrite.READ);

			if (graphName == null){
				log.error("Resource URI was NULL!");
                resultFuture.set(null);
                return resultFuture;
            }

			Model model = dataset.getNamedModel(graphName.toString());

			if (model.isEmpty()) {
				log.warn("No cached status found for resource {}", graphName);
				resultFuture.set(null);
			}

            else{
                log.info("Cached status found for resource {}", graphName);

                ExpiringNamedGraph expiringNamedGraph = new ExpiringNamedGraph(graphName, model, new Date());
                resultFuture.set(new ExpiringGraphStatusMessage(HttpResponseStatus.OK, expiringNamedGraph));
            }

            return resultFuture;
		}

        catch(Exception ex){
            resultFuture.setException(ex);
            return resultFuture;
        }

        finally {
			dataset.end();
		}

	}

    @Override
    public ListenableFuture<Boolean> containsNamedGraph(URI graphName) {

        SettableFuture<Boolean> resultFuture = SettableFuture.create();

        try{
            dataset.begin(ReadWrite.READ);
            Boolean result = !dataset.getNamedModel(graphName.toString()).isEmpty();

            resultFuture.set(result);
            return resultFuture;
        }

        catch(Exception ex){
            resultFuture.setException(ex);
            return resultFuture;

        }

        finally {
            dataset.end();
        }
    }


    @Override
	public ListenableFuture<Void> putNamedGraphToCache(URI graphName, Model namedGraph){

        SettableFuture<Void> resultFuture = SettableFuture.create();

        try {
            dataset.begin(ReadWrite.WRITE);

			Model model = dataset.getNamedModel(graphName.toString());
			model.removeAll();
//			model.add(ModelFactory.createInfModel(reasoner, namedGraph));
            model.add(namedGraph);
//            InfModel im = ModelFactory.createInfModel(reasoner, resourceStatus);
//			InfModel im = ModelFactory.createInfModel(reasoner, resourceStatus);
			// force starting the rule execution
//			im.prepare();
//			model.add(im);
			dataset.commit();
			log.debug("Added status for resource {}", graphName);

            resultFuture.set(null);
            return resultFuture;
		}

        catch(Exception ex){
            resultFuture.setException(ex);
            return resultFuture;
        }

        finally {
			dataset.end();
		}
	}


	@Override
	public ListenableFuture<Void> deleteNamedGraph(URI graphName){

		SettableFuture<Void> resultFuture = SettableFuture.create();

        try {
            dataset.begin(ReadWrite.WRITE);

			dataset.removeNamedModel(graphName.toString());
			dataset.commit();
			log.debug("Removed status for resource {}", graphName);

            resultFuture.set(null);
            return resultFuture;
		}

        catch(Exception ex){
            resultFuture.setException(ex);
            return resultFuture;
        }

        finally {
			dataset.end();
		}
	}

//	@Override
//	public void updateStatement(Statement statement) throws Exception {
//
//		dataset.begin(ReadWrite.WRITE);
//		try {
//			Model tdbModel = dataset.getNamedModel(statement.getSubject().toString());
//
//			Statement oldStatement = tdbModel.getProperty(statement.getSubject(), statement.getPredicate());
//			Statement updatedStatement;
//			if (oldStatement != null) {
//				if ("http://spitfire-project.eu/ontology/ns/value".equals(oldStatement.getPredicate().toString())) {
//					RDFNode object =
//							tdbModel.createTypedLiteral(statement.getObject().asLiteral().getFloat(), XSDDatatype.XSDfloat);
//					updatedStatement = oldStatement.changeObject(object);
//					dataset.commit();
//
//				} else {
//					updatedStatement = oldStatement.changeObject(statement.getObject());
//					dataset.commit();
//				}
//				log.info("Updated property {} of resource {} to {}", new Object[]{updatedStatement.getPredicate(),
//						updatedStatement.getSubject(), updatedStatement.getObject()});
//			} else
//				log.warn("Resource {} not (yet?) found. Could not update property {}.", statement.getSubject(),
//						statement.getPredicate());
//		} finally {
//			dataset.end();
//		}
//
//	}

    @Override
	public ListenableFuture<GraphStatusMessage> processSparqlQuery(Query sparqlQuery) {

        SettableFuture<GraphStatusMessage> resultFuture = SettableFuture.create();
		dataset.begin(ReadWrite.READ);

		try {
			log.info("Start SPARQL query processing: {}", sparqlQuery.toString(Syntax.syntaxSPARQL));

			QueryExecution queryExecution = QueryExecutionFactory.create(sparqlQuery, dataset);

            Model resultGraph = ModelFactory.createDefaultModel();

//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ResultSet resultSet = queryExecution.execSelect();

                while(resultSet.hasNext()){
                    QuerySolution querySolution = resultSet.next();
                    Statement statement = resultGraph.createStatement(
                            querySolution.getResource("?s"),
                            resultGraph.createProperty(querySolution.getResource("?p").getURI()),
                            querySolution.get("?o")
                    );
                    resultGraph.add(statement);
                }
//                resultSet.ne
//                ResultSetFormatter.asRDF(resultGraph, resultSet);

//				ResultSetFormatter.outputAsXML(baos, resultSet);
//
//                log.info("SPARQL query result: {}", baos.toString());

			} finally {
				queryExecution.close();
			}
//			String result = baos.toString("UTF-8");
            ExpiringGraph expiringGraph = new ExpiringGraph(resultGraph, new Date());
			resultFuture.set(new ExpiringGraphStatusMessage(HttpResponseStatus.OK, expiringGraph));

            return resultFuture;
		}

        catch (Exception ex) {
			resultFuture.setException(ex);
            return resultFuture;
		}

        finally {
			dataset.end();
		}
	}

//
//	@Override
//	public boolean supportsSPARQL() {
//		return true;
//	}
}

