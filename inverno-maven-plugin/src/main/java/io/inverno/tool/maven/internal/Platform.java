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
package io.inverno.tool.maven.internal;

/**
 * <p>
 * Represents a system platform.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public enum Platform {

	/**
	 * Windows platform. 
	 */
	WINDOWS("windows"),
	/**
	 * Linux platform.
	 */
	LINUX("linux"),
	/**
	 * MacOS platform.
	 */
	MACOS("macos"),
	/**
	 * Unknown platform.
	 */
	UNKNOWN("unkown");
	
	private static final Platform SYSTEM_PLATFORM;
	
	static {
		String osName = System.getProperty("os.name").toLowerCase();
		if(osName.indexOf("win") >= 0) {
			SYSTEM_PLATFORM = Platform.WINDOWS;
		}
		else if(osName.indexOf("mac") >= 0) {
			SYSTEM_PLATFORM = Platform.MACOS;
		}
		else if(osName.indexOf("linux") >= 0) {
			SYSTEM_PLATFORM = Platform.LINUX;
		}
		else {
			SYSTEM_PLATFORM = Platform.UNKNOWN;
		}
	}
	
	private String os;
	
	private String arch;
	
	private Platform(String os) {
		this.os = os;
		this.arch = System.getProperty("os.arch");
	}
	
	/**
	 * <p>
	 * Returns the operating system.
	 * </p>
	 * 
	 * @return the operating system
	 */
	public String getOs() {
		return os;
	}
	
	/**
	 * <p>
	 * Returns the system architecture.
	 * </p>
	 * 
	 * @return the system architecture
	 */
	public String getArch() {
		return arch;
	}
	
	/**
	 * <p>
	 * Returns the system platform.
	 * </p>
	 * 
	 * @return the system platform
	 */
	public static Platform getSystemPlatform() {
		return SYSTEM_PLATFORM;
	}
	
	@Override
	public String toString() {
		return this.os + "_" + this.arch;
	}
}
