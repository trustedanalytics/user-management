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
package org.trustedanalytics.user.current;

import org.trustedanalytics.user.invite.config.AccessTokenDetails;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.Collection;
import java.util.UUID;

public final class AuthDetailsFinder implements UserDetailsFinder {

    private static final String ADMIN_ROLE = "console.admin";

    @Override
    public UserRole getRole(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if(authorities == null || authorities.isEmpty())
            return UserRole.USER;

        boolean isAdmin = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .filter(ADMIN_ROLE::equalsIgnoreCase)
            .count() > 0;

        return isAdmin ? UserRole.ADMIN : UserRole.USER;
    }

    @Override
    public UUID findUserId(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication argument must not be null");
        }
        OAuth2Authentication oauth2 = (OAuth2Authentication) authentication;
        AccessTokenDetails details = (AccessTokenDetails) oauth2.getUserAuthentication().getDetails();
        return details.getUserGuid();
    }

    @Override
    public String findUserName(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication argument must not be null");
        }
        OAuth2Authentication oauth2 = (OAuth2Authentication) authentication;
        return oauth2.getName();
    }
}
