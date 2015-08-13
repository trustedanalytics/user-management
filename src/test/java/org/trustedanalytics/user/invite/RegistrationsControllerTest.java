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
import org.trustedanalytics.user.invite.rest.InvitationModel;
import org.trustedanalytics.user.invite.rest.RegistrationModel;
import org.trustedanalytics.user.invite.rest.RegistrationsController;
import org.trustedanalytics.user.invite.rest.EntityNotFoundException;
import org.trustedanalytics.user.invite.securitycode.InvalidSecurityCodeException;
import org.trustedanalytics.user.invite.securitycode.SecurityCode;
import org.trustedanalytics.user.invite.securitycode.SecurityCodeService;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.trustedanalytics.user.invite.access.AccessInvitationsService;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@RunWith(MockitoJUnitRunner.class)
public class RegistrationsControllerTest {

    private static final String USER_EMAIL = "email@example.com";
    private static final String SECURITY_CODE = "code";

    private RegistrationsController sut;

    @Mock
    private SecurityCodeService securityCodeService;

    @Mock
    private InvitationsService invitationsService;

    @Mock
    private AccessInvitationsService accessInvitationsService;

    @Before
    public void setUp() throws Exception {
        sut = new RegistrationsController(securityCodeService, invitationsService, accessInvitationsService);
    }

    @Test(expected = InvalidSecurityCodeException.class)
    public void testAddUser_nullCodeGiven_throwInvalidCode() {
        sut.addUser(new RegistrationModel(), null);
    }

    @Test(expected = InvalidSecurityCodeException.class)
    public void testAddUser_emptyCodeGiven_throwInvalidCode() {
        sut.addUser(new RegistrationModel(), "");
    }

    @Test(expected = InvalidSecurityCodeException.class)
    public void testAddUser_securityCodeInvalid_throwInvalidCode() {
        doThrow(new InvalidSecurityCodeException("")).when(securityCodeService).verify(Matchers.anyString());

        sut.addUser(new RegistrationModel(), SECURITY_CODE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddUser_passwordTooShort_throwIllegarArgument() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        registration.setPassword("123");

        sut.addUser(new RegistrationModel(), SECURITY_CODE);
    }

    @Test(expected = EntityAlreadyExists.class)
    public void testAddUser_createUserAlreadyExists_throwEntityAlreadyExists() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        registration.setPassword("123456");
        registration.setOrg("abcdefgh");
        doReturn(true).when(accessInvitationsService).getOrgCreationEligibility(Matchers.anyString());
        doThrow(new HttpClientErrorException(HttpStatus.CONFLICT)).when(invitationsService).createUser(
                Matchers.anyString(), Matchers.anyString(), Matchers.anyString());

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = EntityAlreadyExists.class)
    public void testAddUser_createUserAlreadyExistsNoOrg_throwEntityAlreadyExists() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        registration.setPassword("123456");
        doThrow(new HttpClientErrorException(HttpStatus.CONFLICT)).when(invitationsService).createUser(
                Matchers.anyString(), Matchers.anyString());

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = HttpClientErrorException.class)
    public void testAddUser_createUserHttpConnectionError_throwHttpError() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        registration.setPassword("123456");
        registration.setOrg("abcdefgh");
        doReturn(true).when(accessInvitationsService).getOrgCreationEligibility(Matchers.anyString());
        doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(invitationsService).createUser(
                Matchers.anyString(), Matchers.anyString(), Matchers.anyString());

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test(expected = HttpClientErrorException.class)
    public void testAddUser_createUserNoOrgHttpConnectionError_throwHttpError() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        registration.setPassword("123456");
        doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)).when(invitationsService).createUser(
                Matchers.anyString(), Matchers.anyString());

        sut.addUser(registration, SECURITY_CODE);
    }

    @Test()
    public void testAddUser_allOk_useCode() {
        SecurityCode sc = new SecurityCode(USER_EMAIL, SECURITY_CODE);
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());
        RegistrationModel registration = new RegistrationModel();
        registration.setPassword("123456");

        sut.addUser(registration, SECURITY_CODE);

        Mockito.verify(securityCodeService).use(sc);
    }

    @Test
    public void testGetInvitation_invitationExists_returnInvitation() {
        SecurityCode sc = mock(SecurityCode.class);
        doReturn(USER_EMAIL).when(sc).getEmail();
        doReturn(sc).when(securityCodeService).verify(Matchers.anyString());

        InvitationModel result = sut.getInvitation("");

        Assert.assertEquals(sc.getEmail(), result.getEmail());
    }

    @Test(expected = EntityNotFoundException.class)
    public void testGetInvitation_invitationExists_throwInvalidSecurityCode() {
        doThrow(new InvalidSecurityCodeException("")).when(securityCodeService).verify(Matchers.anyString());

        sut.getInvitation("");
    }

}
