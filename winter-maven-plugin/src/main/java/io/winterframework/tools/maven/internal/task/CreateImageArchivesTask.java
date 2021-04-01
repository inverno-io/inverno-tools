/*
 * Copyright 2021 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.winterframework.tools.maven.internal.task;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;

import io.winterframework.tools.maven.internal.ProjectModule;
import io.winterframework.tools.maven.internal.Task;
import io.winterframework.tools.maven.internal.TaskExecutionException;

/**
 * @author jkuhn
 *
 */
public class CreateImageArchivesTask extends Task<Void> {

	private final ArchiverManager archiverManager;
	private final ProjectModule projectModule;
	private final Path imagePath;
	
	private String prefix;
	
	public CreateImageArchivesTask(AbstractMojo mojo, ArchiverManager archiverManager, ProjectModule projectModule, Path imagePath) {
		super(mojo);
		this.archiverManager = archiverManager;
		this.projectModule = projectModule;
		this.imagePath = imagePath;
	}

	@Override
	public Void call() throws TaskExecutionException {
		if(this.verbose) {
			this.getLog().info("[ Creating archives of " + this.imagePath + "... ]");
		}
		try {
			Map<String, Path> result = new HashMap<>();
			for(Entry<String, Path> e : projectModule.getImageArchivesPaths().entrySet()) {
				if(!CreateProjectPackageTask.PACKAGE_TYPES.contains(e.getKey())) {
					if(this.verbose) {
						this.getLog().info(" - " + e.getValue() + "...");
					}
					Archiver archiver = this.getArchiver(e.getKey());
					archiver.addFileSet(DefaultFileSet.fileSet(this.imagePath.toFile()).prefixed(this.prefix).includeExclude( null, null ).includeEmptyDirs(true));
					archiver.setDestFile(e.getValue().toFile());
					
					archiver.createArchive();
					
					result.put(e.getKey(), e.getValue());
				}
			}
		} 
		catch (ArchiverException | IOException e) {
			throw new TaskExecutionException("Error creating archives, activate '-Dwinter.image.verbose=true' to display full log", e);
		}
		return null;
	}
	
	private Archiver getArchiver(String format) throws TaskExecutionException {
		try {
			if("txz".equals(format) || "tgz".equals(format) || "tbz2".equals(format) || format.startsWith("tar")) {
				TarArchiver tarArchiver = (TarArchiver) this.archiverManager.getArchiver( "tar" );
				final int index = format.indexOf('.');
				if (index >= 0) {
					TarArchiver.TarCompressionMethod tarCompressionMethod;
					final String compression = format.substring(index + 1);
					if ("gz".equals(compression)) {
						tarCompressionMethod = TarArchiver.TarCompressionMethod.gzip;
					} 
					else if ("bz2".equals(compression)) {
						tarCompressionMethod = TarArchiver.TarCompressionMethod.bzip2;
					} 
					else if ("xz".equals(compression)) {
						tarCompressionMethod = TarArchiver.TarCompressionMethod.xz;
					} 
					else if ("snappy".equals(compression)) {
						tarCompressionMethod = TarArchiver.TarCompressionMethod.snappy;
					} 
					else {
						throw new TaskExecutionException("Error creating archive: unknown compression format: " + compression);
					}
					tarArchiver.setCompression(tarCompressionMethod);
				} 
				else if ("tgz".equals(format)) {
					tarArchiver.setCompression(TarArchiver.TarCompressionMethod.gzip);
				} 
				else if ("tbz2".equals(format)) {
					tarArchiver.setCompression(TarArchiver.TarCompressionMethod.bzip2);
				} 
				else if ("txz".equals(format)) {
					tarArchiver.setCompression(TarArchiver.TarCompressionMethod.xz);
				}
				return tarArchiver;
			}
			else {
				return this.archiverManager.getArchiver(format);
			}
		} 
		catch (NoSuchArchiverException e) {
			throw new TaskExecutionException("Error creating archive: unsupported format " + format, e);
		}
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
}
