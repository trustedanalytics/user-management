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
package org.trustedanalytics.user.invite.rest;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.trustedanalytics.user.manageusers.UsersService;
import org.trustedanalytics.user.common.InvitationPendingException;
import org.trustedanalytics.user.common.UserExistsException;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.InvitationNotSentException;
import org.trustedanalytics.user.invite.InvitationsService;

import org.trustedanalytics.user.common.BlacklistEmailValidator;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.util.Set;

@RestController
@RequestMapping("/rest/invitations")
public class InvitationsController {

    public static final String IS_ADMIN_CONDITION = "hasRole('console.admin')";

    public static final String RESEND_INVITATION_URL = "/{email}/resend";

    //:.+ is required, otherwise Spring truncates value with @PathVariable up to last dot
    public static final String DELETE_INVITATION_URL = "/{email:.+}";

    private final InvitationsService invitationsService;

    private final AccessInvitationsService accessInvitationsService;

    private final UserDetailsFinder detailsFinder;

    private final BlacklistEmailValidator emailValidator;

    @Autowired
    public InvitationsController(InvitationsService invitationsService,
                                 UserDetailsFinder detailsFinder,
                                 AccessInvitationsService accessInvitationsService,
                                 BlacklistEmailValidator emailValidator){
        this.invitationsService = invitationsService;
        this.detailsFinder = detailsFinder;
        this.accessInvitationsService = accessInvitationsService;
        this.emailValidator = emailValidator;
    }

    @ApiOperation(value = "Add a new invitation for email.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = ErrorDescriptionModel.class),
            @ApiResponse(code = 409, message = "Invalid email format."),
            @ApiResponse(code = 409, message = "User already exists."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize(IS_ADMIN_CONDITION)
    public ErrorDescriptionModel addInvitation(@RequestBody InvitationModel invitation,
                                               @ApiParam(hidden = true) Authentication authentication) {

        emailValidator.validate(invitation.getEmail());

        String userName = detailsFinder.findUserName(authentication);
        if (invitationsService.userExists(invitation.getEmail())) {
            throw new UserExistsException(String.format("User %s already exists", invitation.getEmail()));
        }

        return accessInvitationsService.getAccessInvitations(invitation.getEmail())
            .map(inv -> {
                inv.setEligibleToCreateOrg(true);
                accessInvitationsService.updateAccessInvitation(invitation.getEmail(), inv);
                return new ErrorDescriptionModel(ErrorDescriptionModel.State.UPDATED, "Updated pending invitation");
            }).orElseGet(() -> {
                accessInvitationsService.addEligibilityToCreateOrg(invitation.getEmail());
                String invitationLink = invitationsService.sendInviteEmail(invitation.getEmail(), userName);
                return new ErrorDescriptionModel(ErrorDescriptionModel.State.NEW, invitationLink);
            });
    }

    @ApiOperation(value = "Get pending invitations.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = String.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize(IS_ADMIN_CONDITION)
    public Set<String> getPendingInvitations() {
        return invitationsService.getPendingInvitationsEmails();
    }

    @ApiOperation(value = "Resend invitation to the email.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Invitation not found."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = RESEND_INVITATION_URL,  method = RequestMethod.POST)
    @PreAuthorize(IS_ADMIN_CONDITION)
    public void resendInvitation(@PathVariable("email") String email,
                                 @ApiParam(hidden = true) Authentication authentication) {
        String userName = detailsFinder.findUserName(authentication);
        invitationsService.resendInviteEmail(email, userName);
    }

    @ApiOperation(value = "Delete an invitation.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Invitation not found."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = DELETE_INVITATION_URL, method = RequestMethod.DELETE)
    @PreAuthorize(IS_ADMIN_CONDITION)
    public void deleteInvitation(@PathVariable("email") String email) {
        invitationsService.deleteInvitation(email);
    }


    @ExceptionHandler(InvitationNotSentException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected ErrorDescriptionModel invitationNotSend(InvitationNotSentException e) {
        return new ErrorDescriptionModel(ErrorDescriptionModel.State.ERROR, e.getInvContent());
    }
}
