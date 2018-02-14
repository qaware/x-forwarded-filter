package de.qaware.web.util.uri;

/**
 *
 */
final class FullPathComponentBuilder implements PathComponentBuilder {

	private final StringBuilder pathBuilder = new StringBuilder();

	/**
	 * Append path to this builder
	 * @param  path to append
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
	 * if theres a '/' at the end, strip it,
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
