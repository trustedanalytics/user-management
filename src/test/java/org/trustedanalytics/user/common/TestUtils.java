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

import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.Charset;
import java.util.UUID;

public class TestUtils {

    public static HttpClientErrorException createDummyHttpClientException(UUID userId) {
        String body =
                "{\"message\":\"Username already in use: gerbszt@wp.pl\",\"error\":\"scim_resource_already_exists\",\"verified\":false,\"active\":true,\"user_id\":\""
                        + userId + "\"}";
        HttpClientErrorException exception =
                new HttpClientErrorException(HttpStatus.CONFLICT, "Conflict", body.getBytes(),
                        Charset.defaultCharset());
        return exception;
    }

}
