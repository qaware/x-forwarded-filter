package de.qaware.web.filter;

import org.apache.commons.collections4.map.CaseInsensitiveMap;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Set;

/**
 *
 */
class ForwardedHeaderConstants {

	private ForwardedHeaderConstants() {
		//utility class
	}

	/**
	 * Use this init param to enable relative redirects as explained in and also
	 * using the same response wrapper as {@link RelativeRedirectFilter} does.
	 * Or if both filters are used, only one will wrap the response.
	 * <p>By default, if this property is set to false, in which case calls to
	 * {@link HttpServletResponse#sendRedirect(String)} are overridden in order
	 * to turn relative into absolute URLs since (which Servlet containers are
	 * also required to do) also taking forwarded headers into consideration.
	 */
	public static final String ENABLE_RELATIVE_REDIRECTS_INIT_PARAM = "enableRelativeRedirects";

	/**
	 * Enables mode in which any "Forwarded" or "X-Forwarded-*" headers are
	 * removed only and the information in them ignored.
	 */
	public static final String REMOVE_ONLY_INIT_PARAM = "removeOnly";


	protected static final Set<String> FORWARDED_HEADER_NAMES;

	static {
		Set<String> headerNames = Collections.newSetFromMap(new CaseInsensitiveMap<>(5));
		headerNames.add("Forwarded");
		headerNames.add("X-Forwarded-Host");
		headerNames.add("X-Forwarded-Port");
		headerNames.add("X-Forwarded-Proto");
		headerNames.add("X-Forwarded-Prefix");
		FORWARDED_HEADER_NAMES=headerNames;
	}
}
