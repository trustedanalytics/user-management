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

import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.trustedanalytics.cloud.cc.api.manageusers.Role;
import org.trustedanalytics.user.common.BlacklistEmailValidator;
import org.trustedanalytics.user.common.SpaceUserRolesValidator;
import org.trustedanalytics.user.common.WrongUserRolesException;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.invite.config.AccessTokenDetails;
import org.trustedanalytics.user.manageusers.UserRequest;
import org.trustedanalytics.user.manageusers.UsersController;
import org.trustedanalytics.user.manageusers.UsersService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class UsersControllerTest {
    private UsersController sut;
    private UserRequest req;

    @Mock
    private UsersService usersService;
    @Mock
    UsersService priviledgedUsersService;
    @Mock
    UserDetailsFinder detailsFinder;
    @Mock
    private Authentication userAuthentication;
    @Mock
    private BlacklistEmailValidator emailValidator;

    private SpaceUserRolesValidator spaceRolesValidator = new SpaceUserRolesValidator();

    @Before
    public void setup() {
        sut = new UsersController(usersService, priviledgedUsersService, detailsFinder, emailValidator, spaceRolesValidator);
        AccessTokenDetails details = new AccessTokenDetails(UUID.randomUUID());
        when(userAuthentication.getDetails()).thenReturn(details);
        req = new UserRequest();
    }

    @Test
    public void getOrgUsers_ByNonManager_PriviledgedServiceNotUsed() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);

        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(usersService.isOrgManager(userId, orgId)).thenReturn(false);
        sut.getOrgUsers(orgId, auth);

        verify(usersService).isOrgManager(userId, orgId);
        verify(usersService, times(1)).getOrgUsers(orgId);
        verify(priviledgedUsersService, times(0)).getOrgUsers(orgId);
    }

    @Test
    public void getOrgUsers_ByManager_PriviledgedServiceUsed() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);

        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(usersService.isOrgManager(userId, orgId)).thenReturn(true);
        sut.getOrgUsers(orgId, auth);

        verify(usersService).isOrgManager(userId, orgId);
        verify(usersService, times(0)).getOrgUsers(orgId);
        verify(priviledgedUsersService, times(1)).getOrgUsers(orgId);
    }

    @Test
    public void createOrgUser_ByNonManager_PriviledgedServiceNotUsed() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);

        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(detailsFinder.findUserName(auth)).thenReturn("admin_test");
        when(usersService.isOrgManager(userId, orgId)).thenReturn(false);
        sut.createOrgUser(req, orgId, auth);

        verify(usersService).isOrgManager(userId, orgId);
        verify(usersService, times(1)).addOrgUser(req, orgId, "admin_test");
        verify(priviledgedUsersService, times(0)).addOrgUser(req, orgId, "admin_test");
    }

    @Test
    public void deleteOrgUser_ByNonManager_PriviledgedServiceNotUsed() {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);

        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(usersService.isOrgManager(userId, orgId)).thenReturn(false);
        sut.deleteUserFromOrg(orgId, userId, auth);

        verify(usersService).isOrgManager(userId, orgId);
        verify(usersService, times(1)).deleteUserFromOrg(userId, orgId);
        verify(priviledgedUsersService, times(0)).deleteUserFromOrg(userId, orgId);
    }

    @Test(expected = WrongUserRolesException.class)
    public void createSpaceUser_ByNonManager_PriviledgedServiceNotUsed_EmptyRole() {
        UUID spaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);

        req.setRoles(new ArrayList<>());
        when(detailsFinder.findUserId(auth)).thenReturn(userId);
        when(detailsFinder.findUserName(auth)).thenReturn("admin_test");

        sut.createSpaceUser(req, spaceId, auth);

        verify(usersService).isSpaceManager(userId, spaceId);
        verify(usersService, times(1)).addSpaceUser(req, spaceId, "admin_test");
        verify(priviledgedUsersService, times(0)).addSpaceUser(req, spaceId, "admin_test");
    }
}
