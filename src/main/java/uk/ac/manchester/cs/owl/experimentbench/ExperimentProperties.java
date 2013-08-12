package uk.ac.manchester.cs.owl.experimentbench;

import org.semanticweb.owlapi.reasoner.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 06/02/2011
 */
public class ExperimentProperties {

    public static final String REASONER_FACTORY_CLASS_PROPERTY = "reasonerfactoryclass";

    public static final String REASONER_ENTAILMENT_CHECK_TIMEOUT_IN_MS_PROPERTY = "reasonerentailmentchecktimeoutms";

    public static final String EXPERIMENT_TIME_OUT_IN_MS_PROPERTY_NAME = "experimenttimeoutms";


    public static final long DEFAULT_REASONER_ENTAILMENT_CHECK_TIMEOUT_IN_MS = Long.MAX_VALUE;

    public static final long DEFAULT_EXPERIMENT_TIME_OUT_MS = Long.MAX_VALUE;

    private Properties properties = new Properties();


    public ExperimentProperties() {
    }

    public ExperimentProperties(File propertiesFile) {
        addProperties(propertiesFile);
    }

    public void setProperty(String property, String value) {
        properties.setProperty(property, value);
    }

    public void addProperties(File propertiesFile) {
        if (propertiesFile.exists()) {
            try {
                properties.load(new BufferedInputStream(new FileInputStream(propertiesFile)));
            }
            catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
        else {
            System.err.println("Experiment properties file not found: " + propertiesFile);
        }
    }

    public String getProperty(String propertyName, String defaultValue) {
        String value = properties.getProperty(propertyName);
        if (value == null) {
            return defaultValue;
        }
        else {
            return value;
        }
    }

    public long getLongProperty(String propertyName, long defaultValue) {
        String value = properties.getProperty(propertyName);
        if (value == null) {
            return defaultValue;
        }
        else {
            try {
                return Long.parseLong(value);
            }
            catch (NumberFormatException e) {
                System.err.println(e.getMessage());
                return defaultValue;
            }

        }
    }

    public boolean getBooleanProperty(String propertyName, boolean defaultValue) {
        String value = properties.getProperty(propertyName);
        if(value == null) {
            return defaultValue;
        }
        else {
            if(value.toLowerCase().equals("true")) {
                return true;
            }
            else if(value.toLowerCase().equals("false")) {
                return false;
            }
            else {
                return defaultValue;
            }
        }
    }

    public int getIntProperty(String propertyName, int defaultValue) {
        String value = properties.getProperty(propertyName);
        if (value == null) {
            return defaultValue;
        }
        else {
            try {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException e) {
                System.err.println(e.getMessage());
                return defaultValue;
            }

        }
    }

    public OWLReasonerFactory getReasonerFactory(OWLReasonerFactory defaultReasonerFactory) {
        return getObjectFromClass(REASONER_FACTORY_CLASS_PROPERTY, defaultReasonerFactory);
    }


    public OWLReasonerConfiguration getReasonerConfiguration() {
        return getReasonerConfiguration(new NullReasonerProgressMonitor());
    }

    public OWLReasonerConfiguration getReasonerConfiguration(ReasonerProgressMonitor progressMonitor) {
        long timeout = getReasonerEntailmentCheckTimeOutInMilliSeconds();
        return new SimpleConfiguration(progressMonitor, timeout);
    }


    public <T> T getObjectFromClass(String classNameProperty, T defaultInstance) {
        String className = properties.getProperty(classNameProperty);
        if (className != null) {
            try {
                Class<?> cls = Class.forName(className);
                return (T) cls.newInstance();
            }
            catch (InstantiationException e) {
                e.printStackTrace();
            }
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
        // Fall back to default
        return defaultInstance;
    }

    @Override
    public String toString() {
        return properties.toString();
    }

    public Set<String> getProperties() {
        Set<String> result = new LinkedHashSet<String>();
        for (Object key : properties.keySet()) {
            result.add(key.toString());
        }
        return result;
    }

    public long getReasonerEntailmentCheckTimeOutInMilliSeconds() {
        return getLongProperty(REASONER_ENTAILMENT_CHECK_TIMEOUT_IN_MS_PROPERTY, DEFAULT_REASONER_ENTAILMENT_CHECK_TIMEOUT_IN_MS);
    }

    public long getOntologyDocumentExperimentTimeOut() {
        return getLongProperty(EXPERIMENT_TIME_OUT_IN_MS_PROPERTY_NAME, DEFAULT_EXPERIMENT_TIME_OUT_MS);
    }
}

