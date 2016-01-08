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
package org.trustedanalytics.user;

import static org.mockito.Mockito.mock;

import java.io.UnsupportedEncodingException;

import javax.mail.internet.MimeMessage;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.trustedanalytics.cloud.auth.AuthTokenRetriever;
import org.trustedanalytics.user.common.BlacklistEmailValidator;
import org.trustedanalytics.user.common.FormatUserRolesValidator;
import org.trustedanalytics.user.common.UserPasswordValidator;
import org.trustedanalytics.user.current.UserDetailsFinder;
import org.trustedanalytics.user.invite.*;
import org.trustedanalytics.user.invite.access.AccessInvitations;
import org.trustedanalytics.user.invite.access.AccessInvitationsService;
import org.trustedanalytics.user.manageusers.UsersService;

@Configuration
public class TestConfiguration {

    @Value("${smtp.email}")
    private String SUPPORT_EMAIL;

    @Value("${smtp.email_name}")
    private String EMAIL_NAME;

    private static final String uaaBaseUrl = "https://uaa.example.com";

    private static final String apiBaseUrl = "https://api.example.com";

    public static String getUaaUsersUrl() {
        return uaaBaseUrl + "/Users";
    }

    public static String getCreateUserUrl() {
        return apiBaseUrl + "/v2/users";
    }

    public static String getCreateOrgUrl() {
        return apiBaseUrl + "/v2/organizations";
    }

    public static String getCreateSpaceUrl() {
        return apiBaseUrl + "/v2/spaces";
    }

    public static String getAddUserToOrgUrl() {
        return apiBaseUrl + "/v2/organizations/{org}/users/{user}";
    }

    public static String getAddManagerToOrgUrl() {
        return apiBaseUrl + "/v2/organizations/{org}/managers/{user}";
    }

    public static String getAddManagerToSpaceUrl() {
        return apiBaseUrl + "/v2/spaces/{space}/managers/{user}";
    }

    public static String getAddDeveloperToSpaceUrl() {
        return apiBaseUrl + "/v2/spaces/{space}/developers/{user}";
    }

    @Bean
    protected UsersService usersService() {
        return mock(UsersService.class);
    }

    @Bean
    protected RestOperations userRestTemplate() {
        return mock(RestTemplate.class);
    }

    @Bean
    protected CloudFoundryOperations cloudFoundryClient() {
        return mock(CloudFoundryOperations.class);
    }

    @Bean
    protected RestOperations clientRestTemplate() {
        //we have to mock RestTempate class, because the bean is downcasted in UsersConfig.setAccessToken()
        return mock(RestTemplate.class);
    }

    @Bean
    protected InvitationsService invitationsService(SpringTemplateEngine mailTemplateEngine) {
        return new EmailInvitationsService(mailTemplateEngine);
    }

    @Bean
    protected String TOKEN() {
        //we can use any string as a token, because all the rest endpoints have been ignored for the test purposes in src/test/application.yml
        return "jhksdf8723kjhdfsh4i187y91hkajl";
    }

    @Bean
    protected AccessInvitationsService accessInvitationsService() {
        return mock(AccessInvitationsService.class);
    }

    @Bean
    protected AuthTokenRetriever tokenRetriever() {
        return mock(AuthTokenRetriever.class);
    }
    
    @Bean
    protected UserDetailsFinder detailsFinder() {
        return mock(UserDetailsFinder.class);
    }

    @Bean
    public MimeMessage mimeMessage(){
        return mock(MimeMessage.class);
    }

    @Bean
    public JavaMailSender mailSender(){
        return mock(JavaMailSender.class);
    }

    @Bean
    public MessageService service() throws UnsupportedEncodingException{
        return new EmailService(mailSender(), SUPPORT_EMAIL, EMAIL_NAME);
    }

    @Bean
    public BeanPostProcessor securityDisabler(){
        return new SecurityDisabler();
    }

    @Bean
    protected BlacklistEmailValidator emailValidator() {
        return mock(BlacklistEmailValidator.class);
    }

    @Bean
    protected FormatUserRolesValidator formatRolesValidator() {
        return mock(FormatUserRolesValidator.class);
    }

    @Bean
    protected UserPasswordValidator passwordValidator() {
        return mock(UserPasswordValidator.class);
    }

    @Bean
    protected AccessInvitations accessInvitations() {
        return mock(AccessInvitations.class);
    }

    @Bean
    protected InvitationLinkGenerator invitationLinkGenerator() {
        return mock(InvitationLinkGenerator.class);
    }

    public static String getUaaGetUser(String username) {
        return uaaBaseUrl + "/Users?attributes=id,userName&filter=userName eq '{name}'";
    }
}
