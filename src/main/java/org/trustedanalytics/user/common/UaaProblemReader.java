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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;

public class UaaProblemReader {
    private static final Log LOGGER = LogFactory.getLog(UaaProblemReader.class);

    private UaaProblemReader() {
    }

    public static UaaProblem read(HttpStatusCodeException e) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(e.getResponseBodyAsString(), UaaProblem.class);
        } catch (IOException e1) {
            LOGGER.error(e1);
            return null;
        }
    }
}
