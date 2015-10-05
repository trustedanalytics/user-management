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
package org.trustedanalytics.user.common;

import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;
import static org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.trustedanalytics.cloud.auth.AuthTokenRetriever;
import org.trustedanalytics.cloud.auth.OAuth2TokenRetriever;
import org.trustedanalytics.user.current.AuthDetailsFinder;
import org.trustedanalytics.user.current.UserDetailsFinder;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.web.client.RestTemplate;

@Profile("cloud")
@Configuration
public class RestTemplatesConfiguration {

    @Bean
    public OAuth2ClientContext oauth2ClientContext() {
        return new DefaultOAuth2ClientContext(new DefaultAccessTokenRequest());
    }

    @Bean
    @Scope(value = SCOPE_REQUEST, proxyMode = TARGET_CLASS)
    protected RestTemplate userRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        return new RestTemplate(factory);
    }

    @Bean
    @ConfigurationProperties("spring.oauth2.client")
    public OAuth2ProtectedResourceDetails clientCredentials() {
        return new ClientCredentialsResourceDetails();
    }

    @Bean
    public OAuth2RestTemplate clientRestTemplate(OAuth2ClientContext oauth2ClientContext, OAuth2ProtectedResourceDetails clientCredentials) {
        OAuth2RestTemplate template = new OAuth2RestTemplate(clientCredentials, oauth2ClientContext);
        ClientCredentialsAccessTokenProvider provider = new ClientCredentialsAccessTokenProvider();
        template.setAccessTokenProvider(provider);
        return template;
    }

    @Bean
    protected AuthTokenRetriever tokenRetriever() {
        return new OAuth2TokenRetriever();
    }

    @Bean
    protected UserDetailsFinder detailsFinder() {
        return new AuthDetailsFinder();
    }

    @Bean
    protected UserPasswordValidator userPasswordValidator() { return new UserPasswordValidator(); }
}
