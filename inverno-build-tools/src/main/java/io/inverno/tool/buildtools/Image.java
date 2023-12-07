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

import java.nio.file.Path;
import java.util.Optional;

/**
 * <p>
 * Represents a native project image.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public interface Image {

	/**
	 * <p>
	 * Returns the native image classifier (e.g. {@code application_linux_amd64}).
	 * </p>
	 * 
	 * @return the image classifier
	 */
	String getClassifier();
	
	/**
	 * <p>
	 * Returns the image archive format (e.g. {@code zip})
	 * </p>
	 * 
	 * @return an optional returning the archive format or an empty optional when the image has not been generated as an archive
	 */
	Optional<String> getFormat();
	
	/**
	 * <p>
	 * Returns the path to the image.
	 * </p>
	 * 
	 * @return an optional returning the path to the image or an empty optional when the image has not been generated to the file system
	 */
	Optional<Path> getPath();
}
