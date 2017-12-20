package de.qaware.util;

import de.qaware.http.HttpHeaders;
import de.qaware.http.InvalidMediaTypeException;
import de.qaware.http.MediaType;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 *
 */
public class HttpServletRequestUtil {

	private HttpServletRequestUtil(){
		//utility class
	}

	/**
	 * Reconstructs full url: e.g. http://server:port/foo/bar;xxx=yyy?param1=value1&param2=value2
	 * <pre>
	 * request.getRequestURL() = ttp://server:port/foo/bar;xxx=yyy
	 * request.getQueryString() = param1=value1&param2=value2
	 * request.getRequestURL() + "?" + request.getQueryString();
	 * </pre>
	 *
	 * Difference of this method to {@link HttpServletRequest#getRequestURI()} <br/>
	 * {@link HttpServletRequest#getRequestURI()} returns: /foo/bar;xxx=yyy?param1=value1&param2=value2
	 *
	 * @param request {@see HttpServletRequest}
	 * @return request.getRequestURL() + "?" + request.getQueryString();
	 */
	public static URI getURI(HttpServletRequest request) {
		try {
			StringBuffer url = request.getRequestURL();
			String query = request.getQueryString();
			if (!isBlank(query)) {
				url.append('?').append(query);
			}
			return new URI(url.toString());
		} catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get HttpServletRequest URI: " + ex.getMessage(), ex);
		}
	}


	public static HttpHeaders getHeaders(HttpServletRequest servletRequest) {
		HttpHeaders headers = new HttpHeaders();
		setHeaderNames(headers, servletRequest);
		setContentType(headers, servletRequest);
		setContentLength(headers, servletRequest);
		return headers;
	}

	private static void setHeaderNames(HttpHeaders headers, HttpServletRequest servletRequest) {
		for (Enumeration<?> headerNames = servletRequest.getHeaderNames(); headerNames.hasMoreElements(); ) {
			String headerName = (String) headerNames.nextElement();
			for (Enumeration<?> headerValues = servletRequest.getHeaders(headerName);
			     headerValues.hasMoreElements(); ) {
				String headerValue = (String) headerValues.nextElement();
				headers.add(headerName, headerValue);
			}
		}
	}

	private static void setContentLength(HttpHeaders headers, HttpServletRequest servletRequest) {
		if (headers.getContentLength() < 0) {
			int requestContentLength = servletRequest.getContentLength();
			if (requestContentLength != -1) {
				headers.setContentLength(requestContentLength);
			}
		}
	}

	private static void setContentType(HttpHeaders headers, HttpServletRequest servletRequest) {
		// HttpServletRequest exposes some headers as properties: we should include those if not already present
		try {
			MediaType contentType = headers.getContentType();
			if (contentType == null) {
				String requestContentType = servletRequest.getContentType();
				if (!StringUtils.isBlank(requestContentType)) {
					contentType = MediaType.parseMediaType(requestContentType);

					headers.setContentType(contentType);
				}
			}
			if (contentType != null && contentType.getCharset() == null) {
				String requestEncoding = servletRequest.getCharacterEncoding();
				if (!StringUtils.isBlank(requestEncoding)) {
					Charset charSet = Charset.forName(requestEncoding);
					Map<String, String> params = new CaseInsensitiveMap<>();
					params.putAll(contentType.getParameters());
					params.put("charset", charSet.toString());
					MediaType newContentType = new MediaType(contentType.getType(), contentType.getSubtype(), params);

					headers.setContentType(newContentType);
				}
			}
		} catch (InvalidMediaTypeException ex) {
			// Ignore: simply not exposing an invalid content type in HttpHeaders...
		}
	}
}
