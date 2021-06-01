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
package io.inverno.tool.maven.internal.task;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.dump.DumpArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.StringUtils;

import io.inverno.tool.maven.internal.ProgressBar;
import io.inverno.tool.maven.internal.ProjectModule;
import io.inverno.tool.maven.internal.Task;
import io.inverno.tool.maven.internal.TaskExecutionException;
import io.inverno.tool.maven.internal.ProgressBar.Step;

/**
 * <p>
 * Creates the project module image archives.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class CreateImageArchivesTask extends Task<Void> {

	private final ProjectModule projectModule;
	private final Path imagePath;
	
	private String prefix;
	
	private Set<String> includeFormats;
	private Set<String> excludeFormats;
	
	public CreateImageArchivesTask(AbstractMojo mojo, ProjectModule projectModule, Path imagePath) {
		super(mojo);
		this.projectModule = projectModule;
		this.imagePath = imagePath;
	}
	
	@Override
	public void setStep(Step step) {
		if(step != null) {
			step.setDescription("Creating image archives..");
		}
		super.setStep(step);
	}

	@Override
	protected Void execute() throws TaskExecutionException {
		if(this.projectModule.isMarked() || 
			this.projectModule.getModuleDependencies().stream().anyMatch(dependency -> dependency.isMarked()) || 
			this.projectModule.getImageArchivesPaths().entrySet().stream().filter(e -> this.isFormatIncluded(e.getKey())).anyMatch(e -> !Files.exists(e.getValue()))) {
			if(this.verbose) {
				this.getLog().info("[ Creating image archives of " + this.imagePath + "... ]");
			}
			
			List<Entry<String, Path>> filteredImageArchivesPaths = this.projectModule.getImageArchivesPaths().entrySet().stream().filter(e -> this.isFormatIncluded(e.getKey())).collect(Collectors.toList());
			List<ArchivingTask> archivingTasks = filteredImageArchivesPaths.stream().map(e -> new ArchivingTask(e.getKey(), e.getValue(), (float)1/filteredImageArchivesPaths.size())).collect(Collectors.toList());
			
			for(ArchivingTask task : archivingTasks) {
				if(this.verbose) {
					this.getLog().info(" - " + task.getArchivePath() + "...");
				}
				
				try(ArchiveOutputStream archiveOutput = this.createArchiveOutputStream(task.getFormat(), task.getArchivePath());Stream<Path> walk = Files.walk(this.imagePath);) {
					for(Iterator<Path> imageSourcePathIterator = walk.iterator();imageSourcePathIterator.hasNext();) {
						Path imageSourcePath = imageSourcePathIterator.next();
						Path imageTargetPath = this.imagePath.relativize(imageSourcePath);
						if(StringUtils.isNotEmpty(this.prefix)) {
							imageTargetPath = Paths.get(this.prefix).resolve(imageTargetPath);
						}
						ArchiveEntry imageArchiveEntry = this.createArchiveEntry(archiveOutput, imageSourcePath, imageTargetPath);
						archiveOutput.putArchiveEntry(imageArchiveEntry);
						if(Files.isRegularFile(imageSourcePath) && !Files.isSymbolicLink(imageTargetPath)) {
							Files.copy(imageSourcePath, archiveOutput);
						}
						archiveOutput.closeArchiveEntry();
					}
					task.done();
				} 
				catch (IOException | ArchiveException | CompressorException e) {
					throw new TaskExecutionException("Error creating " + task.getFormat() + " archive, activate '-Dinverno.verbose=true' to display full log", e);
				}
			}
		}
		else {
			if(this.verbose) {
				this.getLog().info("[ Image archives are up to date ]");
			}
		}
		
		return null;
	}
	
	private boolean isFormatIncluded(String format) {
		return (this.includeFormats == null || this.includeFormats.isEmpty() || this.includeFormats.contains(format)) 
			&& !(this.excludeFormats != null && !this.excludeFormats.isEmpty() && this.excludeFormats.contains(format));
	}

	private ArchiveEntry createArchiveEntry(ArchiveOutputStream archiveOutput, Path sourcePath, Path targetPath) throws IOException {
		ArchiveEntry entry = archiveOutput.createArchiveEntry(sourcePath.toFile(), targetPath.toString());
		
		if(sourcePath.getFileSystem().supportedFileAttributeViews().contains("unix")) {
			Consumer<Integer> modeConfigurer = null;
			
			if(entry instanceof CpioArchiveEntry) {
				modeConfigurer = ((CpioArchiveEntry)entry)::setMode;
			}
			else if(entry instanceof DumpArchiveEntry) {
				modeConfigurer = ((DumpArchiveEntry)entry)::setMode;
			}
			else if(entry instanceof TarArchiveEntry) {
				modeConfigurer = ((TarArchiveEntry)entry)::setMode;
			}
			else if(entry instanceof ZipArchiveEntry) {
				modeConfigurer = ((ZipArchiveEntry)entry)::setUnixMode;
			}
			
			if(modeConfigurer != null) {
				Integer mode = (Integer) Files.getAttribute(sourcePath, "unix:mode", LinkOption.NOFOLLOW_LINKS) & 0xfff;
				modeConfigurer.accept(mode);
			}
		}
		
		return entry;
	}
	
	private ArchiveOutputStream createArchiveOutputStream(String format, Path imageArchivePath) throws ArchiveException, CompressorException, IOException {
		OutputStream imageArchiveOutput = Files.newOutputStream(imageArchivePath, StandardOpenOption.CREATE_NEW);
		if("txz".equals(format) || "tgz".equals(format) || "tbz2".equals(format) || format.startsWith("tar")) {
			final int index = format.indexOf('.');
			if (index >= 0 || !"tar".equals(format)) {
				String compressorName = format.substring(index + 1);
				CompressorStreamFactory csf = new CompressorStreamFactory();
				if(compressorName.equals("bz2")) {
					// strange gzip is 'gz' but bzip2 is 'bzip2'...
					compressorName = CompressorStreamFactory.BZIP2;
				}
				return new TarArchiveOutputStream(csf.createCompressorOutputStream(compressorName, imageArchiveOutput));
			} 
			else {
				return new TarArchiveOutputStream(imageArchiveOutput);
			} 
		}
		else {
			ArchiveStreamFactory asf = new ArchiveStreamFactory();
			return asf.createArchiveOutputStream(format, imageArchiveOutput);
		}
	}
	
	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public Set<String> getIncludeFormats() {
		return includeFormats;
	}

	public void setIncludeFormats(Set<String> includeFormats) {
		this.includeFormats = includeFormats;
	}

	public Set<String> getExcludeFormats() {
		return excludeFormats;
	}

	public void setExcludeFormats(Set<String> excludeFormats) {
		this.excludeFormats = excludeFormats;
	}
	
	private class ArchivingTask {
		
		private String format;
		
		private Path archivePath;
		
		private Optional<ProgressBar.Step> step;
		
		public ArchivingTask(String format, Path archivePath, float stepWeight) {
			this.format = format;
			this.archivePath = archivePath;
			
			this.step = CreateImageArchivesTask.this.getStep().map(step -> step.addStep(stepWeight, "Creating archive " + archivePath.getFileName() + "..."));
		}
		
		public String getFormat() {
			return format;
		}
		
		public Path getArchivePath() {
			return archivePath;
		}
		
		public void done() {
			this.step.ifPresent(ProgressBar.Step::done);
		}
	}
}
