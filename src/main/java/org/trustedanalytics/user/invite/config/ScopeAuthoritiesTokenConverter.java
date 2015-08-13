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

import static org.springframework.security.oauth2.provider.token.AccessTokenConverter.SCOPE;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ScopeAuthoritiesTokenConverter extends DefaultUserAuthenticationConverter {

    public static final String USER_ID = "user_id";

    @Override
    public Authentication extractAuthentication(Map<String, ?> map) {
        if (!map.containsKey(USERNAME)) {
            return null;
        }

        if (map.containsKey(AUTHORITIES)) {
            return super.extractAuthentication(map);
        }
        
        if (map.containsKey(SCOPE)) {
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(map.get(USERNAME), "N/A", getAuthorities(map));
            if (map.containsKey(USER_ID)) {
                AccessTokenDetails details = new AccessTokenDetails(UUID.fromString((String)map.get(USER_ID)));
                token.setDetails(details);
            }
            return token;
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> getAuthorities(Map<String, ?> map) {
        if (!map.containsKey(SCOPE)) {
            return Collections.emptySet();
        }

        return ((Collection<String>) map.get(SCOPE)).stream()
                .map(v -> new SimpleGrantedAuthority(v))
                .collect(Collectors.toSet());
    }
}
