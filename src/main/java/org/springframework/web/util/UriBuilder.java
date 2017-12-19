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
 * WITHOUUriBuilder WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.web.util;

import java.net.URI;
import java.util.Map;

import org.springframework.util.MultiValueMap;

/**
 * Builder-style methods to prepare and expand a URI template with variables.
 *
 * <p>Effectively a generalization of {@link UriComponentsBuilder} but with
 * shortcuts to expand directly into {@link URI} rather than
 * {@link UriComponents} and also leaving common concerns such as encoding
 * preferences, a base URI, and others as implementation concerns.
 *
 * <p>Typically obtained via {@link UriBuilderFactory} which serves as a central
 * component configured once and used to create many URLs.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see UriBuilderFactory
 * @see UriComponentsBuilder
 */
public interface UriBuilder {

}
