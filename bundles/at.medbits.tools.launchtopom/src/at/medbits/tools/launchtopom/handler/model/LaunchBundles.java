package at.medbits.tools.launchtopom.handler.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LaunchBundles {
	public enum Type {
		WORKSPACE, TARGET
	}

	private Type type;
	private List<LaunchBundle> bundles = new ArrayList<>();

	public static LaunchBundles of(Collection<String> launchBundles, Type type) {
		LaunchBundles ret = new LaunchBundles();
		ret.type = type;
		if (launchBundles != null) {
			for (String part : launchBundles) {
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
