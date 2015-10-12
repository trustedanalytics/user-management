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
package org.trustedanalytics.user.invite.access;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.trustedanalytics.user.invite.keyvaluestore.KeyValueStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


import java.util.UUID;
import java.util.function.Consumer;


@RunWith(MockitoJUnitRunner.class)
public class AccessInvitationsServiceTest {

    private static final String USER_EMAIL = "email@example.com";
    private static final UUID ORG = UUID.randomUUID();
    private static final UUID SPACE = UUID.randomUUID();

    private AccessInvitationsService sut;

    @Mock
    private AccessInvitations mockUserInvitations;

    @Mock
    private KeyValueStore<AccessInvitations> mockInvitationsStore;

    @Before
    public void setUp() {
        sut = new AccessInvitationsService(mockInvitationsStore);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSpaceAccessInvitations_nullEmailGiven_throwIllegalArgument() {
        sut.getAccessInvitations(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSpaceAccessInvitations_emptyEmailGiven_throwIllegalArgument() {
        sut.getAccessInvitations("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOrgCreationEligibility_nullEmailGiven_throwIllegalArgument() {
        sut.getOrgCreationEligibility(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOrgCreationEligibility_emptyEmailGiven_throwIllegalArgument() {
        sut.getOrgCreationEligibility("");
    }

    @Test
    public void testGetOrgCreationEligibility_userEligibleToCreateOrg_returnTrue() {
        when(mockInvitationsStore.hasKey(USER_EMAIL)).thenReturn(true);
        when(mockInvitationsStore.get(USER_EMAIL)).thenReturn(mockUserInvitations);

        sut.addEligibilityToCreateOrg(USER_EMAIL);
        when(mockUserInvitations.isEligibleToCreateOrg()).thenReturn(true);

        boolean eligible = sut.getOrgCreationEligibility(USER_EMAIL);

        assertTrue(eligible);
    }

    @Test
    public void testGetOrgCreationEligibility_userDoesNotExist_returnFalse() {
        boolean eligible = sut.getOrgCreationEligibility(USER_EMAIL);

        verify(mockUserInvitations, never()).isEligibleToCreateOrg();
        assertEquals(false, eligible);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOrUpdateInvitation_emptyEmail_throwIllegalArgument() {
        Consumer consumer = mock(Consumer.class);
        sut.createOrUpdateInvitation("", consumer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOrUpdateInvitation_nullEmail_throwIllegalArgument() {
        Consumer consumer = mock(Consumer.class);
        sut.createOrUpdateInvitation(null, consumer);
    }

    @Test
    public void testCreateOrUpdateInvitation_invitationDoesNotExists_createsNew() {
        when(mockInvitationsStore.hasKey(USER_EMAIL)).thenReturn(false);
        Consumer consumer = mock(Consumer.class);
        AccessInvitationsService.CreateOrUpdateState state = sut.createOrUpdateInvitation(USER_EMAIL, consumer);

        verify(consumer).accept(any());
        verify(mockInvitationsStore, never()).get(anyString());
        assertEquals(AccessInvitationsService.CreateOrUpdateState.CREATED, state);
    }

    @Test
    public void testCreateOrUpdateInvitation_invitationExists_replace() {
        when(mockInvitationsStore.hasKey(USER_EMAIL)).thenReturn(true);
        Consumer consumer = mock(Consumer.class);
        AccessInvitationsService.CreateOrUpdateState state = sut.createOrUpdateInvitation(USER_EMAIL, consumer);

        verify(consumer).accept(any());
        verify(mockInvitationsStore).get(anyString());
        assertEquals(AccessInvitationsService.CreateOrUpdateState.UPDATED, state);

    }



}
