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

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.net.URI;

public class EmailOrgUserInvitationService implements OrgUserInvitationService {

    private final MessageService messageService;
    private final TemplateEngine templateEngine;

    @Value("${app.url}")
    private String appUrl;

    @Value("${app.console.host}")
    private String consoleHost;

    @Value("${app.console.useSsl}")
    private boolean consoleUsesSsl;

    @Value("${spring.oauth2.client.userAuthorizationUri}")
    private String authorizationHost;

    public EmailOrgUserInvitationService(MessageService messageService,
            TemplateEngine templateEngine) {
        this.messageService = messageService;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendInvitation(String username) {
        messageService.sendMimeMessage(username, "Welcome to Trusted Analytics platform", getEmailHtml(username));
    }

    private String getEmailHtml(String username) {
        final Context ctx = new Context();
        ctx.setVariable("serviceName", "Trusted Analytics");
        ctx.setVariable("username", username);
        URI authorizationUrl =  URI.create(authorizationHost);
        String resetPasswordUrl = authorizationUrl.toString().replaceAll(authorizationUrl.getPath(), "/forgot_password");
        ctx.setVariable("resetPasswordUrl", resetPasswordUrl);
        ctx.setVariable("consoleUrl", getConsoleUrl());
        return templateEngine.process("invite_org", ctx);
    }

    private String getConsoleUrl() {
        if(Strings.isNullOrEmpty(appUrl)) {
            throw new IllegalArgumentException("Application url not set");
        }

        appUrl = appUrl.replaceAll("^http(s)?://", "");

        String[] split = appUrl.split("\\.");
        if(split.length == 0) {
            throw new IllegalArgumentException("Invalid application url: " + appUrl);
        }

        split[0] = consoleHost;
        return (consoleUsesSsl ? "https://" : "http://") + String.join(".", split);
    }
    
}
