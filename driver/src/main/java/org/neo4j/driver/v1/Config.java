/**
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.v1;

import java.io.File;
import java.util.logging.Level;

import org.neo4j.driver.internal.logging.JULogging;
import org.neo4j.driver.internal.spi.Logging;

import static org.neo4j.driver.v1.Config.TlsAuthenticationConfig.usingKnownCerts;

/**
 * A configuration class to config driver properties.
 * <p>
 * To create a config:
 * <pre>
 * {@code
 * Config config = Config
 *                  .build()
 *                  .withLogging(new MyLogging())
 *                  .toConfig();
 * }
 * </pre>
 */
@Immutable
public class Config
{
    public static final String SCHEME = "bolt";
    public static final int DEFAULT_PORT = 7687;

    /** User defined logging */
    private final Logging logging;

    /** The size of connection pool for each database url */
    private final int connectionPoolSize;

    /** Connections that have been idle longer than this threshold will have a ping test performed on them. */
    private final long idleTimeBeforeConnectionTest;

    /* Whether TLS is enabled on all connections */
    private final boolean isTlsEnabled;

    /* Defines how to authenticate a server in TLS connections */
    private TlsAuthenticationConfig tlsAuthConfig;

    private Config( ConfigBuilder builder )
    {
        this.logging = builder.logging;

        this.connectionPoolSize = builder.connectionPoolSize;
        this.idleTimeBeforeConnectionTest = builder.idleTimeBeforeConnectionTest;

        this.isTlsEnabled = builder.isTlsEnabled;
        this.tlsAuthConfig = builder.tlsAuthConfig;
    }

    /**
     * Logging provider
     * @return the logging provider to use
     */
    public Logging logging()
    {
        return logging;
    }

    /**
     * Max number of connections per URL for this driver.
     * @return the max number of connections
     */
    public int connectionPoolSize()
    {
        return connectionPoolSize;
    }

    /**
     * Pooled connections that have been unused for longer than this timeout will be tested before they are
     * used again, to ensure they are still live.
     * @return idle time in milliseconds
     */
    public long idleTimeBeforeConnectionTest()
    {
        return idleTimeBeforeConnectionTest;
    }

    /**
     * If TLS is enabled in all socket connections
     * @return if TLS is enabled
     */
    public boolean isTlsEnabled()
    {
        return isTlsEnabled;
    }

    /**
     * Specify an approach to authenticate the server when establishing TLS connections with the server
     * @return a TLS configuration
     */
    public TlsAuthenticationConfig tlsAuthConfig()
    {
        return tlsAuthConfig;
    }

    /**
     * Return a {@link ConfigBuilder} instance
     * @return a {@link ConfigBuilder} instance
     */
    public static ConfigBuilder build()
    {
        return new ConfigBuilder();
    }

    /**
     * @return A config with all default settings
     */
    public static Config defaultConfig()
    {
        return Config.build().toConfig();
    }

    /**
     * Used to build new config instances
     */
    public static class ConfigBuilder
    {
        private Logging logging = new JULogging( Level.INFO );
        private int connectionPoolSize = 10;
        private long idleTimeBeforeConnectionTest = 200;
        private boolean isTlsEnabled = false;
        private TlsAuthenticationConfig tlsAuthConfig =
                usingKnownCerts( new File( System.getProperty( "user.home" ), "neo4j/neo4j_known_certs" ) );

        private ConfigBuilder() {}

        /**
         * Provide an alternative logging implementation for the driver to use. By default we use
         * java util logging.
         * @param logging the logging instance to use
         * @return this builder
         */
        public ConfigBuilder withLogging( Logging logging )
        {
            this.logging = logging;
            return this;
        }

        /**
         * The max number of sessions to keep open at once. Configure this
         * higher if you want more concurrent sessions, or lower if you want
         * to lower the pressure on the database instance.
         *
         * If the driver is asked to provide more sessions than this, it will
         * block waiting for another session to be closed, with a timeout.
         *
         * @param size the max number of sessions to keep open
         * @return this builder
         */
        public ConfigBuilder withMaxSessions( int size )
        {
            this.connectionPoolSize = size;
            return this;
        }

        /**
         * Pooled sessions that have been unused for longer than this timeout
         * will be tested before they are used again, to ensure they are still live.
         *
         * If this option is set too low, an additional network call will be
         * incurred when acquiring a session, which causes a performance hit.
         *
         * If this is set high, you may receive sessions that are no longer live,
         * which will lead to exceptions in your application. Assuming the
         * database is running, these exceptions will go away if you retry acquiring
         * sessions.
         *
         * Hence, this parameter tunes a balance between the likelihood of your
         * application seeing connection problems, and performance.
         *
         * You normally should not need to tune this parameter.
         *
         * @param timeout minimum idle time in milliseconds
         * @return this builder
         */
        public ConfigBuilder withSessionLivenessCheckTimeout( long timeout )
        {
            this.idleTimeBeforeConnectionTest = timeout;
            return this;
        }

        /**
         * Enable TLS in all connections with the server.
         * When TLS is enabled, if a trusted certificate is provided by invoking {@code withTrustedCert}, then only the
         * connections with certificates signed by the trusted certificate will be accepted;
         * If no certificate is provided, then we will trust the first certificate received from the server.
         * See {@code withKnownCerts} for more info about what will happen when no trusted certificate is
         * provided.
         * @param value true to enable tls and flase to disable tls
         * @return this builder
         */
        public ConfigBuilder withTlsEnabled( boolean value )
        {
            this.isTlsEnabled = value;
            return this;
        }

        /**
         * Defines how to authenticate a server in TLS connections.
         * @param tlsAuthConfig TLS authentication config
         * @return this builder
         */
        public ConfigBuilder withTlsAuthConfig( TlsAuthenticationConfig tlsAuthConfig )
        {
            this.tlsAuthConfig = tlsAuthConfig;
            return this;
        }

        /**
         * Create a config instance from this builder.
         * @return a {@link Config} instance
         */
        public Config toConfig()
        {
            return new Config( this );
        }
    }

    /**
     * A configuration to configure TLS authentication
     */
    public static class TlsAuthenticationConfig
    {
        private enum Mode
        {
            KNOWN_CERTS,
            TRUSTED_CERT
        }
        private final Mode mode;
        private final File certFile;

        private TlsAuthenticationConfig( Mode mode, File certFile )
        {
            this.mode = mode;
            this.certFile = certFile;
        }

        /**
         * Return true if full authentication is enabled, which suggests a trusted certificate is provided with
         * {@link #usingTrustedCert(File)}. Otherwise, return false.
         * @return true if full authentication is enabled.
         */
        public boolean isFullAuthEnabled()
        {
            return mode == Mode.TRUSTED_CERT;
        }

        public File certFile()
        {
            return certFile;
        }

        /**
         * Using full authentication: only TLS connections with certificates signed by a given trusted CA will be
         * accepted.
         * The trusted CA is given in the trusted certificate file. The file could contain multiple certificates of
         * multiple CAs.
         * The certificates in the file should be encoded using Base64 encoding,
         * and each of the certificate is bounded at the beginning by -----BEGIN CERTIFICATE-----,
         * and bounded at the end by -----END CERTIFICATE-----.
         * @param certFile the trusted certificate file
         * @return an authentication config
         */
        public static TlsAuthenticationConfig usingTrustedCert( File certFile )
        {
            return new TlsAuthenticationConfig( Mode.TRUSTED_CERT, certFile );
        }

        /**
         * Using trust-on-first-use authentication.
         * Use this method to change the default file where known certificates are stored.
         * It is not recommend to change the default position, however if we have a problem that we cannot create the
         * file at the default position, then this method enables us to specify a new position for the file.
         * <p>
         * The known certificate file stores a list of {@code (neo4j_server, cert)} pairs, where each pair stores
         * a neo4j server and the first certificate received from the server.
         * When we establish a TLS connection with a server, we record the server and the first certificate we
         * received from it. Then when we establish more connections with the same server, only the connections with
         * the same certificate recorded in this file will be accepted.
         *
         * @param certFile the new file where known certificates are stored.
         * @return an authentication config
         */
        public static TlsAuthenticationConfig usingKnownCerts( File certFile )
        {
            return new TlsAuthenticationConfig( Mode.KNOWN_CERTS, certFile );
        }
    }
}