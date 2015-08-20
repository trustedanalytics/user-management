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
package org.trustedanalytics.user.manageusers;

import static java.util.Collections.singletonList;
import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;
import static org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST;

import org.trustedanalytics.cloud.auth.AuthTokenRetriever;
import org.trustedanalytics.cloud.auth.HeaderAddingHttpInterceptor;
import org.trustedanalytics.cloud.cc.CcClient;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.uaa.UaaClient;
import org.trustedanalytics.cloud.uaa.UaaOperations;
import org.trustedanalytics.user.invite.EmailOrgUserInvitationService;
import org.trustedanalytics.user.invite.InvitationsService;
import org.trustedanalytics.user.invite.MessageService;
import org.trustedanalytics.user.invite.OrgUserInvitationService;
import org.springframework.web.client.RestTemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.client.RestOperations;
import org.thymeleaf.TemplateEngine;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;

@Configuration
public class UsersConfig {
    @Value("${oauth.uaa}")
    private String uaaBaseUrl;

    @Value("${oauth.resource}")
    private String apiBaseUrl;

    @Autowired
    private AuthTokenRetriever tokenRetriever;

    @Bean
    @Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
    protected CcOperations ccPrivilegedClient(RestOperations clientRestTemplate) {
        return new CcClient(clientRestTemplate, apiBaseUrl);
    }

    @Bean
    @Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
    protected UaaOperations uaaPrivilegedClient(RestOperations clientRestTemplate) {
        return new UaaClient(clientRestTemplate, uaaBaseUrl);
    }

    @Bean
    @Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
    protected CcOperations ccClient(RestTemplate userRestTemplate) {
        return new CcClient(setAccessToken(userRestTemplate), apiBaseUrl);
    }

    @Bean
    @Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
    protected UaaOperations uaaClient(RestTemplate userRestTemplate) {
        return new UaaClient(setAccessToken(userRestTemplate), uaaBaseUrl);
    }

    private OAuth2Authentication getAuthentication() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context == null) {
            return null;
        }

        return (OAuth2Authentication) context.getAuthentication();
    }

    private RestTemplate setAccessToken(RestTemplate restTemplate) {
        OAuth2Authentication authentication = getAuthentication();
        String token = tokenRetriever.getAuthToken(authentication);
        ClientHttpRequestInterceptor interceptor =
            new HeaderAddingHttpInterceptor("Authorization", "bearer " + token);
        restTemplate.setInterceptors(singletonList(interceptor));

        return restTemplate;
    }

    @Bean
    protected UsersService usersService(CcOperations ccClient,
                                        UaaOperations uaaClient,
                                        PasswordGenerator passwordGenerator,
                                        InvitationsService invitationsService,
                                        AccessInvitationsService accessInvitationsService) {
        return new CfUsersService(ccClient, uaaClient, passwordGenerator, invitationsService, accessInvitationsService);
    }

    @Bean
    protected UsersService priviledgedUsersService(CcOperations ccPrivilegedClient,
                                                   UaaOperations uaaPrivilegedClient,
                                                   PasswordGenerator passwordGenerator,
                                                   InvitationsService invitationsService,
                                                   AccessInvitationsService accessInvitationsService) {
        return new CfUsersService(ccPrivilegedClient,
                uaaPrivilegedClient,
                passwordGenerator,
                invitationsService,
                accessInvitationsService);
    }


    @Bean
    protected PasswordGenerator passwordGenerator() {
        return new RandomPasswordGenerator();
    }

    @Bean
    protected OrgUserInvitationService orgUserInvitationService(MessageService messageService,
        TemplateEngine templateEngine) {
        return new EmailOrgUserInvitationService(messageService, templateEngine);
    }
}
