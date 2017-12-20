package de.qaware.web.filter;

import de.qaware.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 *
 */
class ForwardedHeaderExtractingResponse extends HttpServletResponseWrapper {

	private static final String FOLDER_SEPARATOR = "/";

	private final HttpServletRequest request;

	public ForwardedHeaderExtractingResponse(HttpServletResponse response, HttpServletRequest request) {
		super(response);
		this.request = request;
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(location);

		// Absolute location
		if (builder.build().getScheme() != null) {
			super.sendRedirect(location);
			return;
		}

		// Network-path reference
		if (location.startsWith("//")) {
			String scheme = this.request.getScheme();
			super.sendRedirect(builder.scheme(scheme).toUriString());
			return;
		}

		// Relative to Servlet container root or to current request
		String path = (location.startsWith(FOLDER_SEPARATOR) ? location :
				applyRelativePath(this.request.getRequestURI(), location));

		String result = UriComponentsBuilder
				.fromHttpRequest(this.request)
				.replacePath(path)
				.build().normalize().toUriString();

		super.sendRedirect(result);
	}

		public static String applyRelativePath(String path, String relativePath) {
		int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR);
		if (separatorIndex != -1) {
			String newPath = path.substring(0, separatorIndex);
			if (!relativePath.startsWith(FOLDER_SEPARATOR)) {
				newPath += FOLDER_SEPARATOR;
			}
			return newPath + relativePath;
		} else {
			return relativePath;
		}
	}
}
