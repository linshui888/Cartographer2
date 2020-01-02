package io.github.bananapuncher714.cartographer.core.module;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.NoSuchFileException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang.Validate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import io.github.bananapuncher714.cartographer.core.util.ReflectionUtil;

/**
 * Load modules and their descriptions, like a plugin.
 * 
 * @author BananaPuncher714
 */
public class ModuleLoader {
	/**
	 * Load a module with the given description and file.
	 * 
	 * @param file
	 * The module jar. Cannot be null.
	 * @param description
	 * A {@link ModuleDescription} of the module being loaded. Cannot be null.
	 * @return
	 * A new module if successful.
	 */
	public static Module load( File file, ModuleDescription description ) {
		Validate.notNull( file );
		Validate.notNull( description );
		Validate.isTrue( file.exists(), file + " does not exist!" );
		Validate.isTrue( file.isFile(), file + " is not a file!" );
		
		try {
			URLClassLoader child = new URLClassLoader(
			        new URL[] { file.toURI().toURL() },
			        ReflectionUtil.class.getClassLoader()
			);
			
			Class< ? > jarClass = Class.forName( description.getMain(), true, child );
			Class< ? extends Module > moduleClass = jarClass.asSubclass( Module.class );
			return moduleClass.newInstance();
		} catch ( MalformedURLException | IllegalArgumentException | SecurityException | ClassNotFoundException | InstantiationException | IllegalAccessException e ) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Get a {@link ModuleDescription} from a module jar.
	 * 
	 * @param file
	 * The module jar. Cannot be null.
	 * @return
	 * A ModuleDescription if successful.
	 */
	public static ModuleDescription getDescriptionFor( File file ) {
		if ( file == null ) {
			throw new IllegalArgumentException( "File cannot be null!" );
		}
		if ( !file.exists() ) {
			try {
				throw new FileNotFoundException( "File does not exist! " + file.getAbsolutePath() );
			} catch ( FileNotFoundException e ) {
				e.printStackTrace();
			}
			return null;
		}
		try ( JarFile jar = new JarFile( file ) ) {
			JarEntry entry = jar.getJarEntry( "module.json" );
			if ( entry == null ) {
				throw new NoSuchFileException( "module.json does not exist! " + file.getAbsolutePath() );
			}
			InputStream stream = jar.getInputStream( entry );
			
			return getDescriptionFor( stream );
		} catch ( IOException e ) {
			e.printStackTrace();
		} catch ( IllegalArgumentException e ) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Get a ModuleDescription from an input stream.
	 * 
	 * @param stream
	 * The stream to read from. Cannot be null.
	 * @return
	 * A {@link ModuleDescription} generated from the stream.
	 */
	public static ModuleDescription getDescriptionFor( InputStream stream ) {
		Validate.notNull( stream );
		JsonReader reader = new JsonReader( new InputStreamReader( stream ) );
		
		JsonParser parser = new JsonParser();
		JsonElement element = parser.parse( reader );
		
		JsonObject object = element.getAsJsonObject();
		
		if ( !( object.has( "name" ) && object.has( "main" ) && object.has( "author" ) && object.has( "description" ) && object.has( "version" ) ) ) {
			throw new IllegalArgumentException( "Missing required information from module.json! (name/main/author/version)" );
		}
		
		String name = object.get( "name" ).getAsString();
		String main = object.get( "main" ).getAsString();
		String author = object.get( "author" ).getAsString();
		String version = object.get( "version" ).getAsString();

		ModuleDescription moduleDescription = new ModuleDescription( name, main, author, version );
		
		if ( object.has( "description" ) ) {
			moduleDescription.setDescription( object.get( "description" ).getAsString() );
		}
		
		if ( object.has( "website" ) ) {
			moduleDescription.setWebsite( object.get( "website" ).getAsString() );
		}
		
		if ( object.has( "depend" ) ) {
			JsonArray array = object.get( "depend" ).getAsJsonArray();
			for ( JsonElement dependElement : array ) {
				moduleDescription.getDependencies().add( dependElement.getAsString() );
			}
		}
		
		if ( object.has( "dependencies" ) ) {
			JsonArray array = object.get( "dependencies" ).getAsJsonArray();
			for ( JsonElement dependElement : array ) {
				moduleDescription.getDependencies().add( dependElement.getAsString() );
			}
		}
		
		return moduleDescription;
	}
}
