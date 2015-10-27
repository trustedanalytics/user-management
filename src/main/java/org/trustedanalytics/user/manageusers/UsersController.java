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
package org.trustedanalytics.user.manageusers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import org.trustedanalytics.cloud.cc.api.manageusers.User;
import org.trustedanalytics.cloud.cc.api.manageusers.Role;
import org.trustedanalytics.user.common.BlacklistEmailValidator;
import org.trustedanalytics.user.common.SpaceUserRolesValidator;
import org.trustedanalytics.user.common.StringToUuidConverter;
import org.trustedanalytics.user.current.UserDetailsFinder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
public class UsersController {

    public static final String ORG_USERS_URL = "/rest/orgs/{org}/users";
    public static final String SPACE_USERS_URL = "/rest/spaces/{space}/users";
    
    private final UsersService usersService;
    private final UsersService priviledgedUsersService;
    private final UserDetailsFinder detailsFinder;
    private final BlacklistEmailValidator emailValidator;
    private final SpaceUserRolesValidator spaceRolesValidator;
    private final StringToUuidConverter stringToUuidConverter = new StringToUuidConverter();

    @Autowired
    public UsersController(UsersService usersService, UsersService priviledgedUsersService,
        UserDetailsFinder detailsFinder, BlacklistEmailValidator emailValidator, SpaceUserRolesValidator spaceRolesValidator) {
        this.usersService = usersService;
        this.priviledgedUsersService = priviledgedUsersService;
        this.detailsFinder = detailsFinder;
        this.emailValidator = emailValidator;
        this.spaceRolesValidator = spaceRolesValidator;
    }

    enum AuthorizationScope {
        ORG,
        SPACE
    }

    private UsersService determinePriviledgeLevel(Authentication auth, AuthorizationScope scope,
        UUID scopeId) {
        UUID userId = detailsFinder.findUserId(auth);
        if(scope == AuthorizationScope.ORG && usersService.isOrgManager(userId, scopeId)) {
            return priviledgedUsersService;
        }
        if(scope == AuthorizationScope.SPACE && usersService.isSpaceManager(userId, scopeId)) {
            return priviledgedUsersService;
        }
        return usersService;
    }

    @RequestMapping(value = ORG_USERS_URL, method = GET, produces = APPLICATION_JSON_VALUE)
    public Collection<User> getOrgUsers(@PathVariable String org, Authentication auth) {
        UUID orgUuid = stringToUuidConverter.convert(org);
        return determinePriviledgeLevel(auth, AuthorizationScope.ORG, orgUuid)
            .getOrgUsers(orgUuid);
    }

    @RequestMapping(value = SPACE_USERS_URL, method = GET, produces = APPLICATION_JSON_VALUE)
    public Collection<User> getSpaceUsers(@PathVariable String space, Authentication auth) {
        UUID spaceUuid = stringToUuidConverter.convert(space);
        return determinePriviledgeLevel(auth, AuthorizationScope.SPACE, spaceUuid)
            .getSpaceUsers(spaceUuid);
    }

    @RequestMapping(value = ORG_USERS_URL, method = POST,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public User createOrgUser(@RequestBody UserRequest userRequest, @PathVariable String org, Authentication auth) {
        UUID orgUuid = stringToUuidConverter.convert(org);
        String currentUser = detailsFinder.findUserName(auth);
        emailValidator.validate(userRequest.getUsername());
        return determinePriviledgeLevel(auth, AuthorizationScope.ORG, orgUuid)
            .addOrgUser(userRequest, orgUuid, currentUser).orElse(null);
    }

    @RequestMapping(value = SPACE_USERS_URL, method = POST,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public User createSpaceUser(@RequestBody UserRequest userRequest, @PathVariable String space, Authentication auth) {
        UUID spaceUuid = stringToUuidConverter.convert(space);
        String currentUser = detailsFinder.findUserName(auth);
        emailValidator.validate(userRequest.getUsername());
        spaceRolesValidator.validate(userRequest.getRoles());
        return determinePriviledgeLevel(auth, AuthorizationScope.SPACE, spaceUuid)
            .addSpaceUser(userRequest, spaceUuid, currentUser).orElse(null);
    }

    @RequestMapping(value = ORG_USERS_URL+"/{user}", method = POST,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public List<Role> updateOrgUserRoles(@RequestBody UserRolesRequest userRolesRequest, @PathVariable String org, @PathVariable String user) {
        UUID userGuid = stringToUuidConverter.convert(user);
        UUID orgGuid = stringToUuidConverter.convert(org);
        return usersService.updateOrgUserRoles(userGuid, orgGuid, userRolesRequest);
    }

    @RequestMapping(value = SPACE_USERS_URL+"/{user}", method = POST,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public List<Role> updateSpaceUserRoles(@RequestBody UserRolesRequest userRolesRequest, @PathVariable String space, @PathVariable String user) {
        UUID userGuid = stringToUuidConverter.convert(user);
        UUID spaceGuid = stringToUuidConverter.convert(space);
        return usersService.updateSpaceUserRoles(userGuid, spaceGuid, userRolesRequest);
    }

    @RequestMapping(value = ORG_USERS_URL+"/{user}", method = DELETE)
    public void deleteUserFromOrg(@PathVariable String org, @PathVariable String user, Authentication auth) {
        UUID orgUuid = stringToUuidConverter.convert(org);
        UUID userUuid = stringToUuidConverter.convert(user);
        determinePriviledgeLevel(auth, AuthorizationScope.ORG, orgUuid)
            .deleteUserFromOrg(userUuid, orgUuid);
    }

    @RequestMapping(value = SPACE_USERS_URL+"/{user}", method = DELETE)
    public void deleteUserFromSpace(@PathVariable String space, @PathVariable String user, Authentication auth) {
        UUID spaceUuid = stringToUuidConverter.convert(space);
        UUID userUuid = stringToUuidConverter.convert(user);
        determinePriviledgeLevel(auth, AuthorizationScope.SPACE, spaceUuid)
            .deleteUserFromSpace(userUuid, spaceUuid);
    }
}
