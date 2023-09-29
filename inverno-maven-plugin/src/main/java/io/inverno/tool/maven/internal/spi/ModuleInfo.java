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
package io.inverno.tool.maven.internal.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.4
 * 
 */
public class ModuleInfo {
	
	protected String name;
	
	protected boolean open;
	
	protected List<ModuleInfo.ImportDeclaration> imports;
	
	protected List<ModuleInfo.RequiresDirective> requires;
	
	protected List<ModuleInfo.ExportsDirective> exports;
	
	protected List<ModuleInfo.OpensDirective> opens;
	
	protected List<ModuleInfo.UsesDirective> uses;
	
	protected List<ModuleInfo.ProvidesDirective> provides;
	
	public ModuleInfo() {
		this(null, false, null, null, null, null, null, null);
	}

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

	public String getName() {
		return this.name;
	}

	public boolean isOpen() {
		return this.open;
	}

	public List<ModuleInfo.ImportDeclaration> getImports() {
		return this.imports;
	}

	public List<ModuleInfo.RequiresDirective> getRequires() {
		return this.requires;
	}

	public List<ModuleInfo.ExportsDirective> getExports() {
		return this.exports;
	}

	public List<ModuleInfo.OpensDirective> getOpens() {
		return this.opens;
	}

	public List<ModuleInfo.UsesDirective> getUses() {
		return this.uses;
	}

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
	
	public static class ImportDeclaration {

		protected String packageName;
		
		protected boolean onDemand;
		
		protected boolean isStatic;

		public ImportDeclaration() {
		}

		public ImportDeclaration(String packageName, boolean isStatic, boolean onDemand) {
			this.packageName = packageName;
			this.isStatic = isStatic;
			this.onDemand = onDemand;
		}
		
		public String getPackage() {
			return this.packageName;
		}
		
		public boolean isStatic() {
			return this.isStatic;
		}

		public boolean isOnDemand() {
			return this.onDemand;
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append("import ");
			if(this.isStatic) {
				s.append("static ");
			}
			s.append(this.packageName);
			if(this.onDemand) {
				s.append(".*");
			}
			s.append(";");
			return s.toString();
		}
	}
	
	public static class RequiresDirective {
		
		protected String moduleName;
		
		protected boolean isStatic;
		
		protected boolean isTransitive;

		public RequiresDirective() {
		}

		public RequiresDirective(String moduleName, boolean isStatic, boolean isTransitive) {
			this.moduleName = moduleName;
			this.isStatic = isStatic;
			this.isTransitive = isTransitive;
		}

		public String getModule() {
			return this.moduleName;
		}

		public boolean isStatic() {
			return this.isStatic;
		}

		public boolean isTransitive() {
			return this.isTransitive;
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
	
	public static class ExportsDirective {
		
		protected String packageName;
		
		protected List<String> to;

		public ExportsDirective() {
		}
		
		public ExportsDirective(String name, List<String> to) {
			this.packageName = name;
			this.to = to != null ? to : new ArrayList<>();
		}

		public String getPackage() {
			return this.packageName;
		}

		public List<String> getTo() {
			return this.to;
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
	
	public static class OpensDirective {
		
		protected String packageName;
		
		protected List<String> to;

		public OpensDirective() {
		}

		public OpensDirective(String packageName, List<String> to) {
			this.packageName = packageName;
			this.to = to != null ? to : new ArrayList<>();
		}

		public String getPackage() {
			return this.packageName;
		}

		public List<String> getTo() {
			return this.to;
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
	
	public static class UsesDirective {
		
		protected String typeName;

		public UsesDirective() {
		}
		
		public UsesDirective(String typeName) {
			this.typeName = typeName;
		}
		
		public String getType() {
			return this.typeName;
		}
		
		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append("uses ").append(this.typeName).append(";");
			return s.toString();
		}
	}
	
	public static class ProvidesDirective {

		protected String typeName;
		
		protected List<String> with;

		public ProvidesDirective() {
		}

		public ProvidesDirective(String typeName, List<String> with) {
			this.typeName = typeName;
			this.with = with != null ? with : new ArrayList<>();
		}
		
		public String getType() {
			return this.typeName;
		}

		public List<String> getWith() {
			return this.with;
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
