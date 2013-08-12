package uk.ac.manchester.cs.owl.experimentbench;

import java.util.*;

/**
 * Author: Matthew Horridge<br>
 * The University of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 27/02/2011
 */
public class CommandLineArgs {

    private String[] args;

        private Map<String, List<String>> argsMap = new LinkedHashMap<String, List<String>>();

        public CommandLineArgs(String[] args) {
            this.args = args;
            processCommandLineArguments();
        }

        private void processCommandLineArguments() {
            List<String> currentSwitchArgs = new ArrayList<String>();
            for (String s : args) {
                s = s.trim();
                if (s.startsWith("-")) {
                    // Switch
                    currentSwitchArgs = new ArrayList<String>();
                    argsMap.put(s.substring(1), currentSwitchArgs);
                }
                else {
                    currentSwitchArgs.add(unescape(s));
                }
            }
        }

        private String unescape(String s) {
            if (s.startsWith("\"") && s.endsWith("\"") && s.length() > 2) {
                return s.substring(1, s.length() - 1);
            }
            else {
                return s;
            }
        }

        public List<String> getArguments(String sw) {
            List<String> args = argsMap.get(sw);
            if (args == null) {
                return Collections.emptyList();
            }
            else {
                return args;
            }
        }

        public String getFirstArgument(String sw, String defaultValue) {
            List<String> args = getArguments(sw);
            if (args.isEmpty()) {
                return defaultValue;
            }
            else {
                return args.get(0);
            }
        }

        public void dump() {
            System.out.println("----------------------------------------------");
            System.out.println("COMMAND LINE ARGUMENTS");
            System.out.println("----------------------------------------------");
            for(String sw : argsMap.keySet()) {
                System.out.println(sw);
                for(String arg : argsMap.get(sw)) {
                    System.out.println("\t" + arg);
                }
                System.out.println("----------------------------------------------");
            }
        }

    public static void main(String[] args) {
        CommandLineArgs cmd = new CommandLineArgs(args);
        cmd.dump();
        System.out.println("Ontologies: " + cmd.getFirstArgument("ontologies", "ontologies"));
    }

}
