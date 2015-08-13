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

import org.mockito.Spy;
import org.trustedanalytics.user.invite.rest.EntityNotFoundException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.junit.Before;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Profile("redis")
@RunWith(MockitoJUnitRunner.class)
public class RedisAccessInvitationsServiceTest {

    private static final String ACCESS_INVITATIONS_KEY = "access-invitations";
    private static final String USER_EMAIL = "email@example.com";
    private static final UUID ORG = UUID.randomUUID();
    private static final UUID SPACE = UUID.randomUUID();

    private AccessInvitationsService sut;

    @Mock
    private RedisOperations<String, AccessInvitations> redisAccessInvitationsTemplate;

    @Mock
    private AccessInvitations mockUserInvitations;

    @Mock
    private HashOperations<String, Object, Object> mockHashOps;

    @Before
    public void setUp() {
        when(redisAccessInvitationsTemplate.opsForHash()).thenReturn(mockHashOps);
        sut = new RedisAccessInvitationsService(redisAccessInvitationsTemplate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSpaceAccessInvitations_nullEmailGiven_throwIllegalArgument() {
        sut.getAccessInvitations(null, AccessInvitations.AccessInvitationsType.SPACE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSpaceAccessInvitations_emptyEmailGiven_throwIllegalArgument() {
        sut.getAccessInvitations("", AccessInvitations.AccessInvitationsType.SPACE);
    }

    @Test
    public void testGetSpaceAccessInvitations_userDoesNotExist_returnEmptyList() {
        List<UUID> emptyListFixture = new LinkedList<>();
        when(mockHashOps.hasKey(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(false);
        when(mockHashOps.get(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(mockUserInvitations);
        when(mockUserInvitations.getSpaceAccessInvitations()).thenReturn(emptyListFixture);

        List<UUID> spaceAccessInvitations = sut.getAccessInvitations(USER_EMAIL, AccessInvitations.AccessInvitationsType.SPACE);
        int size = spaceAccessInvitations.size();

        assertNotNull(spaceAccessInvitations);
        assertEquals(0, size);
    }

    @Test
    public void testGetSpaceAccessInvitations_noSpaceAccessInvitations_returnEmptyList() {
        List<UUID> emptyListFixture = new LinkedList<>();
        when(mockHashOps.hasKey(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(true);
        when(mockHashOps.get(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(mockUserInvitations);
        when(mockUserInvitations.getSpaceAccessInvitations()).thenReturn(emptyListFixture);

        sut.addEligibilityToCreateOrg(USER_EMAIL);

        List<UUID> spaceAccessInvitations = sut.getAccessInvitations(USER_EMAIL, AccessInvitations.AccessInvitationsType.SPACE);
        int size = spaceAccessInvitations.size();

        assertNotNull(spaceAccessInvitations);
        assertEquals(0, size);
    }

    @Test
    public void testGetSpaceAccessInvitations_spaceAccessesExist_returnNonEmptyList() {
        LinkedList<UUID> notEmptyListFixture = new LinkedList<>();
        notEmptyListFixture.add(UUID.randomUUID());
        when(mockHashOps.hasKey(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(true);
        when(mockHashOps.get(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(mockUserInvitations);
        when(mockUserInvitations.getSpaceAccessInvitations()).thenReturn(notEmptyListFixture);

        sut.addAccessInvitation(USER_EMAIL, SPACE, AccessInvitations.AccessInvitationsType.SPACE);
        List<UUID> spaceAccessInvitations = sut.getAccessInvitations(USER_EMAIL, AccessInvitations.AccessInvitationsType.SPACE);
        int size = spaceAccessInvitations.size();

        assertNotNull(spaceAccessInvitations);
        assertNotEquals(0, size);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOrgAccessInvitations_nullEmailGiven_throwIllegalArgument() {
        sut.getAccessInvitations(null, AccessInvitations.AccessInvitationsType.ORG);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOrgAccessInvitations_emptyEmailGiven_throwIllegalArgument() {
        sut.getAccessInvitations("", AccessInvitations.AccessInvitationsType.ORG);
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
    public void testGetOrgAccessInvitations_userDoesNotExist_returnEmptyList() {
        List<UUID> emptyListFixture = new LinkedList<>();
        when(mockHashOps.hasKey(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(false);
        when(mockHashOps.get(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(mockUserInvitations);
        when(mockUserInvitations.getOrgAccessInvitations()).thenReturn(emptyListFixture);

        List<UUID> orgAccessInvitations = sut.getAccessInvitations(USER_EMAIL, AccessInvitations.AccessInvitationsType.ORG);
        int size = orgAccessInvitations.size();

        assertNotNull(orgAccessInvitations);
        assertEquals(0, size);
    }

    @Test
    public void testGetOrgAccessInvitations_noOrgAccessInvitations_returnEmptyList() {
        List<UUID> emptyListFixture = new LinkedList<>();
        when(mockHashOps.hasKey(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(true);
        when(mockHashOps.get(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(mockUserInvitations);
        when(mockUserInvitations.getOrgAccessInvitations()).thenReturn(emptyListFixture);
        sut.addEligibilityToCreateOrg(USER_EMAIL);

        List<UUID> orgAccessInvitations = sut.getAccessInvitations(USER_EMAIL, AccessInvitations.AccessInvitationsType.ORG);
        int size = orgAccessInvitations.size();

        assertNotNull(orgAccessInvitations);
        assertEquals(0, size);
    }

    @Test
    public void testGetOrgAccessInvitations_orgAccessesExist_returnNonEmptyList() {
        sut.addAccessInvitation(USER_EMAIL, ORG, AccessInvitations.AccessInvitationsType.ORG);
        List<UUID> notEmptyListFixture = new LinkedList<>();
        notEmptyListFixture.add(UUID.randomUUID());
        when(mockHashOps.hasKey(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(true);
        when(mockHashOps.get(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(mockUserInvitations);
        when(mockUserInvitations.getOrgAccessInvitations()).thenReturn(notEmptyListFixture);

        List<UUID> orgAccessInvitations = sut.getAccessInvitations(USER_EMAIL, AccessInvitations.AccessInvitationsType.ORG);
        int size = orgAccessInvitations.size();

        assertNotNull(orgAccessInvitations);
        assertNotEquals(0, size);
    }

    @Test
    public void testGetOrgCreationEligibility_userEligibleToCreateOrg_returnTrue() {
        when(mockHashOps.hasKey(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(true);
        when(mockHashOps.get(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(mockUserInvitations);
        when(mockUserInvitations.isEligibleToCreateOrg()).thenReturn(true);

        sut.addEligibilityToCreateOrg(USER_EMAIL);
        boolean eligible = sut.getOrgCreationEligibility(USER_EMAIL);

        assertEquals(true, eligible);
    }

    @Test
    public void testGetOrgCreationEligibility_userNotEligibleToCreateOrg_returnFalse() {
        when(mockHashOps.hasKey(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(true);
        when(mockHashOps.get(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(mockUserInvitations);
        when(mockUserInvitations.isEligibleToCreateOrg()).thenReturn(false);

        sut.addAccessInvitation(USER_EMAIL, SPACE, AccessInvitations.AccessInvitationsType.SPACE);

        boolean eligible = sut.getOrgCreationEligibility(USER_EMAIL);

        assertEquals(false, eligible);
    }

    @Test
    public void testGetOrgCreationEligibility_userDoesNotExist_returnFalse() {
        when(mockHashOps.hasKey(ACCESS_INVITATIONS_KEY, USER_EMAIL)).thenReturn(false);

        boolean eligible = sut.getOrgCreationEligibility(USER_EMAIL);

        verify(mockUserInvitations, never()).isEligibleToCreateOrg();
        assertEquals(false, eligible);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddSpaceAccessInvitation_nullSpaceNameGiven_throwIllegalArgument() {
        sut.addAccessInvitation(USER_EMAIL, null, AccessInvitations.AccessInvitationsType.SPACE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddSpaceAccessInvitation_nullEmailGiven_throwIllegalArgument() {
        sut.addAccessInvitation(null, SPACE, AccessInvitations.AccessInvitationsType.SPACE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddSpaceAccessInvitation_emptyEmailGiven_throwIllegalArgument() {
        sut.addAccessInvitation("", SPACE, AccessInvitations.AccessInvitationsType.SPACE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddOrgAccessInvitation_nullOrgNameGiven_throwIllegalArgument() {
        sut.addAccessInvitation(USER_EMAIL, null, AccessInvitations.AccessInvitationsType.ORG);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddOrgAccessInvitation_nullEmailGiven_throwIllegalArgument() {
        sut.addAccessInvitation(null, ORG, AccessInvitations.AccessInvitationsType.ORG);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddOrgAccessInvitation_emptyEmailGiven_throwIllegalArgument() {
        sut.addAccessInvitation("", ORG, AccessInvitations.AccessInvitationsType.ORG);
    }
}