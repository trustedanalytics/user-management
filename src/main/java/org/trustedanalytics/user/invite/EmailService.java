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

import java.io.UnsupportedEncodingException;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mail.javamail.JavaMailSender;

public class EmailService implements MessageService {

    private static final Log LOGGER = LogFactory.getLog(EmailService.class);

    private final JavaMailSender mailSender;

    private final Address[] senderAddresses;

    public EmailService(JavaMailSender mailSender, String supportEmail, String personalName)
            throws UnsupportedEncodingException {
        if(mailSender==null) {
            throw new IllegalArgumentException("EmailService constructor contains a null JavaMailSender argument");
        }
        if(supportEmail==null) {
            throw new IllegalArgumentException("EmailService constructor contains a null String argument");
        }
        Address[] temp = null;
        try {
            temp = new Address[] {new InternetAddress(supportEmail, personalName)};
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
        }

        this.senderAddresses = temp;
        this.mailSender = mailSender;
    }

    @Override
    public void sendMimeMessage(String email, String subject, String htmlContent) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            message.addFrom(senderAddresses);
            message.addRecipients(Message.RecipientType.TO, email);
            message.setSubject(subject);
            message.setContent(htmlContent, "text/html");
        } catch (Exception e) {
            LOGGER.error(e);
        }
        mailSender.send(message);
    }
}
