package com.github.hermannpencole.nifi.config;

import com.github.hermannpencole.nifi.config.model.ConfigException;
import com.github.hermannpencole.nifi.config.service.AccessService;
import com.github.hermannpencole.nifi.config.service.ExtractProcessorService;
import com.github.hermannpencole.nifi.swagger.ApiClient;
import com.github.hermannpencole.nifi.swagger.ApiException;
import com.github.hermannpencole.nifi.swagger.Configuration;
import com.github.hermannpencole.nifi.swagger.client.AccessApi;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.github.hermannpencole.nifi.config.service.TemplateService;
import com.github.hermannpencole.nifi.config.service.UpdateProcessorService;
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

    /**
     * Print to the console the usage.
     *
     * @param options the options
     */
    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("config_nifi [OPTIONS]", options);
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
            options.addOption("accessFromTicket", true, "Access via Kerberos ticket exchange / SPNEGO negotiation");
            options.addOption("noVerifySsl", true, "turn off ssl verification certificat");

            // parse the command line arguments
            CommandLine cmd = commandLineParser.parse(options, args);
            if (cmd.hasOption("h")) {
                printUsage(options);
                System.exit(1);
            } else if (!cmd.hasOption("n") || (!cmd.hasOption("c") && !cmd.getOptionValue("m").equals("undeploy") )) {
                printUsage(options);
                System.exit(1);
            } else if (!"updateConfig".equals(cmd.getOptionValue("m")) && !"extractConfig".equals(cmd.getOptionValue("m"))
                    && !"deployTemplate".equals(cmd.getOptionValue("m")) && !"undeploy".equals(cmd.getOptionValue("m")) ) {
                printUsage(options);
                System.exit(1);
            } else if ( (cmd.hasOption("user") || cmd.hasOption("password")) && (!cmd.hasOption("user") || !cmd.hasOption("password")) ) {
                printUsage(options);
                System.exit(1);
            } else {

                String adresseNifi = cmd.getOptionValue("n");
                String fileConfiguration = cmd.getOptionValue("c");

                String branch = "root";
                if (cmd.hasOption("b")) {
                    branch = cmd.getOptionValue("b");
                }
                List<String> branchList = Arrays.stream(branch.split(">")).map(String::trim).collect(Collectors.toList());


                setConfiguration(adresseNifi, !cmd.hasOption("noVerifySsl"));
                Injector injector = Guice.createInjector();
                AccessService accessService = injector.getInstance(AccessService.class);
                accessService.addTokenOnConfiguration(cmd.hasOption("accessFromTicket"), cmd.getOptionValue("user"), cmd.getOptionValue("password"));
                if ("updateConfig".equals(cmd.getOptionValue("m"))) {
                    //Get an instance of the bean from the context
                    UpdateProcessorService processorService = injector.getInstance(UpdateProcessorService.class);
                    processorService.updateByBranch(branchList, fileConfiguration);
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


    public static void setConfiguration(String basePath, boolean verifySsl) throws ApiException {

        ApiClient client = new ApiClient().setBasePath(basePath).setVerifyingSsl(verifySsl);
        Configuration.setDefaultApiClient(client);
    }
}
