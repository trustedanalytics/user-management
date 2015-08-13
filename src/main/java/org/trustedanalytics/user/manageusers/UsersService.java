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
package org.trustedanalytics.user.manageusers;


import org.trustedanalytics.cloud.cc.api.manageusers.Role;
import org.trustedanalytics.cloud.cc.api.manageusers.User;

import java.util.Collection;
import java.util.UUID;

public interface UsersService {
    Collection<User> getOrgUsers(UUID orgGuid);

    Collection<User> getSpaceUsers(UUID spaceGuid);

    User addOrgUser(UserRequest userRequest, UUID org, String currentUser);
    
    User addSpaceUser(UserRequest userRequest, UUID spaceGuid, String currentUser);
    
    void deleteUser(UUID guid);
    
    User updateOrgUser(User user, UUID fromString);

    User updateSpaceUser(User user, UUID fromString);

    void revokeOrgRolesFromUser(UUID userGuid, UUID orgGuid, Role ... roles);

    void revokeSpaceRolesFromUser(UUID userGuid, UUID spaceGuid, Role ... roles);

    void assignOrgRolesToUser(UUID userGuid, UUID orgGuid, Role ... roles);

    void assignSpaceRolesToUser(UUID userGuid, UUID spaceGuid, Role ... roles);

    void deleteUserFromOrg(UUID userGuid, UUID orgId);

    void deleteUserFromSpace(UUID userGuid, UUID spaceId);

    boolean isOrgManager(UUID userId, UUID orgId);

    boolean isSpaceManager(UUID userId, UUID spaceId);
}
