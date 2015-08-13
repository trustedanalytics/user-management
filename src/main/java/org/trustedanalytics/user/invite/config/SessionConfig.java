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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

@Configuration
@Lazy
class SessionConfig {

    @Autowired
    private ResourceServerProperties resource;

    @Bean
    public ResourceServerConfigurer resourceServer() {
        return new ResourceSecurityConfigurer(resource);
    }

    protected static class ResourceSecurityConfigurer extends ResourceServerConfigurerAdapter {

        private ResourceServerProperties resource;

        @Autowired
        public ResourceSecurityConfigurer(ResourceServerProperties resource) {
            this.resource = resource;
        }

        @Override
        public void configure(ResourceServerSecurityConfigurer resources)
            throws Exception {
            resources.resourceId(resource.getResourceId());
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            // @formatter:off
            http.sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                .authorizeRequests().anyRequest().authenticated();
            // @formatter:on
        }

    }


}
