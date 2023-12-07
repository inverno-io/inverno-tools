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

import io.inverno.tool.buildtools.Image;
import java.nio.file.Path;
import java.util.Optional;

/**
 * <p>
 * Generic {@link Image} implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class GenericImage implements Image {

	private final String classifier;
	private final Optional<String> format;
	private final Optional<Path> path;

	/**
	 * <p>
	 * Creates a generic image.
	 * </p>
	 * 
	 * @param imageType the image type
	 * @param format    the archive format or null
	 * @param path      the path to the image or null
	 */
	public GenericImage(ImageType imageType, String format, Path path) {
		this.classifier = imageType.getNativeClassifier();
		this.format = Optional.ofNullable(format);
		this.path = Optional.ofNullable(path);
	}
	
	@Override
	public String getClassifier() {
		return this.classifier;
	}

	@Override
	public Optional<String> getFormat() {
		return this.format;
	}

	@Override
	public Optional<Path> getPath() {
		return this.path;
	}
}
