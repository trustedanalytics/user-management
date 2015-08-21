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
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;

import java.util.Collection;
import java.util.UUID;


import org.trustedanalytics.cloud.cc.api.CcOperationsOrgsSpaces;
import org.trustedanalytics.cloud.cc.api.CcSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpacesController {

    public static final String GET_ALL_SPACES_URL = "/rest/spaces";
    public static final String GET_SPACES_OF_ORG_URL = "/rest/orgs/{org}/spaces";

    private final CcOperationsOrgsSpaces ccClient;

    @Autowired
    public SpacesController(CcOperationsOrgsSpaces ccClient) {
        this.ccClient = ccClient;
    }

    @RequestMapping(value = GET_ALL_SPACES_URL, method = GET, produces = APPLICATION_JSON_VALUE)
    public Collection<CcSpace> getSpaces() {
        return ccClient.getSpaces().toList().toBlocking().single();
    }

    @RequestMapping(value = GET_SPACES_OF_ORG_URL, method = GET, produces = APPLICATION_JSON_VALUE)
    public Collection<CcSpace> getSpaces(@PathVariable String org) {
        return ccClient.getSpaces(UUID.fromString(org)).toList().toBlocking().single();
    }

    @RequestMapping(value = GET_ALL_SPACES_URL, method = POST, consumes = APPLICATION_JSON_VALUE)
    public UUID createSpace(@RequestBody NewSpaceRequest request) {
        return ccClient.createSpace(request.getOrgGuid(), request.getName());
    }

    @RequestMapping(value = GET_ALL_SPACES_URL+"/{space}", method = DELETE)
    public void deleteSpace(@PathVariable String space) {
        ccClient.deleteSpace(UUID.fromString(space));
    }
}
