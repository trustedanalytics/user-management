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

import static org.mockito.Matchers.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.trustedanalytics.user.orgs.RestOperationsHelpers.postForEntityWithToken;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.hamcrest.CoreMatchers;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.manageusers.Role;
import org.trustedanalytics.cloud.uaa.UaaOperations;
import org.trustedanalytics.cloud.uaa.UserIdNameList;
import org.trustedanalytics.cloud.uaa.UserIdNamePair;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.trustedanalytics.org.cloudfoundry.identity.uaa.scim.ScimUserFactory;
import org.trustedanalytics.user.Application;
import org.trustedanalytics.user.TestConfiguration;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.rest.InvitationModel;
import org.trustedanalytics.user.invite.rest.RegistrationModel;
import org.trustedanalytics.user.invite.securitycode.SecurityCodeService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestOperations;
import org.trustedanalytics.user.manageusers.UsersService;
import org.trustedanalytics.user.orgs.RestOperationsHelpers;
import org.trustedanalytics.cloud.cc.api.CcOrg;
import org.trustedanalytics.cloud.cc.api.Page;
import rx.Observable;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


@ActiveProfiles("in-memory")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest
public class InvitationsIT {

    @Value("http://localhost:${local.server.port}/")
    private String baseUrl;

    @Value("${smtp.email}")
    private String SUPPORT_EMAIL;

    @Autowired
    private SecurityCodeService codeService;

    @Autowired
    private CcOperations ccClient;

    @Autowired
    private UaaOperations uaaClient;


    @Autowired
    private AccessInvitationsService accessInvitationsService;

    @Autowired
    private String TOKEN;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MimeMessage mimeMessage;

    @Autowired
    private UserDetailsFinder detailsFinder;

    @Autowired
    private AccessInvitations accessInvitations;

    @Autowired
    private InvitationLinkGenerator invitationLinkGenerator;

    private TestRestTemplate restTemplate;

    private static final String USER = "user";

    private static final String INVITATION_MAIL = "invited@example.com";

    private static final String EMAIL_NAME = "Example Support";

    @Captor
    private ArgumentCaptor addressCaptor;

    @Before
    public void setUp() {
        restTemplate = new TestRestTemplate();
    }

    @Test
    public void addInvitation_sendInvitation_properConfiguration()
        throws MessagingException, UnsupportedEncodingException {
        MockitoAnnotations.initMocks(this);
        when(detailsFinder.findUserName(any(Authentication.class))).thenReturn(USER);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        setUpNotExistingUserRequest(INVITATION_MAIL);
        when(accessInvitationsService.getAccessInvitations(INVITATION_MAIL)).thenReturn(Optional.empty());
        when(invitationLinkGenerator.getLink(anyString())).thenReturn("http://example.com");

        InvitationModel invitation = InvitationModel.of(INVITATION_MAIL, false);

        ResponseEntity<String> response =
            postForEntityWithToken(restTemplate, TOKEN, baseUrl + "rest/invitations", invitation,
                    String.class);

        assertEquals(response.getStatusCode(), HttpStatus.OK);

        verify(mailSender).send(any(MimeMessage.class));
        verify(mimeMessage).addRecipients(any(Message.RecipientType.class), eq(INVITATION_MAIL));
        verify(mimeMessage).addFrom((Address[]) addressCaptor.capture());
        Address[] addresses = (Address[]) addressCaptor.getValue();
        assertEquals(new InternetAddress(SUPPORT_EMAIL, EMAIL_NAME), addresses[0]);
    }

    @Test
    public void getInvitation_invalidCode_notFoundStatus() {
        ResponseEntity<String> response =
            restTemplate.getForEntity(baseUrl + "rest/registrations/{code}", String.class, "asdf");
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    public void getInvitation_validCode_statusOkAndInvitationReturned() {
        String code = codeService.generateCode("test@test").getCode();
        ResponseEntity<String> response =
            restTemplate.getForEntity(baseUrl + "rest/registrations/{code}", String.class, code);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), containsString("test@test"));
    }

    @Test
    public void newRegistration_validData_statusOkAndUserCreated() {
        final String username = "validcode@test";
        final String password = "asdasd";
        final String organization = "testorg";
        final String defaultSpaceName = "default";
        final UUID orgGuid = UUID.randomUUID();
        final UUID userGuid = UUID.randomUUID();
        final UUID spaceGuid = UUID.randomUUID();
        final String userGuidString = userGuid.toString();
        final String code = codeService.generateCode(username).getCode();

        when(uaaClient.createUser(anyString(), anyString()))
            .thenReturn(new ScimUser(userGuidString, username, null, null));

        when(ccClient.getOrgs()).thenReturn(Observable.<CcOrg>empty());

        setUpNotExistingUserRequest(username);

        when(ccClient.createOrganization(organization)).thenReturn(orgGuid);
        when(ccClient.createSpace(orgGuid, defaultSpaceName)).thenReturn(spaceGuid);


        when(accessInvitations.isEligibleToCreateOrg()).thenReturn(true);
        when(accessInvitationsService.getAccessInvitations(anyString())).thenReturn(Optional.of(accessInvitations));
        when(accessInvitationsService.getOrgCreationEligibility(anyString())).thenReturn(true);

        ResponseEntity<String> response =
            restTemplate.getForEntity(baseUrl + "rest/registrations/{code}", String.class, code);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), containsString(username));

        RegistrationModel user = new RegistrationModel();
        user.setOrg(organization);
        user.setPassword(password);

        response = restTemplate
            .postForEntity(baseUrl + "rest/registrations?code={code}", user, String.class, code);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));

        verify(uaaClient).createUser(username, password);
        verify(ccClient).createUser(userGuid);
        verify(ccClient).createOrganization(organization);
        verify(ccClient).assignUserToOrganization(userGuid, orgGuid);
        verify(ccClient).assignUserToSpace(userGuid, spaceGuid);
    }

    private void setUpNotExistingUserRequest(String username) {
        UserIdNameList userList = new UserIdNameList();
        userList.setUsers(Lists.newArrayList());
        when(uaaClient.findUserIdByName(username)).thenReturn(Optional.<UserIdNamePair>empty());
    }

    private static String getCfCreateJSONExpectedResponse(String guidString) {
        //@formatter:off
        return
            "{\"metadata\":" +
                "{" +
                "\"guid\":\"" + guidString + "\"" +  //, " +
                //"\"created_at\":\"2013-09-19T21:56:36+00:00\", " +
                //"\"updated_at\":\"2013-09-19T21:56:36+00:00\"" +
                "}" +
                "}";
        //@formatter:on
    }
}
