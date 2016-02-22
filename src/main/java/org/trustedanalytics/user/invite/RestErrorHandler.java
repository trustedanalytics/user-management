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

import org.springframework.web.bind.annotation.ResponseBody;
import org.trustedanalytics.user.common.EmptyPasswordException;
import org.trustedanalytics.user.common.TooShortPasswordException;
import org.trustedanalytics.user.common.NoPendingInvitationFoundException;
import org.trustedanalytics.user.common.UserExistsException;
import org.trustedanalytics.user.common.WrongUserRolesException;
import org.trustedanalytics.user.common.WrongUuidFormatException;
import org.trustedanalytics.user.invite.rest.EntityNotFoundException;
import org.trustedanalytics.user.invite.securitycode.InvalidSecurityCodeException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.trustedanalytics.user.invite.securitycode.NoSuchUserException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestErrorHandler {
    //It is a way to specify HTTP status as a response to particular exception

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(InvalidSecurityCodeException.class)
    public void incorrectSocurityCode() {
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UserExistsException.class)
    public UserConflictResponse userExists(UserExistsException e) throws IOException {
        return UserConflictResponse.of(UserConflictResponse.ConflictedField.USER , e.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(OrgExistsException.class)
    public UserConflictResponse orgExists(OrgExistsException e) throws IOException {
        return UserConflictResponse.of(UserConflictResponse.ConflictedField.ORG, e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidOrganizationNameException.class)
    public void invalidOrgName(InvalidOrganizationNameException e, HttpServletResponse response)
            throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WrongUuidFormatException.class)
    public void invalidUuidString(WrongUuidFormatException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(EntityNotFoundException.class)
    public void entityNotFound(Exception e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(WrongUserRolesException.class)
    public void incorrectRoles(Exception e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.CONFLICT.value(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(WrongEmailAddressException.class)
    public void wrongEmailAddress(Exception e, HttpServletResponse response) throws IOException {
        //It is a way to specify HTTP status as a response to particular exception
        response.sendError(HttpStatus.CONFLICT.value(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(EmptyPasswordException.class)
    public void emptyPassword(Exception e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(TooShortPasswordException.class)
    public void tooShortPassword(Exception e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.CONFLICT.value(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoPendingInvitationFoundException.class)
    public void noPendingInvitation(NoPendingInvitationFoundException e, HttpServletResponse response)
            throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoSuchUserException.class)
    public void userNotExists(NoSuchUserException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public void accessDenied(Exception e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.FORBIDDEN.value(), e.getMessage());
    }
}
