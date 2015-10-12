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
import org.trustedanalytics.cloud.cc.api.manageusers.Role;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class AccessInvitationsService {
    public static enum CreateOrUpdateState {
        CREATED,
        UPDATED
    }

    private final AccessInvitationsStore store;

    public AccessInvitationsService(AccessInvitationsStore store){
        this.store = store;
    }

    public Optional<AccessInvitations> getAccessInvitations(String email) {
        validateStringArgument(email);
        return Optional.ofNullable(store.get(email));
    }

    public boolean getOrgCreationEligibility(String email) {
        validateStringArgument(email);
        return store.hasKey(email) && store.get(email).isEligibleToCreateOrg();
    }

    public void addEligibilityToCreateOrg(String email) {
        validateStringArgument(email);
        AccessInvitations userInvitations;

        if (store.hasKey(email)) {
            userInvitations = store.get(email);
            userInvitations.setEligibleToCreateOrg(true);
        } else {
            userInvitations = new AccessInvitations(true);
        }
        store.put(email, userInvitations);
    }

    public void updateAccessInvitation(String email, AccessInvitations invitations) {
        validateStringArgument(email);
        store.put(email, invitations);
    }

    public void useAccessInvitations(String email) {
        validateStringArgument(email);
        store.remove(email);
    }

    public CreateOrUpdateState createOrUpdateInvitation(String email, Consumer<AccessInvitations> consumer) {
        validateStringArgument(email);
        AccessInvitations userInvitations;
        CreateOrUpdateState state;

        if (store.hasKey(email)) {
            userInvitations = store.get( email);
            state = CreateOrUpdateState.UPDATED;
        } else {
            userInvitations = new AccessInvitations(false);
            state = CreateOrUpdateState.CREATED;
        }

        consumer.accept(userInvitations);
        store.put(email, userInvitations);
        return state;
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

    private void validateRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role needs to be provided");
        }
    }
}
