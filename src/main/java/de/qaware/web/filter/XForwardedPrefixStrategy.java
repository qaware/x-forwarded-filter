package de.qaware.web.filter;

/**
 * Prepend or Replace the the context path with the value from X-Forwarded-Prefix
 */
public enum XForwardedPrefixStrategy {
	PREPEND,
	REPLACE
}
