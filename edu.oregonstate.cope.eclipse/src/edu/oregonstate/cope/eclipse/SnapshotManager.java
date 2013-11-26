package edu.oregonstate.cope.eclipse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileExportOperation;

public class SnapshotManager {

	private String knownProjectsFileName = "known-projects";
	private List<String> knownProjects;
	private String parentDirectory;

	protected SnapshotManager(String parentDirectory) {
		this.parentDirectory = parentDirectory;
		File knownProjectsFile = new File(parentDirectory, knownProjectsFileName);
		try {
			knownProjectsFile.createNewFile();
			knownProjects = Files.readAllLines(knownProjectsFile.toPath(), Charset.defaultCharset());
		} catch (IOException e) {
		}
	}

	public boolean isProjectKnown(String name) {
		return knownProjects.contains(name);
	}
	
	public boolean isProjectKnown(IProject project) {
		return isProjectKnown(project.getName());
	}

	protected void knowProject(String string) {
		knownProjects.add(string);
		try {
			Files.write(Paths.get(parentDirectory, knownProjectsFileName), (string + "\n").getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
		}
	}
	
	private void knowProject(IProject project) {
		knowProject(project.getName());
	}

	@SuppressWarnings("restriction")
	public String takeSnapshot(IProject project) {
		if (!isProjectKnown(project))
			knowProject(project);
		String zipFile = parentDirectory + "/" + project.getName() + "-" + System.currentTimeMillis() + ".zip";
		ArchiveFileExportOperation archiveFileExportOperation = new ArchiveFileExportOperation(project, zipFile);
		archiveFileExportOperation.setUseCompression(true);
		archiveFileExportOperation.setUseTarFormat(false);
		archiveFileExportOperation.setCreateLeadupStructure(true);
		try {
			archiveFileExportOperation.run(new NullProgressMonitor());
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		if (JavaProject.hasJavaNature(project)) {
			IJavaProject javaProject = JavaCore.create(project);
			List<String> nonWorkspaceLibraries = getNonWorkspaceLibraries(javaProject);
			addLibsToZipFile(nonWorkspaceLibraries, zipFile);
			COPEPlugin.getDefault().getClientRecorder().recordSnapshot(zipFile);
			snapshotRequiredProjects(javaProject);
		}
		return zipFile;
	}

	private void snapshotRequiredProjects(IJavaProject javaProject) {
		try {
			String[] requiredProjectNames = javaProject.getRequiredProjectNames();
			for (String requiredProjectName : requiredProjectNames) {
				takeSnapshot(requiredProjectName);
			}
		} catch (JavaModelException e) {
		}
	}

	private void takeSnapshot(String requiredProjectName) {
		IProject requiredProject = ResourcesPlugin.getWorkspace().getRoot().getProject(requiredProjectName);
		takeSnapshot(requiredProject);
	}

	public List<String> getNonWorkspaceLibraries(IJavaProject project) {
		IClasspathEntry[] resolvedClasspath = null;
		try {
			resolvedClasspath = project.getRawClasspath();
		} catch (JavaModelException e) {
			return new ArrayList<String>();
		}
		List<String> pathsOfLibraries = new ArrayList<String>();
		for (IClasspathEntry iClasspathEntry : resolvedClasspath) {
			if (iClasspathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				pathsOfLibraries.add(iClasspathEntry.getPath().toPortableString());
			}
		}
		return pathsOfLibraries;
	}
	
	public void addLibsToZipFile(List<String> pathOfLibraries, String zipFilePath) {
		try {
			String libFolder = "libs/";
			ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFilePath+"-libs", true));
			copyExistingEntries(zipFilePath, zipOutputStream);
			for (String library : pathOfLibraries) {
				ZipEntry libraryZipEntry = new ZipEntry(libFolder + Paths.get(library).getFileName());
				zipOutputStream.putNextEntry(libraryZipEntry);
				byte[] libraryContents = Files.readAllBytes(Paths.get(library));
				zipOutputStream.write(libraryContents);
			}
			zipOutputStream.close();
			new File(zipFilePath).delete();
			new File(zipFilePath+"-libs").renameTo(new File(zipFilePath));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
	}

	private void copyExistingEntries(String zipFilePath, ZipOutputStream zipOutputStream) {
		try {
			ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath));
			while(zipInputStream.available() == 1) {
				ZipEntry entry = zipInputStream.getNextEntry();
				if (entry == null)
					continue;
				zipOutputStream.putNextEntry(new ZipEntry(entry.getName()));
				long entrySize = entry.getSize();
				if (entrySize < 0)
					continue;
				byte[] contents = new byte[(int) entrySize];
				int count = 0;
				do {
					count = zipInputStream.read(contents, count, (int) entrySize);
				} while (count < entrySize);
				zipOutputStream.write(contents);
			}
			zipInputStream.close();
		} catch (IOException e) {
		}
	}
}
