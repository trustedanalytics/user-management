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

package org.trustedanalytics.user.invite.securitycode;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasSize;
import static java.util.stream.Collectors.toList;

import java.util.HashSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.trustedanalytics.user.invite.keyvaluestore.RedisStore;


@RunWith(MockitoJUnitRunner.class)
public class RedisSecurityCodeServiceTest {

    @Mock
    private RedisOperations<String, SecurityCode> redisOps;

    @Mock
    private HashOperations<String, String, SecurityCode> hashOps;

    @Test
    public void shouldRetryAndSucceed() {
        when(redisOps.<String, SecurityCode>opsForHash()).thenReturn(hashOps);
        when(hashOps.putIfAbsent(anyString(), anyString(), any(SecurityCode.class)))
            .thenReturn(Boolean.FALSE)
            .thenReturn(Boolean.FALSE)
            .thenReturn(Boolean.TRUE);


        RedisStore<SecurityCode> redisStore = new RedisStore<>(redisOps, "test-key");
        SecurityCodeService service = new SecurityCodeService(redisStore);
        SecurityCode code = service.generateCode("test@example.com");

        assertThat(code.getCode(), not(isEmptyOrNullString()));
        ArgumentCaptor<SecurityCode> codeCaptor = ArgumentCaptor.forClass(SecurityCode.class);
        verify(hashOps, times(3)).putIfAbsent(anyString(), anyString(), codeCaptor.capture());

        //check that all generated codes were different
        assertThat("Expected 3 different values for generated codes", 
                new HashSet<String>(codeCaptor.getAllValues().stream().map(x -> x.getCode()).collect(toList())), hasSize(3));
    }

    @Test(expected = SecurityCodeGenerationException.class)
    public void shouldFailAfterRetries() {
        when(redisOps.<String, SecurityCode>opsForHash()).thenReturn(hashOps);
        when(hashOps.putIfAbsent(anyString(), anyString(), any(SecurityCode.class))).thenReturn(Boolean.FALSE);

        RedisStore<SecurityCode> redisStore = new RedisStore<>(redisOps, "test-key");
        SecurityCodeService service = new SecurityCodeService(redisStore);
        service.generateCode("test@example.com");
    }

}
