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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.trustedanalytics.cloud.cc.api.manageusers.User;
import org.trustedanalytics.cloud.cc.api.manageusers.Role;
import org.trustedanalytics.user.common.BlacklistEmailValidator;
import org.trustedanalytics.user.common.FormatUserRolesValidator;
import org.trustedanalytics.user.common.StringToUuidConverter;
import org.trustedanalytics.user.current.UserDetailsFinder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class UsersController {

    public static final String ORG_USERS_URL = "/rest/orgs/{org}/users";
    public static final String SPACE_USERS_URL = "/rest/spaces/{space}/users";
    
    private final UsersService usersService;
    private final UsersService priviledgedUsersService;
    private final UserDetailsFinder detailsFinder;
    private final BlacklistEmailValidator emailValidator;
    private final FormatUserRolesValidator formatRolesValidator;
    private final StringToUuidConverter stringToUuidConverter = new StringToUuidConverter();

    @Autowired
    public UsersController(UsersService usersService, UsersService priviledgedUsersService,
        UserDetailsFinder detailsFinder, BlacklistEmailValidator emailValidator, FormatUserRolesValidator formatRolesValidator) {
        this.usersService = usersService;
        this.priviledgedUsersService = priviledgedUsersService;
        this.detailsFinder = detailsFinder;
        this.emailValidator = emailValidator;
        this.formatRolesValidator = formatRolesValidator;
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

    @ApiOperation(value = "Returns list of users which has at least one role in the organization. NOTE: The CF role " +
        "'Users' is not included ")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = User.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = "Request was malformed. eg. 'org' is not a valid UUID or organization with" +
            "ID 'org' doesn't exist"),
        @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = ORG_USERS_URL, method = GET, produces = APPLICATION_JSON_VALUE)
    public Collection<User> getOrgUsers(@PathVariable String org, @ApiParam(hidden = true) Authentication auth) {
        UUID orgUuid = stringToUuidConverter.convert(org);
        return determinePriviledgeLevel(auth, AuthorizationScope.ORG, orgUuid)
            .getOrgUsers(orgUuid);
    }

    @ApiOperation(value = "Returns all users with given role within space identified by given GUID")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = User.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Request was malformed. eg. 'space' is not a valid UUID or space with" +
                    "ID 'space' doesn't exist"),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = SPACE_USERS_URL, method = GET, produces = APPLICATION_JSON_VALUE)
    public Collection<User> getSpaceUsers(@PathVariable String space, @ApiParam(hidden = true) Authentication auth,
                                          @RequestParam(value = "username") Optional<String> username) {
        UUID spaceUuid = stringToUuidConverter.convert(space);
        return determinePriviledgeLevel(auth, AuthorizationScope.SPACE, spaceUuid)
            .getSpaceUsers(spaceUuid, username);
    }

    @ApiOperation(value = "Sends invitations message for new users or returns user for existing one in organization.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = User.class),
            @ApiResponse(code = 400, message = "Request was malformed. eg. 'org' is not a valid UUID or organization with" +
                    "ID 'org' doesn't exist"),
            @ApiResponse(code = 409, message = "Email is not valid or it belongs to forbidden domains."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = ORG_USERS_URL, method = POST,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public User createOrgUser(@RequestBody UserRequest userRequest, @PathVariable String org,
                              @ApiParam(hidden = true) Authentication auth) {
        UUID orgUuid = stringToUuidConverter.convert(org);
        String currentUser = detailsFinder.findUserName(auth);
        emailValidator.validate(userRequest.getUsername());
        return determinePriviledgeLevel(auth, AuthorizationScope.ORG, orgUuid)
            .addOrgUser(userRequest, orgUuid, currentUser).orElse(null);
    }

    @ApiOperation(value = "Sends invitations message for new users or returns user for existing one in space.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = User.class),
            @ApiResponse(code = 400, message = "Request was malformed. eg. 'space' is not a valid UUID or space with" +
                    "ID 'space' doesn't exist"),
            @ApiResponse(code = 409, message = "Email is not valid or it belongs to forbidden domains."),
            @ApiResponse(code = 409, message = "User must have at least one role."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = SPACE_USERS_URL, method = POST,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public User createSpaceUser(@RequestBody UserRequest userRequest, @PathVariable String space,
                                @ApiParam(hidden = true) Authentication auth) {
        UUID spaceUuid = stringToUuidConverter.convert(space);
        String currentUser = detailsFinder.findUserName(auth);
        emailValidator.validate(userRequest.getUsername());
        formatRolesValidator.validateSpaceRoles(userRequest.getRoles());
        return determinePriviledgeLevel(auth, AuthorizationScope.SPACE, spaceUuid)
            .addSpaceUser(userRequest, spaceUuid, currentUser).orElse(null);
    }

    @ApiOperation(value = "Updates user roles in organization")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Role.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Request was malformed. eg. 'org' is not a valid UUID or organization with" +
                    "ID 'org' doesn't exist"),
            @ApiResponse(code = 404, message = "User not found in organization."),
            @ApiResponse(code = 409, message = "Roles should be specified."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = ORG_USERS_URL+"/{user}", method = POST,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public List<Role> updateOrgUserRoles(@RequestBody UserRolesRequest userRolesRequest, @PathVariable String org,
                                         @PathVariable String user, @ApiParam(hidden = true) Authentication auth) {
        formatRolesValidator.validateOrgRoles(userRolesRequest.getRoles());
        UUID userGuid = stringToUuidConverter.convert(user);
        UUID orgGuid = stringToUuidConverter.convert(org);
        return determinePriviledgeLevel(auth, AuthorizationScope.ORG, orgGuid)
                .updateOrgUserRoles(userGuid, orgGuid, userRolesRequest);
    }

    @ApiOperation(value = "Updates user roles in space")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = Role.class, responseContainer = "List"),
            @ApiResponse(code = 400, message = "Request was malformed. eg. 'space' is not a valid UUID or space with" +
                    "ID 'space' doesn't exist"),
            @ApiResponse(code = 404, message = "User not found in space."),
            @ApiResponse(code = 409, message = "User must have at least one role."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = SPACE_USERS_URL+"/{user}", method = POST,
            produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public List<Role> updateSpaceUserRoles(@RequestBody UserRolesRequest userRolesRequest, @PathVariable String space,
                                           @PathVariable String user, @ApiParam(hidden = true) Authentication auth) {
        formatRolesValidator.validateSpaceRoles(userRolesRequest.getRoles());
        UUID userGuid = stringToUuidConverter.convert(user);
        UUID spaceGuid = stringToUuidConverter.convert(space);
        return  determinePriviledgeLevel(auth, AuthorizationScope.SPACE, spaceGuid)
                .updateSpaceUserRoles(userGuid, spaceGuid, userRolesRequest);
    }

    @ApiOperation(value = "Deletes user from organization.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Request was malformed. eg. 'org' is not a valid UUID or organization with" +
                    "ID 'org' doesn't exist"),
            @ApiResponse(code = 404, message = "User 'user' not found in organization."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = ORG_USERS_URL+"/{user}", method = DELETE)
    public void deleteUserFromOrg(@PathVariable String org, @PathVariable String user,
                                  @ApiParam(hidden = true) Authentication auth) {
        UUID orgUuid = stringToUuidConverter.convert(org);
        UUID userUuid = stringToUuidConverter.convert(user);
        determinePriviledgeLevel(auth, AuthorizationScope.ORG, orgUuid)
            .deleteUserFromOrg(userUuid, orgUuid);
    }

    @ApiOperation(value = "Deletes user from space.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Request was malformed. eg. 'space' is not a valid UUID or space with" +
                    "ID 'space' doesn't exist"),
            @ApiResponse(code = 404, message = "User 'user' not found in space."),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = SPACE_USERS_URL+"/{user}", method = DELETE)
    public void deleteUserFromSpace(@PathVariable String space, @PathVariable String user,
                                    @ApiParam(hidden = true) Authentication auth) {
        UUID spaceUuid = stringToUuidConverter.convert(space);
        UUID userUuid = stringToUuidConverter.convert(user);
        determinePriviledgeLevel(auth, AuthorizationScope.SPACE, spaceUuid)
            .deleteUserFromSpace(userUuid, spaceUuid);
    }
}
