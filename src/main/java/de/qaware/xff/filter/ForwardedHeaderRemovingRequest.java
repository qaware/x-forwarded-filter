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
package de.qaware.xff.filter;

import de.qaware.xff.util.ForwardedHeader;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * Hide "Forwarded" or "X-Forwarded-*" headers.
 */
class ForwardedHeaderRemovingRequest extends HttpServletRequestWrapper {

    private final Map<String, List<String>> headers;

    public ForwardedHeaderRemovingRequest(HttpServletRequest request) {
        super(request);
        this.headers = initHeaders(request);
    }

    private static Map<String, List<String>> initHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new CaseInsensitiveMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (!ForwardedHeader.isForwardedHeader(name)) {
                headers.put(name, Collections.list(request.getHeaders(name)));
            }
        }
        return headers;
    }

    // Override header accessors to not expose forwarded headers

    @Override
    /*@Nullable*/
    public String getHeader(String name) {
        List<String> value = this.headers.get(name);
        return (CollectionUtils.isEmpty(value) ? null : value.get(0));
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> value = this.headers.get(name);
        return (Collections.enumeration(value != null ? value : Collections.emptySet()));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(this.headers.keySet());
    }
}
