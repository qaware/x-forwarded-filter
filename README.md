[![Build Status](https://travis-ci.org/qaware/x-forwarded-filter.svg?branch=master)](https://travis-ci.org/qaware/x-forwarded-filter) [![sonarcloud](https://sonarcloud.io/api/project_badges/measure?project=de.qaware.xff%3Ax-forwarded-filter&metric=alert_status)]() [![Coverage Status](https://coveralls.io/repos/github/qaware/x-forwarded-filter/badge.svg?branch=master)](https://coveralls.io/github/qaware/x-forwarded-filter?branch=master) [![License](https://img.shields.io/badge/license-APACHE2.0-green.svg?style=flat)() [![Download](https://api.bintray.com/packages/qaware-oss/maven/x-forwarded-filter/images/download.svg)](https://bintray.com/qaware-oss/maven/x-forwarded-filter/_latestVersion)




# (x-)forwarded*  Filter

Standalone (x-)forwarded* Filter.

The (x-)forwarded*  Http Headers are a pseudo standard with varying and mostly lacking support in most products.

Supported Http Headers:
-  `Forwarded` [as of RFC 7239](http://tools.ietf.org/html/rfc7239)
- or if `Forwarded` header is NOT found:
  - `X-Forwarded-Host`
  - `X-Forwarded-Port`
  - `X-Forwarded-Proto`
- Additionally for both cases: `X-Forwarded-Prefix` to adapt the `getContextPath` result is supported.

Features:
- Supports adapting the scheme, host, port and prefix(contextPath) of `HttpServletRequest`'s by replacing them transparently using the information from the (x-)forwarded* header(s).
- Supports `HttpServletResponse.sendRedirect(location)` by rewriting them accordingly
- Processing of headers and extracting parts (e.g. form `Forwarded` ) is case insensitive
  - valid: X-Forwarded-Host, x-forwarded-host, X-FORWARED-HOST,..
- Supports multiple headers of same name in request -> use Find-First-Strategy
  - e.g.<br/>
    X-Forwarded-Host: "hostA"<br/>
    X-Forwarded-Host: "hostB"   => filter will use "hostA" 
- Supports multiple COMMA-SPACE separated values inside a headers value ->use Find-First-Strategy
  - e.g. X-Forwarded-Host: "hostA, hostB"  => filter will use "hostA"
- Configurable processing strategy for `X-Forwarded-Proto` =>  PREPEND or REPLACE the contextPath
  `ForwardedFilter.xForwardedProtoStrategy=[PREPEND, REPLACE]`
- Configurable header processing and removal strategy
  `ForwardedFilter.headerProcessingStrategy=[USE_AND_KEEP, USE_AND_REMOVE, DONT_USE_AND_REMOVE]`
  - EVAL_AND_KEEP - process headers and keep them in the list of headers for downstream processing
  - EVAL_AND_REMOVE - process headers and remove them. Wont be visible any more when accessing getHeader(s)
  - DONT_EVAL_AND_REMOVE - don't process the headers, just remove them.
  - ~~DONT_EVAL_AND_DONT_REMOVE~~ => just don't activate this filter - same effect
   

# TOC
  - [Why would i use this filter?](#why-would-i-use-this-filter)
  - [What this filter is not](#what-this-filter-is-not)
  - [Dependencies Maven](#dependencies)
  - [Usage](#usage)
    - [SpringBoot](#springboot)
    - [Websphere liberty]()
    - [Disable other (x-)forwarded* header processing in various products](#disable-other-x-forwarded-header-processing-in-various-products)
      - [Websphere liberty](#websphere-liberty)
      - [Spring](#spring)
      - [Tomcat](#tomcat)
  - [Implementation Details](#implementation-details)
  - [How to Build](#how-to-build)
  - [(x-)forwarded* support in various products](#x-forwarded-support-in-various-products)

## Why would i use this filter?
1. Imagine your applications sits behind a proxy or a chain of proxies
2. Imagine your application is reachable over different DNS names 
 
Now you require, for whatever reason, the exact URL - as the client calling you sees it - your where called with.
For example because you want to redirect you client to another URL/Service (e.g. a login service), lets call this service 'FOO' and you require 'FOO' to send the user back to you after he's done - hence requiring your service to tell 'FOO' the exact URL as the clients browser used to contact you in order to send him back to you.
  
Therefore you require the protocol, host, port and maybe the prefix form `HttpServletRequest` to reconstruct the original URL.

Without a mechanism aware of "forwarded" headers your `HttpServletRequest` does only contain the protocol, host and port with which the proxyserver calling you connected to your service - probably  some internal dns address and port.
Furthermore the proxy may strips some prefix form your request path - for example because it maps maps multiple services behind a single domain

To support this usecase many proxies support adding (x-)forwarded* header.

Unfortunately many frameworks and webservers have very bad support for these (pseudo-)standard forwarded headers, implement them poorly or lack the support completely.

Add this filter and it will transparently take care of these concerns for you by wrapping the `HttpServletRequest` which will overwrite various methods to return the correct information.
 
## What this filter is not
- no support for "client identification" with x-forwarded-for
 
## Dependencies

The JARs are available via Maven Central and JCenter. 

If you are using Maven to build your project, add the following to the `pom.xml` file.

```XML
<!-- https://mvnrepository.com/artifact/de.qaware.majx/majx -->
<dependency>
    <groupId>de.qaware.xff</groupId>
    <artifactId>x-forwarded-filter</artifactId>
    <version>1.0</version>
</dependency>
```

In case you are using Gradle to build your project, add the following to the `build.gradle` file:

```groovy
repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/de.qaware.xff/x-forwarded-filter
    compile group: 'de.qaware.xff', name: 'x-forwarded-filter', version: '1.0'
}
```
 
## Usage
You probably should disable all other x-forwarded processing code - like done by your underlying webserver.

### SpringBoot
```java
import de.qaware.web.filter.ForwardedHeaderFilter;
//..
@Bean
FilterRegistrationBean forwardedHeaderFilter() {
  FilterRegistrationBean frb = new FilterRegistrationBean();
  frb.setFilter(new ForwardedHeaderFilter());
  frb.setOrder(Ordered.HIGHEST_PRECEDENCE);
  return frb;
}
```

### Websphere liberty
TODO

### Disable other (x-)forwarded* header processing in various products
#### Websphere liberty
Disable [trustedHeaderOrigin](https://www.ibm.com/support/knowledgecenter/beta/SSEQTJ_8.5.5/com.ibm.websphere.wlp.nd.doc/ae/rwlp_config_httpDispatcher.html#rwlp_config_httpDispatcher__trustedHeaderOrigin) inside server.xml 
`<httpDispatcher trustedHeaderOrigin="none" />`

#### Spring
 1. Don't register the org.springframework.web.filter.ForwardedHeaderFilter
 2. `server.use-forward-headers=false` generically turns off the forward engine in the underlying webserver! (e.g. tomcat) see: ["howto-use-tomcat-behind-a-proxy-server"](https://docs.spring.io/spring-boot/docs/current/reference/html/howto-embedded-servlet-containers.html#howto-use-tomcat-behind-a-proxy-server")

#### Tomcat
TODO
  
## Implementation Details
Based on [Springs ForwardedHeaderFilter](https://github.com/spring-projects/spring-framework/blob/master/spring-web/src/main/java/org/springframework/web/filter/ForwardedHeaderFilter.java) but
 - without Spring dependency -> easily integrable into many projects
 - has toogle to NOT remove the evaluated headers from the request 
   - allows using this filter inside a Proxy to forward/append to these headers to downstream services (e.g. Zuul)
 - Selectable processing strategy for `X-Forwarded-Prefix`  
   - `PREPEND` the Context-Path of the Application with the (first) value from `X-forwarded-Prefix`
     -  example:
   - `REPLACE` the Context-Path of the Application with the (first) value from `X-forwarded-Prefix`
     -  example:
 
## How to build 
Based on Gradle. First trigger of build will take longer and download the buildtool.
execute:
`gradlew build`


## (x-)forwarded* support in various products

|                                           | this filter | Spring 4.3.9 | Tomcat | IBM Websphere Liberty | Jetty | Apache mod_proxy                  | nginx | 
| ----------------------------------------- |------------ | ------------ | ------ | --------------------- | ----- | --------------------------------- | ------| 
| Forwarded                                 | YES         | YES          | ?      | NO                    | YES   | NO(manually with rewrite engine?) | manually with custom proxy_set_header  | 
| X-Forwarded-Proto                         | YES         | YES          | YES    | YES                   | YES   | NO(manually with rewrite engine?) | manually with custom proxy_set_header  | 
| X-Forwarded-Host                          | YES         | YES          | NO     | NO                    | YES   | YES                               | manually with custom proxy_set_header  | 
| X-Forwarded-Port                          | YES         | YES          | YES    | NO                    | NO    | NO(manually with rewrite engine?) | manually with custom proxy_set_header  | 
| X-Forwarded-Prefix                        | YES         | YES          | NO     | NO                    | NO    | NO(manually with rewrite engine?) | manually with custom proxy_set_header  | 
| X-Forwarded-By                            | NO          | NO           | YES    | NO                    | NO    | NO(manually with rewrite engine?) | manually with custom proxy_set_header  | 
| X-Forwarded-For                           | NO          | NO           | YES    | NO                    | YES   | YES                               | YES | 
| X-Forwarded-Server                        | NO          | NO           | NO     | NO                    | YES   | YES                               | manually with custom proxy_set_header  | 
| X-Real-IP                                 | NO          | NO           | NO     | NO                    | NO    | NO(manually with rewrite engine?) | YES | 
| X-Proxied-Https                           | NO          | NO           | NO     | NO                    | YES   | NO(manually with rewrite engine?) | manually with custom proxy_set_header  | 
| X-Forwarded-SSL                           | NO          | NO           | YES    | NO                    | NO    | NO?                               | manually with custom proxy_set_header  | 
| Supports COMMA+SPACE separated values     | YES         | YES          | NO     | NO                    | YES   | ?                                 | ? |
| Supports multiple Headers with same name  | YES         | YES          | YES    | NO                    | NO    | ?                                 | ? | 
| Strip forwarded header from `Request`     | YES(toggle) | YES(always)  | ?      | ?                     | ?     | ?                                 | ? | 
| Supports relative redirects in `Response` | YES         | YES          | NO     | NO                    | NO    | ?                                 | ? |
| `X-Forwarded-Prefix` processing strategy    | PREPEND or REPLACE | REPLACE | ?    | ?                     | ?     | ?                                 | ? | 


| Header                 | Description |
| ---------------------- | ----------- |
| Forwarded              | same function as x-forwarded-{proto,host,port} [see RFC 7239](http://tools.ietf.org/html/rfc7239) |
| X-Forwarded-Proto      | The original protocol requested by the client in the Host HTTP request header |
| X-Forwarded-Host       | The original host requested by the client in the Host HTTP request header |
| X-Forwarded-Port       | The original port requested by the client in the Host HTTP request header |
| X-Forwarded-Prefix     | If a proxy strips a prefix from the path before forwarding it might add an X-Forwarded-Prefix header |
| X-Forwarded-By         | Name of the http header created by this valve to hold the list of proxies that have been processed in the incoming remoteIpHeader |
| X-Forwarded-For        | The IP address of the client |
| X-Forwarded-Server     | The hostname of the proxy server |
| X-Real-IP              | nginx synonym for x-forwarded-for |
| X-Proxied-Https        | ? |
| X-Forwarded-SSL        | ? |



# Maintainer

Michael Frank, <michael.frank@qaware.de>

# License

This software is provided under the Apache2.0 open source license, read the `LICENSE` file for details.
