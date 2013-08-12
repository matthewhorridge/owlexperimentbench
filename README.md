owlexperimentbench
==================

A framework for performing experiments on OWL ontologies using the OWL API

Experiments are annotated with the @Experiment annotation

To run an experiment run Java with the main class:

java -Xmx1000M -jar experiments.jar -class explanation.experiments.ComputeJustifications -ontologies <PathToOntologiesDirectory> -out <PathToOutputDirectory>

The ontologies directory is a directory of directories.  Each sub-directory contains ONE ontology that must have a
.owl.xml extension.