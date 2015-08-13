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

import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.invite.AngularInvitationLinkGenerator;
import org.trustedanalytics.user.invite.InvitationNotSentException;
import org.trustedanalytics.user.invite.InvitationsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;

@RestController
@RequestMapping("/rest/invitations")
public class InvitationsController {

    public static final String IS_ADMIN_CONDITION = "hasRole('console.admin')";

    private final InvitationsService invitationsService;

    private final AccessInvitationsService accessInvitationsService;

    private final UserDetailsFinder detailsFinder;

    @Autowired
    public InvitationsController(InvitationsService invitationsService,
                                 UserDetailsFinder detailsFinder,
                                 AccessInvitationsService accessInvitationsService){
        this.invitationsService = invitationsService;
        this.detailsFinder = detailsFinder;
        this.accessInvitationsService = accessInvitationsService;
    }

    @RequestMapping(method = RequestMethod.POST)
    @PreAuthorize(IS_ADMIN_CONDITION)
    public ErrorDescriptionModel addInvitation
            (@RequestBody InvitationModel invitation,
             Authentication authentication) {

        String userName = detailsFinder.findUserName(authentication);

        accessInvitationsService.addEligibilityToCreateOrg(invitation.getEmail());

        String invitationLink = invitationsService.sendInviteEmail(invitation.getEmail(), userName,
                new AngularInvitationLinkGenerator());
        return new ErrorDescriptionModel(invitationLink);
    }

    @ExceptionHandler(InvitationNotSentException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    protected ErrorDescriptionModel invitationNotSend(InvitationNotSentException e) {
        return new ErrorDescriptionModel(e.getInvContent());
    }
}
