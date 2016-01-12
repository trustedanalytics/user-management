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

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.http.annotation.Immutable;
import org.hamcrest.CoreMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;
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
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.rest.EntityNotFoundException;
import org.trustedanalytics.user.invite.securitycode.NoSuchUserException;
import org.trustedanalytics.user.manageusers.CfUsersService;
import org.trustedanalytics.user.manageusers.PasswordGenerator;
import org.trustedanalytics.user.manageusers.UserRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.trustedanalytics.user.manageusers.UserRolesRequest;
import rx.Observable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class CfUsersServiceTest {

    @Mock
    private UaaOperations uaaOperations;

    @Mock
    private CcOperations ccClient;

    @Mock
    private InvitationsService invitationService;

    @Mock
    private AccessInvitationsService accessInvitationsService;

    class UserComparator implements Comparator<User> {
        @Override
        public int compare(User o1, User o2) {
            return o1.getGuid().compareTo(o2.getGuid());
        }
    }

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

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
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

        UserIdNamePair idNamePair = UserIdNamePair.of(
            expectedUser.getGuid(),
            expectedUser.getUsername());
        when(uaaOperations.findUserIdByName(expectedUser.getUsername())).thenReturn(Optional.ofNullable(idNamePair));

        when(uaaOperations.createUser(expectedUser.getUsername(), password)).thenReturn(scimUser);
        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
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
            new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);

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
            new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);

        assertFalse(sut.isOrgManager(userId, orgId));
        verify(ccClient).getOrgUsers(orgId, Role.MANAGERS);
    }

    @Test
    public void test_deleteUserFromOrg() {
        UUID orgGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();

        ArrayList<Role> roles = new ArrayList<>();
        roles.add(Role.USERS);
        User user = new User("testuser", userGuid, roles);
        ArrayList<User> users = new ArrayList<>();
        users.add(user);

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

        when(ccClient.getOrgUsers(orgGuid, Role.BILLING_MANAGERS)).thenReturn(Collections.emptyList());
        when(ccClient.getOrgUsers(orgGuid, Role.MANAGERS)).thenReturn(Collections.emptyList());
        when(ccClient.getOrgUsers(orgGuid, Role.AUDITORS)).thenReturn(Collections.emptyList());
        when(ccClient.getOrgUsers(orgGuid, Role.USERS)).thenReturn(users);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        cfUsersService.deleteUserFromOrg(userGuid, orgGuid);

        verify(ccClient, times(1)).revokeSpaceRole(eq(userGuid), any(), eq(Role.MANAGERS));
        verify(ccClient, times(2)).revokeSpaceRole(eq(userGuid), any(), eq(Role.AUDITORS));
        verify(ccClient, times(3)).revokeSpaceRole(eq(userGuid), any(), eq(Role.DEVELOPERS));

        verify(ccClient, times(1)).revokeOrgRole(eq(userGuid), eq(orgGuid), eq(Role.MANAGERS));
        verify(ccClient, times(1)).revokeOrgRole(eq(userGuid), eq(orgGuid), eq(Role.AUDITORS));
        verify(ccClient, times(1)).revokeOrgRole(eq(userGuid), eq(orgGuid), eq(Role.BILLING_MANAGERS));

        verify(uaaOperations, never()).createUser(any(), any());
    }

    @Test(expected = EntityNotFoundException.class)
    public void test_deleteUserFromOrg_userDoesNotExist_throwEntityNotFound() {
        UUID orgGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();

        when(ccClient.getUsersSpaces(userGuid, Role.MANAGERS, orgGuid)).thenReturn(Collections.emptyList());
        when(ccClient.getUsersSpaces(userGuid, Role.AUDITORS, orgGuid)).thenReturn(Collections.emptyList());
        when(ccClient.getUsersSpaces(userGuid, Role.DEVELOPERS, orgGuid)).thenReturn(Collections.emptyList());

        when(ccClient.getOrgUsers(orgGuid, Role.BILLING_MANAGERS)).thenReturn(Collections.emptyList());
        when(ccClient.getOrgUsers(orgGuid, Role.MANAGERS)).thenReturn(Collections.emptyList());
        when(ccClient.getOrgUsers(orgGuid, Role.AUDITORS)).thenReturn(Collections.emptyList());

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        cfUsersService.deleteUserFromOrg(userGuid, orgGuid);
    }

    @Test
    public void test_deleteUserFromSpace() {
        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();

        ArrayList<Role> roles = new ArrayList<>();
        roles.add(Role.AUDITORS);
        User user = new User("testuser", userGuid, roles);
        ArrayList<User> users = new ArrayList<>();
        users.add(user);

        when(ccClient.getSpaceUsers(spaceGuid, Role.MANAGERS)).thenReturn(Collections.emptyList());
        when(ccClient.getSpaceUsers(spaceGuid, Role.DEVELOPERS)).thenReturn(Collections.emptyList());
        when(ccClient.getSpaceUsers(spaceGuid, Role.AUDITORS)).thenReturn(users);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        cfUsersService.deleteUserFromSpace(userGuid, spaceGuid);

        verify(ccClient, times(1)).revokeSpaceRole(eq(userGuid), eq(spaceGuid), eq(Role.MANAGERS));
        verify(ccClient, times(1)).revokeSpaceRole(eq(userGuid), eq(spaceGuid), eq(Role.DEVELOPERS));
        verify(ccClient, times(1)).revokeSpaceRole(eq(userGuid), eq(spaceGuid), eq(Role.AUDITORS));
    }

    @Test(expected = EntityNotFoundException.class)
    public void test_deleteUserFromSpace_throwEntityNotFound() {
        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();

        when(ccClient.getSpaceUsers(spaceGuid, Role.MANAGERS)).thenReturn(Collections.emptyList());
        when(ccClient.getSpaceUsers(spaceGuid, Role.DEVELOPERS)).thenReturn(Collections.emptyList());
        when(ccClient.getSpaceUsers(spaceGuid, Role.AUDITORS)).thenReturn(Collections.emptyList());

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        cfUsersService.deleteUserFromSpace(userGuid, spaceGuid);
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
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);

        UserRequest ur = new UserRequest();
        ur.setOrgGuid(orgGuid.toString());
        ur.setRoles(Lists.newArrayList(Role.DEVELOPERS));
        ur.setUsername(username);
        Optional<User> result = cfUsersService.addSpaceUser(ur, spaceGuid, currentUsername);

        assertFalse(result.isPresent());
        verify(accessInvitationsService).createOrUpdateInvitation(eq(username), any());
        verify(invitationService).sendInviteEmail(eq(username), eq(currentUsername));
    }

    @Test
    public void testAddSpaceUser_userDoesExist_sendInvitation() {
        UUID orgGuid = UUID.randomUUID();
        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        String username = "czeslaw@example.com";
        String currentUsername = "kazimierz@example.com";
        UserIdNamePair pair = UserIdNamePair.of(userGuid, username);

        when(uaaOperations.findUserIdByName(eq(username))).thenReturn(Optional.of(pair));
        when(accessInvitationsService.createOrUpdateInvitation(eq(username), any())).thenReturn(AccessInvitationsService.CreateOrUpdateState.UPDATED);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);

        UserRequest ur = new UserRequest();
        ur.setOrgGuid(orgGuid.toString());
        ur.setRoles(Lists.newArrayList(Role.DEVELOPERS));
        ur.setUsername(username);
        Optional<User> result = cfUsersService.addSpaceUser(ur, spaceGuid, currentUsername);

        assertTrue(result.isPresent());
        verify(accessInvitationsService, never()).createOrUpdateInvitation(eq(username), any());
        verify(invitationService, never()).sendInviteEmail(eq(username), eq(currentUsername));
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
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);

        UserRequest ur = new UserRequest();
        ur.setOrgGuid(orgGuid.toString());
        ur.setRoles(Lists.newArrayList(Role.MANAGERS));
        ur.setUsername(username);
        Optional<User> result = cfUsersService.addOrgUser(ur, orgGuid, currentUsername);

        assertFalse(result.isPresent());
        verify(accessInvitationsService).createOrUpdateInvitation(eq(username), any());
        verify(invitationService).sendInviteEmail(eq(username), eq(currentUsername));
    }

    @Test
    public void testAddOrgUser_userDoesExist_sendInvitation() {
        UUID orgGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        String username = "czeslaw@example.com";
        String currentUsername = "kazimierz@example.com";
        UserIdNamePair pair = UserIdNamePair.of(userGuid, username);

        when(uaaOperations.findUserIdByName(eq(username))).thenReturn(Optional.of(pair));
        when(accessInvitationsService.createOrUpdateInvitation(eq(username), any())).thenReturn(AccessInvitationsService.CreateOrUpdateState.UPDATED);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);

        UserRequest ur = new UserRequest();
        ur.setOrgGuid(orgGuid.toString());
        ur.setRoles(Lists.newArrayList(Role.BILLING_MANAGERS));
        ur.setUsername(username);
        Optional<User> result = cfUsersService.addOrgUser(ur, orgGuid, currentUsername);

        assertTrue(result.isPresent());
        verify(accessInvitationsService, never()).createOrUpdateInvitation(eq(username), any());
        verify(invitationService, never()).sendInviteEmail(eq(username), eq(currentUsername));
        verify(ccClient).assignOrgRole(any(), eq(orgGuid), eq(Role.BILLING_MANAGERS));
    }

    @Test(expected = NoSuchUserException.class)
    public void updateOrgUserRoles_userDoesNotExistInOrg() {
        UUID orgGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        List<Role> roles = new ArrayList<>();
        UserRolesRequest userRolesRequest = new UserRolesRequest();
        userRolesRequest.setRoles(roles);

        when(ccClient.getOrgUsers(anyObject(), anyObject())).thenReturn(new ArrayList<User>());

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);

        cfUsersService.updateOrgUserRoles(userGuid, orgGuid, userRolesRequest);
    }

    @Test(expected = NoSuchUserException.class)
    public void updateSpaceUserRoles_userDoesNotExistInSpaceAndOrg() {
        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        List<Role> roles = new ArrayList<>();
        UserRolesRequest userRolesRequest = new UserRolesRequest();
        userRolesRequest.setRoles(roles);

        when(ccClient.getSpace(anyObject())).thenReturn(Observable.empty());
        when(ccClient.getSpaceUsers(anyObject(), anyObject())).thenReturn(new ArrayList<User>());
        when(ccClient.getOrgUsers(anyObject(), anyObject())).thenReturn(new ArrayList<User>());
        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);

        cfUsersService.updateSpaceUserRoles(userGuid, spaceGuid, userRolesRequest);
    }

    @Test
    public void updateSpaceUserRoles_userExistsButHaveNoRolesAssigned() {
        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        UUID orgGuid = UUID.randomUUID();
        CcSpace space = new CcSpace(spaceGuid, "randomName", orgGuid);
        String username = "mariusz@example.com";
        List<Role> orgRoles = Lists.newArrayList(Role.USERS);
        User users = new User(username, userGuid, orgRoles);
        List<User> orgUsers = Lists.newArrayList(users);

        when(ccClient.getSpace(anyObject())).thenReturn(Observable.just(space));
        when(ccClient.getSpaceUsers(anyObject(), anyObject())).thenReturn(new ArrayList<User>());
        when(ccClient.getOrgUsers(anyObject(), anyObject())).thenReturn(orgUsers);
        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);

        List<Role> newRoles = Lists.newArrayList(Role.AUDITORS);
        UserRolesRequest userAssignRolesRequest = new UserRolesRequest();
        userAssignRolesRequest.setRoles(newRoles);
        List<Role> resultRoles = cfUsersService.updateSpaceUserRoles(userGuid, spaceGuid, userAssignRolesRequest);

        assertTrue(resultRoles.equals(newRoles));
        verify(ccClient, never()).assignOrgRole(userGuid, spaceGuid, Role.USERS);
        verify(ccClient, times(1)).assignSpaceRole(userGuid, spaceGuid, Role.AUDITORS);
    }

    @Test
    public void updateOrgUserRoles() {
        UUID orgGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        String username = "mariusz@example.com";
        List<Role> currentRoles = Lists.newArrayList(Role.USERS, Role.AUDITORS);
        User orgUser = new User(username, userGuid, currentRoles);
        List<User> orgUsers = Lists.newArrayList(orgUser);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        when(ccClient.getOrgUsers(anyObject(), anyObject())).thenReturn(orgUsers);

        List<Role> expectedRoles = Lists.newArrayList(Role.AUDITORS, Role.MANAGERS, Role.BILLING_MANAGERS);
        UserRolesRequest userRolesRequest = new UserRolesRequest();
        userRolesRequest.setRoles(expectedRoles);
        List<Role> resultRoles = cfUsersService.updateOrgUserRoles(userGuid, orgGuid, userRolesRequest);

        assertTrue(resultRoles.equals(expectedRoles));
        verify(ccClient, never()).revokeOrgRole(userGuid, orgGuid, Role.USERS);
        verify(ccClient, never()).assignOrgRole(userGuid, orgGuid, Role.USERS);
        verify(ccClient, never()).revokeOrgRole(userGuid, orgGuid, Role.AUDITORS);
        verify(ccClient, times(1)).assignOrgRole(userGuid, orgGuid, Role.AUDITORS);
        verify(ccClient, times(1)).assignOrgRole(userGuid, orgGuid, Role.MANAGERS);
        verify(ccClient, times(1)).assignOrgRole(userGuid, orgGuid, Role.BILLING_MANAGERS);
    }

    @Test
    public void updateSpaceUserRoles() {
        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        UUID orgGuid = UUID.randomUUID();
        CcSpace space = new CcSpace(spaceGuid, "randomName", orgGuid);
        String username = "mariusz@example.com";
        List<Role> currentRoles = Lists.newArrayList(Role.USERS, Role.DEVELOPERS);
        User spaceUser = new User(username, userGuid, currentRoles);
        List<User> spaceUsers = Lists.newArrayList(spaceUser);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        when(ccClient.getSpaceUsers(anyObject(), anyObject())).thenReturn(spaceUsers);
        when(ccClient.getSpace(anyObject())).thenReturn(Observable.just(space));

        List<Role> expectedRoles = Lists.newArrayList(Role.DEVELOPERS, Role.AUDITORS, Role.MANAGERS);
        UserRolesRequest userRolesRequest = new UserRolesRequest();
        userRolesRequest.setRoles(expectedRoles);
        List<Role> resultRoles = cfUsersService.updateSpaceUserRoles(userGuid, spaceGuid, userRolesRequest);

        assertTrue(resultRoles.equals(expectedRoles));
        verify(ccClient, never()).revokeSpaceRole(userGuid, spaceGuid, Role.USERS);
        verify(ccClient, never()).assignSpaceRole(userGuid, spaceGuid, Role.USERS);
        verify(ccClient, never()).revokeSpaceRole(userGuid, spaceGuid, Role.DEVELOPERS);
        verify(ccClient, times(1)).assignSpaceRole(userGuid, spaceGuid, Role.DEVELOPERS);
        verify(ccClient, times(1)).assignSpaceRole(userGuid, spaceGuid, Role.AUDITORS);
        verify(ccClient, times(1)).assignSpaceRole(userGuid, spaceGuid, Role.MANAGERS);
    }


    @Test
    public void test_getOrgUsers() {

        UUID orgId = UUID.fromString("dcb23ded-4afc-4844-b88c-63840a87a6d0");

        List<String> userIds = ImmutableList.of("6a1c2f2c-5a65-4844-ad3f-f011e1b082e1",
                "f9db1460-9c10-4640-8621-2903f6f6571c", "3f4095d4-5b16-4d61-bf63-dda65aa782a8",
                "8982e872-f12d-4bce-8aa4-b53e349d10e5", "3f073ac6-88a9-40ba-83bc-52699a03da04");

        List<String> originalNames = ImmutableList.of("Czeslaw", "Leslaw", "Wieslaw", "Zdzislaw", "Miroslaw" );

        List<String> expectedNames = ImmutableList.of("Viacheslav", "Lavrenty", "Vadim", "Zakhary", "Maksim");

        List<List<Role>> originalRoles = ImmutableList.of(
            ImmutableList.of(Role.USERS, Role.MANAGERS),
            ImmutableList.of(Role.USERS, Role.MANAGERS, Role.BILLING_MANAGERS),
            ImmutableList.of(Role.USERS),
            ImmutableList.of(Role.USERS, Role.BILLING_MANAGERS),
            ImmutableList.of(Role.USERS, Role.AUDITORS, Role.BILLING_MANAGERS)
        );

        List<List<Role>> expectedRoles = ImmutableList.of(
                ImmutableList.of(Role.MANAGERS),
                ImmutableList.of(Role.MANAGERS, Role.BILLING_MANAGERS),
                ImmutableList.of(),
                ImmutableList.of(Role.BILLING_MANAGERS),
                ImmutableList.of(Role.AUDITORS, Role.BILLING_MANAGERS)
        );

        List<User> mockUsersList = new ArrayList<User>();
        List<User> expected = new ArrayList<User>();
        List<UserIdNamePair> idNamePairs = new ArrayList<>();
        for(int i =0; i < 5; i++) {
            UUID userGuid = UUID.fromString(userIds.get(i));
            mockUsersList.add(new User(originalNames.get(i), userGuid , originalRoles.get(i)));
            idNamePairs.add(UserIdNamePair.of(userGuid, expectedNames.get(i)));
            expected.add(new User(expectedNames.get(i), userGuid, expectedRoles.get(i)));
        }

        Role.ORG_ROLES.forEach(role -> when(ccClient.getOrgUsers(orgId, role))
                .thenReturn(getUsersFilteredByRole(mockUsersList, role)));

        Collection<UUID> userUUIDs = userIds.stream()
                .map(uid -> UUID.fromString(uid))
                .collect(Collectors.toSet());

        when(uaaOperations.findUserNames(userUUIDs))
                .thenReturn(idNamePairs);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        ArrayList<User> result = new ArrayList<User>(cfUsersService.getOrgUsers(orgId));


        Collections.sort(expected, new UserComparator());
        Collections.sort(result, new UserComparator());

        assertEquals(expected.size(), result.size());
        int l = expected.size();
        for(int i = 0; i < l; i++) {
            assertEquals(expected.get(i).getUsername(), result.get(i).getUsername());
            assertEquals(expected.get(i).getGuid(), result.get(i).getGuid());
            assertEquals(expected.get(i).getOrgGuid(), result.get(i).getOrgGuid());
            assertEquals(new HashSet<Role>(expected.get(i).getRoles()), new HashSet<Role>(result.get(i).getRoles()));
        }
    }

    private List<User> getUsersFilteredByRole(List<User> all, Role role) {
        return all.stream().filter(u -> u.getRoles().contains(role))
                .map(u -> new User(u.getUsername(), u.getGuid(), new ArrayList<Role>(asList(role))))
                        .collect(Collectors.toList());
    }

}
