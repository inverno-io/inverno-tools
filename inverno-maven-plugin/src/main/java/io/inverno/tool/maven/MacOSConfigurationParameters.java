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
package io.inverno.tool.maven;

import io.inverno.tool.buildtools.PackageApplicationTask;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>
 * Parameters for the creation a MacOS application package.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class MacOSConfigurationParameters implements PackageApplicationTask.MacOSConfiguration {
	
	/**
	 * An identifier that uniquely identifies the application for macOSX. Defaults to the main class name. May only use alphanumeric (A-Z,a-z,0-9), hyphen (-), and period (.) characters.
	 */
	@Parameter(property = "inverno.app.macos.packageIdentifier", required = false)
	private String packageIdentifier;

	/**
	 * Name of the application as it appears in the Menu Bar. This can be different from the application name. This name must be less than 16 characters long and be suitable for displaying in the menu
	 * bar and the application Info window. Defaults to the application name.
	 */
	@Parameter(property = "inverno.app.macos.packageName", required = false)
	private String packageName;

	/**
	 * When signing the application package, this value is prefixed to all components that need to be signed that don't have an existing package identifier.
	 */
	@Parameter(property = "inverno.app.macos.packageSigningPrefix", required = false)
	private String packageSigningPrefix;

	/**
	 * Requests that the bundle be signed.
	 */
	@Parameter(property = "inverno.app.macos.sign", required = false)
	private boolean sign;

	/**
	 * Name of the keychain to search for the signing identity. If not specified, the standard keychains are used.
	 */
	@Parameter(property = "inverno.app.macos.signingKeychain", required = false)
	private String signingKeychain;

	/**
	 * Team name portion in Apple signing identities' names.
	 * For example "Developer ID Application: <team name>"
	 */
	@Parameter(property = "inverno.app.macos.signingKeychain", required = false)
	private String signingKeyUserName;

	/**
	 * <p>
	 * Sets the identifier that uniquely identifies the application for macOS.
	 * </p>
	 * 
	 * @param packageIdentifier the package identifier
	 */
	public void setPackageIdentifier(String packageIdentifier) {
		this.packageIdentifier = packageIdentifier;
	}

	@Override
	public Optional<String> getPackageIdentifier() {
		return Optional.ofNullable(this.packageIdentifier).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the name of the application as it appears in the Menu Bar.
	 * </p>
	 * 
	 * @param packageName the package name
	 */
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	@Override
	public Optional<String> getPackageName() {
		return Optional.ofNullable(this.packageName).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the value prefixed to all components that need to be signed that don't have an existing package identifier when signing the application package.
	 * </p>
	 * 
	 * @param packageSigningPrefix the package signing prefix
	 */
	public void setPackageSigningPrefix(String packageSigningPrefix) {
		this.packageSigningPrefix = packageSigningPrefix;
	}

	@Override
	public Optional<String> getPackageSigningPrefix() {
		return Optional.ofNullable(this.packageSigningPrefix).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets whether the package should be signed.
	 * </p>
	 * 
	 * @param sign true to sign the image, false otherwise
	 */
	public void setSign(boolean sign) {
		this.sign = sign;
	}

	@Override
	public boolean isSign() {
		return this.sign;
	}

	/**
	 * <p>
	 * Sets the name of the keychain to search for the signing identity.
	 * </p>
	 * 
	 * @param signingKeychain the signing keychain
	 */
	public void setSigningKeychain(String signingKeychain) {
		this.signingKeychain = signingKeychain;
	}

	@Override
	public Optional<String> getSigningKeychain() {
		return Optional.ofNullable(this.signingKeychain).filter(StringUtils::isNotEmpty);
	}

	/**
	 * <p>
	 * Sets the team or username portion in Apple signing identities.
	 * </p>
	 * 
	 * @param signingKeyUserName the signing key username
	 */
	public void setSigningKeyUserName(String signingKeyUserName) {
		this.signingKeyUserName = signingKeyUserName;
	}

	@Override
	public Optional<String> getSigningKeyUserName() {
		return Optional.ofNullable(this.signingKeyUserName).filter(StringUtils::isNotEmpty);
	}
}
