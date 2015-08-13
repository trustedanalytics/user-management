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

import com.google.common.base.Strings;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class RedisAccessInvitationsService implements AccessInvitationsService {

    private static final String ACCESS_INVITATIONS_KEY = "access-invitations";

    private HashOperations<String, String, AccessInvitations> hashOps;

    public RedisAccessInvitationsService(RedisOperations<String, AccessInvitations> redisTemplate){
        this.hashOps = redisTemplate.opsForHash();
    }

    @Override
    public List<UUID> getAccessInvitations(String email, AccessInvitations.AccessInvitationsType type) {
        validateStringArgument(email);
        if (!hashOps.hasKey(ACCESS_INVITATIONS_KEY, email)) {
            return new LinkedList<>();
        }

        AccessInvitations userInvitations = hashOps.get(ACCESS_INVITATIONS_KEY, email);
        switch (type) {
            case ORG:
                return userInvitations.getOrgAccessInvitations();
            case SPACE:
                return userInvitations.getSpaceAccessInvitations();
            default:
                throw new IllegalArgumentException("Illegal access invitation type");
        }
    }

    @Override
    public boolean getOrgCreationEligibility(String email) {
        validateStringArgument(email);
        if (!hashOps.hasKey(ACCESS_INVITATIONS_KEY, email)) {
            return false;
        } else {
            return hashOps.get(ACCESS_INVITATIONS_KEY, email).isEligibleToCreateOrg();
        }
    }

    @Override
    public void addAccessInvitation(String email, UUID uuid, AccessInvitations.AccessInvitationsType type) {
        validateStringArgument(email);
        validateUUID(uuid);

        AccessInvitations userInvitations;

        if (hashOps.hasKey(ACCESS_INVITATIONS_KEY, email)) {
            userInvitations = hashOps.get(ACCESS_INVITATIONS_KEY, email);
        } else {
            userInvitations = new AccessInvitations(false);
        }

        switch (type) {
            case ORG:
                userInvitations.addOrgAccessInvitation(uuid);
                break;
            case SPACE:
                userInvitations.addSpaceAccessInvitation(uuid);
                break;
            default:
                throw new IllegalArgumentException("Illegal access invitation type");
        }

        hashOps.delete(ACCESS_INVITATIONS_KEY, email);
        hashOps.put(ACCESS_INVITATIONS_KEY, email, userInvitations);
    }

    @Override
    public void addEligibilityToCreateOrg(String email) {
        validateStringArgument(email);
        AccessInvitations userInvitations;

        if (hashOps.hasKey(ACCESS_INVITATIONS_KEY, email)) {
            userInvitations = hashOps.get(ACCESS_INVITATIONS_KEY, email);
            userInvitations.setEligibleToCreateOrg(true);
            hashOps.delete(ACCESS_INVITATIONS_KEY, email);
        } else {
            userInvitations = new AccessInvitations(true);
        }
        hashOps.put(ACCESS_INVITATIONS_KEY, email, userInvitations);
    }

    @Override
    public void useAccessInvitations(String email) {
        validateStringArgument(email);
        if (hashOps.hasKey(ACCESS_INVITATIONS_KEY, email)) {
            AccessInvitations userInvitations = hashOps.get(ACCESS_INVITATIONS_KEY, email);
            userInvitations.clearAccessInvitations();
            hashOps.delete(ACCESS_INVITATIONS_KEY, email);
            hashOps.put(ACCESS_INVITATIONS_KEY, email, userInvitations);
            cleanupAccessInvitations(email);
        }
    }

    private void cleanupAccessInvitations(String email) {
        validateStringArgument(email);
        if (hashOps.hasKey(ACCESS_INVITATIONS_KEY, email)) {
            AccessInvitations userInvitations = hashOps.get(ACCESS_INVITATIONS_KEY, email);
            if (userInvitations.getOrgAccessInvitations().isEmpty() &&
                    userInvitations.getSpaceAccessInvitations().isEmpty() &&
                    !userInvitations.isEligibleToCreateOrg()) {
                hashOps.delete(ACCESS_INVITATIONS_KEY, email);
            }
        }
    }

    private void validateStringArgument(String arg) {
        if(Strings.isNullOrEmpty(arg)) {
            throw new IllegalArgumentException("String argument is null or empty");
        }
    }

    private void validateUUID(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
    }
}
