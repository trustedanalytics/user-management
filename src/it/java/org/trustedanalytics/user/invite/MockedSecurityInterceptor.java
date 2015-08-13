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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

import java.io.IOException;

import javax.servlet.ServletException;

class MockedSecurityInterceptor extends FilterSecurityInterceptor {

    public MockedSecurityInterceptor() {
        this.setAuthenticationManager(authentication -> null);

        AccessDecisionManager accessDecisionManager = mock(AccessDecisionManager.class);
        doReturn(true).when(accessDecisionManager).supports(any(Class.class));
        this.setAccessDecisionManager(accessDecisionManager);

        FilterInvocationSecurityMetadataSource filterInvocationSecurityMetadataSource =
            mock(FilterInvocationSecurityMetadataSource.class);
        doReturn(true).when(filterInvocationSecurityMetadataSource).supports(any(Class.class));
        this.setSecurityMetadataSource(filterInvocationSecurityMetadataSource);
    }

    @Override
    public void invoke(FilterInvocation fi) throws IOException, ServletException {
        if ((fi.getRequest() != null)) {
            fi.getChain().doFilter(fi.getRequest(), fi.getResponse());
        }
    }
}
