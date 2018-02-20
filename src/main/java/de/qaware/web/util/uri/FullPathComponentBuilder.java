/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.qaware.web.util.uri;

/**
 *
 */
final class FullPathComponentBuilder implements PathComponentBuilder {

	private final StringBuilder pathBuilder = new StringBuilder();

	/**
	 * Append path to this builder
	 *
	 * @param path to append
	 */
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
		return new FullPathComponent(path);
	}

	/**
	 * if there's a '/' at the end, strip it,
	 */
	void removeTrailingSlash() {
		int index = this.pathBuilder.length() - 1;
		if (this.pathBuilder.charAt(index) == '/') {
			//delete trailing slash by set the current length mark of this builder to length()-1
			//same effect as deleteCharAt(index) but A LOT FASTER as it does not require copping the array
			this.pathBuilder.setLength(index);
		}
	}

	@Override
	public FullPathComponentBuilder cloneBuilder() {
		FullPathComponentBuilder builder = new FullPathComponentBuilder();
		builder.append(this.pathBuilder.toString());
		return builder;
	}
}
