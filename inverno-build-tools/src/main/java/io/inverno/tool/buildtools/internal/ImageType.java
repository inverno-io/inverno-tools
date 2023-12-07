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

/**
 * <p>
 * Represents the type of an image to build.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public enum ImageType {
   /**
	* Indicates a runtime classifier. 
	*/
   RUNTIME("runtime"),
   /**
	* Indicates an application classifier. 
	*/
   APPLICATION("application"),
   /**
	* Indicates a container classifier. 
	*/
   CONTAINER("container");

   private final String nativeClassifier;

   /**
	* <p>
	* Creates an image type.
	* </p>
	* 
	* @param classifier the image qualifier
	*/
   private ImageType(String classifier) {
	   this.nativeClassifier = classifier + "_" + Platform.getSystemPlatform();
   }

   /**
	* <p>
	* Returns the classifier of the image type including OS and architecture.
	* </p>
	* 
	* @return The image type
	*/
   public String getNativeClassifier() {
	   return this.nativeClassifier;
   }

   /**
	* <p>
	* Returns the operating system.
	* </p>
	* 
	* @return the operating system
	*/
   public String getOs() {
	   return Platform.getSystemPlatform().getOs();
   }

   /**
	* <p>
	* Returns the system architecture.
	* </p>
	* 
	* @return the system architecture
	*/
   public String getArch() {
	   return Platform.getSystemPlatform().getArch();
   }
}
