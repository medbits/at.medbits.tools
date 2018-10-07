package at.medbits.tools.launchtopom.handler.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class FragmentRequirementsXml {

	private StringBuilder sb = new StringBuilder();

	public static FragmentRequirementsXml of(LaunchBundles workspaceBundles,
			LaunchBundles targetBundles) {
		FragmentRequirementsXml ret = new FragmentRequirementsXml();

		StringBuilder sb = ret.sb;
		sb.append("<configuration>");
		sb.append("<dependency-resolution>");
		sb.append("<extraRequirements>");
		// add dependencies for workspace fragments
		for (LaunchBundle launchBundle : workspaceBundles.getLaunchBundles()) {
			// add fragments only
			if (launchBundle.isFragment() && isNotPlatformSpecific(launchBundle)) {
				addRequirement(sb, launchBundle);
			}
		}
		// add dependencies for target fragments
		for (LaunchBundle launchBundle : targetBundles.getLaunchBundles()) {
			// add fragments only
			if (launchBundle.isFragment() && isNotPlatformSpecific(launchBundle)) {
				addRequirement(sb, launchBundle);
			}
		}
		sb.append("</extraRequirements>");
		sb.append("</dependency-resolution>");
		sb.append("</configuration>");
		return ret;
	}

	private static boolean isNotPlatformSpecific(LaunchBundle launchBundle) {
		return !launchBundle.getId().contains("x86") && !launchBundle.getId().contains("gtk")
				&& !launchBundle.getId().contains("win32") && !launchBundle.getId().contains("cocoa");
	}

	private static void addRequirement(StringBuilder sb, LaunchBundle launchBundle) {
		sb.append("<requirement>");
		sb.append("<type>eclipse-plugin</type>");
		sb.append("<id>").append(launchBundle.getId()).append("</id>");
		sb.append("<versionRange>0.0.0</versionRange>");
		sb.append("</requirement>");
	}

	public InputStream getInputStream() {
		return new ByteArrayInputStream(sb.toString().getBytes());
	}
}
