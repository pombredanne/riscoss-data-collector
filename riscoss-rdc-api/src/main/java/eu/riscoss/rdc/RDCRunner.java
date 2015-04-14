package eu.riscoss.rdc;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.riscoss.datacollector.RiskDataCollector;
import eu.riscoss.datacollector.common.IndicatorsMap;
import eu.riscoss.dataproviders.RDR;
import eu.riscoss.dataproviders.RiskData;

public class RDCRunner
{
    private static Logger LOGGER = LoggerFactory.getLogger(RDCRunner.class);

    private static String TARGET_ENTITY_PROPERTY = "targetEntity";

    /**
     * 
     * @param args The arguments: 
     * 			-properties <propertiesfile>
     * 			-property <key=value>
     * @param riskDataCollector The data collector to execute
     */
    public static void exec(String[] args, RiskDataCollector riskDataCollector)
    {
        try {
            Options options = new Options();
            @SuppressWarnings("static-access")
			Option propertiesOption = OptionBuilder.hasArgs()
												   .withArgName("propertiesFile")
												   .create("properties");
            
            @SuppressWarnings("static-access")
			Option propertyOption = OptionBuilder.withArgName("key=value")
            									 .hasArgs()
            									 .withDescription("parameter values, described in the associated XWiki class")
            									 .create("property");
            
            options.addOption(propertiesOption);
            options.addOption(propertyOption);

            CommandLineParser parser = new GnuParser();
            CommandLine commandLine = parser.parse(options, args);

            Properties properties = new Properties();

            /* If there is data on the standard input, then parse properties passed there */
            if (System.in.available() != 0) {
                String stdin = IOUtils.toString(System.in, "UTF-8");
                JSONObject input = new JSONObject(stdin);

                Iterator it = input.keys();
                while (it.hasNext()) {
                    String k = (String) it.next();
                    properties.put(k, String.format("%s", input.get(k)));
                }
            }

            if (commandLine.hasOption("properties")) {
                Properties commandLineSpecifiedProperties = new Properties();
                for (String arg : commandLine.getOptionValues("properties")) {
                    commandLineSpecifiedProperties.load(new FileInputStream(arg));
                    properties.putAll(commandLineSpecifiedProperties);
                }
            }

            if (commandLine.hasOption("property")) {
                for (String arg : commandLine.getOptionValues("property")) {
                    String parts[] = arg.split("=", 2);
                    if (parts.length == 2) {
                        properties.put(parts[0], parts[1]);
                    }
                }
            }

            LOGGER.debug("Defined properties");
            for (Map.Entry e : properties.entrySet()) {
                LOGGER.debug(String.format("  %s=%s", e.getKey(), e.getValue()));
            }

            String targetEntity = properties.getProperty(TARGET_ENTITY_PROPERTY);

            if (targetEntity != null) {
                IndicatorsMap indicatorsMap = new IndicatorsMap(targetEntity);
                riskDataCollector.createIndicators(indicatorsMap, properties);
                System.out.println("\n\n-----BEGIN RISK DATA-----");
                System.out.format("%s\n",
                        RDR.getRiskDataJson(new ArrayList<RiskData>(indicatorsMap.values())).toString());
                System.out.println("-----END RISK DATA-----");
            } else {
                LOGGER.error(String.format("No %s specified in properties", TARGET_ENTITY_PROPERTY));
                System.exit(1);
            }
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.error(e.getMessage(), e);
            } else {
                LOGGER.error(e.getMessage());
            }

            System.exit(1);
        }
    }
}
