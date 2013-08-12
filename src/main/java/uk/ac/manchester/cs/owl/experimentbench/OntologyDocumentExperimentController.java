package uk.ac.manchester.cs.owl.experimentbench;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 06/02/2011
 */
public class OntologyDocumentExperimentController {

    private static final String EXPERIMENT_CLASS_NAME_COMMAND_LINE_SWITCH = "class";

    private static final String ONTOLOGIES_DIRECTORY_NAME_COMMAND_LINE_SWITCH = "ontologies";

    private static final String OUTPUT_DIRECTORY_NAME_COMMAND_LINE_SWITCH = "out";

    private static final String EXPERIMENT_PROPERTIES_FILE_NAME_COMMAND_LINE_SWITCH = "properties";


    private static final String ONTOLOGIES_DIRECTORY_NAME = "ontologies";

    private static final String OUTPUT_DIRECTORY_NAME = "telemetry-out";

    private static final String STOP_LIST_FILE_NAME = "stoplist.txt";

    private static final String EXPERIMENT_PROPERTIES_FILE_NAME = "experiment.properties";

    private Class experimentClass;

    private File ontologiesDirectory;

    private File rootOutputDirectory;

    private static final String MAX_MEMORY = "4000M";

    private long experimentTimeOut;

    private Set<String> stopList = new HashSet<String>();

    public OntologyDocumentExperimentController(Class experimentClass) {
        this(experimentClass, new File(EXPERIMENT_PROPERTIES_FILE_NAME), new File(ONTOLOGIES_DIRECTORY_NAME), new File(OUTPUT_DIRECTORY_NAME));
    }

    public OntologyDocumentExperimentController(Class experimentClass, File experiementPropertiesFile, File ontologiesDirectory, File rootOutputDirectory) {
        this.experimentClass = experimentClass;
        this.ontologiesDirectory = ontologiesDirectory;
        this.rootOutputDirectory = rootOutputDirectory;
        System.out.println("////////////////////////////////////////////////////////////////////");
        System.out.println("//  RUN EXPERIMENT:");
        System.out.println("////////////////////////////////////////////////////////////////////");
        System.out.println("Experiment class: " + experimentClass.getName());
        System.out.println("Experiment properties: " + experiementPropertiesFile.getAbsolutePath());
        System.out.println("Ontologies directory: " + ontologiesDirectory);
        System.out.println("Output directory: " + rootOutputDirectory);
        loadStopList();
        ExperimentProperties properties = new ExperimentProperties(experiementPropertiesFile);
        experimentTimeOut = properties.getOntologyDocumentExperimentTimeOut();
        System.out.println("Experiment time out (ms): " + experimentTimeOut);
        System.out.println("--------------------------------------------------------------");
        System.out.println("STANDARD PROPERTIES");
        System.out.println("--------------------------------------------------------------");
        System.out.println("Ontology Document Experiment Time Out (ms): " + experimentTimeOut);
        System.out.println("Reasoner Entailment Check Time Out (ms): " + properties.getReasonerEntailmentCheckTimeOutInMilliSeconds());
        System.out.println("--------------------------------------------------------------");
        System.out.println();
        System.out.println();
        System.out.println("--------------------------------------------------------------");
        System.out.println("SET PROPERTIES");
        System.out.println("--------------------------------------------------------------");
        for (String propertyName : properties.getProperties()) {
            String propertyValue = properties.getProperty(propertyName, null);
            if (propertyValue != null) {
                System.out.println(propertyName + ": " + properties.getProperty(propertyName, ""));
            }
        }
        System.out.println("--------------------------------------------------------------");
        System.out.println();
        System.out.println();
    }

    private void loadStopList() {
        File stopListFile = new File(STOP_LIST_FILE_NAME);
        if (stopListFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(stopListFile));
                String line;
                while ((line = br.readLine()) != null) {
                    String trimmedLine = line.trim();
                    stopList.add(trimmedLine);
                }
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void runExperiments() {
        if (!ontologiesDirectory.exists()) {
            System.out.println("ontologies directory does not exists");
            System.exit(1);
        }
        File[] experimentPackagesDirectories = ontologiesDirectory.listFiles();
        if (experimentPackagesDirectories != null) {
            List<File> fileList = Arrays.asList(experimentPackagesDirectories);
            for (File experimentPackageDirectory : fileList) {
                if (!experimentPackageDirectory.isHidden() && !stopList.contains(experimentPackageDirectory.getName())) {
                    processOntologyDocumentDirectory(experimentPackageDirectory);
                }
            }
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        CommandLineArgs commandLineArgs = new CommandLineArgs(args);
        if (commandLineArgs.getFirstArgument("help", null) != null) {
            System.out.println("Runs an experiment on an ontology");
            System.out.println("Arguments:");
            printUsage();
            System.out.println();
            System.exit(0);
        }

        String experimentClassName = commandLineArgs.getFirstArgument(EXPERIMENT_CLASS_NAME_COMMAND_LINE_SWITCH, null);
        if (experimentClassName == null) {
            System.out.println("Missing experiment class argument.  Expected -" + EXPERIMENT_CLASS_NAME_COMMAND_LINE_SWITCH + " argument.");
            printUsage();
            System.exit(1);
        }

        String propertiesFileName = commandLineArgs.getFirstArgument(EXPERIMENT_PROPERTIES_FILE_NAME_COMMAND_LINE_SWITCH, EXPERIMENT_PROPERTIES_FILE_NAME);

        String ontologiesDirectoryName = commandLineArgs.getFirstArgument(ONTOLOGIES_DIRECTORY_NAME_COMMAND_LINE_SWITCH, ONTOLOGIES_DIRECTORY_NAME);

        String outputDirectoryName = commandLineArgs.getFirstArgument(OUTPUT_DIRECTORY_NAME_COMMAND_LINE_SWITCH, OUTPUT_DIRECTORY_NAME);


        Class experimentClass = Class.forName(experimentClassName);
        OntologyDocumentExperimentController controller = new OntologyDocumentExperimentController(experimentClass, new File(propertiesFileName), new File(ontologiesDirectoryName), new File(outputDirectoryName));
        controller.runExperiments();
    }

    private static void printUsage() {
        System.out.println("\t-" + EXPERIMENT_CLASS_NAME_COMMAND_LINE_SWITCH + " (required) The name of the experiment class.");
        System.out.println("\t-" + ONTOLOGIES_DIRECTORY_NAME_COMMAND_LINE_SWITCH + " (optional default=ontologies) A directory containing directories that contain ontologies. Each ontology directory must contain a single ontology file with an extension .owl.xml");
        System.out.println("\t-" + OUTPUT_DIRECTORY_NAME_COMMAND_LINE_SWITCH + " (optional default=" + OUTPUT_DIRECTORY_NAME + ") The directory where telemetry info will be output to.");
    }

    private void processOntologyDocumentDirectory(File ontologyDocumentDirectory) {
        for (Method method : experimentClass.getMethods()) {
            if (method.getAnnotation(Experiment.class) != null) {
                if (method.getParameterTypes().length == 0) {
                    launch(ontologyDocumentDirectory, method);
                }
            }

        }
    }


    private void launch(File ontologyDocumentDirectory, Method experimentMethod) {
        Project project = new Project();
        File workingDirectory = new File(System.getProperty("user.dir"));
        project.setBaseDir(workingDirectory);
        project.init();

        DefaultLogger logger = new DefaultLogger();
        project.addBuildListener(logger);
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        logger.setMessageOutputLevel(Project.MSG_INFO);




        project.log("Running " + experimentClass.getSimpleName() + " on " + ontologyDocumentDirectory);
        project.fireBuildStarted();

        Throwable caught = null;
        try {
            Java javaTask = new Java();
            javaTask.setTaskName(experimentClass.getSimpleName() + " - " + ontologyDocumentDirectory.getName());
            javaTask.setProject(project);
            javaTask.setFork(true);
            javaTask.setFailonerror(true);
            if (experimentTimeOut != Long.MAX_VALUE) {
                javaTask.setTimeout(experimentTimeOut);
            }
            javaTask.setMaxmemory(MAX_MEMORY);
            javaTask.setClassname(OntologyDocumentExperimentRunner.class.getName());

            String classPath = System.getProperty("java.class.path");
            Path path = new Path(project, classPath);
            javaTask.setClasspath(path);


            Environment.Variable var = new Environment.Variable();
            var.setKey("java.library.path");
            String libraryPath = System.getProperty("java.library.path");
            var.setValue(libraryPath);

            javaTask.addSysproperty(var);

            // Arguments
            // (1) Experiment class
            // (2) Experiment method
            // (3) Ontology document directory
            // (4) Experiment output directory
            Commandline.Argument experimentClassArgument = javaTask.createArg();
            experimentClassArgument.setValue(experimentClass.getName());

            Commandline.Argument experimentMethodArgument = javaTask.createArg();
            experimentMethodArgument.setValue(experimentMethod.getName());

            Commandline.Argument ontologyDocumentDirectoryArgument = javaTask.createArg();
            ontologyDocumentDirectoryArgument.setFile(ontologyDocumentDirectory);


            File ontologyDocumentOutputDirectory = new File(rootOutputDirectory, ontologyDocumentDirectory.getName());
            String experimentDirectoryName = getExperimentDirectoryName(experimentMethod);
            File experimentOutputDirectory = new File(ontologyDocumentOutputDirectory, experimentDirectoryName);
            experimentOutputDirectory.mkdirs();

            Commandline.Argument outputDirectoryArgument = javaTask.createArg();
            outputDirectoryArgument.setFile(experimentOutputDirectory);


            // Log
            File outFile = new File(experimentOutputDirectory, "out.txt");
            PrintStream outPrintStream = null;
            PrintStream errorPrintStream = null;
            if (!outFile.exists()) {
                DefaultLogger persistentLogger = new DefaultLogger();
                project.addBuildListener(persistentLogger);
                outPrintStream = new PrintStream(outFile);
                persistentLogger.setOutputPrintStream(outPrintStream);
                File errFile = new File(experimentOutputDirectory, "err.txt");
                errorPrintStream = new PrintStream(errFile);
                persistentLogger.setErrorPrintStream(errorPrintStream);
                persistentLogger.setMessageOutputLevel(Project.MSG_INFO);
            }


            javaTask.init();
            int returnCode = javaTask.executeJava();

            if (outPrintStream != null) {
                outPrintStream.close();
            }
            if (errorPrintStream != null) {
                errorPrintStream.close();
            }


        }
        catch (Throwable t) {
            caught = t;
        }
        project.log("Finished running experiment");
        project.fireBuildFinished(caught);
    }

    private String getExperimentDirectoryName(Method experimentMethod) {
        String experimentDirectoryName = null;
        Experiment experimentAnnotation = experimentMethod.getAnnotation(Experiment.class);
        if (experimentAnnotation != null) {
            if (!experimentAnnotation.name().isEmpty()) {
                experimentDirectoryName = experimentAnnotation.name();
            }
        }
        if (experimentDirectoryName == null) {
            experimentDirectoryName = experimentMethod.getName();
        }
        return experimentDirectoryName;
    }

}
