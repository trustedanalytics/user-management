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

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class AccessInvitations {

    public enum AccessInvitationsType {
        ORG,
        SPACE
    }

    @Getter @Setter
    private boolean eligibleToCreateOrg;

    @Getter
    private List<UUID> orgAccessInvitations;

    @Getter
    private List<UUID> spaceAccessInvitations;

    public AccessInvitations() {
    }

    public AccessInvitations(boolean eligibleToCreateOrg) {
        this.eligibleToCreateOrg = eligibleToCreateOrg;
        this.orgAccessInvitations = new LinkedList<>();
        this.spaceAccessInvitations = new LinkedList<>();
    }

    public void addOrgAccessInvitation(UUID uuid) {
        this.orgAccessInvitations.add(uuid);
    }

    public void addSpaceAccessInvitation(UUID uuid) {
        this.spaceAccessInvitations.add(uuid);
    }

    public void clearAccessInvitations() {
        this.orgAccessInvitations.clear();
        this.spaceAccessInvitations.clear();
        this.eligibleToCreateOrg = false;
    }
}
