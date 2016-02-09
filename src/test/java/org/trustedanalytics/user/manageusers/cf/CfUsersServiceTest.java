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
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.hamcrest.CoreMatchers;
import org.mockito.Mockito;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.CcOrg;
import org.trustedanalytics.cloud.cc.api.CcSpace;
import org.trustedanalytics.cloud.cc.api.manageusers.Role;
import org.trustedanalytics.cloud.cc.api.manageusers.User;
import org.trustedanalytics.cloud.cc.api.queries.Filter;
import org.trustedanalytics.cloud.cc.api.queries.FilterOperator;
import org.trustedanalytics.cloud.cc.api.queries.FilterQuery;
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

import java.util.*;
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
    public void test_getOrgUsers() {
        String username = "testuser";
        UUID userGuid = UUID.randomUUID();
        List<Role> roles = Collections.singletonList(Role.MANAGERS);
        UUID orgGuid = UUID.randomUUID();
        User testUser = new User(username, userGuid, roles, orgGuid);
        Observable<User> users = Observable.just(testUser);
        List<UserIdNamePair> fromUaa = Collections.singletonList(UserIdNamePair.of(testUser.getGuid(), testUser.getUsername()));

        when(ccClient.getOrgUsersWithRoles(orgGuid)).thenReturn(users);
        when(uaaOperations.findUserNames(anyCollection())).thenReturn(fromUaa);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        Collection<User> result = cfUsersService.getOrgUsers(orgGuid);

        verify(ccClient).getOrgUsersWithRoles(orgGuid);
        assertTrue(result.containsAll(users.toList().toBlocking().single()));
    }

    @Test
    public void test_getOrgUsers_usernameMissing_setUsernamesFromUaa() {
        UUID userGuid = UUID.randomUUID();
        List<Role> roles = Collections.singletonList(Role.MANAGERS);
        UUID orgGuid = UUID.randomUUID();
        User testUserWithoutUsername = new User(null, userGuid, roles, orgGuid);
        Observable<User> users = Observable.just(testUserWithoutUsername);
        String usernameFromUaa = "testuser";
        UserIdNamePair fromUaa = UserIdNamePair.of(userGuid, usernameFromUaa);

        when(ccClient.getOrgUsersWithRoles(orgGuid)).thenReturn(users);
        when(uaaOperations.findUserNames(anySet())).thenReturn(Collections.singletonList(fromUaa));

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        Collection<User> result = cfUsersService.getOrgUsers(orgGuid);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(user -> user.getUsername() == usernameFromUaa));
    }

    @Test
    public void test_getOrgUsers_filterOutInvalidUsersAndRedundantRoles() {
        String username = "testuser";
        UUID userGuid = UUID.randomUUID();
        List<Role> roles = new ArrayList<>();
        roles.add(Role.USERS);
        roles.add(Role.MANAGERS);
        UUID orgGuid = UUID.randomUUID();
        User validUserWithRedundantRoles = new User(username, userGuid, roles, orgGuid);

        UUID invalidUserGuid = UUID.randomUUID();
        User invalidUser = new User(null, invalidUserGuid, roles, orgGuid);

        Observable<User> users = Observable.just(validUserWithRedundantRoles, invalidUser);

        List<UserIdNamePair> fromUaa = new ArrayList<>();
        UserIdNamePair validFromUaa = UserIdNamePair.of(userGuid, username);
        UserIdNamePair invalidFromUaa = UserIdNamePair.of(invalidUserGuid, null);
        fromUaa.add(validFromUaa);
        fromUaa.add(invalidFromUaa);

        when(ccClient.getOrgUsersWithRoles(orgGuid)).thenReturn(users);
        when(uaaOperations.findUserNames(anyCollection())).thenReturn(fromUaa);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        Collection<User> result = cfUsersService.getOrgUsers(orgGuid);

        assertTrue(result.size() == 1);
        assertTrue(result.stream().noneMatch(user -> user.getRoles().contains(Role.USERS)));
        assertFalse(result.contains(invalidFromUaa));
    }

    @Test
    public void test_getSpaceUsersWithoutUsernameFilter_returnAll() {
        List<Role> roles = Collections.singletonList(Role.DEVELOPERS);
        UUID spaceGuid = UUID.randomUUID();
        UUID orgGuid = UUID.randomUUID();

        User testUser1 = new User("testuser", UUID.randomUUID(), roles, orgGuid);
        User testUser2 = new User("testuser2", UUID.randomUUID(), roles, orgGuid);

        Observable<User> users = Observable.just(testUser1, testUser2);

        List<UserIdNamePair> fromUaa = Lists.newArrayList(UserIdNamePair.of(testUser1.getGuid(), testUser1.getUsername()),
                UserIdNamePair.of(testUser2.getGuid(), testUser2.getUsername()));

        when(ccClient.getSpaceUsersWithRoles(spaceGuid)).thenReturn(users);
        when(uaaOperations.findUserNames(anyCollection())).thenReturn(fromUaa);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        Collection<User> result = cfUsersService.getSpaceUsers(spaceGuid, Optional.empty());

        verify(ccClient).getSpaceUsersWithRoles(spaceGuid);
        assertTrue(result.containsAll(users.toList().toBlocking().single()));
    }

    @Test
    public void test_getSpaceUsersWithUsernameFilter_returnOne() {
        String username = "testuser";
        List<Role> roles = Collections.singletonList(Role.DEVELOPERS);
        UUID spaceGuid = UUID.randomUUID();

        User testUser1 = new User(username, UUID.randomUUID(), roles);
        User testUser2 = new User("testuser2", UUID.randomUUID(), roles);
        Observable<User> users = Observable.just(testUser1, testUser2);

        List<UserIdNamePair> fromUaa = Lists.newArrayList(UserIdNamePair.of(testUser1.getGuid(), testUser1.getUsername()),
                UserIdNamePair.of(testUser2.getGuid(), testUser2.getUsername()));

        when(ccClient.getSpaceUsersWithRoles(spaceGuid)).thenReturn(users);
        when(uaaOperations.findUserNames(anyCollection())).thenReturn(fromUaa);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        Collection<User> result = cfUsersService.getSpaceUsers(spaceGuid, Optional.of(username));

        verify(ccClient).getSpaceUsersWithRoles(spaceGuid);
        assertTrue(result.contains(testUser1));
        assertFalse(result.contains(testUser2));
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
        String username = "testuser";


        ArrayList<Role> roles = new ArrayList<>();
        roles.add(Role.USERS);
        User user = new User(username, userGuid, roles);
        Observable<User> users = Observable.just(user);

        ArrayList<CcSpace> managedSpaces = new ArrayList<CcSpace>();
        managedSpaces.add(new CcSpace(UUID.randomUUID(), "test1", orgGuid));

        ArrayList<CcSpace> auditedSpaces = new ArrayList<CcSpace>();
        auditedSpaces.add(new CcSpace(UUID.randomUUID(), "test1", orgGuid));
        auditedSpaces.add(new CcSpace(UUID.randomUUID(), "test2", orgGuid));

        ArrayList<CcSpace> spaces = new ArrayList<CcSpace>();
        spaces.add(new CcSpace(UUID.randomUUID(), "test1", orgGuid));
        spaces.add(new CcSpace(UUID.randomUUID(), "test2", orgGuid));
        spaces.add(new CcSpace(UUID.randomUUID(), "test3", orgGuid));

        when(ccClient.getUsersSpaces(eq(userGuid), eq(Role.MANAGERS), anyObject())).thenReturn(managedSpaces);
        when(ccClient.getUsersSpaces(eq(userGuid), eq(Role.AUDITORS), anyObject())).thenReturn(auditedSpaces);
        when(ccClient.getUsersSpaces(eq(userGuid), eq(Role.DEVELOPERS), anyObject())).thenReturn(spaces);

        when(ccClient.getUserOrgs(userGuid)).thenReturn(Collections.singletonList(new CcOrg(orgGuid, "testorg")));

        when(ccClient.getOrgUsersWithRoles(orgGuid)).thenReturn(users);

        when(uaaOperations.findUserNames(anyCollection())).thenReturn(Lists.newArrayList(UserIdNamePair.of(userGuid, username)));
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

        when(ccClient.getUsersSpaces(eq(userGuid), eq(Role.MANAGERS), anyObject())).thenReturn(Collections.emptyList());
        when(ccClient.getUsersSpaces(eq(userGuid), eq(Role.AUDITORS), anyObject())).thenReturn(Collections.emptyList());
        when(ccClient.getUsersSpaces(eq(userGuid), eq(Role.DEVELOPERS), anyObject())).thenReturn(Collections.emptyList());
        when(ccClient.getOrgUsersWithRoles(orgGuid)).thenReturn(Observable.<User>empty());

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        cfUsersService.deleteUserFromOrg(userGuid, orgGuid);
    }

    @Test
    public void test_deleteUserFromSpace() {
        UUID spaceGuid = UUID.randomUUID();
        UUID userGuid = UUID.randomUUID();
        String username = "testuser";

        ArrayList<Role> roles = new ArrayList<>();
        roles.add(Role.AUDITORS);
        User user = new User(username, userGuid, roles);
        Observable<User> users = Observable.just(user);

        when(ccClient.getSpaceUsersWithRoles(spaceGuid)).thenReturn(users);
        when(uaaOperations.findUserNames(anyCollection())).thenReturn(Lists.newArrayList(UserIdNamePair.of(userGuid, username)));

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

        when(ccClient.getSpaceUsersWithRoles(spaceGuid)).thenReturn(Observable.empty());

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

        when(ccClient.getOrgUsersWithRoles(anyObject())).thenReturn(Observable.empty());

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
        when(ccClient.getSpaceUsersWithRoles(anyObject())).thenReturn(Observable.empty());
        when(ccClient.getOrgUsersWithRoles(anyObject())).thenReturn(Observable.empty());
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
        Observable<User> orgUsers = Observable.just(users);

        when(ccClient.getSpace(anyObject())).thenReturn(Observable.just(space));
        when(ccClient.getSpaceUsersWithRoles(spaceGuid)).thenReturn(Observable.empty());
        when(ccClient.getOrgUsersWithRoles(orgGuid)).thenReturn(orgUsers);
        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);

        List<Role> newRoles = Lists.newArrayList(Role.AUDITORS);
        UserRolesRequest userAssignRolesRequest = new UserRolesRequest();
        userAssignRolesRequest.setRoles(newRoles);

        when(uaaOperations.findUserNames(anyCollection())).thenReturn(Lists.newArrayList(UserIdNamePair.of(userGuid, username)));


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
        Observable<User> orgUsers = Observable.just(orgUser);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        when(ccClient.getOrgUsersWithRoles(orgGuid)).thenReturn(orgUsers);

        List<Role> expectedRoles = Lists.newArrayList(Role.AUDITORS, Role.MANAGERS, Role.BILLING_MANAGERS);
        UserRolesRequest userRolesRequest = new UserRolesRequest();
        userRolesRequest.setRoles(expectedRoles);

        when(uaaOperations.findUserNames(anyCollection())).thenReturn(Lists.newArrayList(UserIdNamePair.of(userGuid, username)));

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
        Observable<User> spaceUsers = Observable.just(spaceUser);

        CfUsersService cfUsersService =
                new CfUsersService(ccClient, uaaOperations, invitationService, accessInvitationsService);
        when(ccClient.getSpaceUsersWithRoles(spaceGuid)).thenReturn(spaceUsers);
        when(ccClient.getSpace(anyObject())).thenReturn(Observable.just(space));

        when(uaaOperations.findUserNames(anyCollection())).thenReturn(Lists.newArrayList(UserIdNamePair.of(userGuid, username)));


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
}
