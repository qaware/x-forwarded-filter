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

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;


/**
 * Filter base class that aims to guarantee a single execution per request
 * dispatch, on any servlet container. It provides a {@link #doFilterInternal}
 * method with HttpServletRequest and HttpServletResponse arguments.
 * <p>
 * <p>As of Servlet 3.0, a filter may be invoked as part of a
 * {@link javax.servlet.DispatcherType#REQUEST REQUEST} or
 * {@link javax.servlet.DispatcherType#ASYNC ASYNC} dispatches that occur in
 * separate threads. A filter can be configured in {@code web.xml} whether it
 * should be involved in async dispatches. However, in some cases servlet
 * containers assume different default configuration. Therefore sub-classes can
 * override the method  to declare
 * statically if they should indeed be invoked, <em>once</em>, during both types
 * of dispatches in order to provide thread initialization, logging, security,
 * and so on. This mechanism complements and does not replace the need to
 * configure a filter in {@code web.xml} with dispatcher types.
 * <p>
 * <p>
 * <p>Yet another dispatch type that also occurs in its own thread is
 * {@link javax.servlet.DispatcherType#ERROR ERROR}. Subclasses can override
 * {@link #shouldNotFilterErrorDispatch()} if they wish to declare statically
 * if they should be invoked <em>once</em> during error dispatches.
 * <p>
 * <p>The {@link #getAlreadyFilteredAttributeName} method determines how to
 * identify that a request is already filtered. The default implementation is
 * based on the configured name of the concrete filter instance.
 *
 * @author Michael Frank
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 06.12.2003
 */
public abstract class OncePerRequestFilter implements Filter {

    /**
     * Suffix that gets appended to the filter name for the
     * "already filtered" request attribute.
     *
     * @see #getAlreadyFilteredAttributeName
     */
    public static final String ALREADY_FILTERED_SUFFIX = ".FILTERED";

    private String alreadyFilteredAttributeName;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.alreadyFilteredAttributeName = Optional.ofNullable(filterConfig.getFilterName()).orElse(getClass().getName()) + ALREADY_FILTERED_SUFFIX;
    }

    /**
     * This {@code doFilter} implementation stores a request attribute for
     * "already filtered", proceeding without filtering again if the
     * attribute is already there.
     *
     * @see #getAlreadyFilteredAttributeName
     * @see #shouldNotFilter
     * @see #doFilterInternal
     */
    @Override
    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            throw new ServletException("OncePerRequestFilter just supports HTTP requests");
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String alreadyFilteredAttribute = getAlreadyFilteredAttributeName();
        boolean hasAlreadyFilteredAttribute = request.getAttribute(alreadyFilteredAttribute) != null;

        if (hasAlreadyFilteredAttribute || skipDispatch(httpRequest) || shouldNotFilter(httpRequest)) {

            // Proceed without invoking this filter...
            filterChain.doFilter(request, response);
        } else {
            // Do invoke this filter...
            request.setAttribute(alreadyFilteredAttribute, Boolean.TRUE);
            try {
                doFilterInternal(httpRequest, httpResponse, filterChain);
            } finally {
                // Remove the "already filtered" request attribute for this request.
                request.removeAttribute(alreadyFilteredAttribute);
            }
        }
    }


    private boolean skipDispatch(HttpServletRequest request) {
        return request.getAttribute(WebUtilsConstants.ERROR_REQUEST_URI_ATTRIBUTE) != null && shouldNotFilterErrorDispatch();
    }


    /**
     * Return the name of the request attribute that identifies that a request
     * is already filtered.
     * <p>The default implementation takes the configured name of the concrete filter
     * instance and appends ".FILTERED". If the filter is not fully initialized,
     * it falls back to its class name.
     *
     * @see #ALREADY_FILTERED_SUFFIX
     */
    protected String getAlreadyFilteredAttributeName() {
        return alreadyFilteredAttributeName;
    }


    /**
     * Can be overridden in subclasses for custom filtering control,
     * returning {@code true} to avoid filtering of the given request.
     * <p>The default implementation always returns {@code false}.
     *
     * @param request current HTTP request
     * @return whether the given request should <i>not</i> be filtered
     * @throws ServletException in case of errors
     */
    @SuppressWarnings("squid:S1172")//Unused parameters - default implementation for subclasses
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return false;
    }

    /**
     * Whether to filter error dispatches such as when the servlet container
     * processes and error mapped in {@code web.xml}. The default return value
     * is "true", which means the filter will not be invoked in case of an error
     * dispatch.
     *
     * @since 3.2
     */
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }


    /**
     * Same contract as for {@code doFilter}, but guaranteed to be
     * just invoked once per request within a single request thread.
     * See  for details.
     * <p>Provides HttpServletRequest and HttpServletResponse arguments instead of the
     * default ServletRequest and ServletResponse ones.
     */
    protected abstract void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException;

    /**
     * Subclasses may override this to perform custom filter shutdown.
     * <p>Note: This method will be called from standard filter destruction
     * as well as filter bean destruction in a Spring application context.
     * <p>This default implementation is empty.
     */
    @Override
    public void destroy() {
        // No action
    }

}
