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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.UUID;

import org.trustedanalytics.cloud.cc.api.CcOperationsOrgsSpaces;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.trustedanalytics.cloud.cc.api.CcSpace;
import org.trustedanalytics.user.orgs.SpacesController;
import rx.Observable;

@RunWith(MockitoJUnitRunner.class)
public class SpacesControllerTest {

    private SpacesController sut;

    @Mock private CcOperationsOrgsSpaces cfClient;

    @Before
    public void setUp() {
        sut = new SpacesController(cfClient);
    }

    @Test
    public void getSpaces_noInput_returnSpacesFromCloudfoundry() throws Exception {

        Observable<CcSpace> spacesReturnedByCfAdapter = Observable.from( OrgsTestsResources.getSpacesReturnedByCfAdapter() );

        when(cfClient.getSpaces()).thenReturn(spacesReturnedByCfAdapter);

        Collection<CcSpace> spaces = sut.getSpaces();
        assertEquals(spacesReturnedByCfAdapter.toList().toBlocking().single(), spaces);

        verify(cfClient).getSpaces();
    }

    @Test
    public void getSpaces_orgSpecified_returnSpacesFromCloudfoundry() {

        String org = "8efd7c5c-d83c-4786-b399-b7bd548839e1";
        String expectedSpaces = "list of spaces returned by cfClient";

        when(cfClient.getSpaces(any(UUID.class))).thenReturn(expectedSpaces);

        String spaces = sut.getSpaces(org);
        assertEquals(expectedSpaces, spaces);

        verify(cfClient).getSpaces(UUID.fromString(org));
    }
}
