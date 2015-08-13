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
import org.trustedanalytics.user.common.BlacklistEmailValidator;
import org.trustedanalytics.user.current.UserDetailsFinder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.UUID;

@RestController
public class UsersController {

    public static final String ORG_USERS_URL = "/rest/orgs/{org}/users";
    public static final String SPACE_USERS_URL = "/rest/spaces/{space}/users";
    
    private final UsersService usersService;
    private final UsersService priviledgedUsersService;
    private final UserDetailsFinder detailsFinder;
    private final BlacklistEmailValidator emailValidator;
    
    @Autowired
    public UsersController(UsersService usersService, UsersService priviledgedUsersService,
        UserDetailsFinder detailsFinder, BlacklistEmailValidator emailValidator) {
        this.usersService = usersService;
        this.priviledgedUsersService = priviledgedUsersService;
        this.detailsFinder = detailsFinder;
        this.emailValidator = emailValidator;
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
    public Collection<User> getOrgUsers(@PathVariable UUID org, Authentication auth) {
        return determinePriviledgeLevel(auth, AuthorizationScope.ORG, org)
            .getOrgUsers(org);
    }

    @RequestMapping(value = SPACE_USERS_URL, method = GET, produces = APPLICATION_JSON_VALUE)
    public Collection<User> getSpaceUsers(@PathVariable UUID space, Authentication auth) {
        return determinePriviledgeLevel(auth, AuthorizationScope.SPACE, space)
            .getSpaceUsers(space);
    }

    @RequestMapping(value = ORG_USERS_URL, method = POST,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public User createOrgUser(@RequestBody UserRequest userRequest, @PathVariable UUID org, Authentication auth) {
        String currentUser = detailsFinder.findUserName(auth);
        emailValidator.validate(userRequest.getUsername());
        return determinePriviledgeLevel(auth, AuthorizationScope.ORG, org)
            .addOrgUser(userRequest, org, currentUser);
    }

    @RequestMapping(value = SPACE_USERS_URL, method = POST,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public User createSpaceUser(@RequestBody UserRequest userRequest, @PathVariable UUID space, Authentication auth) {
        String currentUser = detailsFinder.findUserName(auth);
        emailValidator.validate(userRequest.getUsername());
        return determinePriviledgeLevel(auth, AuthorizationScope.SPACE, space)
            .addSpaceUser(userRequest, space, currentUser);
    }

    @RequestMapping(value = ORG_USERS_URL+"/{user}", method = PUT,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public void updateOrgUser(@RequestBody UserRequest userRequest, @PathVariable UUID org, @PathVariable UUID user) {
        User userObj = new User(userRequest.getUsername(), user, userRequest.getRoles());
        usersService.updateOrgUser(userObj, org);
    }

    @RequestMapping(value = SPACE_USERS_URL+"/{user}", method = PUT,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public void updateSpaceUser(@RequestBody UserRequest userRequest, @PathVariable UUID space, @PathVariable UUID user) {
        User userObj = new User(userRequest.getUsername(), user, userRequest.getRoles(), UUID.fromString(userRequest.getOrgGuid()));
        usersService.updateSpaceUser(userObj, space);
    }

    @RequestMapping(value = ORG_USERS_URL+"/{user}", method = DELETE)
    public void deleteUserFromOrg(@PathVariable UUID org, @PathVariable UUID user, Authentication auth) {
        determinePriviledgeLevel(auth, AuthorizationScope.ORG, org)
            .deleteUserFromOrg(user, org);
    }

    @RequestMapping(value = SPACE_USERS_URL+"/{user}", method = DELETE)
    public void deleteUserFromSpace(@PathVariable UUID space, @PathVariable UUID user, Authentication auth) {
        determinePriviledgeLevel(auth, AuthorizationScope.SPACE, space)
            .deleteUserFromSpace(user, space);
    }
}
