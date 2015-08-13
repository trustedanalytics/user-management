/**
 *  Copyright (c) 2015 Intel Corporation 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.trustedanalytics.user.invite.config;

import java.util.Arrays;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("smtp")
public class SmtpProperties {

    private String host;
    private int port;
    private String email;
    private String username;
    private String password;
    private int timeout;
    private boolean debug;
    private String emailName;
    
    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }
    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }
    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }
    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }
    /**
     * @return the username
     */
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }
    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }
    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }
    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
    /**
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }
    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    /**
     * @return the debug
     */
    public boolean isDebug() {
        return debug;
    }
    /**
     * @param debug the debug to set
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    /**
     * @return the emailName
     */
    public String getEmailName() {
        return emailName;
    }

    /**
     * @param emailName the emailName to set
     */
    public void setEmailName(String emailName) {
        this.emailName = emailName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, username, password, timeout, debug, emailName);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        
        SmtpProperties other;
        if (obj instanceof SmtpProperties) {
            other = (SmtpProperties) obj;
            return allTrue(Objects.equals(host, other.host),
                   Objects.equals(port, other.port),
                   Objects.equals(username, other.username),
                   Objects.equals(password, other.password),
                   Objects.equals(timeout, other.timeout),
                   Objects.equals(debug, other.debug),
                   Objects.equals(emailName, other.emailName));
        }
        
        return false;
    }

    private boolean allTrue(Boolean... args) {
        return !Arrays.asList(args).contains(false);
    }
}
