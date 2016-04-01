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

import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.trustedanalytics.cloud.auth.AuthTokenRetriever;
import org.trustedanalytics.cloud.auth.HeaderAddingHttpInterceptor;
import org.trustedanalytics.cloud.cc.FeignClient;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.customizations.OAuth2RequestInterceptor;
import org.trustedanalytics.cloud.uaa.UaaClient;
import org.trustedanalytics.cloud.uaa.UaaOperations;
import org.trustedanalytics.user.common.OAuth2PriviligedInterceptor;
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

@Profile("cloud")
@Configuration
public class UsersConfig {
    @Value("${oauth.uaa}")
    private String uaaBaseUrl;

    @Value("${oauth.resource}")
    private String apiBaseUrl;

    @Autowired
    private AuthTokenRetriever tokenRetriever;

    @Bean
    protected OAuth2PriviligedInterceptor oauth2PrivilegedInterceptor(OAuth2ProtectedResourceDetails clientCredentials) {
        return new OAuth2PriviligedInterceptor(clientCredentials);
    }

    @Bean
    protected CcOperations ccPrivilegedClient(OAuth2PriviligedInterceptor oauth2PrivilegedInterceptor) {

        return new FeignClient(apiBaseUrl,
                builder -> builder
                        .requestInterceptor(oauth2PrivilegedInterceptor));
    }

    @Bean
    protected UaaOperations uaaPrivilegedClient(RestOperations clientRestTemplate) {
        return new UaaClient(clientRestTemplate, uaaBaseUrl);
    }

    @Bean
    @Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
    protected CcOperations ccClient() {
        return new FeignClient(apiBaseUrl,
                builder -> builder
                        .requestInterceptor(new OAuth2RequestInterceptor(getAccessToken())));
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
        ClientHttpRequestInterceptor interceptor =
                new HeaderAddingHttpInterceptor("Authorization", "bearer " + getAccessToken());
        restTemplate.setInterceptors(singletonList(interceptor));

        return restTemplate;
    }

    private String getAccessToken() {
        OAuth2Authentication authentication = getAuthentication();
        return tokenRetriever.getAuthToken(authentication);
    }

    @Bean
    protected UsersService usersService(CcOperations ccClient,
                                        UaaOperations uaaClient,
                                        InvitationsService invitationsService,
                                        AccessInvitationsService accessInvitationsService) {
        return new CfUsersService(ccClient, uaaClient, invitationsService, accessInvitationsService);
    }

    @Bean
    protected UsersService priviledgedUsersService(CcOperations ccPrivilegedClient,
                                                   UaaOperations uaaPrivilegedClient,
                                                   InvitationsService invitationsService,
                                                   AccessInvitationsService accessInvitationsService) {
        return new CfUsersService(ccPrivilegedClient,
                uaaPrivilegedClient,
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
