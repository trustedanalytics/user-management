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

import org.trustedanalytics.user.common.BlacklistEmailValidator;
import org.trustedanalytics.user.common.SpaceUserRolesValidator;

import org.trustedanalytics.user.invite.AngularInvitationLinkGenerator;
import org.trustedanalytics.user.invite.EmailInvitationsService;
import org.trustedanalytics.user.invite.EmailService;
import org.trustedanalytics.user.invite.InvitationLinkGenerator;
import org.trustedanalytics.user.invite.InvitationsService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.spring4.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;

@Configuration
@Profile({"dev", "cloud"})
public class InvitationsConfig {

    @Value("#{'${smtp.forbidden_domains}'.split(',')}")
    private List<String> forbiddenDomains;

    @Autowired
    private SmtpProperties smtpProperties;

    @Bean(name="emailService")
    protected EmailService emailService() throws UnsupportedEncodingException {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        int port = smtpProperties.getPort();

        if (port > 0) {
            sender.setPort(port);
        }
        sender.setProtocol(smtpProperties.getProtocol());
        sender.setHost(smtpProperties.getHost());

        Properties mailProps = new Properties();

        if (!StringUtils.isBlank(smtpProperties.getUsername()) && !StringUtils.isBlank(smtpProperties.getPassword()) ) {
            sender.setUsername(smtpProperties.getUsername());
            sender.setPassword(smtpProperties.getPassword());
            mailProps.setProperty(String.format("mail.%s.auth", smtpProperties.getProtocol()), "true");
        }
        else {
            mailProps.setProperty(String.format("mail.%s.auth", smtpProperties.getProtocol()), "false");
        }

        if(smtpProperties.getProtocol().equals("smtps")) {
            mailProps.setProperty(String.format("mail.smtps.ssl.enable", smtpProperties.getProtocol()), "true");
        }

        mailProps.setProperty("mail.smtps.connectiontimeout", Integer.toString(smtpProperties.getTimeout()));

        if (smtpProperties.isDebug()) {
            mailProps.setProperty("mail.debug", "true");
            System.setProperty("mail.socket.debug", "true");
        }

        sender.setJavaMailProperties(mailProps);

        return new EmailService(sender, smtpProperties.getEmail(), smtpProperties.getEmailName());
    }

    @Bean(name="invitationsService")
    protected InvitationsService invitationsService(SpringTemplateEngine mailTemplateEngine) {
        return new EmailInvitationsService(mailTemplateEngine);
    }

    @Bean
    protected BlacklistEmailValidator emailValidator(){
        return new BlacklistEmailValidator(forbiddenDomains);
    }

    @Bean
    protected SpaceUserRolesValidator spaceRolesValidator(){
        return new SpaceUserRolesValidator();
    }

    @Bean
    protected InvitationLinkGenerator intitationLinkGenerator() {
        return new AngularInvitationLinkGenerator();
    }
}
