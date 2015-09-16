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
package org.trustedanalytics.user.invite;

import org.trustedanalytics.user.invite.rest.EntityAlreadyExists;
import org.trustedanalytics.user.invite.rest.EntityNotFoundException;
import org.trustedanalytics.user.invite.securitycode.InvalidSecurityCodeException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestErrorHandler {

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(InvalidSecurityCodeException.class)
    public void incorrectSocurityCode() {
        //It is a way to specify HTTP status as a response to particular exception
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(EntityAlreadyExists.class)
    public void entityAlreadyExists() {
        //It is a way to specify HTTP status as a response to particular exception
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException.class)
    public void entityNotFound() {
        //It is a way to specify HTTP status as a response to particular exception
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(WrongEmailAddressException.class)
    public void wrongEmailAddress(Exception e, HttpServletResponse response) throws IOException {
        //It is a way to specify HTTP status as a response to particular exception
        response.sendError(HttpStatus.CONFLICT.value(), e.getMessage());
    }
}
