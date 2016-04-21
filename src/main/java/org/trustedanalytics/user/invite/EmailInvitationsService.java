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
package org.trustedanalytics.user.invite;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.trustedanalytics.cloud.cc.api.CcOperations;
import org.trustedanalytics.cloud.cc.api.manageusers.Role;
import org.trustedanalytics.cloud.uaa.UaaOperations;
import org.trustedanalytics.org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.trustedanalytics.user.common.NoPendingInvitationFoundException;
import org.trustedanalytics.user.common.OrgAndUserGuids;
import org.trustedanalytics.user.common.UserExistsException;
import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.securitycode.SecurityCode;
import org.trustedanalytics.user.invite.securitycode.SecurityCodeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EmailInvitationsService implements InvitationsService {

    private static final Log LOGGER = LogFactory.getLog(EmailInvitationsService.class);

    private final SpringTemplateEngine templateEngine;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SecurityCodeService securityCodeService;

    @Autowired
    private AccessInvitationsService accessInvitationsService;

    @Autowired
    private UaaOperations uaaPrivilegedClient;

    @Autowired
    private CcOperations ccPrivilegedClient;

    @Autowired
    private InvitationLinkGenerator invitationLinkGenerator;

    public EmailInvitationsService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String sendInviteEmail(String email, String currentUser) {
        SecurityCode sc = securityCodeService.generateCode(email);
        return sendEmail(email, currentUser, sc.getCode());
    }

    @Override
    public String resendInviteEmail(String email, String currentUser) {
        Optional<SecurityCode> sc = securityCodeService.findByMail(email);
        if(!sc.isPresent()) {
            throw new NoPendingInvitationFoundException("No pending invitation for "+email);
        }

        return sendEmail(email, currentUser, sc.get().getCode());
    }

    private String sendEmail(String email, String currentUser, String code) {
        validateUsername(email);
        String subject = "Invitation to join Trusted Analytics platform";
        String invitationLink = invitationLinkGenerator.getLink(code);
        String htmlContent = getEmailHtml(email, currentUser, invitationLink);
        messageService.sendMimeMessage(email, subject, htmlContent);
        LOGGER.info("Sent invitation to user " + email);
        return invitationLink;
    }

    private String getEmailHtml(String email, String currentUser, String invitationLink) {
        final Context ctx = new Context();
        ctx.setVariable("serviceName", "Trusted Analytics");
        ctx.setVariable("email", email);
        ctx.setVariable("currentUser", currentUser);
        ctx.setVariable("accountsUrl", invitationLink);
        return templateEngine.process("invite", ctx);
    }

    @Override
    public Optional<OrgAndUserGuids> createUser(String username, String password, String orgName) {
        validateOrgName(orgName);
        validateUsername(username);
        return createAndRetrieveUser(username, password)
                .map(userGuid -> {
                    UUID orgGuid = createOrganization(userGuid, orgName);
                    createSpace(orgGuid, userGuid, "default");
                    return new OrgAndUserGuids(userGuid, orgGuid);
                });
    }

    @Override
    public Optional<UUID> createUser(String username, String password) {
        validateUsername(username);
        return createAndRetrieveUser(username, password);
    }

    @Override
    public boolean userExists(String username) {
        return uaaPrivilegedClient.findUserIdByName(username).isPresent();
    }

    @Override
    public Set<String> getPendingInvitationsEmails() {
        return accessInvitationsService.getKeys();
    }

    @Override
    public void deleteInvitation(String email) {
        Optional<SecurityCode> sc = securityCodeService.findByMail(email);
        if(!sc.isPresent()) {
            throw new NoPendingInvitationFoundException("No pending invitation for "+email);
        }

        securityCodeService.redeem(sc.get());
        accessInvitationsService.redeemAccessInvitations(email);
    }

    private Optional<UUID> createAndRetrieveUser(String username, String password) {
        return accessInvitationsService.getAccessInvitations(username)
                .map(invitations -> {
                    final ScimUser user = uaaPrivilegedClient.createUser(username, password);
                    final UUID userGuid = UUID.fromString(user.getId());
                    ccPrivilegedClient.createUser(userGuid);
                    retrieveAndAssignAccessInvitations(userGuid, invitations);
                    return userGuid;
                });
    }

    private void validateUsername(String username) {
        uaaPrivilegedClient.findUserIdByName(username).ifPresent(user -> {
            throw new UserExistsException(String.format("Username %s is already taken", username));
        });
    }

    private void validateOrgName(String orgName) {
        if(StringUtils.isBlank(orgName)) {
            throw new InvalidOrganizationNameException("Organization cannot contain only whitespace characters");
        }

        ccPrivilegedClient.getOrgs()
            .exists(org -> org.getName().equals(orgName))
            .doOnNext(orgExists -> {
                if (orgExists) {
                    throw new OrgExistsException(String.format("Organization \"%s\" already exists.", orgName));
                }
            })
            .toBlocking()
            .single();
    }

    private UUID createOrganization(UUID userGuid, String orgName) {
        final UUID orgGuid = ccPrivilegedClient.createOrganization(orgName);
        ccPrivilegedClient.assignUserToOrganization(userGuid, orgGuid);
        return orgGuid;
    }

    private UUID createSpace(UUID orgGuid, UUID userGuid, String spaceName) {
        final UUID spaceGuid = ccPrivilegedClient.createSpace(orgGuid, spaceName);
        ccPrivilegedClient.assignUserToSpace(userGuid, spaceGuid);
        return spaceGuid;
    }

    private void retrieveAndAssignAccessInvitations(UUID userGuid, AccessInvitations invtiations) {
        getFlatOrgRoleMap(invtiations.getOrgAccessInvitations())
                .forEach(pair -> ccPrivilegedClient.assignOrgRole(userGuid, pair.getKey(), pair.getValue()));
        getFlatOrgRoleMap(invtiations.getSpaceAccessInvitations())
                .forEach(pair -> ccPrivilegedClient.assignSpaceRole(userGuid, pair.getKey(), pair.getValue()));
    }

    private List<Pair<UUID, Role>> getFlatOrgRoleMap(Map<UUID, Set<Role>> orgRoleMap) {
        return orgRoleMap.entrySet()
                .stream()
                .flatMap(orgRoles -> orgRoles.getValue().stream().map(role -> Pair.of(orgRoles.getKey(), role)))
                .collect(Collectors.toList());
    }

}
