package uk.ac.manchester.cs.owl.experimentbench;


import org.semanticweb.owl.explanation.telemetry.DefaultTelemetryInfo;
import org.semanticweb.owl.explanation.telemetry.TelemetryTransmitter;
import org.semanticweb.owl.explanation.telemetry.XMLTelemetryReceiver;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.model.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 06/02/2011
 */
public class OntologyDocumentExperimentRunner {

    public static final String PROPERTIES_FILE_NAME = "experiment.properties";

    public static final String ONTOLOGY_FILE_NAME_EXTENSION = ".owl.xml";

    private Class experiementClass;

    private Method experimentMethod;

    private File ontologyDirectory;

    private File outputDirectory;

    private File ontologyDocument;


    public OntologyDocumentExperimentRunner(Class<?> experiementClass, Method experimentMethod, File ontologyDirectory, File outputDirectory) {
        Annotation annotation = experiementClass.getAnnotation(OntologyDocumentExperiment.class);
        if(annotation == null) {
            throw new RuntimeException("Experiment class is not annotated with OntologyDocumentExperiment annotation");
        }
        this.experiementClass = experiementClass;
        this.experimentMethod = experimentMethod;
        this.ontologyDirectory = ontologyDirectory;
        this.outputDirectory = outputDirectory;
        findOntologyDocument();
//        setupTelemetry();
    }

    private void findOntologyDocument() {
        File[] files = ontologyDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().endsWith(ONTOLOGY_FILE_NAME_EXTENSION)) {
                    ontologyDocument = file;
                    return;
                }
            }
        }
        throw new RuntimeException("Ontology Document Not Found in " + ontologyDirectory);
    }

    private void setupTelemetry() {
        File telemetryFile = getTelemetryFile();
//        for(int i = 1; ; i++) {
//            String candidateName = ontologyDirectory.getName() + "-" + experiementClass.getSimpleName().toLowerCase() + "-telemetry-" + i + ".xml";
//            telemetryFile = new File(outputDirectory, candidateName);
//            if(!telemetryFile.exists()) {
//                break;
//            }
//        }
        XMLTelemetryReceiver telemetryReceiver = new XMLTelemetryReceiver(telemetryFile);
        Set<String> ignore = getIgnoreTelemetry();
        for(String ignoreName : ignore) {
            telemetryReceiver.addIgnoreName(ignoreName);
        }
        TelemetryTransmitter.getTransmitter().setTelemetryReceiver(telemetryReceiver);
//        recordSystemProperties();
                    
    }

    private File getTelemetryFile() {
        return new File(outputDirectory, "telemetry.xml");
    }

    private ExperimentProperties loadProperties() {
        ExperimentProperties experimentProperties = new ExperimentProperties();
        // Working directory
        File workingDirectory = new File(".");
        addProperties(experimentProperties, workingDirectory);
        // Parent
        File ontologyDirectoryParentFile = ontologyDirectory.getParentFile();
        addProperties(experimentProperties, ontologyDirectoryParentFile);
        // Experiment
        addProperties(experimentProperties, ontologyDirectory);
        return experimentProperties;
    }

    private void recordSystemProperties() {
        TelemetryTransmitter transmitter = TelemetryTransmitter.getTransmitter();
        DefaultTelemetryInfo info = new DefaultTelemetryInfo("systemproperties");
        try {
            transmitter.beginTransmission(info);
            Properties properties = System.getProperties();
            for(String propertyName : properties.stringPropertyNames()) {
                String value = properties.getProperty(propertyName, null);
                if(value != null) {
                    transmitter.recordMeasurement(info, propertyName, value);
                }
            }
        }
        finally {
            transmitter.endTransmission(info);
        }
    }

    private void recordProperties(ExperimentProperties properties) {
        TelemetryTransmitter transmitter = TelemetryTransmitter.getTransmitter();
        DefaultTelemetryInfo info = new DefaultTelemetryInfo("properties");
        try {
            transmitter.beginTransmission(info);
            for(String propertyName : properties.getProperties()) {
                String value = properties.getProperty(propertyName, null);
                if(value != null) {
                    transmitter.recordMeasurement(info, propertyName, value);
                }
            }
        }
        finally {
            transmitter.endTransmission(info);
        }

    }

    private void addProperties(ExperimentProperties experimentProperties, File experimentDirectoryParent) {
        if (experimentDirectoryParent.exists()) {
            File propertiesFile = new File(experimentDirectoryParent, PROPERTIES_FILE_NAME);
            if (propertiesFile.exists()) {
                experimentProperties.addProperties(propertiesFile);
            }
        }
    }

    public void run() {
        File telemetryFile = getTelemetryFile();
        if(telemetryFile.exists()) {
            System.out.println("TELEMETRY ALREADY EXISTS.  NOT RUNNING EXPERIMENT. (" + telemetryFile.getAbsolutePath() + ")");
            return;
        }
        setupTelemetry();
        TelemetryTransmitter transmitter = TelemetryTransmitter.getTransmitter();
        DefaultTelemetryInfo rootInfo = new DefaultTelemetryInfo("ontologydocument");
        try {
            transmitter.beginTransmission(rootInfo);
            ExperimentProperties properties = loadProperties();
            System.out.println("Running experiment with properties:");
            for(String property : properties.getProperties()) {
                System.out.print("\t[Property] ");
                System.out.print(property);
                System.out.print(": ");
                System.out.println(properties.getProperty(property, ""));
            }
            recordProperties(properties);
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntologyLoaderConfiguration config = new OWLOntologyLoaderConfiguration().setLoadAnnotationAxioms(false);
            FileDocumentSource documentSource = new FileDocumentSource(ontologyDocument);
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(documentSource, config);
            Constructor constructor = experiementClass.getConstructor(ExperimentProperties.class, File.class, OWLOntology.class, File.class);
            Object object = constructor.newInstance(properties, ontologyDocument, ontology, outputDirectory);
            invokeExperimentMethods(object);
        }
        catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                transmitter.recordException(rootInfo, cause);
            }
            else {
                transmitter.recordException(rootInfo, cause);
            }
        }
        catch (Throwable t) {
            transmitter.recordException(rootInfo, t);
        }
        finally {
            transmitter.endTransmission(rootInfo);
        }
    }

    private void invokeExperimentMethods(Object object) throws IllegalAccessException, InvocationTargetException {
        experimentMethod.invoke(object);
    }

    private Set<String> getIgnoreTelemetry() {
        try {
            Set<String> result = new HashSet<String>();
            for (Method method : experiementClass.getMethods()) {
                if (method.getAnnotation(IgnoreTelemetryTransmissions.class) != null) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == 0 && method.getReturnType().equals(Set.class)) {
                        Set<String> ret = (Set<String>) method.invoke(experiementClass);
                        result.addAll(ret);
                    }
                }
            }
            return result;
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, IOException, NoSuchMethodException, OWLOntologyCreationException {
        try {
            if (args.length != 4) {
                throw new RuntimeException("Expected 4 arguments:  Argument 1 should be the experiment class name. Argument 3 should be the experiment method name.  Argument 2 should be a directory containing an experiment package.  Argument 3 should be the output directory for the package.");
            }
            String className = args[0];
            Class experimentClass = Class.forName(className);

            String methodName = args[1];
            Method method = experimentClass.getMethod(methodName);

            String ontologyDirectoryName = args[2];
            File experimentDirectory = new File(ontologyDirectoryName);
            if (!experimentDirectory.exists()) {
                throw new RuntimeException("Experiment directory does not exist (" + ontologyDirectoryName + ")");
            }
            String outputDirectoryName = args[3];
            File outputDirectory = new File(outputDirectoryName);
            outputDirectory.mkdirs();
            OntologyDocumentExperimentRunner runner = new OntologyDocumentExperimentRunner(experimentClass, method, experimentDirectory, outputDirectory);
            runner.run();
        }
        catch (Throwable t) {
            System.exit(-1);
        }
    }
}
