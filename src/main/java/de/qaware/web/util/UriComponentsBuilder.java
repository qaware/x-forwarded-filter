/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.qaware.web.util;

import de.qaware.http.HttpHeaders;
import de.qaware.util.HttpServletRequestUtil;
import de.qaware.web.util.HierarchicalUriComponents.PathComponent;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builder for {@link UriComponents}.
 * <p>
 * <p>Typical usage involves:
 * <ol>
 * <li>Create a {@code UriComponentsBuilder} with one of the static factory methods
 * (such as  or {@link #fromUri(URI)})</li>
 * <li>Set the various URI components through the respective methods ({@link #scheme(String)},
 * {@link #userInfo(String)}, {@link #host(String)}, {@link #port(int)}, {@link #path(String)},
 * , {@link #queryParam(String, Object...)}, and
 * {@link #fragment(String)}.</li>
 * <li>Build the {@link UriComponents} instance with the {@link #build()} method.</li>
 * </ol>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author Brian Clozel
 * @see #fromUri(URI)
 * @since 3.1
 */
public class UriComponentsBuilder {

	private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

	private static final String SCHEME_PATTERN = "([^:/?#]+):";

	private static final String USERINFO_PATTERN = "([^@\\[/?#]*)";

	private static final String HOST_IPV4_PATTERN = "[^\\[/?#:]*";

	private static final String HOST_IPV6_PATTERN = "\\[[\\p{XDigit}\\:\\.]*[%\\p{Alnum}]*\\]";

	private static final String HOST_PATTERN = "(" + HOST_IPV6_PATTERN + "|" + HOST_IPV4_PATTERN + ")";

	private static final String PORT_PATTERN = "(\\d*(?:\\{[^/]+?\\})?)";

	private static final String PATH_PATTERN = "([^?#]*)";

	private static final String QUERY_PATTERN = "([^#]*)";

	private static final String LAST_PATTERN = "(.*)";

	// Regex patterns that matches URIs. See RFC 3986, appendix B
	private static final Pattern URI_PATTERN = Pattern.compile(
			"^(" + SCHEME_PATTERN + ")?" + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN +
					")?" + ")?" + PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?");

	private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("host=\"?([^;,\"]+)\"?");

	private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("proto=\"?([^;,\"]+)\"?");


	/*@Nullable*/
	private String scheme;

	/*@Nullable*/
	private String ssp;

	/*@Nullable*/
	private String userInfo;

	/*@Nullable*/
	private String host;

	/*@Nullable*/
	private String port;

	private CompositePathComponentBuilder pathBuilder;

	private final MultiValuedMap<String, String> queryParams = new ArrayListValuedHashMap<>();

	/*@Nullable*/
	private String fragment;


	/**
	 * Default constructor. Protected to prevent direct instantiation.
	 *
	 * @see #fromUri(URI)
	 */
	protected UriComponentsBuilder() {
		this.pathBuilder = new CompositePathComponentBuilder();
	}

	/**
	 * Create a deep copy of the given UriComponentsBuilder.
	 *
	 * @param other the other builder to copy from
	 * @since 4.1.3
	 */
	protected UriComponentsBuilder(UriComponentsBuilder other) {
		this.scheme = other.scheme;
		this.ssp = other.ssp;
		this.userInfo = other.userInfo;
		this.host = other.host;
		this.port = other.port;
		this.pathBuilder = other.pathBuilder.cloneBuilder();
		this.queryParams.putAll(other.queryParams);
		this.fragment = other.fragment;
	}


	// Factory methods

	/**
	 * Create a builder that is initialized with the given {@code URI}.
	 *
	 * @param uri the URI to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUri(URI uri) {
		UriComponentsBuilder builder = new UriComponentsBuilder();
		builder.uri(uri);
		return builder;
	}

	/**
	 * Create a builder that is initialized with the given URI string.
	 * <p><strong>Note:</strong> The presence of reserved characters can prevent
	 * correct parsing of the URI string. For example if a query parameter
	 * contains {@code '='} or {@code '&'} characters, the query string cannot
	 * be parsed unambiguously. Such values should be substituted for URI
	 * variables to enable correct parsing:
	 * <pre class="code">
	 * String uriString = &quot;/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromUriString(uriString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 *
	 * @param uri the URI string to initialize with
	 * @return the new {@code UriComponentsBuilder}
	 */
	@SuppressWarnings("squid:S3776")//spring original
	public static UriComponentsBuilder fromUriString(String uri) {
		Validate.notNull(uri, "URI must not be null");
		Matcher matcher = URI_PATTERN.matcher(uri);
		if (matcher.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();
			String scheme = matcher.group(2);
			String userInfo = matcher.group(5);
			String host = matcher.group(6);
			String port = matcher.group(8);
			String path = matcher.group(9);
			String query = matcher.group(11);
			String fragment = matcher.group(13);
			boolean opaque = false;
			if (StringUtils.isNotBlank(scheme)) {
				String rest = uri.substring(scheme.length());
				if (!rest.startsWith(":/")) {
					opaque = true;
				}
			}
			builder.scheme(scheme);
			if (opaque) {
				String ssp = uri.substring(scheme.length()).substring(1);
				if (StringUtils.isNotBlank(fragment)) {
					ssp = ssp.substring(0, ssp.length() - (fragment.length() + 1));
				}
				builder.schemeSpecificPart(ssp);
			} else {
				builder.userInfo(userInfo);
				builder.host(host);
				if (StringUtils.isNotBlank(port)) {
					builder.port(port);
				}
				builder.path(path);
				builder.query(query);
			}
			if (StringUtils.isNotBlank(fragment)) {
				builder.fragment(fragment);
			}
			return builder;
		} else {
			throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
		}
	}

	/**
	 * Create a new {@code UriComponents} object from the URI associated with
	 * the given HttpRequest while also overlaying with values from the headers
	 * "Forwarded" (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * or "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" if
	 * "Forwarded" is not found.
	 *
	 * @param request the source request
	 * @return the URI components of the URI
	 * @since 4.1.5
	 */
	public static UriComponentsBuilder fromHttpRequest(HttpServletRequest request) {
		URI uri = HttpServletRequestUtil.getURI(request);
		HttpHeaders headers = HttpServletRequestUtil.getHeaders(request);
		return fromUri(uri).adaptFromForwardedHeaders(headers);
	}


	// build methods

	/**
	 * Build a {@code UriComponents} instance from the various components contained in this builder.
	 *
	 * @return the URI components
	 */
	public UriComponents build() {
		return build(false);
	}

	/**
	 * Build a {@code UriComponents} instance from the various components
	 * contained in this builder.
	 *
	 * @param encoded whether all the components set in this builder are
	 *                encoded ({@code true}) or not ({@code false})
	 * @return the URI components
	 */
	public UriComponents build(boolean encoded) {
		if (this.ssp != null) {
			return new OpaqueUriComponents(this.scheme, this.ssp, this.fragment);
		} else {
			return new HierarchicalUriComponents(this.scheme, this.fragment, this.userInfo,
					this.host, this.port, this.pathBuilder.build(), this.queryParams, encoded, true);
		}
	}


	/**
	 * Build a URI String. This is a shortcut method which combines calls
	 * to {@link #build()}, then {@link UriComponents#encode()} and finally
	 * {@link UriComponents#toUriString()}.
	 *
	 * @see UriComponents#toUriString()
	 * @since 4.1
	 */
	public String toUriString() {
		return build(false).encode().toUriString();
	}


	// Instance methods

	/**
	 * Initialize components of this builder from components of the given URI.
	 *
	 * @param uri the URI
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder uri(URI uri) {
		Validate.notNull(uri, "URI must not be null");
		this.scheme = uri.getScheme();
		if (uri.isOpaque()) {
			this.ssp = uri.getRawSchemeSpecificPart();
			resetHierarchicalComponents();
		} else {
			if (uri.getRawUserInfo() != null) {
				this.userInfo = uri.getRawUserInfo();
			}
			if (uri.getHost() != null) {
				this.host = uri.getHost();
			}
			if (uri.getPort() != -1) {
				this.port = String.valueOf(uri.getPort());
			}
			if (StringUtils.isNotBlank(uri.getRawPath())) {
				this.pathBuilder = new CompositePathComponentBuilder();
				this.pathBuilder.addPath(uri.getRawPath());
			}
			if (StringUtils.isNotBlank(uri.getRawQuery())) {
				this.queryParams.clear();
				query(uri.getRawQuery());
			}
			resetSchemeSpecificPart();
		}
		if (uri.getRawFragment() != null) {
			this.fragment = uri.getRawFragment();
		}
		return this;
	}

	/**
	 * Set the URI scheme. The given scheme may contain URI template variables,
	 * and may also be {@code null} to clear the scheme of this builder.
	 *
	 * @param scheme the URI scheme
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder scheme(/*@Nullable*/ String scheme) {
		this.scheme = scheme;
		return this;
	}

	/**
	 * Set the URI scheme-specific-part. When invoked, this method overwrites
	 * {@linkplain #userInfo(String) user-info}, {@linkplain #host(String) host},
	 * {@linkplain #port(int) port}, {@linkplain #path(String) pathBuilder}, and
	 * {@link #query(String) query}.
	 *
	 * @param ssp the URI scheme-specific-part, may contain URI template parameters
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder schemeSpecificPart(String ssp) {
		this.ssp = ssp;
		resetHierarchicalComponents();
		return this;
	}

	/**
	 * Set the URI user info. The given user info may contain URI template variables,
	 * and may also be {@code null} to clear the user info of this builder.
	 *
	 * @param userInfo the URI user info
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder userInfo(/*@Nullable*/ String userInfo) {
		this.userInfo = userInfo;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the URI host. The given host may contain URI template variables,
	 * and may also be {@code null} to clear the host of this builder.
	 *
	 * @param host the URI host
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder host(/*@Nullable*/ String host) {
		this.host = host;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the URI port. Passing {@code -1} will clear the port of this builder.
	 *
	 * @param port the URI port
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder port(int port) {
		Validate.isTrue(port >= -1, "Port must be >= -1");
		this.port = String.valueOf(port);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the URI port. Use this method only when the port needs to be
	 * parameterized with a URI variable. Otherwise use {@link #port(int)}.
	 * Passing {@code null} will clear the port of this builder.
	 *
	 * @param port the URI port
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder port(/*@Nullable*/ String port) {
		this.port = port;
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Append the given pathBuilder to the existing pathBuilder of this builder.
	 * The given pathBuilder may contain URI template variables.
	 *
	 * @param path the URI pathBuilder
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder path(String path) {
		this.pathBuilder.addPath(path);
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the pathBuilder of this builder overriding all existing pathBuilder and pathBuilder segment values.
	 *
	 * @param path the URI pathBuilder (a {@code null} value results in an empty pathBuilder)
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder replacePath(/*@Nullable*/ String path) {
		this.pathBuilder = new CompositePathComponentBuilder();
		if (path != null) {
			this.pathBuilder.addPath(path);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Append the given query to the existing query of this builder.
	 * The given query may contain URI template variables.
	 * <p><strong>Note:</strong> The presence of reserved characters can prevent
	 * correct parsing of the URI string. For example if a query parameter
	 * contains {@code '='} or {@code '&'} characters, the query string cannot
	 * be parsed unambiguously. Such values should be substituted for URI
	 * variables to enable correct parsing:
	 * <pre class="code">
	 * UriComponentsBuilder.fromUriString(&quot;/hotels/42&quot;)
	 * .query(&quot;filter={value}&quot;)
	 * .buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 *
	 * @param query the query string
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder query(/*@Nullable*/ String query) {
		if (query != null) {
			Matcher matcher = QUERY_PARAM_PATTERN.matcher(query);
			while (matcher.find()) {
				String name = matcher.group(1);
				String eq = matcher.group(2);
				String value = matcher.group(3);
				if (value == null) {
					value = StringUtils.isNotBlank(eq) ? "" : null;
				}
				queryParam(name, value);
			}
		} else {
			this.queryParams.clear();
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Append the given query parameter to the existing query parameters. The
	 * given name or any of the values may contain URI template variables. If no
	 * values are given, the resulting URI will contain the query parameter name
	 * only (i.e. {@code ?foo} instead of {@code ?foo=bar}.
	 *
	 * @param name   the query parameter name
	 * @param values the query parameter values
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder queryParam(String name, Object... values) {
		Validate.notNull(name, "Name must not be null");
		if (!ArrayUtils.isNotEmpty(values)) {
			for (Object value : values) {
				String valueAsString = (value != null ? value.toString() : null);
				this.queryParams.put(name, valueAsString);
			}
		} else {
			this.queryParams.put(name, null);
		}
		resetSchemeSpecificPart();
		return this;
	}

	/**
	 * Set the URI fragment. The given fragment may contain URI template variables,
	 * and may also be {@code null} to clear the fragment of this builder.
	 *
	 * @param fragment the URI fragment
	 * @return this UriComponentsBuilder
	 */
	public UriComponentsBuilder fragment(/*@Nullable*/ String fragment) {
		if (fragment != null) {
			Validate.notEmpty(fragment, "Fragment must not be empty");
			this.fragment = fragment;
		} else {
			this.fragment = null;
		}
		return this;
	}

	/**
	 * Adapt this builder's scheme+host+port from the given headers, specifically
	 * "Forwarded" (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>,
	 * or "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" if
	 * "Forwarded" is not found.
	 *
	 * @param headers the HTTP headers to consider
	 * @return this UriComponentsBuilder
	 * @since 4.2.7
	 */
	@SuppressWarnings("squid:S3776")
//spring original
	UriComponentsBuilder adaptFromForwardedHeaders(HttpHeaders headers) {
		String forwardedHeader = headers.getFirst("Forwarded");
		if (StringUtils.isNotBlank(forwardedHeader)) {
			String forwardedToUse = getFirstValueToken(forwardedHeader, ",");
			Matcher matcher = FORWARDED_HOST_PATTERN.matcher(forwardedToUse);
			if (matcher.find()) {
				adaptForwardedHost(matcher.group(1).trim());
			}
			matcher = FORWARDED_PROTO_PATTERN.matcher(forwardedToUse);
			if (matcher.find()) {
				scheme(matcher.group(1).trim());
			}
		} else {
			String hostHeader = headers.getFirst("X-Forwarded-Host");
			if (StringUtils.isNotBlank(hostHeader)) {
				adaptForwardedHost(getFirstValueToken(hostHeader, ","));
			}

			String portHeader = headers.getFirst("X-Forwarded-Port");
			if (StringUtils.isNotBlank(portHeader)) {
				port(Integer.parseInt(getFirstValueToken(portHeader, ",")));
			}

			String protocolHeader = headers.getFirst("X-Forwarded-Proto");
			if (StringUtils.isNotBlank(protocolHeader)) {
				scheme(getFirstValueToken(protocolHeader, ","));
			}
		}

		if ((this.scheme != null) && ((this.scheme.equals("http") && "80".equals(this.port)) ||
				(this.scheme.equals("https") && "443".equals(this.port)))) {
			this.port = null;
		}

		return this;
	}

	/**
	 * Returns first token only. e.g: value="123, 345, 678"  with delim="," will return "123"
	 * or more formally: return value.substring(0,value.indexOf(delim));
	 *
	 * @param value
	 * @param delim
	 * @return
	 */
	private static String getFirstValueToken(String value, String delim) {
		int pos = value.indexOf(delim);
		return (pos == -1) ? value : value.substring(0, pos);
	}

	private void adaptForwardedHost(String hostToUse) {
		int portSeparatorIdx = hostToUse.lastIndexOf(':');
		if (portSeparatorIdx > hostToUse.lastIndexOf(']')) {
			host(hostToUse.substring(0, portSeparatorIdx));
			port(Integer.parseInt(hostToUse.substring(portSeparatorIdx + 1)));
		} else {
			host(hostToUse);
			port(null);
		}
	}

	private void resetHierarchicalComponents() {
		this.userInfo = null;
		this.host = null;
		this.port = null;
		this.pathBuilder = new CompositePathComponentBuilder();
		this.queryParams.clear();
	}

	private void resetSchemeSpecificPart() {
		this.ssp = null;
	}


	/**
	 * Clone this {@code UriComponentsBuilder}.
	 *
	 * @return the cloned {@code UriComponentsBuilder} object
	 * @since 4.2.7
	 */
	public UriComponentsBuilder cloneBuilder() {
		return new UriComponentsBuilder(this);
	}


	private interface PathComponentBuilder {

		/*@Nullable*/
		PathComponent build();

		PathComponentBuilder cloneBuilder();
	}


	private static class CompositePathComponentBuilder implements PathComponentBuilder {

		private final LinkedList<PathComponentBuilder> builders = new LinkedList<>();

		public void addPath(String path) {
			if (StringUtils.isNotBlank(path)) {
				PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
				FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);

				if (fpBuilder == null) {
					fpBuilder = new FullPathComponentBuilder();
					this.builders.add(fpBuilder);
				}
				if (psBuilder != null) {
					fpBuilder.append(path.startsWith("/") ? path : "/" + path);
				} else {
					fpBuilder.append(path);
				}
			}
		}

		@SuppressWarnings("unchecked")
		/*@Nullable*/
		private <T> T getLastBuilder(Class<T> builderClass) {
			if (!this.builders.isEmpty()) {
				PathComponentBuilder last = this.builders.getLast();
				if (builderClass.isInstance(last)) {
					return (T) last;
				}
			}
			return null;
		}

		@Override
		public PathComponent build() {
			int size = this.builders.size();
			List<PathComponent> components = new ArrayList<>(size);
			for (PathComponentBuilder componentBuilder : this.builders) {
				PathComponent pathComponent = componentBuilder.build();
				if (pathComponent != null) {
					components.add(pathComponent);
				}
			}
			if (components.isEmpty()) {
				return HierarchicalUriComponents.NULL_PATH_COMPONENT;
			}
			if (components.size() == 1) {
				return components.get(0);
			}
			return new HierarchicalUriComponents.PathComponentComposite(components);
		}

		@Override
		public CompositePathComponentBuilder cloneBuilder() {
			CompositePathComponentBuilder compositeBuilder = new CompositePathComponentBuilder();
			for (PathComponentBuilder builder : this.builders) {
				compositeBuilder.builders.add(builder.cloneBuilder());
			}
			return compositeBuilder;
		}
	}


	private static class FullPathComponentBuilder implements PathComponentBuilder {

		private final StringBuilder pathBuilder = new StringBuilder();

		public void append(String path) {
			this.pathBuilder.append(path);
		}

		@Override
		public PathComponent build() {
			if (this.pathBuilder.length() == 0) {
				return null;
			}
			String path = this.pathBuilder.toString();
			while (true) {
				int index = path.indexOf("//");
				if (index == -1) {
					break;
				}
				path = path.substring(0, index) + path.substring(index + 1);
			}
			return new HierarchicalUriComponents.FullPathComponent(path);
		}

		@Override
		public FullPathComponentBuilder cloneBuilder() {
			FullPathComponentBuilder builder = new FullPathComponentBuilder();
			builder.append(this.pathBuilder.toString());
			return builder;
		}
	}


	private static class PathSegmentComponentBuilder implements PathComponentBuilder {

		private final List<String> pathSegments = new LinkedList<>();

		@Override
		public PathComponent build() {
			return (this.pathSegments.isEmpty() ? null :
					new HierarchicalUriComponents.PathSegmentComponent(this.pathSegments));
		}

		@Override
		public PathSegmentComponentBuilder cloneBuilder() {
			PathSegmentComponentBuilder builder = new PathSegmentComponentBuilder();
			builder.pathSegments.addAll(this.pathSegments);
			return builder;
		}
	}

}
