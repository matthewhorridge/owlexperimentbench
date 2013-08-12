package uk.ac.manchester.cs.owl.experimentbench;

import org.semanticweb.owlapi.model.OWLOntology;

import java.io.BufferedOutputStream;
import java.io.File;

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 27/02/2011
 */
public class AbstractOntologyDocumentExperiment {

    private ExperimentProperties experimentProperties;

    private File ontologyDocument;

    private OWLOntology ontology;

    private File outputDirectory;

    public AbstractOntologyDocumentExperiment(ExperimentProperties experimentProperties, File ontologyDocument, OWLOntology ontology, File outputDirectory) {
        this.experimentProperties = experimentProperties;
        this.ontologyDocument = ontologyDocument;
        this.ontology = ontology;
        this.outputDirectory = outputDirectory;
    }

    public ExperimentProperties getExperimentProperties() {
        return experimentProperties;
    }

    public File getOntologyDocument() {
        return ontologyDocument;
    }

    public OWLOntology getOntology() {
        return ontology;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public File getOutputFile(String fileName) {
        return new File(outputDirectory, fileName);
    }

}
