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
package io.inverno.tool.buildtools;

import java.util.Set;

/**
 * <p>
 * A task for archiving project delivrables.
 * </p>
 * 
 * <p>
 * The archive tasks is used to package project runtime images or project application images in various archive formats.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 * @see BuildRuntimeTask
 * @see PackageApplicationTask
 */
public interface ArchiveTask extends Task<Set<Image>, ArchiveTask> {

	/**
	 * {@code .zip} archive format.
	 */
	String FORMAT_ZIP = "zip";
	/**
	 * {@code .tar.gz} archive format.
	 */
	String FORMAT_TAR_GZ = "tar.gz";
	/**
	 * {@code .tar.bz2} archive format.
	 */
	String FORMAT_TAR_BZ2 = "tar.bz2";
	
	/**
	 * <p>
	 * Specifies the root path (e.g. {@code path/to/image}) within the archive where to put the project image.
	 * </p>
	 * 
	 * <p>
	 * Defaults to {@link Project#getFinalName() }.
	 * </p>
	 * 
	 * @param prefix a path
	 * 
	 * @return the task
	 */
	ArchiveTask prefix(String prefix);
	
	/**
	 * <p>
	 * Specifies the formats of the archives to generate.
	 * </p>
	 * 
	 * @param formats a list of archive formats
	 * 
	 * @return the task
	 */
	ArchiveTask formats(Set<String> formats);
}
