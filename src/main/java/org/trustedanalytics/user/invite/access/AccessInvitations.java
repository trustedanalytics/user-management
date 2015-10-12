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

import org.trustedanalytics.cloud.cc.api.manageusers.Role;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class AccessInvitations {

    @Getter @Setter
    private boolean eligibleToCreateOrg;

    @Getter
    private Map<UUID, Set<Role>> orgAccessInvitations;

    @Getter
    private Map<UUID, Set<Role>> spaceAccessInvitations;


    public AccessInvitations() {
        //Empty constructor is required by Redis Serializer to properly deserialize object
    }

    public AccessInvitations(boolean eligibleToCreateOrg) {
        this.eligibleToCreateOrg = eligibleToCreateOrg;
        this.orgAccessInvitations = new HashMap<>();
        this.spaceAccessInvitations = new HashMap<>();
    }

    public void addOrgAccessInvitation(UUID uuid, Set<Role> roles) {
        this.orgAccessInvitations.put(uuid, roles);
    }

    public void addSpaceAccessInvitation(UUID uuid, Set<Role> roles) {
        this.spaceAccessInvitations.put(uuid, roles);
    }

    public void clearAccessInvitations() {
        this.orgAccessInvitations.clear();
        this.spaceAccessInvitations.clear();
        this.eligibleToCreateOrg = false;
    }
}
