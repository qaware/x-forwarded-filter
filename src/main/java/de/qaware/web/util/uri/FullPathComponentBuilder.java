package de.qaware.web.util.uri;

/**
 *
 */
class FullPathComponentBuilder implements PathComponentBuilder {

	private final StringBuilder pathBuilder = new StringBuilder();

	public void append(String path) {
		this.pathBuilder.append(path);
	}

	@Override
	public HierarchicalUriComponents.PathComponent build() {
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

	public void removeTrailingSlash() {
		int index = this.pathBuilder.length() - 1;
		if (this.pathBuilder.charAt(index) == '/') {
			this.pathBuilder.deleteCharAt(index);
		}
	}

	@Override
	public FullPathComponentBuilder cloneBuilder() {
		FullPathComponentBuilder builder = new FullPathComponentBuilder();
		builder.append(this.pathBuilder.toString());
		return builder;
	}
}
