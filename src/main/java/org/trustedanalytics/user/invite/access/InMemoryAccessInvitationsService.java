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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedList;

public class InMemoryAccessInvitationsService implements AccessInvitationsService {

    private Map<String, AccessInvitations> invitationsMap = new HashMap<String, AccessInvitations>();

    @Override
    public List<UUID> getAccessInvitations(String email, AccessInvitations.AccessInvitationsType type) {
        validateStringArgument(email);
        if (!invitationsMap.containsKey(email)) {
            return new LinkedList<>();
        }

        AccessInvitations userInvitations = invitationsMap.get(email);
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
        if (!invitationsMap.containsKey(email)) {
            return false;
        } else {
            return invitationsMap.get(email).isEligibleToCreateOrg();
        }
    }

    @Override
    public void addAccessInvitation(String email, UUID uuid, AccessInvitations.AccessInvitationsType type) {
        validateStringArgument(email);
        validateUUID(uuid);

        AccessInvitations userInvitations;

        if (invitationsMap.containsKey(email)) {
            userInvitations = invitationsMap.get(email);
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
        invitationsMap.remove(email);
        invitationsMap.put(email, userInvitations);
    }

    @Override
    public void addEligibilityToCreateOrg(String email) {
        validateStringArgument(email);

        AccessInvitations userInvitations;

        if (invitationsMap.containsKey(email)) {
            userInvitations = invitationsMap.get(email);
            userInvitations.setEligibleToCreateOrg(true);
        } else {
            userInvitations = new AccessInvitations(true);
        }
        invitationsMap.remove(email);
        invitationsMap.put(email, userInvitations);
    }

    @Override
    public void useAccessInvitations(String email) {
        validateStringArgument(email);
        if (invitationsMap.containsKey(email)) {
            invitationsMap.get(email).clearAccessInvitations();
            cleanupAccessInvitations(email);
        }
    }

    private void cleanupAccessInvitations(String email) {
        validateStringArgument(email);
        if (invitationsMap.containsKey(email)) {
            AccessInvitations userInvitations = invitationsMap.get(email);
            if (userInvitations.getOrgAccessInvitations().isEmpty() &&
                    userInvitations.getSpaceAccessInvitations().isEmpty() &&
                    !userInvitations.isEligibleToCreateOrg()) {
                invitationsMap.remove(email);
            }
        }
    }

    private void validateStringArgument(String arg) {
        if(Strings.isNullOrEmpty(arg)) {
            throw new IllegalArgumentException("String argument is null or empty.");
        }
    }

    private void validateUUID(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
    }
}
