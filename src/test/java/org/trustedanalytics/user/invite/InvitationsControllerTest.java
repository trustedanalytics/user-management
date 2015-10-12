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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;
import org.trustedanalytics.cloud.uaa.UaaOperations;
import org.trustedanalytics.cloud.uaa.UserIdNameList;
import org.trustedanalytics.cloud.uaa.UserIdNamePair;
import org.trustedanalytics.user.common.BlacklistEmailValidator;
import org.trustedanalytics.user.common.InvitationPendingException;
import org.trustedanalytics.user.common.UserExistsException;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.rest.ErrorDescriptionModel;
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
import org.trustedanalytics.user.manageusers.UsersService;

import java.util.Optional;

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
    public void testAddInvitation_userDoesNotExistNoPendingInvitation_sendEmail() {
        InvitationModel invitation = InvitationModel.of(USER_EMAIL, true);
        doReturn(ADMIN_EMAIL).when(detailsFinder).findUserName(any(Authentication.class));

        when(invitationsService.userExists(anyString())).thenReturn(false);
        when(accessInvitationsService.getAccessInvitations(anyString())).thenReturn(Optional.empty());

        sut.addInvitation(invitation, null);

        verify(invitationsService).sendInviteEmail(
                eq(USER_EMAIL), eq(ADMIN_EMAIL));
    }

    @Test(expected = UserExistsException.class)
    public void testAddInvitation_userExist_throwUserExistsException() {
        InvitationModel invitation = InvitationModel.of(USER_EMAIL, true);
        doReturn(ADMIN_EMAIL).when(detailsFinder).findUserName(any(Authentication.class));

        when(invitationsService.userExists(anyString())).thenReturn(true);
        when(accessInvitationsService.getAccessInvitations(anyString())).thenReturn(Optional.empty());

        sut.addInvitation(invitation, null);
    }

    @Test
    public void testAddInvitation_userDoesNotExisInvitationPending_addEglibilityToCreateOrg() {
        InvitationModel invitation = InvitationModel.of(USER_EMAIL, true);
        doReturn(ADMIN_EMAIL).when(detailsFinder).findUserName(any(Authentication.class));

        when(invitationsService.userExists(anyString())).thenReturn(false);
        AccessInvitations accessInvitation = new AccessInvitations();
        when(accessInvitationsService.getAccessInvitations(anyString())).thenReturn(Optional.of(accessInvitation));

        ErrorDescriptionModel result =  sut.addInvitation(invitation, null);

        ArgumentCaptor<AccessInvitations> captor = new ArgumentCaptor<>();

        verify(accessInvitationsService).updateAccessInvitation(anyString(), captor.capture());
        assertTrue(captor.getValue().isEligibleToCreateOrg());

        assertEquals("Updated pending invitation", result.getDetails());
    }

    @Test(expected = WrongEmailAddressException.class)
    public void testAddInvitation_WrongEmailAddress() {
        String invalidEmail = "invalidEmail";
        InvitationModel invitation = InvitationModel.of(invalidEmail, true);
        sut.addInvitation(invitation, null);
    }
}
