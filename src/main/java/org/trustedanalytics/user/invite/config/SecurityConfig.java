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

import org.trustedanalytics.user.common.TokenFetchException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Configures ResourceServerTokenServices which takes authorities from the 'scope' field in the access token.
 * 
 * Default implementation ignores that field and we cannot use global method security.
 */
@Configuration
@Lazy
public class SecurityConfig {
    @Autowired
    private ResourceServerProperties resource;

    @Order(1)
    @Bean
    public ResourceServerTokenServices jwtTokenServices() {
        DefaultTokenServices services = new DefaultTokenServices();
        services.setTokenStore(tokenStore());
        return services;
    }

    @Bean
    public TokenStore tokenStore() {
        return new JwtTokenStore(myjwtTokenEnhancer());
    }

    @Bean
    public JwtAccessTokenConverter myjwtTokenEnhancer() {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        DefaultAccessTokenConverter accessTokenConverter = new DefaultAccessTokenConverter();
        DefaultUserAuthenticationConverter userTokenConverter = new ScopeAuthoritiesTokenConverter();
        accessTokenConverter.setUserTokenConverter(userTokenConverter);
        converter.setAccessTokenConverter(accessTokenConverter);
        
        String keyValue = resource.getJwt().getKeyValue();
        if (!StringUtils.hasText(keyValue)) {
            try {
                keyValue = (String) new RestTemplate().getForObject(
                        resource.getJwt().getKeyUri(), Map.class).get("value");
            }
            catch (ResourceAccessException e) {
                throw new TokenFetchException("Failed to fetch token key from " + resource.getJwt().getKeyUri(), e);
            }
        }
        else {
            if (StringUtils.hasText(keyValue) && !keyValue.startsWith("-----BEGIN")) {
                converter.setSigningKey(keyValue);
            }
        }
        converter.setVerifierKey(keyValue);
        
        return converter;
    }
}
