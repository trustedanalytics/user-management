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

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

@ControllerAdvice
public class WebErrorHandlers {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebErrorHandlers.class);

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void badRequest(Exception e) {
        LOGGER.error(HttpStatus.BAD_REQUEST.getReasonPhrase(), e);
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public void handleHttpStatusCodeException(HttpStatusCodeException e,
        HttpServletResponse response) throws IOException {
        String message = extractErrorFromJSON(e.getResponseBodyAsString());
        message = StringUtils.isNotBlank(message) ? message : e.getMessage();
        LOGGER.error(message, e);
        response.sendError(e.getStatusCode().value(), message);
    }

    private String extractErrorFromJSON(String json){
        Map<String, String> map = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<HashMap<String,String>> typeReference = new TypeReference<HashMap<String,String>>() {
            };
            map = mapper.readValue(json, typeReference);
        } catch (Exception e) {
            String msg = "Exception raised while extracting error from JSON " ;
            LOGGER.error(msg, e);
        }
        return map.get("description");
    }
}
