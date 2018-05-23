package at.medbits.tools.launchtopom.handler.model;

import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;

public class LaunchBundle {

	private String id = "";
	private int runlevel = 4;
	private boolean autostart = false;
	private boolean fragment = false;

	public static LaunchBundle of(String part) {
		LaunchBundle ret = new LaunchBundle();
		String[] pluginParts = part.split("@");
		if (pluginParts != null && pluginParts.length == 2) {
			// could contain a version
			String[] pluginSubParts = pluginParts[0].split("\\*");
			if (pluginSubParts.length > 0) {
				ret.id = pluginSubParts[0];
			}
			String[] runlevelParts = pluginParts[1].split(":");
			if (runlevelParts != null && runlevelParts.length == 2) {
				if (!runlevelParts[0].equals("default")
						|| (!runlevelParts[1].equals("default") && !runlevelParts[1].equals("false"))) {
					ret.runlevel = runlevelParts[0].equals("default") ? 4 : Integer.parseInt(runlevelParts[0]);

					ret.autostart = runlevelParts[1].equals("default") ? Boolean.FALSE
							: Boolean.parseBoolean(runlevelParts[1]);
				}
			}
		}
		if (ret.id != null && !ret.id.isEmpty()) {
			ModelEntry entry = PluginRegistry.findEntry(ret.id);
			if (entry != null && entry.getModel().isFragmentModel()) {
				ret.fragment = true;
			}
		}
		return ret;
	}

	public String getId() {
		return id;
	}

	public int getRunlevel() {
		return runlevel;
	}

	public boolean isAutostart() {
		return autostart;
	}

	public boolean isDefaultRunlevel() {
		return runlevel == 4 && autostart == false;
	}

	public boolean isTestBundle() {
		return getId().endsWith(".test");
	}

	public boolean isFragment() {
		return fragment;
	}
}
