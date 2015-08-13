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

import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.io.UnsupportedEncodingException;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mail.javamail.JavaMailSender;


@RunWith(MockitoJUnitRunner.class)
public class EmailServiceTest {

    protected static final String SUPPORT_EMAIL = "support@example.com";

    protected static final String PERSONAL_NAME = "Example Support";

    private EmailService sut;

    @Mock
    private MimeMessage mimeMessage;

    @Mock
    private JavaMailSender mailSender;

    @Captor
    private ArgumentCaptor<Address[]> addressCaptor;

    @Before
    public void setUp() throws UnsupportedEncodingException{
        sut = new EmailService(mailSender, SUPPORT_EMAIL, PERSONAL_NAME);
    }

    @Test
    public void sendMimeMessage_fromAddress() throws MessagingException, UnsupportedEncodingException {
        doReturn(mimeMessage).when(mailSender).createMimeMessage();
        sut.sendMimeMessage("", "", "");
        verify(mimeMessage).addFrom((Address[]) addressCaptor.capture());
        assertThat(addressCaptor.getValue(),
                arrayContaining(new InternetAddress(SUPPORT_EMAIL, PERSONAL_NAME)));
    }

}