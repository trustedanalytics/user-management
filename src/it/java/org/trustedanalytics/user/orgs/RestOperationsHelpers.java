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
package org.trustedanalytics.user.orgs;

import static java.util.Collections.singletonList;

import org.trustedanalytics.cloud.auth.HeaderAddingHttpInterceptor;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.support.InterceptingHttpAccessor;
import org.springframework.web.client.RestOperations;

import java.util.Map;

public class RestOperationsHelpers {
    public static ResponseEntity<String> getForEntityWithToken(RestOperations restTemplate,
        String token, String url, Map<String, Object> pathVars) {
        addAuthHeaderToTemplate(restTemplate, token);
        return restTemplate.getForEntity(url, String.class, pathVars);
    }

    public static ResponseEntity<String> getForEntityWithToken(RestOperations restTemplate,
        String token, String url) {
        addAuthHeaderToTemplate(restTemplate, token);
        return restTemplate.getForEntity(url, String.class);
    }

    public static <T> ResponseEntity<T> postForEntityWithToken(RestOperations restTemplate,
        String token, String url, Object request, Class<T> type) {
        addAuthHeaderToTemplate(restTemplate, token);
        return restTemplate.postForEntity(url, request, type);
    }

    public static <T> ResponseEntity<T> postForEntityWithToken(RestOperations restTemplate,
        String token, String url, Object request, Class<T> type, Map<String, ?> pathVars) {
        addAuthHeaderToTemplate(restTemplate, token);
        return restTemplate.postForEntity(url, request, type, pathVars);
    }

    private static void addAuthHeaderToTemplate(RestOperations restTemplate, String token) {
        ClientHttpRequestInterceptor interceptor =
            new HeaderAddingHttpInterceptor("Authorization", "bearer " + token);
        ((InterceptingHttpAccessor) restTemplate).setInterceptors(singletonList(interceptor));
    }
}
