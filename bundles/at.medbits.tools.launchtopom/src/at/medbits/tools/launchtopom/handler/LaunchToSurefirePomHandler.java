package at.medbits.tools.launchtopom.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

import at.medbits.tools.launchtopom.handler.model.FragmentRequirementsXml;
import at.medbits.tools.launchtopom.handler.model.LaunchBundles;
import at.medbits.tools.launchtopom.handler.model.LaunchBundles.Type;
import at.medbits.tools.launchtopom.handler.model.TychoSurefireXml;

public class LaunchToSurefirePomHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (!selection.isEmpty() && selection instanceof TreeSelection) {
			Object element = ((org.eclipse.jface.viewers.TreeSelection) selection).getFirstElement();
			if (element instanceof IFile) {
				IFile file = (IFile) element;
				if ("launch".equals(file.getFileExtension())) {
					if (writePomFromLaunch(file)) {
						refreshProject(file);
					}
				}
			}
		}

		return null;
	}

	private void refreshProject(IFile file) {
		try {
			file.getProject().refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
		} catch (CoreException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
					"Error refreshing pom." + getStackTrace(e));
		}
	}

	/**
	 * Write a pom file with the information from the launch configuration. Target
	 * platform dependencies are included in the pom file, <b>workspace dependencies
	 * have to be managed in the test project manifest</b>. Tycho uses the
	 * MANIFEST-first approach to resolve the dependencies.
	 * 
	 * @param file
	 * @return
	 */
	private boolean writePomFromLaunch(IFile file) {
		try {
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfiguration launch = manager.getLaunchConfiguration(file);

			Map<String, Object> attributes = launch.getAttributes();
			LaunchBundles targetBundles = LaunchBundles.of((String) attributes.get("selected_target_plugins"),
					Type.TARGET);
			LaunchBundles workspaceBundles = LaunchBundles.of((String) attributes.get("selected_workspace_plugins"),
					Type.WORKSPACE);

			String artifactId = file.getProject().getName();
			org.apache.maven.model.Model model = new Model();
			model.setArtifactId(artifactId);
			model.setPackaging("eclipse-test-plugin");
			model.setModelVersion("4.0.0");

			File parentPom = lookupParentPom(file);
			if (parentPom != null) {
				MavenXpp3Reader reader = new MavenXpp3Reader();
				try (FileInputStream fin = new FileInputStream(parentPom)) {
					Model parentContent = reader.read(fin);
					Parent parent = new Parent();
					parent.setArtifactId(parentContent.getArtifactId());
					parent.setVersion(parentContent.getVersion());
					parent.setGroupId(parentContent.getGroupId());
					model.setParent(parent);
				}
			} else {
				Parent parent = new Parent();
				parent.setArtifactId("parentArtifactId");
				parent.setVersion("1.0.0-SNAPSHOT");
				parent.setGroupId("parentGroupId");
				model.setParent(parent);
			}

			Build build = new Build();
			build.setSourceDirectory("src");
			List<Plugin> plugins = new ArrayList<Plugin>();
			plugins.add(getFragmentRequirementsPlugin(workspaceBundles, targetBundles));
			plugins.add(getTychoSureFirePlugin(attributes, workspaceBundles, targetBundles));
			build.setPlugins(plugins);
			model.setBuild(build);

			ByteArrayOutputStream xmlOutput = new ByteArrayOutputStream();
			MavenXpp3Writer writer = new MavenXpp3Writer();
			writer.write(xmlOutput, model);

			IFile pomFile = file.getProject().getFile("pom.xml");
			if (!pomFile.exists()) {
				pomFile.create(new ByteArrayInputStream(xmlOutput.toByteArray()), true, new NullProgressMonitor());
			} else {
				pomFile.setContents(new ByteArrayInputStream(xmlOutput.toByteArray()), IFile.FORCE,
						new NullProgressMonitor());
			}
			return true;
		} catch (CoreException | IOException | XmlPullParserException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
					"Error writing pom." + getStackTrace(e));
		}
		return false;
	}

	private File lookupParentPom(IFile file) {
		// parent should be project
		IPath fileLocation = file.getRawLocation();
		IPath parentDir = fileLocation.removeLastSegments(2);
		File parentPom = parentDir.append("pom.xml").toFile();
		if (parentPom != null && parentPom.exists()) {
			return parentPom;
		}
		return null;
	}

	private Plugin getTychoSureFirePlugin(Map<String, Object> attributes, LaunchBundles workspaceBundles,
			LaunchBundles targetBundles) {
		Plugin plugin = new Plugin();
		plugin.setGroupId("org.eclipse.tycho");
		plugin.setArtifactId("tycho-surefire-plugin");
		plugin.setVersion("${tycho-version}");
		try {
			TychoSurefireXml xml = TychoSurefireXml.of(attributes, workspaceBundles, targetBundles);
			Xpp3Dom dom = Xpp3DomBuilder.build(xml.getInputStream(), "UTF-8");
			plugin.setConfiguration(dom);
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
					"Error getting Tycho Surefire plugin." + getStackTrace(e));
		}
		return plugin;
	}
	
	private Plugin getFragmentRequirementsPlugin(LaunchBundles workspaceBundles, LaunchBundles targetBundles) {
		Plugin plugin = new Plugin();
		plugin.setGroupId("org.eclipse.tycho");
		plugin.setArtifactId("target-platform-configuration");
		
		workspaceBundles = LaunchBundles.fragments(workspaceBundles);
		targetBundles = LaunchBundles.fragments(targetBundles);
		try {
			FragmentRequirementsXml xml = FragmentRequirementsXml.of(workspaceBundles, targetBundles);
			Xpp3Dom dom = Xpp3DomBuilder.build(xml.getInputStream(), "UTF-8");
			plugin.setConfiguration(dom);
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
					"Error getting Fragment Requirements plugin." + getStackTrace(e));
		}
		return plugin;
	}

	private String getStackTrace(Exception e) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (PrintStream ps = new PrintStream(output, true, "UTF-8")) {
			e.printStackTrace(ps);
		} catch (UnsupportedEncodingException e1) {
			// ignore
		}
		return "\n\n" + new String(output.toByteArray(), StandardCharsets.UTF_8);
	}
}
