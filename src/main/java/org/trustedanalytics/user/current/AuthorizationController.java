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
package org.trustedanalytics.user.current;

import static java.util.stream.Collectors.toList;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.trustedanalytics.cloud.cc.api.CcOperationsOrgsSpaces;
import org.trustedanalytics.cloud.cc.api.CcOrgPermission;

import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
public class AuthorizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationController.class);

    private final CcOperationsOrgsSpaces ccClient;
    private final UserDetailsFinder detailsFinder;

    @Autowired
    public AuthorizationController(CcOperationsOrgsSpaces ccClient,
        UserDetailsFinder detailsFinder) {
        this.detailsFinder = detailsFinder;
        this.ccClient = ccClient;
    }

    @ApiOperation(value = "Returns permissions for user within specified organizations.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK", response = CcOrgPermission.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error, e.g. error connecting to CloudController")
    })
    @RequestMapping(value = "/rest/orgs/permissions", method = GET)
    public Collection<CcOrgPermission> getPermissions(@RequestParam(required = false) String orgs,
        Authentication authentication) {

        final List<UUID> organizations = new ArrayList<>();
        if (!Strings.isNullOrEmpty(orgs)) {
            organizations.addAll(
                Arrays.asList(orgs.split(",")).stream().map(UUID::fromString).collect(toList()));
        }

        return resolvePermissions(organizations, authentication);
    }

    /**
     * Returns permissions for user within specified organizations.
     *
     * @param orgs           UUIDs
     * @param authentication authentication
     * @return permissions
     */
    private Collection<CcOrgPermission> resolvePermissions(Collection<UUID> orgs,
        Authentication authentication) {
        final UUID user = detailsFinder.findUserId(authentication);
        final UserRole role = detailsFinder.getRole(authentication);

        LOGGER.info("Resolving permissions for user: {}", user.toString());
        return UserRole.ADMIN.equals(role) ?
            resolveAdminPermissions(orgs) :
            resolveUserPermissions(user, orgs);
    }

    /**
     * Returns permissions for specified organizations for administrator user. By default
     * administrators have access to every organization.
     *
     * @param orgs organizations
     * @return permissions
     */
    private Collection<CcOrgPermission> resolveAdminPermissions(Collection<UUID> orgs) {
        return ccClient.getOrgs()
            // filter organizations if at least one was specified, otherwise accept all
            .filter(org -> orgs.contains(org.getGuid()) || orgs.isEmpty())
            // grant full access
            .map(org -> new CcOrgPermission(org, true, true, true))
            .toList().toBlocking().single();
    }

    /**
     * Return permissions for specified organizations for regular user.
     *
     * @param user user GUID
     * @param orgs organizations
     * @return permissions
     */
    private Collection<CcOrgPermission> resolveUserPermissions(UUID user, Collection<UUID> orgs) {
        return ccClient.getUserPermissions(user, orgs);
    }

}
