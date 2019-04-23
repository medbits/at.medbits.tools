package at.medbits.tools.launchtopom.handler.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

public class TychoSurefireXml {

	private StringBuilder sb = new StringBuilder();

	public static TychoSurefireXml of(Map<String, Object> attributes, LaunchBundles workspaceBundles,
			LaunchBundles targetBundles) {
		TychoSurefireXml ret = new TychoSurefireXml();

		StringBuilder sb = ret.sb;
		sb.append("<configuration>");
		String applicationString = (String) attributes.get("application");
		String productString = (String) attributes.get("product");
		if (applicationString != null
				&& applicationString.equals("org.eclipse.pde.junit.runtime.coretestapplication")) {
			sb.append("<useUIHarness>false</useUIHarness>");
		} else if (applicationString != null
				&& !applicationString.equals("org.eclipse.pde.junit.runtime.coretestapplication")) {
			sb.append("<useUIHarness>true</useUIHarness>");
			sb.append("<application>" + applicationString + "</application>");
			Object runInUi = attributes.get("run_in_ui_thread");
			if (runInUi instanceof String) {
				runInUi = "true";
			} else if (runInUi instanceof Boolean) {
				runInUi = Boolean.toString((Boolean) runInUi);
			} else {
				runInUi = "false";
			}
			sb.append("<useUIThread>" + runInUi + "</useUIThread>");
		} else if (productString != null && !productString.isEmpty()) {
			sb.append("<useUIHarness>true</useUIHarness>");
			Object runInUi = attributes.get("run_in_ui_thread");
			if (runInUi instanceof String) {
				runInUi = "true";
			} else if (runInUi instanceof Boolean) {
				runInUi = Boolean.toString((Boolean) runInUi);
			} else {
				runInUi = "false";
			}
			sb.append("<useUIThread>" + runInUi + "</useUIThread>");
			sb.append("<product>").append(productString).append("</product>");
			sb.append("<application>org.eclipse.e4.ui.workbench.swt.E4Application</application>");
		}
		String main = (String) attributes.get("org.eclipse.jdt.launching.MAIN_TYPE");
		if (main != null) {
			sb.append("<testClass>").append(main).append("</testClass>");
		}
		sb.append("<showEclipseLog>true</showEclipseLog>");
		String vmArgs = (String) attributes.get("org.eclipse.jdt.launching.VM_ARGUMENTS");
		if (vmArgs != null) {
			sb.append("<argLine>").append(vmArgs).append("</argLine>");
		}

		// add dependencies for workspace bundles
		sb.append("<dependencies>");
		// add dependencies for target bundles
		for (LaunchBundle launchBundle : targetBundles.getLaunchBundles()) {
			// do not add fragments, often platform specific, breaks the build, exception
			// for logging
			if (!launchBundle.isFragment()) {
				addDependency(sb, launchBundle);
			} else if (launchBundle.getId().contains("logback")) {
				addDependency(sb, launchBundle);
			}
		}
		sb.append("</dependencies>");
		// add startup for workspace bundles
		sb.append("<bundleStartLevel>");
		for (LaunchBundle launchBundle : workspaceBundles.getLaunchBundles()) {
			if (!launchBundle.isDefaultRunlevel()) {
				addStartup(sb, launchBundle);
			}
		}
		// add startup for target bundles
		for (LaunchBundle launchBundle : targetBundles.getLaunchBundles()) {
			if (!launchBundle.isDefaultRunlevel()) {
				addStartup(sb, launchBundle);
			}
		}
		sb.append("</bundleStartLevel>");
		sb.append("</configuration>");
		return ret;
	}

	private static void addStartup(StringBuilder sb, LaunchBundle launchBundle) {
		sb.append("<bundle>");
		sb.append("<id>").append(launchBundle.getId()).append("</id>");
		sb.append("<level>").append(launchBundle.getRunlevel()).append("</level>");
		sb.append("<autoStart>").append(launchBundle.isAutostart()).append("</autoStart>");
		sb.append("</bundle>");
	}

	private static void addDependency(StringBuilder sb, LaunchBundle launchBundle) {
		sb.append("<dependency>");
		sb.append("<type>eclipse-plugin</type>");
		sb.append("<artifactId>").append(launchBundle.getId()).append("</artifactId>");
		sb.append("<version>0.0.0</version>");
		sb.append("</dependency>");
	}

	public InputStream getInputStream() {
		return new ByteArrayInputStream(sb.toString().getBytes());
	}
}
