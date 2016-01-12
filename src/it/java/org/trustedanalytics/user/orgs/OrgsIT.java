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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import org.trustedanalytics.cloud.auth.AuthTokenRetriever;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.CcOrgsList;
import org.trustedanalytics.cloud.cc.api.CcOrgPermission;
import org.trustedanalytics.cloud.cc.api.CcSpace;
import org.trustedanalytics.cloud.cc.api.CcOrg;
import org.trustedanalytics.cloud.cc.api.Page;
import org.trustedanalytics.user.Application;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.users.orgs.OrgsTestsResources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
import rx.Observable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("in-memory")
public class OrgsIT {

    @Value("http://localhost:${local.server.port}")
    private String BASE_URL;

    @Value("${oauth.resource}/v2/organizations")
    private String cfOrgsUrl;

    @Value("${oauth.resource}/v2/spaces")
    private String cfSpacesUrl;

    @Value("${oauth.resource}/v2/users")
    private String cfUsersUrl;

    @Autowired
    private String TOKEN;

    //@Autowired
    //;private RestOperations userRestTemplate;

    @Autowired
    private CcOperations ccClient;

    @Autowired
    private AuthTokenRetriever tokenRetriever;

    @Autowired
    private UserDetailsFinder detailsFinder;

    @Before
    public void setUp() {
        when(tokenRetriever.getAuthToken(any(Authentication.class))).thenReturn(TOKEN);
    }

    @Test
    public void permissionsEndpoint_WithoutFilter_ShouldReturnAll() {
        CcOrgsList orgsReturnedByCf = OrgsTestsResources.getOrgsReturnedByCf();
        CcOrgsList emptyOrgList = new CcOrgsList();
        emptyOrgList.setOrgs(new ArrayList<>());

        when(detailsFinder.findUserId(Mockito.any())).thenReturn(UUID.randomUUID());


        /*Observable<CcOrg> orgs = Observable.from(orgsReturnedByCf.getOrgs());
        when(ccClient.getOrgs()).thenReturn(orgs);

        when(ccClient.getManagedOrganizations(any())).thenReturn(orgsReturnedByCf.getOrgs());
        when(ccClient.getAuditedOrganizations(any())).thenReturn(emptyOrgList.getOrgs());
        when(ccClient.getBillingManagedOrganizations(any())).thenReturn(emptyOrgList.getOrgs());*/

        when(ccClient.getUserPermissions(any(), any()))
                .thenReturn(
                        orgsReturnedByCf.getOrgs().stream().map(org ->
                                new CcOrgPermission(org,
                                        /*isManager*/true,
                                        /*isAuditor*/ false,
                                        /*isBillingManager*/false)).collect(Collectors.toList())

                );

        TestRestTemplate testRestTemplate = new TestRestTemplate();
        CcOrgPermission[] valueReturned =
            testRestTemplate.getForObject(BASE_URL + "/rest/orgs/permissions", CcOrgPermission[].class);

        assertEquals(2, valueReturned.length);
        for (CcOrgPermission permission : valueReturned) {
            assertTrue(permission.isManager());
        }
    }

    @Test
    public void getOrgsWithSpaces_shouldAskCfForSpacesAndReturnGroupedByOrg() {

        Page<CcSpace> SPACES_FROM_CF = new Page<>();
        SPACES_FROM_CF.setResources(OrgsTestsResources.getSpacesReturnedByCf().getSpaces());
        Page<CcOrg> ORGS_FROM_CF = new Page<>();
        ORGS_FROM_CF.setResources(OrgsTestsResources.getOrgsReturnedByCf().getOrgs());
        final String EXPECTED_ORGS_WITH_SPACES =
            OrgsTestsResources.getOrgsWithSpacesExpectedToBeReturnedBySc();

        //when(userRestTemplate.exchange(any(String.class), any(HttpMethod.class), any(null), eq(new ParameterizedTypeReference<Page<CcSpace>>() {} )))
        //        .thenReturn(new ResponseEntity<>(SPACES_FROM_CF, HttpStatus.OK));
        //when(userRestTemplate.exchange(any(String.class), any(HttpMethod.class), any(null), eq(new ParameterizedTypeReference<Page<CcOrg>>() {} )))
        //        .thenReturn(new ResponseEntity<>(ORGS_FROM_CF, HttpStatus.OK));

        Observable<CcOrg> orgs = Observable.from(OrgsTestsResources.getOrgsReturnedByCf().getOrgs());
        when(ccClient.getOrgs())
                .thenReturn(orgs);


        Observable<CcSpace> spaces = Observable.from(OrgsTestsResources.getSpacesReturnedByCf().getSpaces());
        when(ccClient.getSpaces())
                .thenReturn(spaces);


        when(detailsFinder.findUserId(Mockito.any())).thenReturn(UUID.randomUUID());


        when(ccClient.getManagedOrganizations(any()))
                .thenReturn(Collections.EMPTY_LIST);

        TestRestTemplate testRestTemplate = new TestRestTemplate();
        ResponseEntity<String> response = RestOperationsHelpers
            .getForEntityWithToken(testRestTemplate, TOKEN,
                    BASE_URL + OrgsController.GENERAL_ORGS_URL);

        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody(), equalTo(EXPECTED_ORGS_WITH_SPACES));
    }
}
