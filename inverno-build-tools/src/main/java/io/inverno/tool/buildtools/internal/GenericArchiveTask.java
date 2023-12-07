/*
 * Copyright 2023 Jeremy KUHN
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
package io.inverno.tool.buildtools.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.inverno.tool.buildtools.ArchiveTask;
import io.inverno.tool.buildtools.Image;
import io.inverno.tool.buildtools.TaskExecutionException;

/**
 * <p>
 * Generic {@link ArchiveTask} implementation.
 * </p>
 * 
 * <p>
 * This implementation relies on <a href="https://commons.apache.org/proper/commons-compress">Apache Commons Compress</a>.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class GenericArchiveTask extends AbstractTask<Set<Image>, ArchiveTask> implements ArchiveTask {

	private static final Logger LOGGER = LogManager.getLogger(GenericArchiveTask.class);
	
	private static final int UNITARY_WEIGHT = 250;
	
	private final ImageType imageType;
	
	private Optional<String> prefix = Optional.empty();
	private Set<String> formats = Set.of();
	
	/**
	 * <p>
	 * Creates a generic archive task.
	 * </p>
	 * 
	 * @param parentTask the parent task
	 * @param imageType  the type of image to archive
	 */
	public GenericArchiveTask(AbstractTask<?, ?> parentTask, ImageType imageType) {
		super(parentTask);
		this.imageType = imageType;
	}
	
	@Override
	protected String getTaskCompletionMessage(BuildProject project) {
		if(!this.formats.isEmpty()) {
			return "Project " + this.imageType.toString().toLowerCase() + " archives created: " + this.formats.stream().collect(Collectors.joining(", "));
		}
		else {
			return "Project " + this.imageType.toString().toLowerCase() + " created";
		}
	}
	
	@Override
	protected int getTaskWeight(BuildProject project) {
		return this.formats.size() * UNITARY_WEIGHT;
	}

	@Override
	public ArchiveTask prefix(String prefix) {
		this.prefix = Optional.ofNullable(prefix);
		return this;
	}

	@Override
	public ArchiveTask formats(Set<String> formats) {
		this.formats = formats != null ? formats : Set.of();
		return this;
	}
	
	@Override
	protected Set<Image> doExecute(BuildProject project, ProgressBar.Step step) throws TaskExecutionException {
		if(step != null) {
			step.setDescription("Creating archives...");
		}
		
		if(this.formats.isEmpty()) {
			return Set.of();
		}
		
		Path imagePath = project.getImagePath(this.imageType);
		Map<String, Path> imageArchivesPaths = project.getImageArchivesPaths(this.imageType, this.formats);
		if(project.isMarked() || project.getDependencies().stream().anyMatch(dependency -> dependency.isMarked()) || 
				imageArchivesPaths.entrySet().stream().anyMatch(e -> !Files.exists(e.getValue()))) {
			LOGGER.info("[ Creating archives of {}... ]", imagePath);
			
			List<SingleArchivingTask> archivingTasks = imageArchivesPaths.entrySet().stream().map(e -> new SingleArchivingTask(step, e.getKey(), e.getValue())).collect(Collectors.toList());
			
			for(SingleArchivingTask task : archivingTasks) {
				LOGGER.info(" - {}...", task.getArchivePath());
				
				try(ArchiveOutputStream archiveOutput = this.createArchiveOutputStream(task.getFormat(), task.getArchivePath());Stream<Path> walk = Files.walk(imagePath);) {
					for(Iterator<Path> imageSourcePathIterator = walk.iterator();imageSourcePathIterator.hasNext();) {
						Path imageSourcePath = imageSourcePathIterator.next();
						Path imageTargetPath = imagePath.relativize(imageSourcePath);
						imageTargetPath = Path.of(this.prefix.orElse(project.getFinalName())).resolve(imageTargetPath);
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
					throw new TaskExecutionException("Error creating " + task.getFormat() + " archive", e);
				}
			}
		}
		else {
			LOGGER.info("[ Archives are up to date ]");
		}
		
		return imageArchivesPaths.entrySet().stream()
			.map(e -> new GenericImage(this.imageType, e.getKey(), e.getValue()))
			.collect(Collectors.toSet());
	}
	
	/**
	 * <p>
	 * Creates an archive entry.
	 * </p>
	 * 
	 * @param archiveOutput the archive output stream
	 * @param sourcePath    the path to the source file to add to the archive
	 * @param targetPath    the path in the archive where to add the source file
	 * 
	 * @return an archive entry
	 * 
	 * @throws IOException if there was an error creating the entry
	 */
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
	
	/**
	 * <p>
	 * Creates an archive output stream.
	 * </p>
	 * 
	 * @param format           the format of the archive
	 * @param imageArchivePath the path where to create the archive
	 * 
	 * @return an archive output stream
	 * 
	 * @throws ArchiveException    if there was an error creating the archive output stream
	 * @throws CompressorException if there was an error obtaining the compressor
	 * @throws IOException         if there was an error creating the archive output stream
	 */
	private ArchiveOutputStream createArchiveOutputStream(String format, Path imageArchivePath) throws ArchiveException, CompressorException, IOException {
		OutputStream imageArchiveOutput = Files.newOutputStream(imageArchivePath, StandardOpenOption.CREATE_NEW);
		
		String normalizedFormat;
		if("txz".equals(format) || "tgz".equals(format) || "tbz2".equals(format)) {
			normalizedFormat = "tar." + format.substring(1);
		}
		else {
			normalizedFormat = format;
		}
		
		if(normalizedFormat.startsWith("tar")) {
			String[] formatSplit = normalizedFormat.split("\\.");
			switch(formatSplit.length) {
				case 2: {
					String compressorName = formatSplit[1];
					CompressorStreamFactory csf = new CompressorStreamFactory();
					if(compressorName.equals("bz2")) {
						// strange gzip is 'gz' but bzip2 is 'bzip2'...
						compressorName = CompressorStreamFactory.BZIP2;
					}
					if(!csf.getOutputStreamCompressorNames().contains(compressorName)) {
						throw new TaskExecutionException("Unsupported compression format: " + compressorName);
					}
					return new TarArchiveOutputStream(imageArchiveOutput);
				}
				case 1: {
					return new TarArchiveOutputStream(imageArchiveOutput);
				}
				default: throw new TaskExecutionException("Invalid archive format: " + format);
			}
		}
		else {
			ArchiveStreamFactory asf = new ArchiveStreamFactory();
			if(!asf.getOutputStreamArchiveNames().contains(format)) {
				throw new TaskExecutionException("Unsupported archive format: " + format);
			}
			return asf.createArchiveOutputStream(format, imageArchiveOutput);
		}
	}
	
	/**
	 * <p>
	 * Holds single archiving task data.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	private static class SingleArchivingTask {
		
		private final String format;
		
		private final Path archivePath;
		
		private final Optional<ProgressBar.Step> step;
		
		/**
		 * <p>
		 * Creates a single archiving task.
		 * </p>
		 * 
		 * @param parentStep  the parent step.
		 * @param format      the format of the archive to build
		 * @param archivePath the path where to build the archive
		 */
		public SingleArchivingTask(ProgressBar.Step parentStep, String format, Path archivePath) {
			this.format = format;
			this.archivePath = archivePath;
			
			this.step = Optional.ofNullable(parentStep).map(s -> s.addStep(1, "Creating archive " + archivePath.getFileName() + "..."));
		}
		
		/**
		 * <p>
		 * Returns the format of the archive.
		 * </p>
		 * 
		 * @return the archive format
		 */
		public String getFormat() {
			return format;
		}
		
		/**
		 * <p>
		 * Returns the path to the archive.
		 * </p>
		 * 
		 * @return the archive path
		 */
		public Path getArchivePath() {
			return archivePath;
		}
		
		/**
		 * <p>
		 * Indicates when the task has completed and terminates the corresponding step.
		 * </p>
		 */
		public void done() {
			this.step.ifPresent(ProgressBar.Step::done);
		}
	}
}
