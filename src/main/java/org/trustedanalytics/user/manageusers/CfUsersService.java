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

import static java.util.stream.Collectors.toList;

import com.google.common.collect.ObjectArrays;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.CcSpace;
import org.trustedanalytics.cloud.cc.api.manageusers.Role;
import org.trustedanalytics.cloud.cc.api.manageusers.User;
import org.trustedanalytics.cloud.uaa.UaaOperations;
import org.trustedanalytics.cloud.uaa.UserIdNamePair;
import org.trustedanalytics.user.invite.AngularInvitationLinkGenerator;
import org.trustedanalytics.user.invite.InvitationsService;
import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.rest.InvitationModel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

public class CfUsersService implements UsersService {

    private final CcOperations ccClient;
    private final UaaOperations uaaClient;
    private final PasswordGenerator passwordGenerator;
    private final InvitationsService invitationsService;
    private final AccessInvitationsService accessInvitationsService;

    public CfUsersService(CcOperations ccClient,
                          UaaOperations uaaClient,
                          PasswordGenerator passwordGenerator,
                          InvitationsService invitationsService,
                          AccessInvitationsService accessInvitationsService) {
        super();
        this.ccClient = ccClient;
        this.uaaClient = uaaClient;
        this.passwordGenerator = passwordGenerator;
        this.invitationsService = invitationsService;
        this.accessInvitationsService = accessInvitationsService;
    }

    @Override
    public Collection<User> getOrgUsers(UUID orgGuid) {
        return getUsers(orgGuid, Role.ORG_ROLES, ccClient::getOrgUsers);
    }

    @Override
    public Collection<User> getSpaceUsers(UUID spaceGuid) {
        return getUsers(spaceGuid, Role.SPACE_ROLES, ccClient::getSpaceUsers);
    }

    @Override
    public User addOrgUser(UserRequest userRequest, UUID orgGuid, String currentUser) {
        UUID userGuid = retrieveOrInviteUser(userRequest.getUsername(), currentUser, null, orgGuid);
        if (userGuid != null) {
            Role[] roles = ObjectArrays
                    .concat(userRequest.getRoles().toArray(new Role[] {}), Role.USERS);
            assignOrgRolesToUser(userGuid, orgGuid, roles);

            return new User(userRequest.getUsername(), userGuid, userRequest.getRoles(), orgGuid);
        }
        return null;
    }

    @Override
    public User addSpaceUser(UserRequest userRequest, UUID spaceGuid, String currentUser) {
        UUID orgGuid = UUID.fromString(userRequest.getOrgGuid());
        UUID userGuid = retrieveOrInviteUser(userRequest.getUsername(), currentUser,
                UUID.fromString(userRequest.getOrgGuid()), spaceGuid);
        if (userGuid != null) {
            assignOrgRolesToUser(userGuid, orgGuid, Role.USERS);

            assignSpaceRolesToUser(userGuid, spaceGuid,
                    userRequest.getRoles().stream().toArray(Role[]::new));

            return new User(userRequest.getUsername(), userGuid, userRequest.getRoles(), orgGuid);
        }
        return null;
    }

    private UUID retrieveOrInviteUser(String username, String currentUser, UUID space, UUID org) {
        Optional<UserIdNamePair> idNamePair = uaaClient.findUserIdByName(username);
        return idNamePair.isPresent() ? idNamePair.get().getGuid() : inviteUser(username, currentUser, space, org);
    }

    private UUID inviteUser(String username, String currentUser, UUID spaceGuid, UUID orgGuid) {
        InvitationModel invitation = new InvitationModel();
        invitation.setEmail(username);
        invitation.setEligibleToCreateOrg(false);

        if (spaceGuid != null) {
            accessInvitationsService.addAccessInvitation(username, spaceGuid, AccessInvitations.AccessInvitationsType.SPACE);
        }
        accessInvitationsService.addAccessInvitation(username, orgGuid, AccessInvitations.AccessInvitationsType.ORG);
        invitationsService.sendInviteEmail(invitation.getEmail(), currentUser, new AngularInvitationLinkGenerator());
        return null;
    }

    @Override
    public User updateOrgUser(User user, UUID orgGuid) {
        Role[] rolesToRemove = Role.ORG_ROLES.stream()
            .filter(x -> !x.equals(Role.USERS) && !user.getRoles().contains(x))
            .toArray(Role[]::new);
        revokeOrgRolesFromUser(user.getGuid(), orgGuid, rolesToRemove);
        assignOrgRolesToUser(user.getGuid(), orgGuid, user.getRoles().stream().toArray(Role[]::new));
        return user;
    }

    @Override
    public User updateSpaceUser(User user, UUID spaceGuid) {
        Role.SPACE_ROLES.stream()
            .forEach(role -> ccClient.revokeSpaceRole(user.getGuid(), spaceGuid, role));
        assignSpaceRolesToUser(user.getGuid(), spaceGuid, user.getRoles().stream().toArray(Role[]::new));
        return user;
    }

    @Override
    public void assignOrgRolesToUser(UUID userGuid, UUID orgGuid, Role ... roles) {
        Arrays.stream(roles).forEach(role -> ccClient.assignOrgRole(userGuid, orgGuid, role));
    }

    @Override
    public void assignSpaceRolesToUser(UUID userGuid, UUID spaceGuid, Role ... roles) {
        Arrays.stream(roles).forEach(role -> ccClient.assignSpaceRole(userGuid, spaceGuid, role));
    }

    @Override
    public void revokeOrgRolesFromUser(UUID userGuid, UUID orgGuid, Role ... roles) {
        Arrays.stream(roles).forEach(role -> ccClient.revokeOrgRole(userGuid, orgGuid, role));
    }

    @Override
    public void revokeSpaceRolesFromUser(UUID userGuid, UUID spaceGuid, Role ... roles) {
        Arrays.stream(roles).forEach(role -> ccClient.revokeSpaceRole(userGuid, spaceGuid, role));
    }

    @Override
    public void deleteUserFromOrg(UUID userGuid, UUID orgGuid) {
        Role.SPACE_ROLES
            .stream()
            .forEach(role -> ccClient.getUsersSpaces(userGuid, role, orgGuid)
                .stream()
                .forEach(space -> ccClient.revokeSpaceRole(userGuid, space.getGuid(), role))
            );

        Role.ORG_ROLES.stream().forEach(role -> ccClient.revokeOrgRole(userGuid, orgGuid, role));
    }

    @Override
    public void deleteUserFromSpace(UUID userGuid, UUID spaceGuid) {
        Role.SPACE_ROLES.stream().forEach(role -> ccClient.revokeSpaceRole(userGuid, spaceGuid, role));
    }

    @Override
    public boolean isOrgManager(UUID userId, UUID orgId) {
        return ccClient.getOrgUsers(orgId, Role.MANAGERS)
            .stream()
            .map(user -> user.getGuid())
            .anyMatch(uuid -> uuid.equals(userId));
    }

    @Override
    public boolean isSpaceManager(UUID userId, UUID spaceId) {
        CcSpace space = ccClient.getSpace(spaceId);
        if(isOrgManager(userId, space.getOrgGuid())) {
            return true;
        }
        return ccClient.getSpaceUsers(spaceId, Role.MANAGERS)
            .stream()
            .map(user -> user.getGuid())
            .anyMatch(uuid -> uuid.equals(userId));
    }

    @Override
    public void deleteUser(UUID guid) {
        ccClient.deleteUser(guid);
        uaaClient.deleteUser(guid);
    }

    private Collection<User> getUsers(UUID guid, Set<Role> roles,
        BiFunction<UUID, Role, Collection<User>> getUserFunc) {
        List<User> users = roles.stream()
                .flatMap(r -> getUserFunc.apply(guid, r).stream())
            .collect(toList());

        Map<UUID, User> usersMap = new HashMap<>();
        for (User user : users) {
            User existing = usersMap.putIfAbsent(user.getGuid(), user);
            if (existing != null) {
                existing.appendRole(user.getRoles().get(0));
            }
        }

        uaaClient.findUserNames(usersMap.keySet())
            .forEach(user -> {
                User u = usersMap.get(user.getGuid());
                if (u != null) {
                    u.setUsername(user.getUserName());
                }
            });

        return usersMap.values().stream()
            .filter(p -> p.getUsername() != null)
            .map(p -> {
                p.getRoles().remove(Role.USERS);
                return p;
            })
            .collect(toList());
    }
}
