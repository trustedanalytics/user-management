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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import org.trustedanalytics.cloud.auth.AuthTokenRetriever;
import org.trustedanalytics.cloud.cc.api.Page;
import org.trustedanalytics.cloud.cc.api.CcSpace;
import org.trustedanalytics.user.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestOperations;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("in-memory")
public class SpacesIT {

    @Value("http://localhost:${local.server.port}")
    private String BASE_URL;

    @Autowired
    private String TOKEN;

    @Autowired
    private RestOperations userRestTemplate;

    @Autowired
    private AuthTokenRetriever tokenRetriever;

    @Value("${oauth.resource}")
    private String cfApiBaseUrl;

    @Before
    public void setUp() {
        when(tokenRetriever.getAuthToken(any(Authentication.class))).thenReturn(TOKEN);
    }

    @Test
    public void getAllSpaces_shouldAskCloudfoundryForAllSpaces() {

        final String CF_SPACES_URL = cfApiBaseUrl+"/v2/spaces";

        Page<CcSpace> SPACES_FROM_CF = new Page<>();
        SPACES_FROM_CF.setResources(SpacesTestsResources.getSpacesReturnedByCf().getSpaces());
        final String EXPECTED_SPACES = SpacesTestsResources.getSpacesExpectedToBeReturnedBySc();

        when(userRestTemplate.exchange(any(String.class), any(HttpMethod.class), any(null), eq(new ParameterizedTypeReference<Page<CcSpace>>() {} )))
                .thenReturn(new ResponseEntity<>(SPACES_FROM_CF, HttpStatus.OK));

        TestRestTemplate testRestTemplate = new TestRestTemplate();
        ResponseEntity<String> response = RestOperationsHelpers.getForEntityWithToken(testRestTemplate, TOKEN,
            BASE_URL + SpacesController.GET_ALL_SPACES_URL);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), equalTo(EXPECTED_SPACES));

        PlatformVerifiers.verifySetTokenThenExchange(userRestTemplate, TOKEN, CF_SPACES_URL);
    }
    
    @Test
    public void getSpacesForSpecificOrg_shouldAskCloudfoundryForSpaces() {

        final String CF_SPACES_OF_ORG_URL =
            cfApiBaseUrl+"/v2/organizations/{org}/spaces?inline-relations-depth=1";

        final String ORG = "6b436ee1-de3c-4996-b312-bacd54ef301a";
        Map<String, Object> pathVars = new HashMap<>();
        pathVars.put("org", ORG);

        Page<CcSpace> SPACES_FROM_CF = new Page<>();
        SPACES_FROM_CF.setResources(SpacesTestsResources.getSpacesReturnedByCf().getSpaces());
        final String EXPECTED_SPACES = SpacesTestsResources.getSpacesExpectedToBeReturnedBySc();

        when(userRestTemplate.exchange(any(String.class), any(HttpMethod.class), any(null), eq(new ParameterizedTypeReference<Page<CcSpace>>() {}), any(Map.class)))
            .thenReturn(new ResponseEntity<>(SPACES_FROM_CF, HttpStatus.OK));

        TestRestTemplate testRestTemplate = new TestRestTemplate();
        ResponseEntity<String> response = RestOperationsHelpers.getForEntityWithToken(testRestTemplate, TOKEN,
            BASE_URL + SpacesController.GET_SPACES_OF_ORG_URL, pathVars);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), equalTo(EXPECTED_SPACES));

        PlatformVerifiers.verifySetTokenThenExchange(userRestTemplate, TOKEN, CF_SPACES_OF_ORG_URL, pathVars);
    }
}
