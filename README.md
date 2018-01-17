# (x-)forwarded-filter

Standalone (x-) filter.
Supports adapting scheme+host+port from HTTP headers. Specifically
-  `Forwarded` [as of RFC 7239]()http://tools.ietf.org/html/rfc7239)
- or if `Forwarded` header is NOT found:
  - `X-Forwarded-Host`
  - `X-Forwarded-Port`
  - `X-Forwarded-Proto`
  - `X-forwarded-prefix`
