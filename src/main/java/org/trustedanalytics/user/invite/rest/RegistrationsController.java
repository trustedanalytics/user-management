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
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.trustedanalytics.cloud.cc.api.manageusers.User;
import org.trustedanalytics.user.common.UserPasswordValidator;
import org.trustedanalytics.user.invite.InvitationsService;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.securitycode.InvalidSecurityCodeException;
import org.trustedanalytics.user.invite.securitycode.SecurityCode;
import org.trustedanalytics.user.invite.securitycode.SecurityCodeService;

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/registrations")
public class RegistrationsController {

    private final SecurityCodeService securityCodeService;
    private final InvitationsService invitationsService;
    private final AccessInvitationsService accessInvitationsService;
    private final UserPasswordValidator userPasswordValidator;

    @Autowired
    public RegistrationsController(SecurityCodeService securityCodeService,
                                   InvitationsService invitationsService,
                                   AccessInvitationsService accessInvitationsService,
                                   UserPasswordValidator userPasswordValidator) {
        this.securityCodeService = securityCodeService;
        this.invitationsService = invitationsService;
        this.accessInvitationsService = accessInvitationsService;
        this.userPasswordValidator = userPasswordValidator;
    }

    @ApiOperation(value = "Registers new user using security code received in email message.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = RegistrationModel.class),
            @ApiResponse(code = 400, message = "Invalid organization name."),
            @ApiResponse(code = 403, message = "Security code 'code' empty or null"),
            @ApiResponse(code = 409, message = "Invalid password (empty or too short)."),
            @ApiResponse(code = 409, message = "Organization already exists."),
            @ApiResponse(code = 409, message = "User already exists."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(method = RequestMethod.POST)
    public RegistrationModel addUser(@RequestBody RegistrationModel newUser,
                                     @RequestParam(value = "code", required = false) String code) {
        if (Strings.isNullOrEmpty(code)) {
            throw new InvalidSecurityCodeException("Security code empty or null");
        }

        SecurityCode sc = securityCodeService.verify(code);

        userPasswordValidator.validate(newUser.getPassword());

        String email = sc.getEmail();
        if (accessInvitationsService.getOrgCreationEligibility(email)) {
            invitationsService.createUser(email, newUser.getPassword(), newUser.getOrg());
        }
        else {
            invitationsService.createUser(email, newUser.getPassword());
        }
        securityCodeService.redeem(sc);
        accessInvitationsService.redeemAccessInvitations(email);
        return newUser;
    }

    @ApiOperation(value = "Gets invitation using security code received in email message.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = InvitationModel.class),
            @ApiResponse(code = 403, message = "Security code 'code' empty or null"),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = "/{code}", method = RequestMethod.GET)
    public InvitationModel getInvitation(@PathVariable("code") String code) {
        try {
            final SecurityCode sc = securityCodeService.verify(code);
            return InvitationModel.of(sc.getEmail(), accessInvitationsService.getOrgCreationEligibility(sc.getEmail()));
        } catch (InvalidSecurityCodeException e) {
            throw new EntityNotFoundException("", e);
        }
    }

}
