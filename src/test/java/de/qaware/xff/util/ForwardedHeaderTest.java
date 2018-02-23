/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.qaware.xff.util;

import org.junit.Test;

import java.util.Locale;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class ForwardedHeaderTest {

    private String[] supportedHeaders = {"Forwarded", "X-Forwarded-Host", "X-Forwarded-Port", "X-Forwarded-Proto", "X-Forwarded-Prefix"};

    @Test
    public void fromName() {
        assertEquals(ForwardedHeader.FORWARDED, ForwardedHeader.forName("Forwarded"));
        assertEquals(ForwardedHeader.X_FORWARDED_HOST, ForwardedHeader.forName("X-Forwarded-Host"));
        assertEquals(ForwardedHeader.X_FORWARDED_PROTO, ForwardedHeader.forName("X-Forwarded-Proto"));
        assertEquals(ForwardedHeader.X_FORWARDED_PORT, ForwardedHeader.forName("X-Forwarded-Port"));
        assertEquals(ForwardedHeader.X_FORWARDED_PREFIX, ForwardedHeader.forName("X-Forwarded-Prefix"));
    }

    @Test
    public void forNameCaseInsensitive() {
        for (String header : supportedHeaders) {
            ForwardedHeader fromCamelCase = ForwardedHeader.forName(header);
            ForwardedHeader fromLowerCase = ForwardedHeader.forName(header.toLowerCase(Locale.ENGLISH));
            ForwardedHeader fromUpperCase = ForwardedHeader.forName(header.toUpperCase(Locale.ENGLISH));
            assertNotNull(fromCamelCase);
            assertNotNull(fromLowerCase);
            assertNotNull(fromUpperCase);
            assertEquals(fromCamelCase, fromLowerCase);
            assertEquals(fromLowerCase, fromUpperCase);
        }
    }

    @Test
    public void isForwardedHeader() {
        for (String header : supportedHeaders) {
            boolean fromCamelCase = ForwardedHeader.isForwardedHeader(header);
            boolean fromLowerCase = ForwardedHeader.isForwardedHeader(header.toLowerCase(Locale.ENGLISH));
            boolean fromUpperCase = ForwardedHeader.isForwardedHeader(header.toUpperCase(Locale.ENGLISH));
            assertTrue(fromCamelCase);
            assertTrue(fromLowerCase);
            assertTrue(fromUpperCase);
        }
    }

    @Test
    public void testToString() {
        for (String header : supportedHeaders) {
            ForwardedHeader fromCamelCase = ForwardedHeader.forName(header);
            assertEquals(header, fromCamelCase.toString());
        }
    }

    @Test
    public void headerName() {
        for (String header : supportedHeaders) {
            ForwardedHeader fromCamelCase = ForwardedHeader.forName(header);
            assertEquals(header, fromCamelCase.headerName());
        }
    }

}
