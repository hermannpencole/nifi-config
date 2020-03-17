package com.github.hermannpencole.nifi.config;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.service.*;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.client.model.PositionDTO;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
    public static final int DEFAULT_TIMEOUT = 120;
    public static final int DEFAULT_INTERVAL = 2;
    public static final int DEFAULT_CONNECTIONTIMEOUT = 10000;
    public static final int DEFAULT_READTIMEOUT = 10000;
    public static final int DEFAULT_WRITETIMEOUT = 10000;
    public static final double DEFAULT_PLACEWIDTH = 1935d;
    public static final String DEFAULT_PLACE = "0,0";
    public static final String ENV_NIFI_PASSWORD = "nifi_password";

    /**
     * Print to the console the usage.
     *
     * @param options the options
     */
    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar nifi-deploy-config-" + version + ".jar [OPTIONS]", options);
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
            options.addOption("b", "branch", true, "Target process group (must begin by root) : root > my processor > my proce2 (default root)");
            options.addOption("m", "mode", true, "Mandatory, possible values : updateConfig/extractConfig/deployTemplate/undeploy");
            options.addOption("c", "conf", true, "Mandatory if mode in [updateConfig, extractConfig, deployTemplate]  : configuration file");
            options.addOption("n", "nifi", true, "Mandatory : Nifi URL (ex : http://localhost:8080/nifi-api)");
            options.addOption("user", true, "User name for access via username/password.");
            options.addOption("password", true, "Password for access via username/password. If present, user is mandatory");
            options.addOption("f", "force", false, "Turn on force mode : empty queue after timeout");
            options.addOption("timeout", true, "Allow specifying the polling timeout in second (defaut 120 seconds); negative value indicates no timeout");
            options.addOption("interval", true, "Allow specifying the polling interval in second (default 2 seconds)");
            options.addOption("accessFromTicket", false, "Access via Kerberos ticket exchange / SPNEGO negotiation");
            options.addOption("noVerifySsl", false, "Turn off ssl verification certificat");
            options.addOption("noStartProcessors", false, "Turn off auto start of the processors after update of the config");
            options.addOption("enableDebugMode", false, "Turn on debug mode");
            options.addOption("connectionTimeout", true, "Configure api client connection timeout (default 10 seconds)");
            options.addOption("readTimeout", true, "configure api client read timeout (default 10 seconds)");
            options.addOption("writeTimeout", true, "Configure api client write timeout (default 10 seconds)");
            options.addOption("keepTemplate", false, "Keep template after installation (default false)");
            options.addOption("placeWidth", true, "Width of place for installing group (default 1935 : 430 * (4 + 1/2) = 4 pro line)");
            options.addOption("startPosition", true, "Starting position for the place for installing group, format x,y (default : 0,0)");
            options.addOption("failOnDuplicateNames", false, "Fail if template contains duplicate processor names in extractConfig mode");

            // parse the command line arguments
            CommandLine cmd = commandLineParser.parse(options, args);
            if (cmd.hasOption("h")) {
                printUsage(options);
                System.exit(1);
            } else if (!cmd.hasOption("n") || (!cmd.hasOption("c") && cmd.hasOption("m") && !cmd.getOptionValue("m").equals("undeploy"))) {
                printUsage(options);
                System.exit(1);
            } else if (!"updateConfig".equals(cmd.getOptionValue("m")) && !"extractConfig".equals(cmd.getOptionValue("m"))
                    && !"deployTemplate".equals(cmd.getOptionValue("m")) && !"undeploy".equals(cmd.getOptionValue("m"))) {
                printUsage(options);
                System.exit(1);
            } else if ((cmd.hasOption("password") && !cmd.hasOption("user"))) {
                printUsage(options);
                System.exit(1);
            } else {
                String password = Optional.ofNullable(System.getenv(ENV_NIFI_PASSWORD))
                        .orElseGet(() -> cmd.getOptionValue("password"));

                //configure options
                Integer timeout = cmd.hasOption("timeout") ? Integer.valueOf(cmd.getOptionValue("timeout")) : DEFAULT_TIMEOUT;
                Integer interval = cmd.hasOption("interval") ? Integer.valueOf(cmd.getOptionValue("interval")) : DEFAULT_INTERVAL;
                Integer connectionTimeout = cmd.hasOption("connectionTimeout") ? Integer.valueOf(cmd.getOptionValue("connectionTimeout")) : DEFAULT_CONNECTIONTIMEOUT;
                Integer readTimeout = cmd.hasOption("readTimeout") ? Integer.valueOf(cmd.getOptionValue("readTimeout")) : DEFAULT_READTIMEOUT;
                Integer writeTimeout = cmd.hasOption("writeTimeout") ? Integer.valueOf(cmd.getOptionValue("writeTimeout")) : DEFAULT_WRITETIMEOUT;
                Double placeWidth = cmd.hasOption("placeWidth") ? Double.valueOf(cmd.getOptionValue("placeWidth")) : DEFAULT_PLACEWIDTH;
                String startPlace = cmd.hasOption("startPosition") ? cmd.getOptionValue("startPosition") : DEFAULT_PLACE;
                Boolean forceMode = cmd.hasOption("force");

                LOG.info(String.format("Starting config_nifi %s on mode %s", version, cmd.getOptionValue("m")));
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

                Injector injector = getInjector(timeout, interval, placeWidth, createPosition(startPlace), forceMode);

                //start
                AccessService accessService = injector.getInstance(AccessService.class);
                accessService.setConfiguration(addressNifi, !cmd.hasOption("noVerifySsl"), cmd.hasOption("enableDebugMode"), connectionTimeout, readTimeout, writeTimeout);

                accessService.addTokenOnConfiguration(cmd.hasOption("accessFromTicket"), cmd.getOptionValue("user"), password);

                InformationService infoService = injector.getInstance(InformationService.class);
                String nifiVersion = infoService.getVersion();
                LOG.info(String.format("Communicate with nifi %s", nifiVersion));

                if ("updateConfig".equals(cmd.getOptionValue("m"))) {
                    //Get an instance of the bean from the context
                    UpdateProcessorService processorService = injector.getInstance(UpdateProcessorService.class);
                    processorService.updateByBranch(branchList, fileConfiguration, cmd.hasOption("noStartProcessors"));
                    LOG.info("The group configuration {} is updated with the file {}.", branch, fileConfiguration);
                } else if ("extractConfig".equals(cmd.getOptionValue("m"))) {
                    //Get an instance of the bean from the context
                    ExtractProcessorService processorService = injector.getInstance(ExtractProcessorService.class);
                    processorService.extractByBranch(branchList, fileConfiguration, cmd.hasOption("failOnDuplicateNames"));
                    LOG.info("The group configuration {} is extrated on file {}", branch, fileConfiguration);
                } else if ("deployTemplate".equals(cmd.getOptionValue("m"))) {
                    TemplateService templateService = injector.getInstance(TemplateService.class);
                    templateService.installOnBranch(branchList, fileConfiguration, cmd.hasOption("keepTemplate"));
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

    public static PositionDTO createPosition(String value) {
        PositionDTO positionDTO = new PositionDTO();
        String[] split = value.split(",");
        if (split.length != 2) {
            throw new ConfigException("startPlace must have format x,y whre y and y are integer");
        }
        positionDTO.setX(Double.valueOf(split[0]));
        positionDTO.setY(Double.valueOf(split[1]));
        return positionDTO;
    }

    /**
     * create injector with the values pass in parameter
     *
     * @param timeout the timeout
     * @param interval the interval
     * @param placeWidth tha placeWidth
     * @param forceMode active forceMode
     * @return the injector
     */
    public static Injector getInjector(Integer timeout, Integer interval, Double placeWidth, PositionDTO startPosition, Boolean forceMode) {
        return Guice.createInjector(new AbstractModule() {
            protected void configure() {
                bind(Integer.class).annotatedWith(Names.named("timeout")).toInstance(timeout);
                bind(Integer.class).annotatedWith(Names.named("interval")).toInstance(interval);
                bind(Boolean.class).annotatedWith(Names.named("forceMode")).toInstance(forceMode);
                bind(PositionDTO.class).annotatedWith(Names.named("startPosition")).toInstance(startPosition);
                bind(Double.class).annotatedWith(Names.named("placeWidth")).toInstance(placeWidth);
            }
        });
    }
}
