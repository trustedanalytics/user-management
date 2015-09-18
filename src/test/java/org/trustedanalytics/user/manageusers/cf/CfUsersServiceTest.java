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
package org.trustedanalytics.user.manageusers.cf;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.CcOrg;
import org.trustedanalytics.cloud.cc.api.CcSpace;
import org.trustedanalytics.cloud.cc.api.manageusers.Role;
import org.trustedanalytics.cloud.cc.api.manageusers.User;
import org.trustedanalytics.cloud.uaa.UaaOperations;
import org.trustedanalytics.cloud.uaa.UserIdNamePair;
import org.trustedanalytics.org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.trustedanalytics.org.cloudfoundry.identity.uaa.scim.ScimUserFactory;
import org.trustedanalytics.user.invite.InvitationsService;
import org.trustedanalytics.user.invite.OrgUserInvitationService;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.manageusers.CfUsersService;
import org.trustedanalytics.user.manageusers.PasswordGenerator;
import org.trustedanalytics.user.manageusers.UserRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class CfUsersServiceTest {

    @Mock
    private UaaOperations uaaOperations;

    @Mock
    private PasswordGenerator passwordGenerator;

    @Mock
    private CcOperations ccClient;

    @Mock
    private InvitationsService invitationService;

    @Mock
    private AccessInvitationsService accessInvitationsService;

    @Test
    public void test_addOrgUser_userDontExist() {
        List<Role> roles = Collections.singletonList(Role.MANAGERS);
        UUID userGuid = UUID.randomUUID();
        UUID orgGuid = UUID.randomUUID();
        User expectedUser = new User("testuser", userGuid, roles, orgGuid);
        String password = "testpassword";
        UserRequest cfCreateUser = new UserRequest(expectedUser.getUsername());
        cfCreateUser.setRoles(roles);

        when(uaaOperations.findUserIdByName(expectedUser.getUsername())).thenReturn(Optional.empty());
        when(passwordGenerator.newPassword()).thenReturn(password);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, passwordGenerator, invitationService, accessInvitationsService);
        Optional<User> resultUser = cfUsersService.addOrgUser(cfCreateUser, orgGuid, "admin_test");
        //user was not created, invitation was sent
        assertFalse(resultUser.isPresent());

        verify(uaaOperations, never()).createUser(eq(expectedUser.getUsername()), eq(password));
        verify(ccClient, never()).assignOrgRole(eq(expectedUser.getGuid()), eq(orgGuid), eq(roles.get(0)));
    }

    @Test
    public void test_addOrgUser_userExists() {
        List<Role> roles = Collections.singletonList(Role.MANAGERS);
        UUID orgGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        User expectedUser = new User("testuser", userGuid, roles, orgGuid);
        String password = "testpassword";
        UserRequest cfCreateUser = new UserRequest(expectedUser.getUsername());
        cfCreateUser.setRoles(roles);

        ScimUser scimUser = ScimUserFactory.newUser(expectedUser.getUsername(), password);
        scimUser.setId(expectedUser.getGuid().toString());

        UserIdNamePair idNamePair = new UserIdNamePair();
        idNamePair.setGuid(expectedUser.getGuid());
        idNamePair.setUserName(expectedUser.getUsername());
        when(uaaOperations.findUserIdByName(expectedUser.getUsername())).thenReturn(Optional.ofNullable(idNamePair));

        when(uaaOperations.createUser(expectedUser.getUsername(), password)).thenReturn(scimUser);
        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, passwordGenerator, invitationService, accessInvitationsService);
        Optional<User> resultUser = cfUsersService.addOrgUser(cfCreateUser, orgGuid, "admin_test");
        assertThat(resultUser.get(), equalTo(expectedUser));

        verify(ccClient).assignOrgRole(eq(userGuid), eq(orgGuid), eq(roles.get(0)));
    }

    @Test
    public void test_isOrgManager_Positive() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        User user = new User("marian", userId, Role.MANAGERS);
        Collection<User> fakeUsers = new ArrayList<>();
        fakeUsers.add(user);

        when(ccClient.getOrgUsers(orgId, Role.MANAGERS)).thenReturn(fakeUsers);

        CfUsersService sut =
            new CfUsersService(ccClient, uaaOperations, passwordGenerator, invitationService, accessInvitationsService);

        assertTrue(sut.isOrgManager(userId, orgId));
        verify(ccClient).getOrgUsers(orgId, Role.MANAGERS);
    }

    @Test
    public void test_isOrgManager_Negative() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        Collection<User> emptyList = new ArrayList<>();

        when(ccClient.getOrgUsers(orgId, Role.MANAGERS)).thenReturn(emptyList);

        CfUsersService sut =
            new CfUsersService(ccClient, uaaOperations, passwordGenerator, invitationService, accessInvitationsService);

        assertFalse(sut.isOrgManager(userId, orgId));
        verify(ccClient).getOrgUsers(orgId, Role.MANAGERS);
    }

    @Test
    public void test_deleteUserFromOrg() {
        UUID orgGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();


        ArrayList<CcSpace> managedSpaces = new ArrayList<CcSpace>();
        managedSpaces.add(new CcSpace(UUID.randomUUID(), "test1", orgGuid));

        ArrayList<CcSpace> auditedSpaces = new ArrayList<CcSpace>();
        auditedSpaces.add(new CcSpace(UUID.randomUUID(), "test1", orgGuid));
        auditedSpaces.add(new CcSpace(UUID.randomUUID(), "test2", orgGuid));

        ArrayList<CcSpace> spaces = new ArrayList<CcSpace>();
        spaces.add(new CcSpace(UUID.randomUUID(), "test1", orgGuid));
        spaces.add(new CcSpace(UUID.randomUUID(), "test2", orgGuid));
        spaces.add(new CcSpace(UUID.randomUUID(), "test3", orgGuid));

        when(ccClient.getUsersSpaces(userGuid, Role.MANAGERS, orgGuid)).thenReturn(managedSpaces);
        when(ccClient.getUsersSpaces(userGuid, Role.AUDITORS, orgGuid)).thenReturn(auditedSpaces);
        when(ccClient.getUsersSpaces(userGuid, Role.DEVELOPERS, orgGuid)).thenReturn(spaces);
        when(ccClient.getUserOrgs(userGuid)).thenReturn(Collections.singletonList(new CcOrg(orgGuid, "testorg")));

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, passwordGenerator, invitationService, accessInvitationsService);
        cfUsersService.deleteUserFromOrg(userGuid, orgGuid);

        verify(ccClient, times(1)).revokeSpaceRole(eq(userGuid), any(), eq(Role.MANAGERS));
        verify(ccClient, times(2)).revokeSpaceRole(eq(userGuid), any(), eq(Role.AUDITORS));
        verify(ccClient, times(3)).revokeSpaceRole(eq(userGuid), any(), eq(Role.DEVELOPERS));

        verify(ccClient, times(1)).revokeOrgRole(eq(userGuid), eq(orgGuid), eq(Role.MANAGERS));
        verify(ccClient, times(1)).revokeOrgRole(eq(userGuid), eq(orgGuid), eq(Role.AUDITORS));
        verify(ccClient, times(1)).revokeOrgRole(eq(userGuid), eq(orgGuid), eq(Role.BILLING_MANAGERS));

        verify(uaaOperations, never()).createUser(any(), any());
    }

    @Test
    public void testAddSpaceUser_userDoesntExist_sendInvitation() {
        UUID orgGuid = UUID.randomUUID();
        UUID spaceGuid = UUID.randomUUID();
        String username = "czeslaw@example.com";
        String currentUsername = "kazimierz@example.com";
        when(uaaOperations.findUserIdByName(eq(username))).thenReturn(Optional.<UserIdNamePair>empty());
        when(accessInvitationsService.createOrUpdateInvitation(eq(username), any())).thenReturn(AccessInvitationsService.CreateOrUpdateState.CREATED);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, passwordGenerator, invitationService, accessInvitationsService);

        UserRequest ur = new UserRequest();
        ur.setOrgGuid(orgGuid.toString());
        ur.setRoles(Lists.newArrayList(Role.DEVELOPERS));
        ur.setUsername(username);
        Optional<User> result = cfUsersService.addSpaceUser(ur, spaceGuid, currentUsername);

        assertFalse(result.isPresent());
        verify(accessInvitationsService).createOrUpdateInvitation(eq(username), any());
        verify(invitationService).sendInviteEmail(eq(username), eq(currentUsername), any());
    }

    @Test
    public void testAddSpaceUser_userDoesExist_sendInvitation() {
        UUID orgGuid = UUID.randomUUID();
        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        String username = "czeslaw@example.com";
        String currentUsername = "kazimierz@example.com";
        UserIdNamePair pair = new UserIdNamePair();
        pair.setGuid(userGuid);
        pair.setUserName(username);

        when(uaaOperations.findUserIdByName(eq(username))).thenReturn(Optional.of(pair));
        when(accessInvitationsService.createOrUpdateInvitation(eq(username), any())).thenReturn(AccessInvitationsService.CreateOrUpdateState.UPDATED);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, passwordGenerator, invitationService, accessInvitationsService);

        UserRequest ur = new UserRequest();
        ur.setOrgGuid(orgGuid.toString());
        ur.setRoles(Lists.newArrayList(Role.DEVELOPERS));
        ur.setUsername(username);
        Optional<User> result = cfUsersService.addSpaceUser(ur, spaceGuid, currentUsername);

        assertTrue(result.isPresent());
        verify(accessInvitationsService, never()).createOrUpdateInvitation(eq(username), any());
        verify(invitationService, never()).sendInviteEmail(eq(username), eq(currentUsername), any());
        verify(ccClient).assignOrgRole(any(), eq(orgGuid), eq(Role.USERS));
        verify(ccClient).assignSpaceRole(any(), eq(spaceGuid), eq(Role.DEVELOPERS));
    }

    @Test
    public void testAddOrgUser_userDoesntExist_sendInvitation() {
        UUID orgGuid = UUID.randomUUID();
        String username = "czeslaw@example.com";
        String currentUsername = "kazimierz@example.com";
        when(uaaOperations.findUserIdByName(eq(username))).thenReturn(Optional.<UserIdNamePair>empty());
        when(accessInvitationsService.createOrUpdateInvitation(eq(username), any())).thenReturn(AccessInvitationsService.CreateOrUpdateState.CREATED);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, passwordGenerator, invitationService, accessInvitationsService);

        UserRequest ur = new UserRequest();
        ur.setOrgGuid(orgGuid.toString());
        ur.setRoles(Lists.newArrayList(Role.MANAGERS));
        ur.setUsername(username);
        Optional<User> result = cfUsersService.addOrgUser(ur, orgGuid, currentUsername);

        assertFalse(result.isPresent());
        verify(accessInvitationsService).createOrUpdateInvitation(eq(username), any());
        verify(invitationService).sendInviteEmail(eq(username), eq(currentUsername), any());
    }

    @Test
    public void testAddOrgUser_userDoesExist_sendInvitation() {
        UUID orgGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        String username = "czeslaw@example.com";
        String currentUsername = "kazimierz@example.com";
        UserIdNamePair pair = new UserIdNamePair();
        pair.setGuid(userGuid);
        pair.setUserName(username);

        when(uaaOperations.findUserIdByName(eq(username))).thenReturn(Optional.of(pair));
        when(accessInvitationsService.createOrUpdateInvitation(eq(username), any())).thenReturn(AccessInvitationsService.CreateOrUpdateState.UPDATED);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, passwordGenerator, invitationService, accessInvitationsService);

        UserRequest ur = new UserRequest();
        ur.setOrgGuid(orgGuid.toString());
        ur.setRoles(Lists.newArrayList(Role.BILLING_MANAGERS));
        ur.setUsername(username);
        Optional<User> result = cfUsersService.addOrgUser(ur, orgGuid, currentUsername);

        assertTrue(result.isPresent());
        verify(accessInvitationsService, never()).createOrUpdateInvitation(eq(username), any());
        verify(invitationService, never()).sendInviteEmail(eq(username), eq(currentUsername), any());
        verify(ccClient).assignOrgRole(any(), eq(orgGuid), eq(Role.BILLING_MANAGERS));
    }
}
