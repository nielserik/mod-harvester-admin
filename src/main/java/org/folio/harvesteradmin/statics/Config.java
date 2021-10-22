package org.folio.harvesteradmin.statics;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;


public class Config
{
    private static final String HARVESTER_HOST_ENV_VAR = "harvester.host";
    private static final String HARVESTER_PORT_ENV_VAR = "harvester.port";
    private static final String HARVESTER_PROTOCOL = "harvester.protocol";
    private static final String HARVESTER_BASIC_AUTH_USERNAME = "harvester.auth.basic.username";
    private static final String HARVESTER_BASIC_AUTH_PASSWORD = "harvester.auth.basic.password";
    private static final String FILTER_BY_TENANT = "acl_filter.by_tenant";
    private static final String SERVICE_PORT_SYS_PROP = "port";
    private static final String SERVICE_PORT_DEFAULT = "8080";

    public static Level logLevel;
    public static int servicePort;
    public static int harvesterPort;
    public static String harvesterHost;
    public static String harvesterProtocol;
    public static String basicAuthUsername;
    public static String basicAuthPassword;
    public static boolean filterByTenant = true;
    private final static Logger logger = LogManager.getLogger( "harvester-admin" );
    private final boolean harvesterConfigOkay;

    public Config()
    {
        setServiceConfig();
        harvesterConfigOkay = setHarvesterConfig();
    }

    private void setServiceConfig()
    {
        servicePort = Integer.parseInt( System.getProperty( SERVICE_PORT_SYS_PROP, SERVICE_PORT_DEFAULT ) );
    }

    public static boolean hasHarvesterPort()
    {
        return harvesterPort > 0;
    }

    public static boolean harvesterRequiresSsl()
    {
        return harvesterProtocol != null && harvesterProtocol.equalsIgnoreCase( "https" );
    }

    public static boolean hasBasicAuthForHarvester()
    {
        return basicAuthUsername != null && basicAuthPassword != null;
    }

    private boolean setHarvesterConfig()
    {
        boolean configOk = true;
        harvesterHost = System.getenv( HARVESTER_HOST_ENV_VAR );
        if ( harvesterHost == null || harvesterHost.isEmpty() )
        {
            logger.error(
                    "No Harvester specified in environment variables. Environment variable 'harvester.host' is required for running the harvester admin module." );
            configOk = false;
        }
        harvesterProtocol = System.getenv().getOrDefault( HARVESTER_PROTOCOL, "http" );
        if ( harvesterProtocol.equals( "http" ) )
        {
            harvesterPort = Integer.parseInt( System.getenv().getOrDefault( HARVESTER_PORT_ENV_VAR, "80" ) );
        }
        else if ( harvesterProtocol.equals( "https" ) )
        {
            harvesterPort = Integer.parseInt( System.getenv().getOrDefault( HARVESTER_PORT_ENV_VAR, "443" ) );
        }
        else
        {
            logger.error(
                    "Unrecognized protocol '" + harvesterProtocol + "', cannot connect to Harvester at " + harvesterHost + ": " + harvesterPort );
            configOk = false;
        }
        if ( configOk )
        {
            logger.info(
                    "Attaching to Harvester at " + harvesterProtocol + "://" + harvesterHost + ":" + harvesterPort );
        }
        basicAuthUsername = System.getenv().get( HARVESTER_BASIC_AUTH_USERNAME );
        basicAuthPassword = System.getenv().get( HARVESTER_BASIC_AUTH_PASSWORD );
        if ( hasBasicAuthForHarvester() )
        {
            logger.info( "Using basic auth user " + basicAuthUsername );
        }
        filterByTenant = !System.getenv().getOrDefault( FILTER_BY_TENANT, "true" ).equalsIgnoreCase( "false" );
        return configOk;
    }

    public boolean isHarvesterConfigOkay()
    {
        return harvesterConfigOkay;
    }

    public String toString()
    {
        return ManagementFactory.getRuntimeMXBean().getName() + " on port " + servicePort + ", proxying " + harvesterProtocol + "://" + harvesterHost + ":" + harvesterPort;
    }

}
