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

import de.qaware.xff.util.HttpHeaders;
import org.apache.commons.lang3.Validate;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * A response wrapper used for the implementation of
 * {@link RelativeRedirectFilter} also shared with {@link ForwardedHeaderFilter}.
 *
 * @author Michael Frank
 * @author Rossen Stoyanchev
 * @since 4.3.10
 */
final class RelativeRedirectResponseWrapper extends HttpServletResponseWrapper {

    private final int redirectStatus;

    private RelativeRedirectResponseWrapper(HttpServletResponse response, int redirectStatus) {
        super(response);
        Validate.notNull(redirectStatus, "'redirectStatus' is required");
        this.redirectStatus = redirectStatus;
    }

    /**
     * Wraps the response if the https status code is 3xx. Prevents wrapping the same response multiple times.
     *
     * @param response       the response to be wrapped
     * @param redirectStatus http status code
     * @return the wrapped response
     */
    public static HttpServletResponse wrapIfNecessary(HttpServletResponse response, int redirectStatus) {
        return hasWrapper(response) ? response : new RelativeRedirectResponseWrapper(response, redirectStatus);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        setStatus(this.redirectStatus);
        setHeader(HttpHeaders.LOCATION, location);
    }


    private static boolean hasWrapper(ServletResponse response) {
        if (response instanceof RelativeRedirectResponseWrapper) {
            return true;
        }
        while (response instanceof HttpServletResponseWrapper) {
            ServletResponse unwrappedResponse = ((HttpServletResponseWrapper) response).getResponse();
            if (unwrappedResponse instanceof RelativeRedirectResponseWrapper) {
                return true;
            }
        }
        return false;
    }

}
