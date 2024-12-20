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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Describes a module-info descriptor as specified in the Java language specification.
 * </p>
 * 
 * <p>
 * Multiple {@code ModuleInfo} instances can be specified on the {@link ModularizeDependenciesTask} to override or extend {@code module-info.java} descriptors generated when modularizing project's 
 * dependencies.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 */
public class ModuleInfo {
	
	/**
	 * The module name
	 */
	protected String name;
	
	/**
	 * Flag indicating whether the module should be declared as opened.
	 */
	protected boolean open;
	
	/**
	 * Module's import declarations.
	 */
	protected List<ModuleInfo.ImportDeclaration> imports;
	
	/**
	 * Module's requires directives.
	 */
	protected List<ModuleInfo.RequiresDirective> requires;
	
	/**
	 * Module's exports directives.
	 */
	protected List<ModuleInfo.ExportsDirective> exports;
	
	/**
	 * Module's opens directives.
	 */
	protected List<ModuleInfo.OpensDirective> opens;
	
	/**
	 * Module's uses directives.
	 */
	protected List<ModuleInfo.UsesDirective> uses;
	
	/**
	 * Module's provides directives.
	 */
	protected List<ModuleInfo.ProvidesDirective> provides;
	
	/**
	 * <p>
	 * Creates a blank module info.
	 * </p>
	 */
	public ModuleInfo() {
		this(null, false, null, null, null, null, null, null);
	}

	/**
	 * <p>
	 * Creates a module info.
	 * </p>
	 * 
	 * @param name     the module's name
	 * @param open     true to open the module, false otherwise
	 * @param imports  the list of import declarations
	 * @param requires the list of requires directives
	 * @param exports  the list of exports directives
	 * @param opens    the list of opens directives
	 * @param uses     the list of uses directives
	 * @param provides the list of provides directives
	 */
	public ModuleInfo(
			String name, 
			boolean open, 
			List<ModuleInfo.ImportDeclaration> imports, 
			List<ModuleInfo.RequiresDirective> requires, 
			List<ModuleInfo.ExportsDirective> exports, 
			List<ModuleInfo.OpensDirective> opens, 
			List<ModuleInfo.UsesDirective> uses,
			List<ModuleInfo.ProvidesDirective> provides
		) {
		this.name = name;
		this.open = open;
		this.imports = imports != null ? imports : new ArrayList<>();
		this.requires = requires != null ? requires : new ArrayList<>();
		this.exports = exports != null ? exports : new ArrayList<>();
		this.opens = opens != null ? opens : new ArrayList<>();
		this.uses = uses != null ? uses : new ArrayList<>();
		this.provides = provides != null ? provides : new ArrayList<>();
	}

	/**
	 * <p>
	 * Returns the name of the module.
	 * </p>
	 * 
	 * @return the module's name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * <p>
	 * Determines whether the module is opened.
	 * </p>
	 * 
	 * @return true if the module is opened, false otherwise
	 */
	public boolean isOpen() {
		return this.open;
	}
	
	/**
	 * <p>
	 * Sets whether the module is opened.
	 * </p>
	 * 
	 * @param open true to open the module, false otherwise
	 */
	public void setOpen(boolean open) {
		this.open = open;
	}

	/**
	 * <p>
	 * Returns module's import declarations.
	 * </p>
	 * 
	 * @return the mutable list of import declarations
	 */
	public List<ModuleInfo.ImportDeclaration> getImports() {
		return this.imports;
	}

	/**
	 * <p>
	 * Returns module's requires directives.
	 * </p>
	 * 
	 * @return the mutable list of requires directives
	 */
	public List<ModuleInfo.RequiresDirective> getRequires() {
		return this.requires;
	}

	/**
	 * <p>
	 * Returns module's exports directives.
	 * </p>
	 * 
	 * @return the mutable list of exports directives
	 */
	public List<ModuleInfo.ExportsDirective> getExports() {
		return this.exports;
	}

	/**
	 * <p>
	 * Returns module's opens directives.
	 * </p>
	 * 
	 * @return the mutable list of opens directives
	 */
	public List<ModuleInfo.OpensDirective> getOpens() {
		return this.opens;
	}

	/**
	 * <p>
	 * Returns module's uses directives.
	 * </p>
	 * 
	 * @return the mutable list of uses directives
	 */
	public List<ModuleInfo.UsesDirective> getUses() {
		return this.uses;
	}

	/**
	 * <p>
	 * Returns module's provides directives.
	 * </p>
	 * 
	 * @return the mutable list of provides directives
	 */
	public List<ModuleInfo.ProvidesDirective> getProvides() {
		return this.provides;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		if(this.imports != null && !this.imports.isEmpty()) {
			s.append(this.imports.stream().map(Object::toString).collect(Collectors.joining("\n")));
			s.append("\n");
		}
		if(this.open) {
			s.append("open ");
		}
		
		s.append("module ").append(this.name).append("{");
		
		if(this.requires != null && !this.requires.isEmpty()) {
			s.append("\n");
			s.append(this.requires.stream().map(Object::toString).collect(Collectors.joining("\n")));
			s.append("\n");
		}
		
		if(this.exports != null && !this.exports.isEmpty()) {
			s.append("\n");
			s.append(this.exports.stream().map(Object::toString).collect(Collectors.joining("\n")));
			s.append("\n");
		}
		
		if(this.opens != null && !this.opens.isEmpty()) {
			s.append("\n");
			s.append(this.opens.stream().map(Object::toString).collect(Collectors.joining("\n")));
			s.append("\n");
		}
		
		if(this.uses != null && !this.uses.isEmpty()) {
			s.append("\n");
			s.append(this.uses.stream().map(Object::toString).collect(Collectors.joining("\n")));
			s.append("\n");
		}
		
		if(this.provides != null && !this.provides.isEmpty()) {
			s.append("\n");
			s.append(this.provides.stream().map(Object::toString).collect(Collectors.joining("\n")));
			s.append("\n");
		}
		
		s.append("}");
		return s.toString();
	}
	
	/**
	 * <p>
	 * Represents an import declaration in a module descriptor.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class ImportDeclaration {

		/**
		 * The name of the imported type 
		 */
		protected String typeName;
		
		/**
		 * Flag indicating whether this is a type-import-on-demand declaration (i.e. {@code import com.example.*}).
		 */
		protected boolean onDemand;
		
		/**
		 * Flag indicating whether this is a static import declaration.
		 */
		protected boolean isStatic;

		/**
		 * Flag indicating whether the directive should be removed if it exists.
		 */
		protected boolean remove;

		/**
		 * <p>
		 * Creates a blank import declaration.
		 * </p>
		 */
		public ImportDeclaration() {
		}

		/**
		 * <p>
		 * Creates an import declaration.
		 * </p>
		 * 
		 * @param typeName the type to import
		 * @param isStatic true for a static import, false otherwise
		 * @param onDemand true for a type-import-on-demand, false otherwise 
		 */
		public ImportDeclaration(String typeName, boolean isStatic, boolean onDemand) {
			this.typeName = typeName;
			this.isStatic = isStatic;
			this.onDemand = onDemand;
		}
		
		/**
		 * <p>
		 * Returns the type name.
		 * </p>
		 * 
		 * @return the type name
		 */
		public String getType() {
			return this.typeName;
		}
		
		/**
		 * <p>
		 * Determines whether this is a static import declaration.
		 * </p>
		 * 
		 * @return true if this is a static import declaration, false otherwise
		 */
		public boolean isStatic() {
			return this.isStatic;
		}

		/**
		 * <p>
		 * Determines whether this is a type-import-on-demand declaration.
		 * </p>
		 * 
		 * @return true if this is a type-import-on-demand declaration, false otherwise
		 */
		public boolean isOnDemand() {
			return this.onDemand;
		}

		/**
		 * <p>
		 * Determines whether the directive should be removed when defined.
		 * </p>
		 *
		 * @return true to remove the directive false otherwise
		 */
		public boolean isRemove() {
			return remove;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append("import ");
			if(this.isStatic) {
				s.append("static ");
			}
			s.append(this.typeName);
			if(this.onDemand) {
				s.append(".*");
			}
			s.append(";");
			return s.toString();
		}
	}
	
	/**
	 * <p>
	 * Represents a requires directive in a module descriptor.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class RequiresDirective {
		
		/**
		 * The name of the required module.
		 */
		protected String moduleName;
		
		/**
		 * Flag indicating whether this is a static dependency.
		 */
		protected boolean isStatic;
		
		/**
		 * Flag indicating whether this is a transitive dependency.
		 */
		protected boolean isTransitive;

		/**
		 * Flag indicating whether the directive should be removed if it exists.
		 */
		protected boolean remove;

		/**
		 * <p>
		 * Creates a blank requires directive.
		 * </p>
		 */
		public RequiresDirective() {
		}

		/**
		 * <p>
		 * Creates a requires directive.
		 * </p>
		 * 
		 * @param moduleName   the name of the required module
		 * @param isStatic     true for a static requires, false otherwise
		 * @param isTransitive true for a transitive requires, false otherwise
		 */
		public RequiresDirective(String moduleName, boolean isStatic, boolean isTransitive) {
			this.moduleName = moduleName;
			this.isStatic = isStatic;
			this.isTransitive = isTransitive;
		}

		/**
		 * <p>
		 * Returns the name of the required module.
		 * </p>
		 * 
		 * @return the required module's name
		 */
		public String getModule() {
			return this.moduleName;
		}

		/**
		 * <p>
		 * Determines whether this is a static dependency.
		 * </p>
		 * 
		 * @return true if this is a static requires, false otherwise
		 */
		public boolean isStatic() {
			return this.isStatic;
		}

		/**
		 * <p>
		 * Determines whether this is a transitive dependency.
		 * </p>
		 * 
		 * @return true if this is a transitive requires, false otherwise
		 */
		public boolean isTransitive() {
			return this.isTransitive;
		}

		/**
		 * <p>
		 * Determines whether the directive should be removed when defined.
		 * </p>
		 *
		 * @return true to remove the directive false otherwise
		 */
		public boolean isRemove() {
			return remove;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append("requires ");
			if(this.isStatic) {
				s.append("static ");
			}
			else if(this.isTransitive) {
				s.append("transitive ");
			}
			s.append(this.moduleName).append(";");
			return s.toString();
		}
	}
	
	/**
	 * <p>
	 * Represents an exports directive in a module descriptor.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class ExportsDirective {
		
		/**
		 * The name of the exported package.
		 */
		protected String packageName;
		
		/**
		 * The names of the modules to which the package is exported.
		 */
		protected List<String> to;

		/**
		 * Flag indicating whether the directive should be removed if it exists.
		 */
		protected boolean remove;

		/**
		 * <p>
		 * Creates a blank exports directive.
		 * </p>
		 */
		public ExportsDirective() {
		}
		
		/**
		 * <p>
		 * Creates an exports directive.
		 * </p>
		 * 
		 * @param name the name of the exported package
		 * @param to   the list of modules to which the package is exported
		 */
		public ExportsDirective(String name, List<String> to) {
			this.packageName = name;
			this.to = to != null ? to : new ArrayList<>();
		}

		/**
		 * <p>
		 * Returns the name of the exported package.
		 * </p>
		 * 
		 * @return the name of the exported package
		 */
		public String getPackage() {
			return this.packageName;
		}

		/**
		 * <p>
		 * Returns the list of modules to which the package is exported.
		 * </p>
		 * 
		 * @return the list of modules to which the package is exported
		 */
		public List<String> getTo() {
			return this.to;
		}

		/**
		 * <p>
		 * Determines whether the directive should be removed when defined.
		 * </p>
		 *
		 * @return true to remove the directive false otherwise
		 */
		public boolean isRemove() {
			return remove;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append("exports ");
			s.append(this.packageName);
			if(this.to != null && !this.to.isEmpty()) {
				s.append(" to ").append(this.to.stream().collect(Collectors.joining(", ")));
			}
			s.append(";");
			return s.toString();
		}
	}
	
	/**
	 * <p>
	 * Represents an opens directive in a module descriptor.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class OpensDirective {
		
		/**
		 * The name of the opened package.
		 */
		protected String packageName;
		
		/**
		 * The names of the modules to which the package is opened.
		 */
		protected List<String> to;

		/**
		 * Flag indicating whether the directive should be removed if it exists.
		 */
		protected boolean remove;

		/**
		 * <p>
		 * Creates an empty opens directive.
		 * </p>
		 */
		public OpensDirective() {
		}

		/**
		 * <p>
		 * Creates an opens directive.
		 * </p>
		 * 
		 * @param packageName the name of the opened package
		 * @param to          the list of modules to which the package is opened
		 */
		public OpensDirective(String packageName, List<String> to) {
			this.packageName = packageName;
			this.to = to != null ? to : new ArrayList<>();
		}

		/**
		 * <p>
		 * Returns the name of the opened package.
		 * </p>
		 * 
		 * @return the name of the opened package
		 */
		public String getPackage() {
			return this.packageName;
		}

		/**
		 * <p>
		 * Returns the list of modules to which the package is opened.
		 * </p>
		 * 
		 * @return the list of modules to which the package is opened
		 */
		public List<String> getTo() {
			return this.to;
		}

		/**
		 * <p>
		 * Determines whether the directive should be removed when defined.
		 * </p>
		 *
		 * @return true to remove the directive false otherwise
		 */
		public boolean isRemove() {
			return remove;
		}
		
		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append("opens ");
			s.append(this.packageName);
			if(this.to != null && !this.to.isEmpty()) {
				s.append(" to ").append(this.to.stream().collect(Collectors.joining(", ")));
			}
			s.append(";");
			return s.toString();
		}
	}
	
	/**
	 * <p>
	 * Represents a uses directive in a module descriptor.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class UsesDirective {
		
		/**
		 * The type which is used in the module.
		 */
		protected String typeName;

		/**
		 * Flag indicating whether the directive should be removed if it exists.
		 */
		protected boolean remove;

		/**
		 * <p>
		 * Creates a blank uses directive.
		 * </p>
		 */
		public UsesDirective() {
		}
		
		/**
		 * <p>
		 * Creates a uses directive.
		 * </p>
		 * 
		 * @param typeName the type used in the module
		 */
		public UsesDirective(String typeName) {
			this.typeName = typeName;
		}
		
		/**
		 * <p>
		 * Returns the type used in the module.
		 * </p>
		 * 
		 * @return the name of the type used in the module
		 */
		public String getType() {
			return this.typeName;
		}

		/**
		 * <p>
		 * Determines whether the directive should be removed when defined.
		 * </p>
		 *
		 * @return true to remove the directive false otherwise
		 */
		public boolean isRemove() {
			return remove;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append("uses ").append(this.typeName).append(";");
			return s.toString();
		}
	}
	
	/**
	 * <p>
	 * Represents a provides directive in a module descriptor.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.4
	 */
	public static class ProvidesDirective {

		/**
		 * The type of the service provided by the module.
		 */
		protected String typeName;
		
		/**
		 * The implementation types in the module providing the service.
		 */
		protected List<String> with;

		/**
		 * Flag indicating whether the directive should be removed if it exists.
		 */
		protected boolean remove;

		/**
		 * <p>
		 * Creates a blank provides directive.
		 * </p>
		 */
		public ProvidesDirective() {
		}

		/**
		 * <p>
		 * Creates a provides directive.
		 * </p>
		 * 
		 * @param typeName the type of the service provided by the module
		 * @param with     the types in the module providing the service
		 */
		public ProvidesDirective(String typeName, List<String> with) {
			this.typeName = typeName;
			this.with = with != null ? with : new ArrayList<>();
		}
		
		/**
		 * <p>
		 * Returns the service type provided by the module.
		 * </p>
		 * 
		 * @return the service type
		 */
		public String getType() {
			return this.typeName;
		}

		/**
		 * <p>
		 * Returns the types in the module providing the service.
		 * </p>
		 * 
		 * @return the types providing the service
		 */
		public List<String> getWith() {
			return this.with;
		}

		/**
		 * <p>
		 * Determines whether the directive should be removed when defined.
		 * </p>
		 *
		 * @return true to remove the directive false otherwise
		 */
		public boolean isRemove() {
			return remove;
		}
		
		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append("provides ");
			s.append(this.typeName);
			if(this.with != null && !this.with.isEmpty()) {
				s.append(" with ").append(this.with.stream().collect(Collectors.joining(", ")));
			}
			s.append(";");
			return s.toString();
		}
	}
}
