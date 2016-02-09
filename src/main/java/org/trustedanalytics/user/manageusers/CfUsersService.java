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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.tuple.Pair;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.CcSpace;
import org.trustedanalytics.cloud.cc.api.manageusers.Role;
import org.trustedanalytics.cloud.cc.api.manageusers.User;
import org.trustedanalytics.cloud.cc.api.queries.Filter;
import org.trustedanalytics.cloud.cc.api.queries.FilterOperator;
import org.trustedanalytics.cloud.cc.api.queries.FilterQuery;
import org.trustedanalytics.cloud.uaa.UaaOperations;
import org.trustedanalytics.cloud.uaa.UserIdNamePair;
import org.trustedanalytics.user.invite.AngularInvitationLinkGenerator;
import org.trustedanalytics.user.invite.InvitationsService;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.rest.EntityNotFoundException;

import org.trustedanalytics.user.invite.securitycode.NoSuchUserException;
import rx.Observable;
import rx.observables.BlockingObservable;
import rx.observables.GroupedObservable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CfUsersService implements UsersService {

    private final CcOperations ccClient;
    private final UaaOperations uaaClient;
    private final InvitationsService invitationsService;
    private final AccessInvitationsService accessInvitationsService;

    public CfUsersService(CcOperations ccClient,
                          UaaOperations uaaClient,
                          InvitationsService invitationsService,
                          AccessInvitationsService accessInvitationsService) {
        super();
        this.ccClient = ccClient;
        this.uaaClient = uaaClient;
        this.invitationsService = invitationsService;
        this.accessInvitationsService = accessInvitationsService;
    }

    @Override
    public Collection<User> getOrgUsers(UUID orgGuid) {
        return getUsersWithRoles(orgGuid, ccClient::getOrgUsersWithRoles);
    }

    @Override
    public Collection<User> getSpaceUsers(UUID spaceGuid, Optional<String> username) {
        Collection<User> users = getUsersWithRoles(spaceGuid, ccClient::getSpaceUsersWithRoles);
        if (username.isPresent()) {
            return users.stream()
                    .filter(user -> user.getUsername().equals(username.get()))
                    .collect(toList());
        }
        return users;
    }

    @Override
    public Optional<User> addOrgUser(UserRequest userRequest, UUID orgGuid, String currentUser) {
        Optional<UserIdNamePair> idNamePair = uaaClient.findUserIdByName(userRequest.getUsername());
        if(!idNamePair.isPresent()) {
            inviteUserToOrg(userRequest.getUsername(), currentUser, orgGuid,
                    ImmutableSet.<Role>builder()
                            .addAll(userRequest.getRoles())
                            .add(Role.USERS)
                            .build());
        }

        return idNamePair.map(pair -> {
            UUID userGuid = pair.getGuid();
            Role[] roles = ObjectArrays
                    .concat(userRequest.getRoles().toArray(new Role[]{}), Role.USERS);
            assignOrgRolesToUser(userGuid, orgGuid, roles);
            return new User(userRequest.getUsername(), userGuid, userRequest.getRoles(), orgGuid);
        });
    }

    @Override
    public Optional<User> addSpaceUser(UserRequest userRequest, UUID spaceGuid, String currentUser) {
        UUID orgGuid = UUID.fromString(userRequest.getOrgGuid());
        Optional<UserIdNamePair> idNamePair = uaaClient.findUserIdByName(userRequest.getUsername());
        if (!idNamePair.isPresent()) {
            inviteUserToSpace(userRequest.getUsername(), currentUser, orgGuid, spaceGuid, Sets.immutableEnumSet(userRequest.getRoles()));
        }
        return idNamePair.map(pair -> {
            UUID userGuid = pair.getGuid();
            assignOrgRolesToUser(userGuid, orgGuid, Role.USERS);
            assignSpaceRolesToUser(userGuid, spaceGuid,
                    userRequest.getRoles().stream().toArray(Role[]::new));
            return new User(userRequest.getUsername(), userGuid, userRequest.getRoles(), orgGuid);
        });
    }

    private void inviteUserToOrg(String username, String currentUser, UUID orgGuid, Set<Role> roles) {

        AccessInvitationsService.CreateOrUpdateState state =
                accessInvitationsService.createOrUpdateInvitation(username, ui -> ui.addOrgAccessInvitation(orgGuid, roles));
        if (state == AccessInvitationsService.CreateOrUpdateState.CREATED) {
            invitationsService.sendInviteEmail(username, currentUser);
        }
    }

    private void inviteUserToSpace(String username, String currentUser, UUID orgGuid, UUID spaceGuid, Set<Role> roles) {
        AccessInvitationsService.CreateOrUpdateState state =
                accessInvitationsService.createOrUpdateInvitation(username, ui -> {
            ui.addOrgAccessInvitation(orgGuid, Sets.immutableEnumSet(Role.USERS));
            ui.addSpaceAccessInvitation(spaceGuid, roles);
        });
        if (state == AccessInvitationsService.CreateOrUpdateState.CREATED) {
            invitationsService.sendInviteEmail(username, currentUser);
        }
    }

    private boolean isUserAssignedToOrg(UUID userGuid, UUID orgGuid) {
        return this.getOrgUsers(orgGuid)
                .stream()
                .anyMatch(member -> member.getGuid().equals(userGuid));
    }

    private boolean isUserAssignedToSpace(UUID userGuid, UUID spaceGuid) {
        return this.getSpaceUsers(spaceGuid, Optional.empty())
                .stream()
                .anyMatch(member -> member.getGuid().equals(userGuid));
    }

    @Override
    public List<Role> updateOrgUserRoles(UUID userGuid, UUID orgGuid, UserRolesRequest userRolesRequest) {
        if(isUserAssignedToOrg(userGuid, orgGuid)) {
            Role[] rolesToRemove = Role.ORG_ROLES.stream()
                    .filter(x -> !x.equals(Role.USERS) && !userRolesRequest.getRoles().contains(x))
                    .toArray(Role[]::new);
            revokeOrgRolesFromUser(userGuid, orgGuid, rolesToRemove);
            assignOrgRolesToUser(userGuid, orgGuid, userRolesRequest.getRoles().stream().toArray(Role[]::new));
            return userRolesRequest.getRoles();
        } else {
            throw new NoSuchUserException(String.format("User %s does not exist in organization %s.",
                    userGuid.toString(), orgGuid.toString()));
        }
    }

    @Override
    public List<Role> updateSpaceUserRoles(UUID userGuid, UUID spaceGuid, UserRolesRequest userRolesRequest) {
        Optional<UUID> orgGuid = getOrgFromSpace(spaceGuid);
        if(isUserAssignedToSpace(userGuid, spaceGuid) ||
                (orgGuid.isPresent() && isUserAssignedToOrg(userGuid, orgGuid.get()))) {
            Role[] rolesToRemove = Role.SPACE_ROLES.stream()
                    .filter(x -> !userRolesRequest.getRoles().contains(x))
                    .toArray(Role[]::new);
            revokeSpaceRolesFromUser(userGuid, spaceGuid, rolesToRemove);

            assignSpaceRolesToUser(userGuid, spaceGuid, userRolesRequest.getRoles().stream().toArray(Role[]::new));
            return userRolesRequest.getRoles();
        } else {
            throw new NoSuchUserException(String.format("User %s does not exist in space %s.",
                    userGuid.toString(), spaceGuid.toString()));
        }
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
        if (getOrgUsers(orgGuid).stream().noneMatch(x -> userGuid.equals(x.getGuid()))) {
            throw new EntityNotFoundException("The user is not in given organization", null);
        }

        Role.SPACE_ROLES
            .stream()
            .forEach(role -> ccClient.getUsersSpaces(userGuid, role,
                            FilterQuery.from(Filter.ORGANIZATION_GUID, FilterOperator.EQ, orgGuid))
                .stream()
                .forEach(space -> ccClient.revokeSpaceRole(userGuid, space.getGuid(), role))
            );

        Role.ORG_ROLES.stream().forEach(role -> ccClient.revokeOrgRole(userGuid, orgGuid, role));
    }

    @Override
    public void deleteUserFromSpace(UUID userGuid, UUID spaceGuid) {
        if (getSpaceUsers(spaceGuid, Optional.empty()).stream().noneMatch(x -> userGuid.equals(x.getGuid()))) {
            throw new EntityNotFoundException("The user is not in given space", null);
        }

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
        Observable<CcSpace> space = ccClient.getSpace(spaceId);
        if(isOrgManager(userId, space.toBlocking().single().getOrgGuid())) {
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

    private Optional<UUID>  getOrgFromSpace(UUID spaceGuid) {
        return ccClient.getSpace(spaceGuid)
                .map(space -> Optional.of(space.getOrgGuid()))
                .toBlocking()
                .singleOrDefault(Optional.empty());
    }

    private Collection<User> getUsersWithRoles(UUID guid, Function<UUID, Observable<User>> getUserFunc) {
        List<User> users = getUserFunc.apply(guid).toList().toBlocking().single();
        Collection<UUID> userIdList = users.stream()
                .map(user -> user.getGuid())
                .collect(Collectors.toSet());

        Map<UUID, String> nameMap = uaaClient
                .findUserNames(userIdList)
                .stream()
                .filter(x -> x.getUserName() != null)
                .collect(Collectors.toMap(UserIdNamePair::getGuid, UserIdNamePair::getUserName));


        return users.stream()
                .filter(x -> nameMap.containsKey(x.getGuid()))
                .map(x-> {
                    List<Role> roles = x.getRoles().stream().filter(y -> y != Role.USERS).collect(Collectors.toList());
                    String usernameFromUaa = nameMap.get(x.getGuid());
                    return new User(usernameFromUaa, x.getGuid(), roles,  x.getOrgGuid());
                })
                .collect(Collectors.toList());
        }
}
