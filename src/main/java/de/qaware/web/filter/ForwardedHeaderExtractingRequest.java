package de.qaware.web.filter;

import de.qaware.web.util.UrlPathHelper;
import de.qaware.web.util.uri.UriComponents;
import de.qaware.web.util.uri.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

import static de.qaware.web.util.uri.UriComponents.PATH_DELIMITER_STRING;

/**
 * Extract and use "Forwarded" or "X-Forwarded-*" headers.
 */
class ForwardedHeaderExtractingRequest extends ForwardedHeaderRemovingRequest {

	/*@Nullable*/
	private final String scheme;

	private final boolean secure;

	/*@Nullable*/
	private final String host;

	private final int port;

	private final String contextPath;

	private final String requestUri;

	private final String requestUrl;

	@SuppressWarnings("squid:S3358")//nested ternary op is more readable in this case
	public ForwardedHeaderExtractingRequest(HttpServletRequest request) {
		super(request);

		UrlPathHelper pathHelper = new UrlPathHelper();
		pathHelper.setUrlDecode(false);
		pathHelper.setRemoveSemicolonContent(false);

		UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(request).build();
		int portFromUri = uriComponents.getPort();

		this.scheme = uriComponents.getScheme();
		this.secure = "https".equals(scheme);
		this.host = uriComponents.getHost();
		this.port = (portFromUri == -1 ? (this.secure ? 443 : 80) : portFromUri);

		String prefix = getForwardedPrefix(request);
		this.contextPath = (prefix != null ? prefix : request.getContextPath());
		this.requestUri = this.contextPath + pathHelper.getPathWithinApplication(request);
		this.requestUrl = this.scheme + "://" + this.host + (portFromUri == -1 ? "" : ":" + portFromUri) + this.requestUri;
	}

	/*@Nullable*/
	private static String getForwardedPrefix(HttpServletRequest request) {
		String prefix = null;
		Enumeration<String> names = request.getHeaderNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if ("X-Forwarded-Prefix".equalsIgnoreCase(name)) {
				prefix = request.getHeader(name);
			}
		}
		if (prefix != null) {
			while (prefix.endsWith(PATH_DELIMITER_STRING)) {
				prefix = prefix.substring(0, prefix.length() - 1);
			}
		}
		return prefix;
	}

	@Override
	/*@Nullable*/
	public String getScheme() {
		return this.scheme;
	}

	@Override
	/*@Nullable*/
	public String getServerName() {
		return this.host;
	}

	@Override
	public int getServerPort() {
		return this.port;
	}

	@Override
	public boolean isSecure() {
		return this.secure;
	}

	@Override
	public String getContextPath() {
		return this.contextPath;
	}

	@Override
	public String getRequestURI() {
		return this.requestUri;
	}

	@Override
	public StringBuffer getRequestURL() {
		return new StringBuffer(this.requestUrl);
	}
}
