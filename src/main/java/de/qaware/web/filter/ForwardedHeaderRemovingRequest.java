package de.qaware.web.filter;


import de.qaware.web.util.ForwardedHeader;
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
