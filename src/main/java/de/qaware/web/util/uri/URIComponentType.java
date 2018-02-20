/*
 * Copyright 2002-2018 the original author or authors.
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

package de.qaware.web.util.uri;

/**
 * Enumeration used to identify the allowed characters per URI component.
 * <p>Contains methods to indicate whether a given character is valid in a specific URI component.
 *
 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
 */
@SuppressWarnings("squid:S1067")
//number of condition operators
enum URIComponentType {

	SCHEME {
		@Override
		public boolean isAllowedCharacter(int c) {
			return isAlpha(c) || isDigit(c) || '+' == c || '-' == c || '.' == c;
		}
	},
	AUTHORITY {
		@Override
		public boolean isAllowedCharacter(int c) {
			return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
		}
	},
	USER_INFO {
		@Override
		public boolean isAllowedCharacter(int c) {
			return isUnreserved(c) || isSubDelimiter(c) || ':' == c;
		}
	},
	HOST_IPV4 {
		@Override
		public boolean isAllowedCharacter(int c) {
			return isUnreserved(c) || isSubDelimiter(c);
		}
	},
	HOST_IPV6 {
		@Override
		public boolean isAllowedCharacter(int c) {
			return isUnreserved(c) || isSubDelimiter(c) || '[' == c || ']' == c || ':' == c;
		}
	},
	PORT {
		@Override
		public boolean isAllowedCharacter(int c) {
			return isDigit(c);
		}
	},
	PATH {
		@Override
		public boolean isAllowedCharacter(int c) {
			return isPchar(c) || '/' == c;
		}
	},
	PATH_SEGMENT {
		@Override
		public boolean isAllowedCharacter(int c) {
			return isPchar(c);
		}
	},
	QUERY {
		@Override
		public boolean isAllowedCharacter(int c) {
			return isPchar(c) || '/' == c || '?' == c;
		}
	},
	QUERY_PARAM {
		@Override
		public boolean isAllowedCharacter(int c) {
			return !('=' == c || '&' == c) && (isPchar(c) || '/' == c || '?' == c);
		}
	},
	FRAGMENT {
		@Override
		public boolean isAllowedCharacter(int c) {
			return isPchar(c) || '/' == c || '?' == c;
		}
	},
	URI {
		@Override
		public boolean isAllowedCharacter(int c) {
			return isUnreserved(c);
		}
	};

	/**
	 * Indicates whether the given character is allowed in this URI component.
	 *
	 * @param c character to check
	 * @return {@code true} if the character is allowed; {@code false} otherwise
	 */
	public abstract boolean isAllowedCharacter(int c);

	/**
	 * Indicates whether the given character is in the {@code ALPHA} set.
	 *
	 * @param c character to check
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isAlpha(int c) {
		return isLowerCaseAtoZ(c) || isUpperCaseAtoZ(c);
	}

	private boolean isUpperCaseAtoZ(int c) {
		return c >= 'A' && c <= 'Z';
	}

	private boolean isLowerCaseAtoZ(int c) {
		return c >= 'a' && c <= 'z';
	}

	/**
	 * Indicates whether the given character is in the {@code DIGIT} set.
	 *
	 * @param c character to check
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isDigit(int c) {
		return (c >= '0' && c <= '9');
	}

	/**
	 * Indicates whether the given character is in the {@code gen-delims} set.
	 *
	 * @param c character to check
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isGenericDelimiter(int c) {
		return (':' == c || '/' == c || '?' == c || '#' == c || '[' == c || ']' == c || '@' == c);
	}

	/**
	 * Indicates whether the given character is in the {@code sub-delims} set.
	 *
	 * @param c character to check
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isSubDelimiter(int c) {
		return ('!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c ||
				',' == c || ';' == c || '=' == c);
	}

	/**
	 * Indicates whether the given character is in the {@code reserved} set.
	 *
	 * @param c character to check
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isReserved(int c) {
		return (isGenericDelimiter(c) || isSubDelimiter(c));
	}

	/**
	 * Indicates whether the given character is in the {@code unreserved} set.
	 *
	 * @param c character to check
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isUnreserved(int c) {
		return (isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c);
	}

	/**
	 * Indicates whether the given character is in the {@code pchar} set.
	 *
	 * @param c character to check
	 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
	 */
	protected boolean isPchar(int c) {
		return (isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c);
	}
}
