package com.github.hermannpencole.nifi.config;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.service.*;
import com.github.hermannpencole.nifi.swagger.ApiClient;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.Configuration;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main Class
 * Created by SFRJ on 01/04/2017.
 */
public class Main {

    /**
     * The logger.
     */
    private final static Logger LOG = LoggerFactory.getLogger(Main.class);

    private final static String version = Main.class.getPackage().getImplementationVersion();

    /**
     * Print to the console the usage.
     *
     * @param options the options
     */
    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar nifi-deploy-config-" + version +".jar [OPTIONS]", options);
    }

    /**
     * The main method.
     *
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {
        try {
            // Command line args parsing
            CommandLineParser commandLineParser = new DefaultParser();
            Options options = new Options();
            options.addOption("h", "help", false, "Usage description");
            options.addOption("n", "nifi", true, "mandatory : Nifi http (ex : http://localhost:8080/nifi-api)");
            options.addOption("b", "branch", true, "branch to begin (must begin by root) : root > my processor > my proce2 (default root)");
            options.addOption("c", "conf", true, "adresse configuration file mandatory with mode (updateConfig/extractConfig/deployTemplate)");
            options.addOption("m", "mode", true, "mandatory : updateConfig/extractConfig/deployTemplate/undeploy");
            options.addOption("user", true, "user name for access via username/password, then password is mandatory");
            options.addOption("password", true, "password for access via username/password, then user is mandatory");
            options.addOption("f", "force", false, "turn on force mode : empty queue after timeout");
            options.addOption("timeout", true, "allows specifying the polling timeout in second (defaut 120 seconds); negative values indicate no timeout");
            options.addOption("interval", true, "allows specifying the polling interval in second (default 2 seconds)");
            options.addOption("accessFromTicket", false, "Access via Kerberos ticket exchange / SPNEGO negotiation");
            options.addOption("noVerifySsl", false, "turn off ssl verification certificat");
            options.addOption("noStartProcessors", false, "turn off auto start of the processors after update of the config");
            options.addOption("enableDebugMode", false, "turn on debug mode");

            // parse the command line arguments
            CommandLine cmd = commandLineParser.parse(options, args);
            if (cmd.hasOption("h")) {
                printUsage(options);
                System.exit(1);
            } else if (!cmd.hasOption("n") || (!cmd.hasOption("c") && cmd.hasOption("m") && !cmd.getOptionValue("m").equals("undeploy") )) {
                printUsage(options);
                System.exit(1);
            } else if (!"updateConfig".equals(cmd.getOptionValue("m")) && !"extractConfig".equals(cmd.getOptionValue("m"))
                    && !"deployTemplate".equals(cmd.getOptionValue("m")) && !"undeploy".equals(cmd.getOptionValue("m")) ) {
                printUsage(options);
                System.exit(1);
            } else if ( (cmd.hasOption("user") && !cmd.hasOption("password")) || (cmd.hasOption("password") && !cmd.hasOption("user")) ) {
                printUsage(options);
                System.exit(1);
            } else {
                //configure options
                Integer timeout = cmd.hasOption("timeout") ? Integer.valueOf(cmd.getOptionValue("timeout")) :120;
                Integer interval = cmd.hasOption("interval") ? Integer.valueOf(cmd.getOptionValue("interval")) :2;
                Boolean forceMode = cmd.hasOption("force");

                LOG.info(String.format("Starting config_nifi %s on mode %s", version, cmd.getOptionValue("m")) );
                String addressNifi = cmd.getOptionValue("n");
                String fileConfiguration = cmd.getOptionValue("c");

                String branch = "root";
                if (cmd.hasOption("b")) {
                    branch = cmd.getOptionValue("b");
                }
                List<String> branchList = Arrays.stream(branch.split(">")).map(String::trim).collect(Collectors.toList());
                if (!branchList.get(0).equals("root")) {
                    throw new ConfigException("The branch address must begin with the element 'root' ( sample : root > branch > sub-branch)");
                }

                setConfiguration(addressNifi, !cmd.hasOption("noVerifySsl"), cmd.hasOption("enableDebugMode"));
                Injector injector = Guice.createInjector(new AbstractModule() {
                    protected void configure() {
                        bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(timeout);
                        bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(interval);
                        bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(forceMode);
                    }
                });

                //start
                AccessService accessService = injector.getInstance(AccessService.class);
                accessService.addTokenOnConfiguration(cmd.hasOption("accessFromTicket"), cmd.getOptionValue("user"), cmd.getOptionValue("password"));

                InformationService infoService = injector.getInstance(InformationService.class);
                String nifiVersion =  infoService.getVersion();
                LOG.info(String.format("Communicate with nifi %s", nifiVersion));

                if ("updateConfig".equals(cmd.getOptionValue("m"))) {
                    //Get an instance of the bean from the context
                    UpdateProcessorService processorService = injector.getInstance(UpdateProcessorService.class);
                    processorService.updateByBranch(branchList, fileConfiguration, cmd.hasOption("noStartProcessors"));
                    LOG.info("The group configuration {} is updated with the file {}.", branch, fileConfiguration);
                } else if ("extractConfig".equals(cmd.getOptionValue("m"))) {
                    //Get an instance of the bean from the context
                    ExtractProcessorService processorService = injector.getInstance(ExtractProcessorService.class);
                    processorService.extractByBranch(branchList, fileConfiguration);
                    LOG.info("The group configuration {} is extrated on file {}", branch, fileConfiguration);
                } else if ("deployTemplate".equals(cmd.getOptionValue("m"))) {
                    TemplateService templateService = injector.getInstance(TemplateService.class);
                    templateService.installOnBranch(branchList, fileConfiguration);
                    LOG.info("Template {} is installed on the group {}", fileConfiguration, branch);
                } else {
                    TemplateService templateService = injector.getInstance(TemplateService.class);
                    templateService.undeploy(branchList);
                    LOG.info("The group {} is deleted", branch);
                }
            }
        } catch (ApiException e) {
            LOG.error(e.getMessage(), e);
            throw new ConfigException(e.getMessage() + ": " + e.getResponseBody(), e);
        }
    }


    public static void setConfiguration(String basePath, boolean verifySsl, boolean debugging) throws ApiException {
        ApiClient client = new ApiClient().setBasePath(basePath).setVerifyingSsl(verifySsl);
        client.setDebugging(debugging);
        Configuration.setDefaultApiClient(client);
    }
}
