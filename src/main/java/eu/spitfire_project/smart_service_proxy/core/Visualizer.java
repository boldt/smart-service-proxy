package eu.spitfire_project.smart_service_proxy.core;

import de.uniluebeck.itm.spitfire.nCoap.message.CoapRequest;
import de.uniluebeck.itm.spitfire.nCoap.message.InvalidMessageException;
import de.uniluebeck.itm.spitfire.nCoap.message.MessageDoesNotAllowPayloadException;
import de.uniluebeck.itm.spitfire.nCoap.message.header.Code;
import de.uniluebeck.itm.spitfire.nCoap.message.header.MsgType;
import de.uniluebeck.itm.spitfire.nCoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.spitfire.nCoap.message.options.OptionRegistry;
import de.uniluebeck.itm.spitfire.nCoap.message.options.ToManyOptionsException;
import de.uniluebeck.itm.spitfire.nCoap.application.CoapClientApplication;
import eu.spitfire_project.smart_service_proxy.utils.TList;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * Written: Cuong Truong
 * Date: 10.10.12
 * Time: 11:22
 * To change this template use File | Settings | File Templates.
 */
public class Visualizer extends SimpleChannelUpstreamHandler{
    private Logger log = Logger.getLogger(Visualizer.class.getName());
    private static final Visualizer instance = new Visualizer();

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(20);

    private class AutoAnnotation extends CoapClientApplication implements Runnable{

        @Override
        public void run() {
            try {
                log.debug("RUNNING!");
                //Crawl sensor readings
                for (int i=0; i<sensors.len(); i++)
                    ((SensorData)sensors.get(i)).crawl();

                //Check if annotation timer of sensors expire then trigger annotation process
                for (int i=0; i<sensors.len(); i++) {
                    SensorData sd = (SensorData)sensors.get(i);
                    if ("none".equalsIgnoreCase(sd.FOI)) {
                        long thre = System.currentTimeMillis() - sd.annoTimer;
                        //Trigger annotation here
                        if (thre > annoTimeThreshold) {
                            //Calculate fuzzy set of other sensors
                            for (int j = 0; j<sensors.len(); j++) {
                                SensorData de = (SensorData)sensors.get(j);
                                if (!"none".equalsIgnoreCase(de.FOI)) {
                                    System.out.print("Computing fuzzy set for sensor " + de.ipv6Addr + "... ");
                                    de.computeFuzzySet();
                                    log.debug(" Done!");
                                }
                            }

                            //Search for annotation
                            log.debug("Search for annotation... ");
                            double maxsc = 0;
                            for (int j = 0; j < sensors.len(); j++) {
                                SensorData de = (SensorData)sensors.get(j);
                                if (!"none".equalsIgnoreCase(de.FOI)) {
                                    double sc = calculateScore(sd.getValues(), de.getFZ(), de.getDFZ());
                                    if (maxsc < sc) {
                                        maxsc = sc;
                                        sd.FOI = de.FOI;
                                    }
                                    log.debug("Similarity to " + de.ipv6Addr + " in " + de.FOI + " is "
                                            + String.format(Locale.GERMANY, "%.10f", sc));
                                }
                            }
                            log.debug("Resulting annotation is " + sd.FOI);

                            //Send POST to sensor
                            CoapRequest annotation = makeCOAPRequest(sd.ipv6Addr, sd.FOI);
                            log.debug("Sending POST request to sensor!");
                            writeCoapRequest(annotation);
                        }
                    }
                }

                //Thread.sleep(sampleRate); //every 0.5 second
                //synchronized (this) { while (pause) wait(); }
//            } catch (InterruptedException e){} catch (MessageDoesNotAllowPayloadException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (InvalidOptionException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (InvalidMessageException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ToManyOptionsException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (URISyntaxException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (MessageDoesNotAllowPayloadException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        private CoapRequest makeCOAPRequest(String remoteIP, String resultAnnotation) throws URISyntaxException, ToManyOptionsException, InvalidOptionException, InvalidMessageException, MessageDoesNotAllowPayloadException {
            URI AnnotationServiceURI = new URI("coap://" + remoteIP + ":5683/rdf");
            CoapRequest coapRequest = new CoapRequest(MsgType.CON, Code.POST, AnnotationServiceURI);
            coapRequest.setContentType(OptionRegistry.MediaType.APP_N3);
            String payloadStr = "\0<coap://"+remoteIP+"/rdf>\0" +
                    "<http://purl.oclc.org/NET/ssnx/ssn#featureOfInterest>\0" +
                    "<http://spitfire-project.eu/foi/"+resultAnnotation+">\0";
            byte[] payload = payloadStr.getBytes(Charset.forName("UTF-8"));
            coapRequest.setPayload(payload);

            return coapRequest;
        }
    }

    private class SensorData {
        public String ipv6Addr = null;
        public String FOI = null;
        private ArrayList<Long> timeStamps = new ArrayList<Long>(); //Time-stamp of a samples
        private ArrayList<Double> values = new ArrayList<Double>(); //Value of the samples
        public int nSamples, sampleRate;
        public long annoTimer;
        private Random random = new Random();
        private FuzzyRule fz, dfz;

        public SensorData(String ipv6Addr, String FOI, int nSamples, int sampleRate) {
            this.ipv6Addr = ipv6Addr;
            this.FOI = FOI;
            this.nSamples = nSamples;
            this.sampleRate = sampleRate;

            if ("none".equalsIgnoreCase(FOI))
                annoTimer = System.currentTimeMillis();
        }

        private void updateReadings(long ts, double vl) {
            timeStamps.add(ts);
            if (timeStamps.size() >= nSamples)
                timeStamps.remove(0);
            values.add(vl);
            if (values.size() >= nSamples)
                values.remove(0);
        }

        public void crawl() {
            long time = System.currentTimeMillis();
            double value = random.nextDouble();
            updateReadings(time, value);
        }

        public ArrayList<Double> getValues() {
            return values;
        }

        public long getLatestTS() {
            long rs = 0;
            if (timeStamps.size() > 0)
                rs = timeStamps.get(timeStamps.size()-1);
            return rs;
        }

        public double getLatestVL() {
            double rs = 0;
            if (values.size() > 0)
                rs = values.get(values.size()-1);
            return rs;
        }

        public void computeFuzzySet() {
            fz = extractRule(values);
            dfz = extractRuleD(values);
        }

        public FuzzyRule getFZ() {
            return fz;
        }

        public FuzzyRule getDFZ() {
            return dfz;
        }

        private FuzzyRule extractRuleD(List<Double> dataList) {
            ArrayList<Double> deriList = new ArrayList<Double>();
            Double[] raw = dataList.toArray(new Double[dataList.size()]);
            for (int i=0; i<raw.length-1; i++) {
                Double tmp = Double.valueOf(raw[i+1]-raw[i]);
                deriList.add(tmp);
            }
            deriList.add(deriList.get(deriList.size()-1));

            return extractRule(deriList);
        }

        private FuzzyRule extractRule(List<Double> dataList) {
            Double[] raw = dataList.toArray(new Double[dataList.size()]);

            // Identify the value range
            double rawMax = Collections.max(dataList);
            double rawMin = Collections.min(dataList);
            double rawRange = rawMax - rawMin;

            // Special case: all values are the same
            if (rawRange <= Double.MIN_VALUE) {
                log.debug("All values in snapshot are identical");
                double epsilon = 0.05;
                FuzzyRule rule = new FuzzyRule();
                rule.setrMax(rawMax + epsilon);
                rule.setrMin(rawMin - epsilon);
                rule.add(raw[0] - epsilon, 1);
                rule.add(raw[0] + epsilon, 1);
                return rule;
            }

            double ra = 0.5*rawRange;
            double alpha = 4/ra/ra;
            double ndc[] = new double[raw.length];

            //Calculate neighborhood density curve of data
            for (int i=0; i<raw.length; i++) {
                ndc[i] = 0;
                //double xi = 2*normRaw[i]-normRaw[i-1];
                double xi = raw[i];
                for (int j=0; j<raw.length; j++) {
                    //double xj = 2*normRaw[j]-normRaw[j-1];
                    double xj = raw[j];
                    double d = Math.abs(xi-xj);
                    ndc[i] += Math.exp(-alpha*d*d);
                }
            }

            // Max-Min normalize neighborhood density
            double ndcMax = Double.MIN_VALUE;
            double ndcMin = Double.MAX_VALUE;
            for (int i=0; i<ndc.length; i++) {
                if (ndcMax < ndc[i]) ndcMax = ndc[i];
                if (ndcMin > ndc[i]) ndcMin = ndc[i];
            }
            double ndcRange = ndcMax - ndcMin;
            for (int i=0; i<ndc.length; i++) {
                ndc[i] = (ndc[i] - ndcMin) / ndcRange;
            }

            //Discretize the neighborhood density curve
            int dSize = 100;
            double delta = rawRange / dSize;

            double dndcX[] = new double[dSize];
            double dndcXN[] = new double[dSize];
            double dndcY[] = new double[dSize];
            int dndcC[] = new int[dSize];
            for (int i=0; i<dSize; i++) {
                dndcX[i] = rawMin + delta/2 + i*delta;
                dndcXN[i] = (1/(double)dSize)/2 + (double)i/(double)dSize;
                dndcY[i] = 0;
                dndcC[i] = 0;
            }
            for (int i=0; i<ndc.length; i++) {
                int dcount = (int)((raw[i] - rawMin) / delta);
                if (dcount >= dSize) dcount = dSize-1;
                dndcY[dcount] += ndc[i];
                dndcC[dcount]++;
            }
            for (int i=0; i<dndcX.length; i++) {
                if (dndcC[i] > 0) {
                    dndcY[i] /= dndcC[i];
                }
            }

            /*------ Linearize the discrete neighborhood density curve ------ */
            TList vx = new TList();
            TList vy = new TList();
            TList dy = new TList();

            //Eliminate no-data points
            int i1 = 0;
            while (dndcC[i1] <= 0) i1++;
            vx.enList(Double.valueOf(dndcX[i1]));
            vy.enList(Double.valueOf(dndcY[i1]));
            dy.enList(Double.valueOf(0));
            for (int i2=i1+1; i2<dndcX.length; i2++) if (dndcC[i2] > 0) {
                vx.enList(Double.valueOf(dndcX[i2]));
                vy.enList(Double.valueOf(dndcY[i2]));
                double d = (dndcY[i2] - dndcY[i1]) / (dndcXN[i2] - dndcXN[i1]);
                dy.enList(Double.valueOf(d));
                i1 = i2;
            }

            //Linearize the has-data points
            FuzzyRule rule = new FuzzyRule();
            rule.setrMax(rawMax);
            rule.setrMin(rawMin);
            rule.add((Double)vx.get(0), (Double)vy.get(0));
            if (dy.len() > 1) {
                double thSlope = 1; // PI/4
                double slope1 = ((Double)dy.get(1)).doubleValue();
                for (int i=2; i<dy.len(); i++) {
                    double slope2 = ((Double)dy.get(i)).doubleValue();
                    double dSlope = Math.abs(slope2-slope1);
                    if (dSlope >= thSlope) {
                        rule.add((Double)vx.get(i-1), (Double)vy.get(i-1));
                        slope1 = slope2;
                    }
                }
                rule.add((Double)vx.get(vx.len()-1), (Double)vy.get(vy.len()-1));
            }

            return rule;
        }
    }

    private int nSamples = 288;
    private int sampleRate = 500;
    private long annoTimeThreshold = nSamples*sampleRate; //24 simulated hours
    private TList sensors = new TList();

    private long startTime, simTime;
    private boolean pause = true;
    private boolean stop = false;

    private Visualizer(){
        executorService.scheduleAtFixedRate(new AutoAnnotation(), 2000, 500, TimeUnit.MILLISECONDS);
    }

    public static Visualizer getInstance(){
        return instance;
    }

    private SensorData searchSensor(String ipv6Addr) {
        SensorData rs = null;
        int ind = 0;
        for (; ind<sensors.len(); ind++) {
            SensorData sd = (SensorData)sensors.get(ind);
            if (sd.ipv6Addr.equalsIgnoreCase(ipv6Addr)) {
                rs = sd;
                break;
            }
        }
        return rs;
    }

    public void updateDB(String ipv6Addr, String newFOI) {
        SensorData sd = searchSensor(ipv6Addr);
        if (sd != null)
            sd.FOI = newFOI;
        else
            sensors.enList(new SensorData(ipv6Addr, newFOI, nSamples, sampleRate));
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me){
        log.debug("Message received!");
        if(!(me.getMessage() instanceof HttpRequest)){
            ctx.sendUpstream(me);
            return;
        }

        //Process the HTTP request
        HttpRequest request = (HttpRequest) me.getMessage();

        //Send a Response
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        String payload = "";
        for (int i=0; i<sensors.len(); i++) {
            SensorData sd = (SensorData)sensors.get(i);
            String timeStamp = String.valueOf(sd.getLatestTS());
            String value = String.format("%.4f", sd.getLatestVL());
            String entry = sd.ipv6Addr+"|"+sd.FOI+"|"+timeStamp+"|"+value;
            payload += entry + "\n";
        }
        if (payload != "")
            payload = payload.substring(0, payload.length()-1);
        response.setContent(ChannelBuffers.copiedBuffer(payload.getBytes(Charset.forName("UTF-8"))));
        ChannelFuture future = Channels.write(ctx.getChannel(), response);
        future.addListener(ChannelFutureListener.CLOSE);
    }

//    public void startService() {
//        pause = false;
//        new Thread(this).start();
//    }

    public void stopService() {
        stop = true;
    }

    public synchronized void pauseService() {
        pause = true;
    }

    public synchronized void resumeService() {
        pause = false; notify();
    }





    private class FuzzyRule {
        private final ArrayList<Double> xList = new ArrayList<Double>();
        private final ArrayList<Double> yList = new ArrayList<Double>();
        private double rMax;
        private double rMin;

        /**
         * @return count of points in the rule.
         */
        public int size() {
            if (xList.size() != yList.size()) {
                throw new RuntimeException("X,Y length not consistent");
            }
            return xList.size();
        }

        /**
         * Add a point as the next point.
         * @param x - x value of the point.
         * @param y - y value of the point.
         */
        public void add(Double x, Double y) {
            xList.add(x);
            yList.add(y);
        }

        /**
         * Primitive version of {@link #add(Double, Double)}.
         */
        public void add(double x, double y) {
            xList.add(x);
            yList.add(y);
        }

        /**
         * Set rule max to the given value.
         * @param rMax - rule max value.
         */
        public void setrMax(double rMax) {
            this.rMax = rMax;
        }

        /**
         * @return rule max value.
         */
        public double getrMax() {
            return rMax;
        }

        /**
         * Set rule min to the given value.
         * @param rMin - rule min value.
         */
        public void setrMin(double rMin) {
            this.rMin = rMin;
        }

        /**
         * @return rule min value.
         */
        public double getrMin() {
            return rMin;
        }

        /**
         * @return list of x value of the points in original order.
         */
        public ArrayList<Double> getxList() {
            return xList;
        }

        /**
         * @return  list of y value of the points in original order.
         */
        public ArrayList<Double> getyList() {
            return yList;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("FuzzyRule [rMax=");
            builder.append(rMax);
            builder.append(", rMin=");
            builder.append(rMin);
            builder.append(", size=");
            builder.append(xList.size());
            builder.append("]");
            return builder.toString();
        }
    }

    private double calculateScore(List<Double> dataList, FuzzyRule rule, FuzzyRule ruleD) {
        if (rule.size() < 2) {
            throw new RuntimeException("Rule size too small");
        }
        if (ruleD.size() < 2) {
            throw new RuntimeException("Rule derivative size too small");
        }

        double rawMax = Collections.max(dataList);
        double rawMin = Collections.min(dataList);

        ArrayList<Double> xList = rule.getxList();
        ArrayList<Double> yList = rule.getyList();
        ArrayList<Double> xListD = ruleD.getxList();
        ArrayList<Double> yListD = ruleD.getyList();
        double ruleRMax = rule.getrMax();
        double ruleRMin = rule.getrMin();

        double us = dataList.size()-1;
        double drange = Math.abs(rawMin-ruleRMin) + Math.abs(rawMax-ruleRMax);

        double sc = 0;
        for (int i = 0; i < dataList.size()-1; i++) {
            double dataValue = dataList.get(i);
            double tmp = dataList.get(i+1);
            double deriValue = tmp - dataValue;
            double scv = 0;
            double scd = 0;

            //Calculate score for value fuzzy set
            int p1 = Collections.binarySearch(xList, dataValue);
            int p2 = 0;
            if (p1 >= 0 ) {
                // found
                scv = yList.get(p1);
            } else {
                // not found
                p1 = -p1 - 1;
                if (p1 == 0) {
                    // smaller than min
                } else if (p1 == rule.size()) {
                    // bigger than max
//					p1 = p1 - 2;
                } else {
                    // data value between rule range
                    p1--;
                    p2 = p1 + 1;
                    double x1 = xList.get(p1);
                    double x2 = xList.get(p2);
                    double y1 = yList.get(p1);
                    double y2 = yList.get(p2);
                    scv = (x1*y2-y1*x2)/(x1-x2) + (y2-y1)/(x2-x1)*dataValue;
                }
            }

            //Calculate score for derivative fuzzy set
            p1 = Collections.binarySearch(xListD, deriValue);
            p2 = 0;
            if (p1 >= 0 ) {
                // found
                scd = yListD.get(p1);
            } else {
                // not found
                p1 = -p1 - 1;
                if (p1 == 0) {
                    // smaller than min
                } else if (p1 == ruleD.size()) {
                    // bigger than max
//					p1 = p1 - 2;
                } else {
                    // data value between rule range
                    p1--;
                    p2 = p1 + 1;
                    double x1 = xListD.get(p1);
                    double x2 = xListD.get(p2);
                    double y1 = yListD.get(p1);
                    double y2 = yListD.get(p2);
                    scd = (x1*y2-y1*x2)/(x1-x2) + (y2-y1)/(x2-x1)*deriValue;
                }
            }

            //Fuzzy rule's "and"-operator
            sc += scv*scd;
        }
        sc /= drange*us*us;
        return sc;
    }
}