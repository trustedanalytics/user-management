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
package org.trustedanalytics.users.orgs;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.trustedanalytics.cloud.cc.api.*;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.invite.config.AccessTokenDetails;
import org.trustedanalytics.user.manageusers.OrgNameRequest;
import org.trustedanalytics.user.orgs.Organization;
import org.trustedanalytics.user.orgs.OrgsController;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import rx.Observable;

import java.util.Collection;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class OrgsControllerTest {

    private OrgsController sut;

    @Mock
    private CcOperationsOrgsSpaces cfClient;

    @Mock
    private Authentication userAuthentication;

    @Mock
    private UserDetailsFinder detailsFinder;

    @Before
    public void Setup() {
        sut = new OrgsController(cfClient, detailsFinder);
    }

    @Test
    public void getOrgsWithSpaces_noInput_askCfForOrgsAndSpacesAndReturnCombinedData() {

        Observable<CcSpace> spacesFromCf = Observable.from( OrgsTestsResources.getSpacesReturnedByCfAdapter() );
        Observable<CcOrg> orgsFromCf = Observable.from( OrgsTestsResources.getOrgsReturnedByCfAdapter() );
        Collection<Organization> expectedOrgs =
            OrgsTestsResources.getOrgsWithSpacesExpectedToBeReturnedByScBeforeJsonization();

        when(cfClient.getSpaces()).thenReturn(spacesFromCf);
        when(cfClient.getOrgs()).thenReturn(orgsFromCf);

        AccessTokenDetails details = new AccessTokenDetails(UUID.randomUUID());
        when(userAuthentication.getDetails()).thenReturn(details);
        OAuth2Authentication auth = new OAuth2Authentication(null, userAuthentication);

        Collection<Organization> orgs = sut.getOrgs(auth);
        assertEquals(expectedOrgs, orgs);

        verify(cfClient).getSpaces();
        verify(cfClient).getOrgs();
    }

    @Test
    public void renameOrg_positive() {
        UUID orgId = UUID.randomUUID();
        String testName = "test-name";

        OrgNameRequest request = new OrgNameRequest();
        request.setName(testName);
        sut.renameOrg(request, orgId.toString());
        verify(cfClient).renameOrg(orgId, testName);
    }

    @Test(expected = RuntimeException.class)
    public void renameOrg_organization_does_not_exist() {
        UUID orgId = UUID.randomUUID();
        String testName = "test-name";

        OrgNameRequest request = new OrgNameRequest();
        request.setName(testName);

        doThrow(new RuntimeException()).when(cfClient).renameOrg(orgId, testName);
        sut.renameOrg(request, orgId.toString());
        verify(cfClient).renameOrg(orgId, testName);
    }

    @Test
    public void deleteOrg_positive() {
        UUID orgId = UUID.randomUUID();
        sut.deleteOrg(orgId.toString());
        verify(cfClient).deleteOrg(orgId);
    }

    @Test
    public void createOrg_validOrgNameRequest_requestSent() {
        // given
        final String orgName = "test-org-name";
        final OrgNameRequest orgNameRequest = new OrgNameRequest();
        orgNameRequest.setName(orgName);

        // when
        sut.createOrg(orgNameRequest);

        // then
        verify(cfClient).createOrganization(orgName);
    }
}
