package at.medbits.tools.launchtopom.handler.model;

import java.util.ArrayList;
import java.util.List;

public class LaunchBundles {
	public enum Type {
		WORKSPACE, TARGET
	}

	private Type type;
	private List<LaunchBundle> bundles = new ArrayList<>();

	public static LaunchBundles of(String launchConfigLine, Type type) {
		LaunchBundles ret = new LaunchBundles();
		ret.type = type;

		String[] parts = launchConfigLine.split(",");
		if (parts != null && parts.length > 0) {
			for (String part : parts) {
				LaunchBundle launchBundle = LaunchBundle.of(part);
				ret.bundles.add(launchBundle);
			}
		}
		return ret;
	}

	public static LaunchBundles fragments(LaunchBundles bundles) {
		LaunchBundles ret = new LaunchBundles();
		ret.type = bundles.type;

		for (LaunchBundle bundle : bundles.getLaunchBundles()) {
			if (bundle.isFragment()) {
				ret.bundles.add(bundle);
			}
		}
		return ret;
	}

	public List<LaunchBundle> getLaunchBundles() {
		return bundles;
	}

	public boolean isType(Type type) {
		return this.type.equals(type);
	}
}
