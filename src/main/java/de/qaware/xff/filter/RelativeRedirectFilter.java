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

import de.qaware.xff.util.WebUtilsConstants;
import org.apache.commons.lang3.Validate;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Overrides {@link HttpServletResponse#sendRedirect(String)} and handles it by
 * setting the HTTP status and "Location" headers. This keeps the Servlet
 * container from re-writing relative redirect URLs and instead follows the
 * recommendation in <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2">
 * RFC 7231 Section 7.1.2</a>.
 * <p>
 * <p><strong>Note:</strong> While relative redirects are more efficient they
 * may not work with reverse proxies under some configurations.
 *
 * @author Michael Frank
 * @author Rob Winch
 * @author Rossen Stoyanchev
 * @since 4.3.10
 */
public class RelativeRedirectFilter extends OncePerRequestFilter {

    private int redirectStatus = WebUtilsConstants.SEE_OTHER;


    /**
     * Set the default HTTP Status to use for redirects.
     * <p>By default this is {@link WebUtilsConstants#SEE_OTHER}.
     *
     * @param status the 3xx redirect status to use
     */
    public void setRedirectStatus(int status) {

        Validate.isTrue(isRedirect(status), "Not a redirect status code: %s", status);
        this.redirectStatus = status;
    }

    @SuppressWarnings("squid:S109") //MagicNumbers: Alternatives would be even more ugly
    private static boolean isRedirect(int status) {
        return (status / 100) == 3;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        HttpServletResponse wrappedResponse = RelativeRedirectResponseWrapper.wrapIfNecessary(response, this.redirectStatus);
        filterChain.doFilter(request, wrappedResponse);
    }

}
