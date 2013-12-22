package edu.oregonstate.cope.eclipse.installer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

abstract class InstallerOperation {

	protected Path workspaceDirectory;
	protected Path permanentDirectory;

	public InstallerOperation(Path workspaceDirectory, Path permanentDirectory) {
		this.permanentDirectory = permanentDirectory;
		this.workspaceDirectory = workspaceDirectory;
	}

	public void perform(String fileName) throws IOException {
		File workspaceFile = workspaceDirectory.resolve(fileName).toFile();
		File permanentFile = permanentDirectory.resolve(fileName).toFile();

		if (filesExist(workspaceFile) && filesExist(permanentFile)) {
			// System.out.println(this.getClass() + " both files exist");
			doBothFilesExists();
		}

		else if (!filesExist(workspaceFile) && filesExist(permanentFile)) {
			// System.out.println(this.getClass() + " only permanent");
			doOnlyPermanentFileExists(workspaceFile, permanentFile);
		}

		else if (filesExist(workspaceFile) && !filesExist(permanentFile)) {
			// System.out.println(this.getClass() + " only workspace");
			doOnlyWorkspaceFileExists(workspaceFile, permanentFile);
		}

		else if (!filesExist(workspaceFile) && !filesExist(permanentFile)) {
			// System.out.println(this.getClass() + " neither files exist");
			doNoFileExists(workspaceFile, permanentFile);
		}
	}

	private boolean filesExist(File workspaceFile) {
		return workspaceFile.exists();
	}

	protected void doOnlyWorkspaceFileExists(File workspaceFile, File permanentFile) throws IOException {
		Files.copy(workspaceFile.toPath(), permanentFile.toPath());
	}

	protected void doOnlyPermanentFileExists(File workspaceFile, File permanentFile) throws IOException {
		Files.copy(permanentFile.toPath(), workspaceFile.toPath());
	}

	protected void doBothFilesExists() {
	}

	protected abstract void doNoFileExists(File workspaceFile, File permanentFile) throws IOException;

	protected void writeContentsToFile(Path filePath, String fileContents) throws IOException {
		Files.write(filePath, fileContents.getBytes(), StandardOpenOption.CREATE);
	}
}