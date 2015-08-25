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

import org.trustedanalytics.cloud.cc.api.CcOperationsOrgsSpaces;
import org.trustedanalytics.cloud.uaa.UaaOperations;
import org.trustedanalytics.org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.invite.securitycode.SecurityCodeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import java.util.UUID;

public class EmailInvitationsService implements InvitationsService {

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
    private CcOperationsOrgsSpaces ccPrivilegedClient;

    public EmailInvitationsService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public String sendInviteEmail(String email, String currentUser,
        InvitationLinkGenerator invitationLinkGenerator) {

        String subject = "Invitation to join Trusted Analytics platform";
        String invitationLink =
                invitationLinkGenerator.getLink(securityCodeService.generateCode(email).getCode());
        String htmlContent = getEmailHtml(email, currentUser, invitationLink);
        messageService.sendMimeMessage(email, subject, htmlContent);
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
    public String createUser(String username, String password, String orgName) {
        final String defaultSpaceName = "default";

        ScimUser user = uaaPrivilegedClient.createUser(username, password);
        UUID userGuid = UUID.fromString(user.getId());

        ccPrivilegedClient.createUser(userGuid);
        UUID orgGuid = ccPrivilegedClient.createOrganization(orgName);
        ccPrivilegedClient.assignUserToOrganization(userGuid, orgGuid);
        UUID spaceGuid = ccPrivilegedClient.createSpace(orgGuid, defaultSpaceName);
        ccPrivilegedClient.assignUserToSpace(userGuid, spaceGuid);

        retrieveAndAssignAccessInvitations(username, userGuid);

        return user.getId();
    }

    @Override
    public String createUser(String username, String password) {

        ScimUser user = uaaPrivilegedClient.createUser(username, password);
        UUID userGuid = UUID.fromString(user.getId());

        ccPrivilegedClient.createUser(userGuid);

        retrieveAndAssignAccessInvitations(username, userGuid);

        return user.getId();
    }

    private void retrieveAndAssignAccessInvitations(String username, UUID userGuid) {

        for (UUID org : accessInvitationsService.getAccessInvitations(username, AccessInvitations.AccessInvitationsType.ORG)) {
            ccPrivilegedClient.assignUserToOrganization(userGuid, org);
        }

        for (UUID space : accessInvitationsService.getAccessInvitations(username, AccessInvitations.AccessInvitationsType.SPACE)) {
            ccPrivilegedClient.assignUserToSpace(userGuid, space);
        }
    }
}
