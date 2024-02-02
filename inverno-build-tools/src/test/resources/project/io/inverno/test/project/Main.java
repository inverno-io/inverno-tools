package io.inverno.test.project;

import io.inverno.test.moduledep.ModuleDepService;
import io.inverno.test.automaticmoduledep.AutomaticModuleDepService;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Objects;
import java.util.Optional;

public class Main {

	public static void main(String[] args) throws Exception {
		// Use class from module
		String moduleDepResult = new ModuleDepService().execute();
		// Use class from automatic module
		String automaticModuleDepResult = new AutomaticModuleDepService().execute();
		
		ModuleLayer moduleLayer = Main.class.getModule().getLayer();
		
		// Resolve resource from webjar
		Module webjarDepModule = moduleLayer.findModule("org.webjars.webjar.dep").get();
		String webjarDepResult;
		try(InputStream webjarInput = webjarDepModule.getResourceAsStream("META-INF/resources/webjars/webjar.dep/1.0.4/info.txt")) {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			int b = -1;
			while((b = webjarInput.read()) != -1) {
				bout.write(b);
			}
			webjarDepResult = new String(bout.toByteArray());
		}
		
		// Resolve class from unnamed module
		Module unnamedDepModule = moduleLayer.findModule("io.inverno.test.unnamed.dep").get();
		
		Class<?> unnamedModuleDepServiceClass = unnamedDepModule.getClassLoader().loadClass("io.inverno.test.unnamedmoduledep.UnnamedModuleDepService");
		String unnamedDepResult = (String)unnamedModuleDepServiceClass.getMethod("execute").invoke(unnamedModuleDepServiceClass.newInstance());

		String testArgs = args != null && args.length > 0 ? Arrays.stream(args).collect(Collectors.joining(", ")) : null;
		
		String testProperty = Optional.ofNullable(System.getProperty("inverno.test.property")).orElse(null);
		
		System.out.println(Stream.of(moduleDepResult, automaticModuleDepResult, webjarDepResult, unnamedDepResult, testArgs, testProperty).filter(Objects::nonNull).collect(Collectors.joining(", ")));
		
		String pidfile = System.getProperty("inverno.test.pidfile");
		if(pidfile != null && !pidfile.isBlank()) {
			Files.write(Path.of(pidfile), Long.toString(ProcessHandle.current().pid()).getBytes(), StandardOpenOption.CREATE_NEW);
		}
		
		if(Boolean.valueOf(System.getProperty("inverno.test.block"))) {
			System.in.read();
		}
	}
}
