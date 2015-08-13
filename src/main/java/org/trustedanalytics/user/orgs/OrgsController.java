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
package org.trustedanalytics.user.orgs;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import org.trustedanalytics.cloud.cc.api.CcOperationsOrgsSpaces;
import org.trustedanalytics.cloud.cc.api.CcOrg;
import org.trustedanalytics.cloud.cc.api.CcSpace;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.manageusers.OrgNameRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.UUID;

@RestController
public class OrgsController {
    public static final String GENERAL_ORGS_URL = "/rest/orgs";

    private final CcOperationsOrgsSpaces ccClient;

    private final UserDetailsFinder detailsFinder;
    
    @Autowired
    public OrgsController(CcOperationsOrgsSpaces ccClient, UserDetailsFinder detailsFinder) {
        this.ccClient = ccClient;
        this.detailsFinder = detailsFinder;
    }

    @RequestMapping(value = GENERAL_ORGS_URL, method = GET,
        produces = APPLICATION_JSON_VALUE)
    public Collection<Organization> getOrgs(Authentication auth) {
        Collection<CcOrg> orgs = ccClient.getOrgs().toList().toBlocking().single();
        Collection<CcSpace> spaces = ccClient.getSpaces().toList().toBlocking().single();
        Collection<CcOrg> managedOrgs =
            ccClient.getManagedOrganizations(detailsFinder.findUserId(auth));
        return FormatTranslator.getOrganizationsWithSpaces(orgs, managedOrgs, spaces);
    }

    @RequestMapping(value = GENERAL_ORGS_URL
        + "/{org}/name", method = PUT, consumes = APPLICATION_JSON_VALUE)
    public void renameOrg(@RequestBody OrgNameRequest request, @PathVariable String org) {
        ccClient.renameOrg(UUID.fromString(org), request.getName());
    }

    @RequestMapping(value = GENERAL_ORGS_URL + "/{org}", method = DELETE)
    public void deleteOrg(@PathVariable String org) {
        ccClient.deleteOrg(UUID.fromString(org));
    }

    @RequestMapping(value = GENERAL_ORGS_URL, method = POST, consumes = APPLICATION_JSON_VALUE)
    public UUID createOrg(@RequestBody OrgNameRequest request) {
        return ccClient.createOrganization(request.getName());
    }
}
