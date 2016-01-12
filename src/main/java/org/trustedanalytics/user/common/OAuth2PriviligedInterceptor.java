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


import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;


public class OAuth2PriviligedInterceptor implements RequestInterceptor{

    private final OAuth2ProtectedResourceDetails clientCredentials;

    public OAuth2PriviligedInterceptor(OAuth2ProtectedResourceDetails clientCredentials) {
        this.clientCredentials = clientCredentials;
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        OAuth2RestTemplate rt = new OAuth2RestTemplate(clientCredentials);
        OAuth2AccessToken accessToken = rt.getAccessToken();
        requestTemplate.header("Authorization", "bearer "+accessToken.getValue());
    }
}
