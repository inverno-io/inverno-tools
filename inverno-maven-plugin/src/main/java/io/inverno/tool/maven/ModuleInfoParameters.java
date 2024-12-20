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

import io.inverno.tool.buildtools.ModuleInfo;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>
 * Parameters for module descriptor overrides.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class ModuleInfoParameters extends ModuleInfo {

	/**
	 * <p>
	 * Sets the module name.
	 * </p>
	 * 
	 * @param name the module name
	 */
	@Parameter(required = true)
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * <p>
	 * Sets whether the module should be declared as opened.
	 * </p>
	 * 
	 * @param open true to open the module, false otherwise
	 */
	@Parameter(required = false)
	@Override
	public void setOpen(boolean open) {
		this.open = open;
	}

	/**
	 * <p>
	 * Sets the module's requires directives.
	 * </p>
	 * 
	 * @param requires the requires directives
	 */
	@Parameter(required = false)
	public void setRequires(List<ModuleInfoParameters.RequiresDirective> requires) {
		this.requires = new ArrayList<>(requires);
	}

	/**
	 * <p>
	 * Sets the module's exports directives.
	 * </p>
	 * 
	 * @param exports the exports directives
	 */
	@Parameter(required = false)
	public void setExports(List<ModuleInfoParameters.ExportsDirective> exports) {
		this.exports = new ArrayList<>(exports);
	}

	/**
	 * <p>
	 * Sets the module's opens directives.
	 * </p>
	 * 
	 * @param opens the opens directives
	 */
	@Parameter(required = false)
	public void setOpens(List<ModuleInfoParameters.OpensDirective> opens) {
		this.opens = new ArrayList<>(opens);
	}

	/**
	 * <p>
	 * Sets the module's uses directives.
	 * </p>
	 * 
	 * @param uses the uses directives
	 */
	@Parameter(required = false)
	public void setUses(List<ModuleInfoParameters.UsesDirective> uses) {
		this.uses = new ArrayList<>(uses);
	}

	/**
	 * <p>
	 * Sets the module's provides directives.
	 * </p>
	 * 
	 * @param provides the provides directives
	 */
	@Parameter(required = false)
	public void setProvides(List<ModuleInfoParameters.ProvidesDirective> provides) {
		this.provides = new ArrayList<>(provides);
	}

	/**
	 * <p>
	 * Requires module directive parameters.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class RequiresDirective extends ModuleInfo.RequiresDirective {

		/**
		 * <p>
		 * Sets the name of the required module.
		 * </p>
		 * 
		 * @param moduleName the module name
		 */
		@Parameter(required = true)
		public void setModule(String moduleName) {
			this.moduleName = moduleName;
		}

		/**
		 * <p>
		 * Sets whether this is a static dependency.
		 * </p>
		 * 
		 * @param isStatic true to set the dependency static, false otherwise
		 */
		@Parameter(name="static", required = false)
		public void setStatic(boolean isStatic) {
			this.isStatic = isStatic;
		}

		/**
		 * <p>
		 * Sets whether this is a transitive dependency.
		 * </p>
		 * 
		 * @param isTransitive true to set the dependency transitive, false otherwise
		 */
		@Parameter(name="transitive", required = false)
		public void setTransitive(boolean isTransitive) {
			this.isTransitive = isTransitive;
		}

		/**
		 * <p>
		 * Removes the directive if it exists.
		 * </p>
		 *
		 * @param remove {@code true} to remove the directive, {@code false} otherwise
		 */
		@Parameter(required = false, defaultValue = "false")
		public void setRemove(boolean remove) {
			this.remove = remove;
		}
	}

	/**
	 * <p>
	 * Exports module directive parameters.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class ExportsDirective extends ModuleInfo.ExportsDirective {

		/**
		 * <p>
		 * Sets the name of the exported package.
		 * </p>
		 * 
		 * @param packageName the package name
		 */
		@Parameter(required = true)
		public void setPackage(String packageName) {
			this.packageName = packageName;
		}

		/**
		 * <p>
		 * Sets The names of the modules to which the package is exported.
		 * </p>
		 * 
		 * @param to a list of package names
		 */
		@Parameter(required = false)
		public void setTo(List<String> to) {
			this.to = to;
		}

		/**
		 * <p>
		 * Removes the directive if it exists.
		 * </p>
		 *
		 * @param remove {@code true} to remove the directive, {@code false} otherwise
		 */
		@Parameter(required = false, defaultValue = "false")
		public void setRemove(boolean remove) {
			this.remove = remove;
		}
	}

	/**
	 * <p>
	 * Opens module directive parameters.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class OpensDirective extends ModuleInfo.OpensDirective {

		/**
		 * <p>
		 * Sets the name of the opened package.
		 * </p>
		 * 
		 * @param packageName the package name
		 */
		@Parameter(required = true)
		public void setPackage(String packageName) {
			this.packageName = packageName;
		}

		/**
		 * <p>
		 * Sets the names of the modules to which the package is opened.
		 * </p>
		 * 
		 * @param to a list of package names
		 */
		@Parameter(required = false)
		public void setTo(List<String> to) {
			this.to = to;
		}

		/**
		 * <p>
		 * Removes the directive if it exists.
		 * </p>
		 *
		 * @param remove {@code true} to remove the directive, {@code false} otherwise
		 */
		@Parameter(required = false, defaultValue = "false")
		public void setRemove(boolean remove) {
			this.remove = remove;
		}
	}

	/**
	 * <p>
	 * Uses module directive parameters.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class UsesDirective extends ModuleInfo.UsesDirective {

		/**
		 * <p>
		 * Sets the type used in the module.
		 * </p>
		 * 
		 * @param typeName the type name
		 */
		@Parameter(required = true)
		public void setType(String typeName) {
			this.typeName = typeName;
		}

		/**
		 * <p>
		 * Removes the directive if it exists.
		 * </p>
		 *
		 * @param remove {@code true} to remove the directive, {@code false} otherwise
		 */
		@Parameter(required = false, defaultValue = "false")
		public void setRemove(boolean remove) {
			this.remove = remove;
		}
	}

	/**
	 * <p>
	 * Provides module directive parameters.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class ProvidesDirective extends ModuleInfo.ProvidesDirective {

		/**
		 * <p>
		 * Sets the type of the service provided by the module.
		 * </p>
		 * 
		 * @param typeName the type name
		 */
		@Parameter(required = true)
		public void setType(String typeName) {
			this.typeName = typeName;
		}

		/**
		 * <p>
		 * Sets the types in the module providing the service.
		 * </p>
		 * 
		 * @param with a list of types
		 */
		@Parameter(name="with", required = true)
		public void setWith(List<String> with) {
			this.with = with;
		}

		/**
		 * <p>
		 * Removes the directive if it exists.
		 * </p>
		 *
		 * @param remove {@code true} to remove the directive, {@code false} otherwise
		 */
		@Parameter(required = false, defaultValue = "false")
		public void setRemove(boolean remove) {
			this.remove = remove;
		}
	}
}
