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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import org.trustedanalytics.user.common.BlacklistEmailValidator;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.rest.InvitationModel;
import org.trustedanalytics.user.invite.rest.InvitationsController;
import org.trustedanalytics.user.invite.securitycode.SecurityCodeService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;


@RunWith(MockitoJUnitRunner.class)
public class InvitationsControllerTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String USER_EMAIL = "email@example.com";
    private List<String> forbiddenDomains = new ArrayList<>();

    private InvitationsController sut;

    @Mock
    private InvitationsService invitationsService;

    @Mock
    private SecurityCodeService securityCodeService;

    @Mock
    private UserDetailsFinder detailsFinder;

    private BlacklistEmailValidator emailValidator = new BlacklistEmailValidator(forbiddenDomains);

    @Mock
    private AccessInvitationsService accessInvitationsService;

    @Before
    public void setUp() throws Exception {
        sut = new InvitationsController(invitationsService, detailsFinder, accessInvitationsService, emailValidator);
    }

    @Test
    public void testAddInvitation_sendInvitationEmail() {
        InvitationModel invitation = InvitationModel.of(USER_EMAIL, true);
        doReturn(ADMIN_EMAIL).when(detailsFinder).findUserName(any(Authentication.class));
        sut.addInvitation(invitation, null);

        Mockito.verify(invitationsService).sendInviteEmail(
                eq(USER_EMAIL), eq(ADMIN_EMAIL), any(InvitationLinkGenerator.class));
    }

    @Test(expected = WrongEmailAddressException.class)
    public void testAddInvitation_WrongEmailAddress() {
        String invalidEmail = "invalidEmail";
        InvitationModel invitation = InvitationModel.of(invalidEmail, true);
        sut.addInvitation(invitation, null);
    }
}
