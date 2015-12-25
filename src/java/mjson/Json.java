/*
 * Copyright (C) 2011 Miami-Dade County.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Note: this file incorporates source code from 3d party entities. Such code 
 * is copyrighted by those entities as indicated below.
 */
package mjson;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 
 * <p>
 * Represents a JSON (JavaScript Object Notation) entity. For more information about JSON, please see
 * <a href="http://www.json.org" target="_">http://www.json.org</a>.  
 * </p>
 *
 * <p>
 * A JSON entity can be one of several things: an object (set of name/Json entity pairs), an array (a list of
 * other JSON entities), a string, a number, a boolean or null. All of those are represented as <code>Json</code>
 * instances. Each of the different types of entities supports a different set of operations. However, this class
 * unifies all operations into a single interface so in Java one is always dealing with a single object type: this class. 
 * The approach effectively amounts to dynamic typing where using an unsupported operation won't be detected at
 * compile time, but will throw a runtime {@link MJsonException}. It simplifies working with JSON
 * structures considerably and it leads to shorter at cleaner Java code. It makes much easier to work
 * with JSON structure without the need to convert to "proper" Java representation in the form of
 * POJOs and the like. When traversing a JSON, there's no need to type-cast at each step because there's
 * only one type: <code>Json</code>.   
 * </p>
 * 
 * <p>
 * One can examine the concrete type of a <code>Json</code> with one of the <code>isXXX</code> methods: 
 * {@link #isObject()}, {@link #isArray()},{@link #isNumber()},{@link #isBoolean()},{@link #isString()},
 * {@link #isNull()}.
 * </p>
 * 
 * <p>
 * The underlying representation of a given <code>Json</code> instance can be obtained by calling
 * the generic {@link #getValue()} method or one of the <code>asXXX</code> methods such 
 * as {@link #asBoolean()} or {@link #asString()} etc. 
 * JSON objects are represented as Java {@link Map}s while JSON arrays are represented as Java
 * {@link List}s. Because those are mutable aggregate structures, there are two versions of the 
 * corresponding <code>asXXX</code> methods: {@link #asMap()} which performs a deep copy of the underlying
 * map, unwrapping every nested Json entity to its Java representation and {@link #asJsonMap()} which
 * simply return the map reference. Similarly there are {@link #asList()} and {@link #asJsonList()}. 
 * </p>
 * 
 * <h3>Constructing and Modifying JSON Structures</h3>
 * 
 * <p>
 * There are several static factory methods in this class that allow you to create new
 * <code>Json</code> instances:
 * </p>
 *  
 * <table>
 * <tr><td>{@link #read(String)}</td>
 * <td>Parse a JSON string and return the resulting <code>Json</code> instance. The syntax
 * recognized is as defined in <a href="http://www.json.org">http://www.json.org</a>.
 * </td> 
 * </tr>
 * <tr><td>{@link #make(Object)}</td>
 * <td>Creates a Json instance based on the concrete type of the parameter. The types
 * recognized are null, numbers, primitives, String, Map, Collection, Java arrays
 * and <code>Json</code> itself.</td>
 * </tr>
 * <tr><td>{@link #nil()}</td>
 * <td>Return a <code>Json</code> instance representing JSON <code>null</code>.</td>
 * </tr>
 * <tr><td>{@link #object()}</td>
 * <td>Create and return an empty JSON object.</td>
 * </tr>
 * <tr><td>{@link #object(Object...)}</td>
 * <td>Create and return a JSON object populated with the key/value pairs
 * passed as an argument sequence. Each even parameter becomes a key (via 
 * <code>toString</code>) and each odd parameter is converted to a <code>Json</code>
 * value.</td>
 * </tr>   
 * <tr><td>{@link #array()}</td>
 * <td>Create and return an empty JSON array.</td>
 * </tr> 
 * <tr><td>{@link #array(Object...)}</td>
 * <td>Create and return a JSON array from the list of arguments.</td>
 * </tr> 
 * </table>
 * 
 * <p>
 * To customize how Json elements are represented and to provide your own version of the
 * {@link #make(Object)} method, you create an implementation of the {@link Factory} interface
 * and configure it either globally with the {@link #setGlobalFactory(Factory)} method or
 * on a per-thread basis with the {@link #attachFactory(Factory)}/{@link #detachFactory()} 
 * methods.
 * </p>
 * 
 * <p>
 * If a <code>Json</code> instance is an object, you can set its properties by
 * calling the {@link #set(String, Object)} method which will add a new property or replace an existing one.
 * Adding elements to an array <code>Json</code> is done with the {@link #add(Object)} method.
 * Removing elements by their index (or key) is done with the {@link #delAt(int)} (or
 * {@link #delAt(String)}) method. You can also remove an element from an array without
 * knowing its index with the {@link #remove(Object)} method. All these methods return the 
 * <code>Json</code> instance being manipulated so that method calls can be chained.
 * If you want to remove an element from an object or array and return the removed element
 * as a result of the operation, call {@link #atDel(int)} or {@link #atDel(String)} instead.  
 * </p>
 * 
 * <p>
 * If you want to add properties to an object in bulk or append a sequence of elements to array, 
 * use the {@link #with(Json, Json...opts)} method. When used on an object, this method expects another
 * object as its argument and it will copy all properties of that argument into itself. Similarly,
 * when called on array, the method expects another array and it will append all elements of its
 * argument to itself.
 * </p>
 * 
 * <p>
 * To make a clone of a Json object, use the {@link #dup()} method. This method will create a new 
 * object even for the immutable primitive Json types. Objects and arrays are cloned 
 * (i.e. duplicated) recursively.
 * </p>
 * 
 * <h3>Navigating JSON Structures</h3>
 * 
 * <p>
 * The {@link #at(int)} method returns the array element at the specified index and the
 * {@link #at(String)} method does the same for a property of an object instance. You can
 * use the {@link #at(String, Object)} version to create an object property with a default
 * value if it doesn't exist already.  
 * </p>
 * 
 * <p>
 * To test just whether a Json object has a given property, use the {@link #has(String)} method. To test
 * whether a given object property or an array elements is equal to a particular value, use the 
 * {@link #is(String, Object)} and {@link #is(int, Object)} methods respectively. Those methods return 
 * true if the given named property (or indexed element) is equal to the passed in Object as the second 
 * parameter. They return false if an object doesn't have the specified property or an index array is out 
 * of bounds. For example is(name, value) is equivalent to 'has(name) &amp;&amp; at(name).equals(make(value))'.
 * </p>
 * 
 * <p>
 * To help in navigating JSON structures, instances of this class contain a reference to the 
 * enclosing JSON entity (object or array) if any. The enclosing entity can be accessed 
 * with {@link #up()} method.
 * </p>
 * 
 * <p>
 * The combination of method chaining when modifying <code>Json</code> instances and
 * the ability to navigate "inside" a structure and then go back to the enclosing 
 * element lets one accomplish a lot in a single Java statement, without the need
 * of intermediary variables. Here for example how the following JSON structure can
 * be created in one statement using chained calls:
 * </p>
 * 
 * <pre><code>
 * {"menu": {
 * "id": "file",
 * "value": "File",
 * "popup": {
 *   "menuitem": [
 *     {"value": "New", "onclick": "CreateNewDoc()"},
 *     {"value": "Open", "onclick": "OpenDoc()"},
 *     {"value": "Close", "onclick": "CloseDoc()"}
 *   ]
 * }
 * "position": 0
 * }}
 * </code></pre>
 * 
 * <pre><code>
 * import mjson.Json;
 * import static mjson.Json.*;
 * ...
 * Json j = object()
 *  .at("menu", object())
 *    .set("id", "file") 
 *    .set("value", "File")
 *    .at("popup", object())
 *      .at("menuitem", array())
 *        .add(object("value", "New", "onclick", "CreateNewDoc()"))
 *        .add(object("value", "Open", "onclick", "OpenDoc()"))
 *        .add(object("value", "Close", "onclick", "CloseDoc()"))
 *        .up()
 *      .up()
 *    .set("position", 0)
 *  .up();       
 * ...
 * </code></pre>
 * 
 * <p>
 * If there's no danger of naming conflicts, a static import of the factory methods (<code>
 * import static json.Json.*;</code>) would reduce typing even further and make the code more
 * readable.
 * </p>
 *
 * <h3>Converting to String</h3>
 * 
 * <p>
 * To get a compact string representation, simply use the {@link #toString()} method. If you
 * want to wrap it in a JavaScript callback (for JSON with padding), use the {@link #pad(String)}
 * method.
 * </p>
 * 
 * <h3>Validating with JSON Schema</h3>
 * 
 * <p>
 * Since version 1.3, mJson supports JSON Schema, draft 4. A schema is represented by the internal
 * class {@link mjson.Json.Schema}. To perform a validation, you have a instantiate a <code>Json.Schema</code>
 * using the factory method {@link mjson.Json.Schema} and then call its <code>validate</code> method
 * on a JSON instance:
 * </p>
 *  
 * <pre><code>
 * import mjson.Json;
 * import static mjson.Json.*;
 * ...
 * Json inputJson = Json.read(inputString);
 * Json schema = Json.schema(new URI("http://mycompany.com/schemas/model"));
 * Json errors = schema.validate(inputJson);
 * for (Json error : errors.asJsonList())
 * 	   System.out.println("Validation error " + err);
 * </code></pre>
 * @author Borislav Iordanov
 * @version 1.4
 */
public class Json
{
	/**
	 * <p>
	 * This interface defines how <code>Json</code> instances are constructed. There is a 
	 * default implementation for each kind of <code>Json</code> value, but you can provide
	 * your own implementation. For example, you might want a different representation of 
	 * an object than a regular <code>HashMap</code>. Or you might want string comparison to be
	 * case insensitive.
	 * </p>
	 *
	 * <p>
	 * In addition, the {@link #make(Object)} method allows you plug-in your own mapping
	 * of arbitrary Java objects to <code>Json</code> instances. You might want to implement
	 * a Java Beans to JSON mapping or any other JSON serialization that makes sense in your 
	 * project.
	 * </p>
	 * 
	 * <p>
	 * To avoid implementing all methods in that interface, you can extend the {@link DefaultFactory}
	 * default implementation and simply overwrite the ones you're interested in.
	 * </p>
	 * 
	 * <p>
	 * The factory implementation used by the <code>Json</code> classes is specified simply by calling
	 * the {@link #setGlobalFactory(Factory)} method. The factory is a static, global variable by default.
	 * If you need different factories in different areas of a single application, you may attach them
	 * to different threads of execution using the {@link #attachFactory(Factory)}. Recall a separate 
	 * copy of static variables is made per ClassLoader, so for example in a web application context, that
	 * global factory can be different for each web application (as Java web servers usually use a separate
	 * class loader per application). Thread-local factories are really a provision for special cases.
	 * </p>
	 * 
	 * @author Borislav Iordanov
	 *
	 */
    public static interface Factory 
    {
    	/**
    	 * Construct and return an object representing JSON <code>null</code>. Implementations are
    	 * free to cache a return the same instance. The resulting value must return
    	 * <code>true</code> from <code>isNull()</code> and <code>null</code> from
    	 * <code>getValue()</code>.
    	 */
        Json nil();
        
        /**
         * Construct and return a JSON boolean. The resulting value must return
    	 * <code>true</code> from <code>isBoolean()</code> and the passed
    	 * in parameter from <code>getValue()</code>.
         * @param value The boolean value.
         * @return A JSON with <code>isBoolean() == true</code>. Implementations
         * are free to cache and return the same instance for true and false.
         */
        Json bool(boolean value);
        
        /**
         * Construct and return a JSON string. The resulting value must return
    	 * <code>true</code> from <code>isString()</code> and the passed
    	 * in parameter from <code>getValue()</code>.
         * @param value The string to wrap as a JSON value.
         */
        Json string(String value);
        
        /**
         * Construct and return a JSON number. The resulting value must return
    	 * <code>true</code> from <code>isNumber()</code> and the passed
    	 * in parameter from <code>getValue()</code>.
    	 *  
         * @param value The numeric value.
         * @return Json instance representing that value.
         */
        Json number(Number value);
        
        /**
         * Construct and return a JSON object. The resulting value must return
    	 * <code>true</code> from <code>isObject()</code> and an implementation
    	 * of <code>java.util.Map</code> from <code>getValue()</code>.
         */
        Json object();

        /**
         * Construct and return a JSON object. The resulting value must return
    	 * <code>true</code> from <code>isArray()</code> and an implementation
    	 * of <code>java.util.List</code> from <code>getValue()</code>.
         */
        Json array();
        
        /**
         * Construct and return a JSON object. The resulting value can be of any
         * JSON type. The method is responsible for examining the type of its
         * argument and performing an appropriate mapping to a <code>Json</code>
         * instance. 
         */        
        Json make(Object anything);
    }

    /**
     * <p>
     * Represents JSON schema - a specific data format that a JSON entity must
     * follow. The idea of a JSON schema is very similar to XML. Its main purpose 
     * is validating input.    
     * </p>
     * 
     *  <p>
     *  More information about the various JSON schema specifications can be 
     *  found at http://json-schema.org. JSON Schema is an  IETF draft (v4 currently) and 
     *  our implementation follows this set of specifications. A JSON schema is specified 
     *  as a JSON object that contains keywords defined by the specification. Here are
     *  a few introductory materials:
     *  <ul>
     *  <li>http://jsonary.com/documentation/json-schema/ - 
     *  a very well-written tutorial covering the whole standard</li>
     *  <li>http://spacetelescope.github.io/understanding-json-schema/ - 
     *  online book, tutorial (Python/Ruby based)</li>
     *  </ul>
     *  </p>
     * @author Borislav Iordanov
     *
     */
    public static interface Schema
    {    	
    	/**
    	 * <p>
    	 * Validate a JSON document according to this schema. The validations attempts to
    	 * proceed even in the face of errors. The return value is always a <code>Json.object</code>
    	 * containing the boolean property <code>ok</code>. When <code>ok</code> is <code>true</code>,
    	 * the return object contains nothing else. When it is <code>false</code>, the return object
    	 * contains a property <code>errors</code> which is an array of error messages for all
    	 * detected schema violations.
    	 * </p>
    	 * 
    	 * @param document The input document.
    	 * @return <code>{"ok":true}</code> or <code>{"ok":false, errors:["msg1", "msg2", ...]}</code>
    	 */
    	Json validate(Json document);
    	
    	/**
    	 * <p>Possible options are: <code>ignoreDefaults:true|false</code>.
    	 * </p>
    	 * @return A newly created <code>Json</code> conforming to this schema.
    	 */
    	//Json generate(Json options);
    }

    static String fetchContent(URL url)
    {
    	java.io.Reader reader = null;
    	try
    	{
	    	reader = new java.io.InputStreamReader((java.io.InputStream)url.getContent());
	    	StringBuilder content = new StringBuilder();
	    	char [] buf = new char[1024];
	    	for (int n = reader.read(buf); n > -1; n = reader.read(buf))
	    	    content.append(buf, 0, n);
//	    	System.out.println("last reaad: " + new StringBuilder(buf))
	    	return content.toString();
    	}
    	catch (Exception ex)
    	{
    		throw new MJsonException(ex);
    	}
    	finally
    	{
    		if (reader != null) try { reader.close(); } catch (IOException iox) { throw new MJsonException(iox); }
        }
    }
    
    static Json resolvePointer(String pointerRepresentation, Json top)
    {
    	String [] parts = pointerRepresentation.split("/");
    	Json result = top;
    	for (String p : parts)
    	{
    		// TODO: unescaping and decoding
    		if (p.length() == 0)
    			continue;
    		p = p.replace("~1", "/").replace("~0", "~");
    		if (result.isArray())
    			result = result.at(Integer.parseInt(p));
    		else if (result.isObject())
    			result = result.at(p);
    		else
    			throw new MJsonException("Can't resolve pointer " + pointerRepresentation + 
    					" on document " + top.toString(200));
    	}
    	return result;
    }
    
    static URI makeAbsolute(URI base, String ref) throws Exception
    {
    	URI refuri;
    	if (base != null && base.getAuthority() != null && !new URI(ref).isAbsolute())
    	{
    		StringBuilder sb = new StringBuilder();
    		if (base.getScheme() != null)
    			sb.append(base.getScheme()).append("://");
    		sb.append(base.getAuthority());
    		if (!ref.startsWith("/"))
    		{
    			if (ref.startsWith("#"))
    				sb.append(base.getPath());
    			else
    			{
	    			int slashIdx = base.getPath().lastIndexOf('/');
	    			sb.append(slashIdx == -1 ? base.getPath() : base.getPath().substring(0,  slashIdx)).append("/");
    			}
    		}
			refuri = new URI(sb.append(ref).toString());
    	}
    	else if (base != null)
    		refuri = base.resolve(ref);
    	else
    		refuri = new URI(ref);
   		return refuri;
    }
    
    static Json resolveRef(URI base, 
    					   Json refdoc, 
    					   URI refuri,  
    					   Map<String, Json> resolved, 
    					   Map<Json, Json> expanded,
    					   Function<URI, Json> uriResolver) throws Exception
    {
    	if (refuri.isAbsolute() &&
    		 (base == null || !base.isAbsolute() ||
    			!base.getScheme().equals(refuri.getScheme()) ||
	    		!Objects.equals(base.getHost(), refuri.getHost()) ||
	    		base.getPort() != refuri.getPort() ||
	    		!base.getPath().equals(refuri.getPath())))
    	{
    		URI docuri = null;
    		refuri = refuri.normalize();    		
    		if (refuri.getHost() == null)
    			docuri = new URI(refuri.getScheme() + ":" + refuri.getPath());
    		else 
    			docuri = new URI(refuri.getScheme() + "://" + refuri.getHost() + 
    				((refuri.getPort() > -1) ? ":" + refuri.getPort() : "") +
    				refuri.getPath());	    		
    		refdoc = uriResolver.apply(docuri);
    		refdoc = expandReferences(refdoc, refdoc, docuri, resolved, expanded, uriResolver);
    	}
    	if (refuri.getFragment() == null)
    		return refdoc;
    	else
    		return resolvePointer(refuri.getFragment(), refdoc);
    }
    
    /**
     * <p>
     * Replace all JSON references, as per the http://tools.ietf.org/html/draft-pbryan-zyp-json-ref-03 
     * specification, by their referants. 
     * </p>
     * @param json
     * @param duplicate
     * @param done
     * @return
     */
	static Json expandReferences(Json json, 
								 Json topdoc, 
								 URI base, 
								 Map<String, Json> resolved, 
								 Map<Json, Json> expanded,
								 Function<URI, Json> uriResolver) throws Exception
	{
		if (expanded.containsKey(json)) return json;
		if (json.isObject())
		{
			if (json.has("id") && json.at("id").isString()) // change scope of nest references
			{
				base = base.resolve(json.at("id").asString());
			}
			
			if (json.has("$ref"))
			{
				URI refuri = makeAbsolute(base, json.at("$ref").asString()); // base.resolve(json.at("$ref").asString());
				Json ref = resolved.get(refuri.toString());
				if (ref == null)
				{
					ref = resolveRef(base, topdoc, refuri, resolved, expanded, uriResolver);
					resolved.put(refuri.toString(), ref);					
					ref = expandReferences(ref, topdoc, base, resolved, expanded, uriResolver);
					resolved.put(refuri.toString(), ref);
				}
				json = ref;
			}
			else 
			{
				Json O = Json.object();
				for (Map.Entry<String, Json> e : json.asJsonMap().entrySet())
					O.set(e.getKey(), expandReferences(e.getValue(), topdoc, base, resolved, expanded, uriResolver));
				json.with(O, new Json[0]);
			}
		}
		else if (json.isArray())
		{
//			Json A = Json.array();
			for (int i = 0; i < json.asJsonList().size(); i++)
			{
				//A.add(expandReferences(j, topdoc, base, resolved));
				Json el = expandReferences(json.at(i), topdoc, base, resolved, expanded, uriResolver);
				json.set(i, el);				
			}
//			return A;
		}
		expanded.put(json,  json);
		return json;
	}

	
	// For the implementation of schema at least, we'd benefit a lot from the
	// new Java 8 function API, so we should rewrite it when that comes
	// along. For now, we just introduce here the same interfaces
	// so then porting to Java 8 becomes trivial.
	static interface Function<T,R>	{ R apply(T t); }
    
    static class DefaultSchema implements Schema
    {
    	static interface Instruction extends Function<Json, Json>{}

        static Json maybeError(Json errors, Json E)
    		{ return E == null ? errors : (errors == null ? Json.array() : errors).with(E, new Json[0]); }

    	// Anything is valid schema
    	static Instruction any = new Instruction() { public Json apply(Json param) { return null; } };
    	
    	// Type validation    	
    	class IsObject implements Instruction { public Json apply(Json param) 
    		{ return param.isObject() ? null : Json.make(param.toString(maxchars)); } }
    	class IsArray implements Instruction { public Json apply(Json param) 
    		{ return param.isArray() ? null : Json.make(param.toString(maxchars)); } }
    	class IsString implements Instruction { public Json apply(Json param) 
    		{ return param.isString() ? null : Json.make(param.toString(maxchars)); } }    	
    	class IsBoolean implements Instruction { public Json apply(Json param) 
			{ return param.isBoolean() ? null : Json.make(param.toString(maxchars)); } }
    	class IsNull implements Instruction { public Json apply(Json param) 
			{ return param.isNull() ? null : Json.make(param.toString(maxchars)); } }    	
    	class IsNumber implements Instruction { public Json apply(Json param) 
			{ return param.isNumber() ? null : Json.make(param.toString(maxchars)); } }    	    	
    	class IsInteger implements Instruction { public Json apply(Json param) 
			{ return param.isNumber() && ((Number)param.getValue()) instanceof Integer  ? null : Json.make(param.toString(maxchars)); } }    	
    	    	
    	class CheckString implements Instruction
    	{
    		int min = 0, max = Integer.MAX_VALUE;
    		Pattern pattern;
    		
    		public Json apply(Json param)
    		{
    			Json errors = null;
    			if (!param.isString()) return errors;    			
    			String s = param.asString();
    	        final int size = s.codePointCount(0, s.length());
    			if (size < min || size > max)
					errors = maybeError(errors,Json.make("String  " + param.toString(maxchars) + 
							" has length outside of the permitted range [" + min + "," + max + "]."));
    			if (pattern != null && !pattern.matcher(s).matches())
					errors = maybeError(errors,Json.make("String  " + param.toString(maxchars) + 
							" does not match regex " + pattern.toString()));    				
    			return errors;
    		}	
    	}

    	class CheckNumber implements Instruction
    	{
    		double min = Double.NaN, max = Double.NaN, multipleOf = Double.NaN;
    		boolean exclusiveMin = false, exclusiveMax = false;
    		public Json apply(Json param)
    		{
    			Json errors = null;
    			if (!param.isNumber()) return errors;    			
    			double value = param.asDouble();
    			if (!Double.isNaN(min) && (value < min || exclusiveMin && value == min))
    				errors = maybeError(errors,Json.make("Number " + param + " is below allowed minimum " + min));    				
    			if (!Double.isNaN(max) && (value > max || exclusiveMax && value == max))
        			errors = maybeError(errors,Json.make("Number " + param + " is above allowed maximum " + max));
    			if (!Double.isNaN(multipleOf) && (value / multipleOf) % 1 != 0)
    				errors = maybeError(errors,Json.make("Number " + param + " is not a multiple of  " + multipleOf));    				
    			return errors;
    		}
    	}
    	
       	class CheckArray implements Instruction
       	{
       		int min = 0, max = Integer.MAX_VALUE;
       		Boolean uniqueitems = null;
       		Instruction additionalSchema = any;
       		Instruction schema;
       		ArrayList<Instruction> schemas;
       		
    		public Json apply(Json param)
    		{
    			Json errors = null;
    			if (!param.isArray()) return errors;
				if (schema == null && schemas == null && additionalSchema == null) // no schema specified
					return errors;    			
    			int size = param.asJsonList().size();
    			for (int i = 0; i < size; i++)
    			{
    				Instruction S = schema != null ? schema
    						: (schemas != null && i < schemas.size()) ? schemas.get(i) : additionalSchema;
    				if (S == null)
    					errors = maybeError(errors,Json.make("Additional items are not permitted: " + 
    									param.at(i) + " in " + param.toString(maxchars)));
    				else
    					errors = maybeError(errors, S.apply(param.at(i)));
    				if (uniqueitems != null && uniqueitems && param.asJsonList().lastIndexOf(param.at(i)) > i)
    					errors = maybeError(errors,Json.make("Element " + param.at(i) + " is duplicate in array."));
    			}
    			if (size < min || size > max)
					errors = maybeError(errors,Json.make("Array  " + param.toString(maxchars) + 
							" has number of elements outside of the permitted range [" + min + "," + max + "]."));    				
    			return errors;
    		}
       	}
       	
       	class CheckPropertyPresent implements Instruction
       	{
       		String propname;
       		public CheckPropertyPresent(String propname) { this.propname = propname; }
       		public Json apply(Json param)
       		{
       			if (!param.isObject()) return null;
       			if (param.has(propname)) return null;
       			else return Json.array().add(Json.make("Required property " + propname + 
       					" missing from object " + param.toString(maxchars)));
       		}
       	}
       	
    	class CheckObject implements Instruction
    	{
    		int min = 0, max = Integer.MAX_VALUE;
    		HashSet<String> checked = new HashSet<String>();
    		Instruction additionalSchema = any;
    		ArrayList<Instruction> props = new ArrayList<Instruction>();
    		ArrayList<Instruction> patternProps = new ArrayList<Instruction>();
    		
        	// Object validation
        	class CheckProperty implements Instruction 
        	{ 
        		String name;
        		Instruction schema; 
        		public CheckProperty(String name, Instruction schema) 
        			{ this.name = name; this.schema = schema; }
        		public Json apply(Json param)
        		{    	
        			Json value = param.at(name);
        			if (value == null)
        				return null;
        			else
        			{
        				checked.add(name);
        				return schema.apply(param.at(name));
        			}
        		} 
        	}
        	
        	class CheckPatternProperty implements Instruction 
        	{ 
        		Pattern pattern;
        		Instruction schema; 
        		public CheckPatternProperty(String pattern, Instruction schema) 
        			{ this.pattern = Pattern.compile(pattern); this.schema = schema; }
        		public Json apply(Json param)
        		{    	
        			Json errors = null;
        			for (Map.Entry<String, Json> e : param.asJsonMap().entrySet())
        				if (pattern.matcher(e.getKey()).find())
        				{
        					errors = maybeError(errors, schema.apply(e.getValue()));
        					checked.add(e.getKey());
        				}
        			return errors;
        		} 
        	}
    		
    		public Json apply(Json param)
    		{
    			Json errors = null;
    			if (!param.isObject()) return errors;
    			checked.clear();
    			for (Instruction I : props)
    				errors = maybeError(errors, I.apply(param));
    			for (Instruction I : patternProps)
    				errors = maybeError(errors, I.apply(param));    			    			
    			if (additionalSchema != any) for (Map.Entry<String, Json> e : param.asJsonMap().entrySet())
    				if (!checked.contains(e.getKey()))
        				errors = maybeError(errors, additionalSchema == null ? 
        							Json.make("Extra property '" + e.getKey() + 
        									  "', schema doesn't allow any properties not explicitly defined:" + 
        									  param.toString(maxchars)) 
        							: additionalSchema.apply(e.getValue()));    	
    			if (param.asJsonMap().size() < min)
    				errors = maybeError(errors, Json.make("Object " + param.toString(maxchars) + 
    							" has fewer than the permitted " + min + "  number of properties."));
    			if (param.asJsonMap().size() > max)
    				errors = maybeError(errors, Json.make("Object " + param.toString(maxchars) + 
    							" has more than the permitted " + min + "  number of properties."));
    			return errors;
    		}
    	}
    	
    	class Sequence implements Instruction
    	{
    		ArrayList<Instruction> seq = new ArrayList<Instruction>();
    		public Json apply(Json param)
    		{
    			Json errors = null;
    			for (Instruction I : seq)
    				errors = maybeError(errors, I.apply(param));
    			return errors;
    		}
    		public Sequence add(Instruction I) { seq.add(I); return this; } 
    	}
    	
    	class CheckType implements Instruction
    	{
    		Json types;
    		public CheckType(Json types) { this.types = types; }
    		public Json apply(Json param)
    		{
    			String ptype = param.isString() ? "string" :
    						   param.isObject() ? "object" :
    						   param.isArray() ? "array" :
    						   param.isNumber() ? "number" :
    						   param.isNull() ? "null" : "boolean";
    			for (Json type : types.asJsonList())
    				if (type.asString().equals(ptype)) 
    					return null;
    				else if (type.asString().equals("integer") && 
    						 param.isNumber() && 
    						 param.asDouble() % 1 == 0)
    					return null;
   				return Json.array().add(Json.make("Type mistmatch for " + param.toString(maxchars) + 
   								", allowed types: " + types));
    		}    		
    	}
    	
    	class CheckEnum implements Instruction
    	{
    		Json theenum;
    		public CheckEnum(Json theenum) { this.theenum = theenum; }
    		public Json apply(Json param)
    		{    			
    			for (Json option : theenum.asJsonList())
    				if (param.equals(option))
    					return null;
    			return Json.array().add("Element " + param.toString(maxchars) + 
    					" doesn't match any of enumerated possibilities " + theenum);    			
    		}    		
    	}
    	
    	class CheckAny implements Instruction
    	{
    		ArrayList<Instruction> alternates = new ArrayList<Instruction>();
    		Json schema;
    		public Json apply(Json param)
    		{    			
    			for (Instruction I : alternates)    				
    				if (I.apply(param) == null)
    					return null;
    			return Json.array().add("Element " + param.toString(maxchars) + 
    					" must conform to at least one of available sub-schemas " + 
    					schema.toString(maxchars));    			
    		}    		
    	}
     	
    	class CheckOne implements Instruction
    	{
    		ArrayList<Instruction> alternates = new ArrayList<Instruction>();
    		Json schema;
    		public Json apply(Json param)
    		{    			
    			int matches = 0;
    			for (Instruction I : alternates)    				
    				if (I.apply(param) == null)
    					matches++;
    			if (matches != 1)
	    			return Json.array().add("Element " + param.toString(maxchars) + 
	    					" must conform to exactly one of available sub-schemas, but not more " + 
	    					schema.toString(maxchars));    			
    			else
    				return null;
    		}    		
    	}
    	
    	class CheckNot implements Instruction
    	{
    		Instruction I;
    		Json schema;
    		public CheckNot(Instruction I, Json schema) { this.I = I; this.schema = schema; }
    		public Json apply(Json param)
    		{    			
   				if (I.apply(param) != null)
    				return null;
   				else
   					return Json.array().add("Element " + param.toString(maxchars) + 
    					" must NOT conform to the schema " + schema.toString(maxchars));    			
    		}    		
    	}

    	class CheckSchemaDependency implements Instruction
    	{
    		Instruction schema;
    		String property;
    		public CheckSchemaDependency(String property, Instruction schema) { this.property = property; this.schema = schema; }
    		public Json apply(Json param)
    		{
    			if (!param.isObject()) return null;
    			else if (!param.has(property)) return null;
    			else return (schema.apply(param));
    		}
    	}

    	class CheckPropertyDependency implements Instruction
    	{
    		Json required;
    		String property;
    		public CheckPropertyDependency(String property, Json required) { this.property = property; this.required = required; }
       		public Json apply(Json param)
       		{
       			if (!param.isObject()) return null;
       			if (!param.has(property)) return null;
       			else
       			{
       				Json errors = null;
       				for (Json p : required.asJsonList())
       					if (!param.has(p.asString()))
		       				errors = maybeError(errors, Json.make("Conditionally required property " + p + 
		       					" missing from object " + param.toString(maxchars)));
       				return errors;
       			}
       		}
    	}
    	
    	Instruction compile(Json S, Map<Json, Instruction> compiled)
    	{
    		Instruction result = compiled.get(S);
    		if (result != null)
    			return result;
    		Sequence seq = new Sequence();
    		compiled.put(S, seq);
    		if (S.has("type") && !S.is("type", "any"))
    			seq.add(new CheckType(S.at("type").isString() ? 
    						Json.array().add(S.at("type")) : S.at("type")));
    		if (S.has("enum"))
    			seq.add(new CheckEnum(S.at("enum")));
    		if (S.has("allOf"))
    		{
    			Sequence sub = new Sequence();
    			for (Json x : S.at("allOf").asJsonList())
    				sub.add(compile(x, compiled));
    			seq.add(sub);
    		}
    		if (S.has("anyOf"))
    		{
    			CheckAny any = new CheckAny();
    			any.schema = S.at("anyOf");
    			for (Json x : any.schema.asJsonList())
    				any.alternates.add(compile(x, compiled));
    			seq.add(any);
    		}
    		if (S.has("oneOf"))
    		{
    			CheckOne any = new CheckOne();
    			any.schema = S.at("oneOf");
    			for (Json x : any.schema.asJsonList())
    				any.alternates.add(compile(x, compiled));
    			seq.add(any);
    		}
    		if (S.has("not"))
    			seq.add(new CheckNot(compile(S.at("not"), compiled), S.at("not")));
    		
    		if (S.has("required"))
    			for (Json p : S.at("required").asJsonList())
    				seq.add(new CheckPropertyPresent(p.asString()));
    		
    		CheckObject objectCheck = new CheckObject();
    		if (S.has("properties"))
    			for (Map.Entry<String, Json> p : S.at("properties").asJsonMap().entrySet())
    				objectCheck.props.add(objectCheck.new CheckProperty(
    						p.getKey(), compile(p.getValue(), compiled)));
    		if (S.has("patternProperties"))
    			for (Map.Entry<String, Json> p : S.at("patternProperties").asJsonMap().entrySet())
    				objectCheck.patternProps.add(objectCheck.new CheckPatternProperty(p.getKey(), 
    								compile(p.getValue(), compiled)));
    		if (S.has("additionalProperties"))
    		{
    			if (S.at("additionalProperties").isObject())
    				objectCheck.additionalSchema = compile(S.at("additionalProperties"), compiled);
    			else if (!S.at("additionalProperties").asBoolean())
    				objectCheck.additionalSchema = null; // means no additional properties allowed
    		}	
    		if (S.has("minProperties"))
    			objectCheck.min = S.at("minProperties").asInteger();
    		if (S.has("maxProperties"))
    			objectCheck.max = S.at("maxProperties").asInteger();
    		
    		if (!objectCheck.props.isEmpty() || !objectCheck.patternProps.isEmpty() || 
    			objectCheck.additionalSchema != any ||
    			objectCheck.min > 0 || objectCheck.max < Integer.MAX_VALUE)
    			seq.add(objectCheck);
    		
    		CheckArray arrayCheck = new CheckArray();
    		if (S.has("items"))
    			if (S.at("items").isObject())
    				arrayCheck.schema = compile(S.at("items"), compiled);
    			else 
    			{
    				arrayCheck.schemas = new ArrayList<Instruction>();
    				for (Json s : S.at("items").asJsonList())
    					arrayCheck.schemas.add(compile(s, compiled));
    			}
    		if (S.has("additionalItems"))
    			if (S.at("additionalItems").isObject())
    				arrayCheck.additionalSchema = compile(S.at("additionalItems"), compiled);
    			else if (!S.at("additionalItems").asBoolean())
    				arrayCheck.additionalSchema = null;
    		if (S.has("uniqueItems"))
    			arrayCheck.uniqueitems = S.at("uniqueItems").asBoolean();
    		if (S.has("minItems"))
    			arrayCheck.min = S.at("minItems").asInteger();
    		if (S.has("maxItems"))
    			arrayCheck.max = S.at("maxItems").asInteger();
    		if (arrayCheck.schema != null || arrayCheck.schemas != null ||
    			arrayCheck.additionalSchema != any ||
    			arrayCheck.uniqueitems != null ||
    			arrayCheck.max < Integer.MAX_VALUE || arrayCheck.min > 0)
    			seq.add(arrayCheck);

    		CheckNumber numberCheck = new CheckNumber();
    		if (S.has("minimum"))
    			numberCheck.min = S.at("minimum").asDouble();
    		if (S.has("maximum"))
    			numberCheck.max = S.at("maximum").asDouble();
    		if (S.has("multipleOf"))
    			numberCheck.multipleOf = S.at("multipleOf").asDouble();
    		if (S.has("exclusiveMinimum"))
    			numberCheck.exclusiveMin = S.at("exclusiveMinimum").asBoolean();
    		if (S.has("exclusiveMaximum"))
    			numberCheck.exclusiveMax = S.at("exclusiveMaximum").asBoolean();
    		if (!Double.isNaN(numberCheck.min) || !Double.isNaN(numberCheck.max) || !Double.isNaN(numberCheck.multipleOf))
    			seq.add(numberCheck);
    		
    		CheckString stringCheck = new CheckString();
    		if (S.has("minLength"))
    			stringCheck.min = S.at("minLength").asInteger();
    		if (S.has("maxLength"))
    			stringCheck.max = S.at("maxLength").asInteger();
    		if (S.has("pattern"))
    			stringCheck.pattern = Pattern.compile(S.at("pattern").asString());
    		if (stringCheck.min > 0 || stringCheck.max < Integer.MAX_VALUE || stringCheck.pattern != null)
    			seq.add(stringCheck);
    		
    		if (S.has("dependencies"))
    			for (Map.Entry<String, Json> e : S.at("dependencies").asJsonMap().entrySet())
    				if (e.getValue().isObject())
    					seq.add(new CheckSchemaDependency(e.getKey(), compile(e.getValue(), compiled)));
    				else if (e.getValue().isArray())
    					seq.add(new CheckPropertyDependency(e.getKey(), e.getValue()));
    				else 
    					seq.add(new CheckPropertyDependency(e.getKey(), Json.array(e.getValue())));
    		result = seq.seq.size() == 1 ?  seq.seq.get(0) : seq;
    		compiled.put(S, result);
    		return result;
    	}
    	
    	int maxchars = 50;
    	URI uri;
    	Json theschema;
    	Instruction start;
    	
    	DefaultSchema(URI uri, Json theschema, Function<URI, Json> relativeReferenceResolver) 
    	{ 
    		try
    		{
        		this.uri = uri == null ? new URI("") : uri;
    			if (relativeReferenceResolver == null)
					relativeReferenceResolver = docuri -> {
						try { return Json.read(fetchContent(docuri.toURL())); } 
						catch(Exception ex) { throw new MJsonException(ex); }
					};
    			this.theschema = expandReferences(theschema, 
    											  theschema, 
    											  this.uri, 
    											  new HashMap<String, Json>(), 
    											  new IdentityHashMap<Json, Json>(),
    											  relativeReferenceResolver);
    		}
    		catch (Exception ex)  { throw new MJsonException(ex); }
    		this.start = compile(this.theschema, new IdentityHashMap<Json, Instruction>());    		
    	}
    	    	    	
    	public Json validate(Json document)
    	{
    		Json result = Json.object("ok", true);
    		Json errors = start.apply(document);    		    		
    		return errors == null ? result : result.set("errors", errors).set("ok", false);
    	}
    	
    	public Json generate(Json options)
    	{
            // TODO...
    		return Json.nil();
    	}
    }
    
    public static Schema schema(Json S)
    {
    	return new DefaultSchema(null, S, null);
    }
    
    public static Schema schema(URI uri)
    {
    	return schema(uri, null);
    }
    
    public static Schema schema(URI uri, Function<URI, Json> relativeReferenceResolver)
    {
    	try { return new DefaultSchema(uri, Json.read(Json.fetchContent(uri.toURL())), relativeReferenceResolver); }
    	catch (Exception ex) { throw new MJsonException(ex); }    	
    }
    
    public static Schema schema(Json S, URI uri)
    {
    	return new DefaultSchema(uri, S, null);
    }
        
    public static class DefaultFactory implements Factory
    {
        public Json nil() { return Json.topnull; }
        public Json bool(boolean x) { return new BooleanJson(x ? Boolean.TRUE : Boolean.FALSE, null); }
        public Json string(String x) { return new StringJson(x, null); }
        public Json number(Number x) { return new NumberJson(x, null); }
        public Json array() { return new ArrayJson(); }
        public Json object() { return new ObjectJson(); }
        public Json make(Object anything) 
        { 
            if (anything == null)
                return topnull;
            else if (anything instanceof Json)
                return (Json)anything;
            else if (anything instanceof String)
                return factory().string((String)anything);
            else if (anything instanceof Collection<?>)
            {
                Json L = array();
                for (Object x : (Collection<?>)anything)
                    L.add(factory().make(x));
                return L;
            }
            else if (anything instanceof Map<?,?>)
            {
                Json O = object();
                for (Map.Entry<?,?> x : ((Map<?,?>)anything).entrySet())
                    O.set(x.getKey().toString(), factory().make(x.getValue()));
                return O;
            }
            else if (anything instanceof Boolean)
                return factory().bool((Boolean)anything);
            else if (anything instanceof Number)
                return factory().number((Number)anything);
            else if (anything.getClass().isArray())
            {
                Class<?> comp = anything.getClass().getComponentType();
                if (!comp.isPrimitive())
                    return Json.array((Object[])anything);
                Json A = array();
                if (boolean.class == comp)
                    for (boolean b : (boolean[])anything) A.add(b);
                else if (byte.class == comp)
                    for (byte b : (byte[])anything) A.add(b);
                else if (char.class == comp)
                    for (char b : (char[])anything) A.add(b);
                else if (short.class == comp)
                    for (short b : (short[])anything) A.add(b);
                else if (int.class == comp)
                    for (int b : (int[])anything) A.add(b);
                else if (long.class == comp)
                    for (long b : (long[])anything) A.add(b);
                else if (float.class == comp)
                    for (float b : (float[])anything) A.add(b);
                else if (double.class == comp)
                    for (double b : (double[])anything) A.add(b);
                return A;
            }
            else
                throw new MJsonException("Don't know how to convert to Json : " + anything);
        }  
    }
    
    public static final Factory defaultFactory = new DefaultFactory();
    
    private static Factory globalFactory = defaultFactory;
    
    // TODO: maybe use initialValue thread-local method to attach global factory by default here... 
    private static ThreadLocal<Factory> threadFactory = new ThreadLocal<Factory>();
    
    /**
     * <p>Return the {@link Factory} currently in effect. This is the factory that the {@link #make(Object)} method
     * will dispatch on upon determining the type of its argument. If you already know the type
     * of element to construct, you can avoid the type introspection implicit to the make method
     * and call the factory directly. This will result in an optimization. </p>
     */
    public static Factory factory() 
    {
    	Factory f = threadFactory.get();
    	return f != null ? f : globalFactory;
    }
    
    /**
     * <p>
     * Specify a global Json {@link Factory} to be used by all threads that don't have a
     * specific thread-local factory attached to them. 
     * </p>
     * 
     * @param factory
     */
    public static void setGlobalFactory(Factory factory) { globalFactory = factory; }
    
    /**
     * <p>
     * Attach a thread-local Json {@link Factory} to be used specifically by this thread. Thread-local
     * Json factories are the only means to have different {@link Factory} implementations used simultaneously
     * in the same application (well, more accurately, the same ClassLoader). 
     * </p>
     * 
     * @param factory
     */
    public static void attachFactory(Factory factory) { threadFactory.set(factory); }
    
    /**
     * <p>
     * Clear the thread-local factory previously attached to this thread via the 
     * {@link #attachFactory(Factory)} method. The global factory takes effect after
     * a call to this method.
     * </p>
     */
    public static void detachFactory() { threadFactory.remove(); }
    
	/**
	 * <p>
	 * Parse a JSON entity from its string representation. 
	 * </p>
	 * 
	 * @param jsonAsString A valid JSON representation as per the <a href="http://www.json.org">json.org</a>
	 * grammar. Cannot be <code>null</code>.
	 * @return The JSON entity parsed: an object, array, string, number or boolean, or null. Note that
	 * this method will never return the actual Java <code>null</code>.
	 */
	public static Json read(String jsonAsString) { return (Json)new Reader().read(jsonAsString); }

	/**
	 * <p>
	 * Parse a JSON entity from a <code>URL</code>. 
	 * </p>
	 * 
	 * @param location A valid URL where to load a JSON document from. Cannot be <code>null</code>.
	 * @return The JSON entity parsed: an object, array, string, number or boolean, or null. Note that
	 * this method will never return the actual Java <code>null</code>.
	 */
	public static Json read(URL location) { return (Json)new Reader().read(fetchContent(location)); }
	
	/**
	 * <p>
	 * Parse a JSON entity from a {@link CharacterIterator}. 
	 * </p>
	 * @see #read(String)
	 */
	public static Json read(CharacterIterator it) { return (Json)new Reader().read(it); }
	/**
	 * <p>Return the <code>null Json</code> instance.</p> 
	 */
	public static Json nil() { return factory().nil(); }	
	/**
	 * <p>Return a newly constructed, empty JSON object.</p>
	 */
	public static Json object()	{ return factory().object();	}
	/**
	 * <p>Return a new JSON object initialized from the passed list of
	 * name/value pairs. The number of arguments must
	 * be even. Each argument at an even position is taken to be a name
	 * for the following value. The name arguments are normally of type
	 * Java String, but they can be of any other type having an appropriate
	 * <code>toString</code> method. Each value is first converted
	 * to a <code>Json</code> instance using the {@link #make(Object)} method.
	 * </p>
	 * @param args A sequence of name value pairs.   
	 */
	public static Json object(Object...args)
	{
		Json j = object();
		if (args.length % 2 != 0)
			throw new MJsonException("An even number of arguments is expected.");
		for (int i = 0; i < args.length; i++)
			j.set(args[i].toString(), factory().make(args[++i]));
		return j;
	}
	
	/**
	 * <p>Return a new constructed, empty JSON array.</p>
	 */
	public static Json array() { return factory().array(); }
	
	/**
	 * <p>Return a new JSON array filled up with the list of arguments.</p>
	 *  
	 * @param args The initial content of the array. 
	 */
	public static Json array(Object...args) 
	{
		Json A = array();
		for (Object x : args)
			A.add(factory().make(x));
		return A;
	}
	
	/**
	 * <p>
	 * Convert an arbitrary Java instance to a {@link Json} instance.   
	 * </p>
	 * 
	 * <p>
	 * Maps, Collections and arrays are recursively copied where each of 
	 * their elements concerted into <code>Json</code> instances as well. The keys
	 * of a {@link Map} parameter are normally strings, but anything with a meaningful
	 * <code>toString</code> implementation will work as well. 
	 * </p>
	 * 
	 * @param anything
	 * @return The <code>Json</code>. This method will never return <code>null</code>. It will
	 * throw an {@link MJsonException} if it doesn't know how to convert the argument
	 * to a <code>Json</code> instance.
	 * @throws MJsonException when unable to handle the concrete type of the parameter
	 */
	public static Json make(Object anything) 
	{
	    return factory().make(anything);
	}
		
	// end of static utility method section

	Json enclosing = null;
	
	protected Json() { }
	protected Json(Json enclosing) { this.enclosing = enclosing; }
	
	/**
	 * <p>Return a string representation of <code>this</code> that does 
	 * not exceed a certain maximum length. This is useful in constructing
	 * error messages or any other place where only a "preview" of the
	 * JSON element should be displayed. Some JSON structures can get 
	 * very large and this method will help avoid string serializing 
	 * the whole of them. </p>
	 * @param maxCharacters The maximum number of characters for
	 * the string representation.
	 */
	public String toString(int maxCharacters) { return toString(); }

    /**
	 * <p>Explicitly set the parent of this element. The parent is presumably an array
	 * or an object. Normally, there's no need to call this method as the parent is
	 * automatically set by the framework. You may need to call it however, if you implement
	 * your own {@link Factory} with your own implementations of the Json types.
	 * </p>
	 *  
	 * @param enclosing The parent element.
	 */
	public void attachTo(Json enclosing) { this.enclosing = enclosing; }
	
	/**
	 * <p>Return the <code>Json</code> entity, if any, enclosing this 
	 * <code>Json</code>. The returned value can be <code>null</code> or
	 * a <code>Json</code> object or list, but not one of the primitive types.</p>
	 */
	public final Json up() { return enclosing; }
	
	/**
	 * <p>Return a clone (a duplicate) of this <code>Json</code> entity. Note that cloning
	 * is deep if array and objects. Primitives are also cloned, even though their values are immutable
	 * because the new enclosing entity (the result of the {@link #up()} method) may be different.
	 * since they are immutable.</p>
	 */
	public Json dup() { return this; }
	
	/**
	 * <p>Return the <code>Json</code> element at the specified index of this
	 * <code>Json</code> array. This method applies only to Json arrays.
	 * </p>
	 * 
	 * @param index The index of the desired element.
	 */
	public Json at(int index) { throw unsupportedOperation(); }
	
	/**
	 * <p>
	 * Return the specified property of a <code>Json</code> object or <code>null</code>
	 * if there's no such property. This method applies only to Json objects.  
	 * </p>
	 */
	public Json at(String property)	{ throw unsupportedOperation(); }
	
	/**
	 * <p>
	 * Return the specified property of a <code>Json</code> object if it exists.
	 * If it doesn't, then create a new property with value the <code>def</code> 
	 * parameter and return that parameter. 
	 * </p>
	 * 
	 * @param property The property to return.
	 * @param def The default value to set and return in case the property doesn't exist.
	 */
	public final Json at(String property, Json def)	
	{
		Json x = at(property);
		if (x == null)
		{
			set(property, def);
			return def;
		}
		else
			return x; 
	}	
	
	/**
	 * <p>
	 * Return the specified property of a <code>Json</code> object if it exists.
	 * If it doesn't, then create a new property with value the <code>def</code> 
	 * parameter and return that parameter. 
	 * </p>
	 * 
	 * @param property The property to return.
	 * @param def The default value to set and return in case the property doesn't exist.
	 */
	public final Json at(String property, Object def)
	{
		return at(property, make(def));
	}
	
	/**
	 * <p>
	 * Return true if this <code>Json</code> object has the specified property
	 * and false otherwise. 
	 * </p>
	 * 
	 * @param property The name of the property.
	 */
	public boolean has(String property)	{ throw unsupportedOperation(); }
	
	/**
	 * <p>
	 * Return <code>true</code> if and only if this <code>Json</code> object has a property with
	 * the specified value. In particular, if the object has no such property <code>false</code> is returned. 
	 * </p>
	 * 
	 * @param property The property name.
	 * @param value The value to compare with. Comparison is done via the equals method. 
	 * If the value is not an instance of <code>Json</code>, it is first converted to
	 * such an instance. 
	 * @return
	 */
	public boolean is(String property, Object value) { throw unsupportedOperation(); }

    /**
     * <p>
     * Return <code>true</code> if and only if this <code>Json</code> array has an element with
     * the specified value at the specified index. In particular, if the array has no element at
     * this index, <code>false</code> is returned. 
     * </p>
     * 
     * @param property The property name.
     * @param value The value to compare with. Comparison is done via the equals method. 
     * If the value is not an instance of <code>Json</code>, it is first converted to
     * such an instance. 
     * @return
     */
    public boolean is(int index, Object value) { throw unsupportedOperation(); }
	
	/**
	 * <p>
	 * Add the specified <code>Json</code> element to this array. 
	 * </p>
	 * 
	 * @return this
	 */
	public Json add(Json el) { throw unsupportedOperation(); }
	
	/**
	 * <p>
	 * Add an arbitrary Java object to this <code>Json</code> array. The object
	 * is first converted to a <code>Json</code> instance by calling the static
	 * {@link #make} method.
	 * </p>
	 * 
	 * @param anything Any Java object that can be converted to a Json instance.
	 * @return this
	 */
	public final Json add(Object anything) { return add(make(anything)); }
	
	/**
	 * <p>
	 * Remove the specified property from a <code>Json</code> object and return
	 * that property.
	 * </p>
	 * 
	 * @param property The property to be removed.
	 * @return The property value or <code>null</code> if the object didn't have such
	 * a property to begin with.
	 */
	public Json atDel(String property)	{ throw unsupportedOperation(); }

	/**
	 * <p>
	 * Remove the element at the specified index from a <code>Json</code> array and return
	 * that element.
	 * </p>
	 * 
	 * @param index The index of the element to delete.
	 * @return The element value.
	 */
	public Json atDel(int index)	{ throw unsupportedOperation(); }
	
	/**
	 * <p>
	 * Delete the specified property from a <code>Json</code> object.
	 * </p>
	 * 
	 * @param property The property to be removed.
	 * @return this
	 */
	public Json delAt(String property)	{ throw unsupportedOperation(); }
	
	/**
	 * <p>
	 * Remove the element at the specified index from a <code>Json</code> array.
	 * </p>
	 * 
	 * @param index The index of the element to delete.
	 * @return this
	 */
	public Json delAt(int index) { throw unsupportedOperation(); }
	
	/**
	 * <p>
	 * Remove the specified element from a <code>Json</code> array.
	 * </p>
	 * 
	 * @param el The element to delete.
	 * @return this
	 */
	public Json remove(Json el)	{ throw unsupportedOperation(); }
	
	/**
	 * <p>
	 * Remove the specified Java object (converted to a Json instance) 
	 * from a <code>Json</code> array. This is equivalent to 
	 * <code>remove({@link #make(anything)})</code>.
	 * </p>
	 * 
	 * @param anything The object to delete.
	 * @return this
	 */
	public final Json remove(Object anything) { return remove(make(anything)); }
	
	/**
	 * <p>
	 * Set a <code>Json</code> objects's property.
	 * </p>
	 * 
	 * @param property The property name.
	 * @param value The value of the property.
	 * @return this
	 */
	public Json set(String property, Json value) { throw unsupportedOperation();	}
	
	/**
	 * <p>
	 * Set a <code>Json</code> objects's property.
	 * </p>
	 * 
	 * @param property The property name.
	 * @param value The value of the property, converted to a <code>Json</code> representation
	 * with {@link #make}.
	 * @return this
	 */
	public final Json set(String property, Object value) { return set(property, make(value)); }
	
	/**
	 * <p>
	 * Change the value of a JSON array element. This must be an array. 
	 * </p>
	 * @param index 0-based index of the element in the array. 
	 * @param value the new value of the element
	 * @return this 
	 */
	public Json set(int index, Object value) { throw unsupportedOperation(); }	
	
	/**
	 * <p>
	 * Combine this object or array with the passed in object or array. The types of 
	 * <code>this</code> and the <code>object</code> argument must match. If both are
	 * <code>Json</code> objects, all properties of the parameter are added to <code>this</code>.
	 * If both are arrays, all elements of the parameter are appended to <code>this</code> 
	 * </p>
	 * @param object The object or array whose properties or elements must be added to this
	 * Json object or array.
	 * @return this
	 */
	public Json with(Json object, Json...options) { throw unsupportedOperation(); }

    /**
     * Same as <code>{}@link #with(Json,Json...options)}</code> with each option
     * argument converted to <code>Json</code> first.
     */
    public Json with(Json object, Object...options)
    {
        Json [] jopts = new Json[options.length];
        for (int i = 0; i < jopts.length; i++)
            jopts[i] = make(options[i]);
        return with(object, jopts);
    }

	/**
	 * <p>Return the underlying value of this <code>Json</code> entity. The actual value will 
	 * be a Java Boolean, String, Number, Map, List or null. For complex entities (objects
	 * or arrays), the method will perform a deep copy and extra underlying values recursively 
	 * for all nested elements.</p>
	 */
	public Object getValue() { throw unsupportedOperation(); }
	
	/**
	 * <p>Return the boolean value of a boolean <code>Json</code> instance. Call
	 * {@link #isBoolean()} first if you're not sure this instance is indeed a
	 * boolean.</p>
	 */
	public boolean asBoolean() { throw unsupportedOperation(); }
	
	/**
	 * <p>Return the string value of a string <code>Json</code> instance. Call
	 * {@link #isString()} first if you're not sure this instance is indeed a
	 * string.</p>
	 */
	public String asString() { throw unsupportedOperation(); }
	
	/**
	 * <p>Return the integer value of a number <code>Json</code> instance. Call
	 * {@link #isNumber()} first if you're not sure this instance is indeed a
	 * number.</p>
	 */
	public int asInteger() { throw unsupportedOperation(); }

	/**
	 * <p>Return the float value of a float <code>Json</code> instance. Call
	 * {@link #isNumber()} first if you're not sure this instance is indeed a
	 * number.</p>
	 */
	public float asFloat() { throw unsupportedOperation(); }

	/**
	 * <p>Return the double value of a number <code>Json</code> instance. Call
	 * {@link #isNumber()} first if you're not sure this instance is indeed a
	 * number.</p>
	 */
	public double asDouble() { throw unsupportedOperation(); }

	/**
	 * <p>Return the long value of a number <code>Json</code> instance. Call
	 * {@link #isNumber()} first if you're not sure this instance is indeed a
	 * number.</p>
	 */
	public long asLong() { throw unsupportedOperation(); }

	/**
	 * <p>Return the short value of a number <code>Json</code> instance. Call
	 * {@link #isNumber()} first if you're not sure this instance is indeed a
	 * number.</p>
	 */
	public short asShort() { throw unsupportedOperation(); }

	/**
	 * <p>Return the byte value of a number <code>Json</code> instance. Call
	 * {@link #isNumber()} first if you're not sure this instance is indeed a
	 * number.</p>
	 */	
	public byte asByte() { throw unsupportedOperation(); }

	/**
	 * <p>Return the first character of a string <code>Json</code> instance. Call
	 * {@link #isString()} first if you're not sure this instance is indeed a
	 * string.</p>
	 */	
	public char asChar() { throw unsupportedOperation(); }		

	/**
	 * <p>Return a map of the properties of an object <code>Json</code> instance. The map
	 * is a clone of the object and can be modified safely without affecting it. Call
	 * {@link #isObject()} first if you're not sure this instance is indeed a
	 * <code>Json</code> object.</p>
	 */	
	public Map<String, Object> asMap() { throw unsupportedOperation(); }
	
	/**
	 * <p>Return the underlying map of properties of a <code>Json</code> object. The returned
	 * map is the actual object representation so any modifications to it are modifications
	 * of the <code>Json</code> object itself. Call
	 * {@link #isObject()} first if you're not sure this instance is indeed a
	 * <code>Json</code> object.
	 * </p>
	 */
	public Map<String, Json> asJsonMap() { throw unsupportedOperation(); }
	
	/**
	 * <p>Return a list of the elements of a <code>Json</code> array. The list is a clone
	 * of the array and can be modified safely without affecting it. Call
	 * {@link #isArray()} first if you're not sure this instance is indeed a
	 * <code>Json</code> array.
	 * </p>  
	 */
	public List<Object> asList()  { throw unsupportedOperation(); }
	
	/**
	 * <p>Return the underlying {@link List} representation of a <code>Json</code> array.
	 * The returned list is the actual array representation so any modifications to it 
	 * are modifications of the <code>Json</code> array itself. Call
	 * {@link #isArray()} first if you're not sure this instance is indeed a
	 * <code>Json</code> array.
	 * </p>
	 */
	public List<Json> asJsonList() { throw unsupportedOperation(); }

	/**
	 * <p>Return <code>true</code> if this is a <code>Json</code> null entity 
	 * and <code>false</code> otherwise.
	 * </p> 
	 */
	public boolean isNull() { return false; }
	/**
	 * <p>Return <code>true</code> if this is a <code>Json</code> string entity 
	 * and <code>false</code> otherwise.
	 * </p> 
	 */
	public boolean isString() { return false; }	
	/**
	 * <p>Return <code>true</code> if this is a <code>Json</code> number entity 
	 * and <code>false</code> otherwise.
	 * </p> 
	 */
	public boolean isNumber() { return false; }	
	/**
	 * <p>Return <code>true</code> if this is a <code>Json</code> boolean entity 
	 * and <code>false</code> otherwise.
	 * </p> 
	 */
	public boolean isBoolean() { return false;	}	
	/**
	 * <p>Return <code>true</code> if this is a <code>Json</code> array (i.e. list) entity 
	 * and <code>false</code> otherwise.
	 * </p> 
	 */
	public boolean isArray() { return false; }	
	/**
	 * <p>Return <code>true</code> if this is a <code>Json</code> object entity 
	 * and <code>false</code> otherwise.
	 * </p> 
	 */
	public boolean isObject(){ return false; }	
	/**
	 * <p>Return <code>true</code> if this is a <code>Json</code> primitive entity 
	 * (one of string, number or boolean) and <code>false</code> otherwise.
	 * </p> 
	 */
	public boolean isPrimitive() { return isString() || isNumber() || isBoolean(); }
	
	/**
	 * <p>
	 * Json-pad this object as an argument to a callback function.  
	 * </p>
	 * 
	 * @param callback The name of the callback function. Can be null or empty, 
	 * in which case no padding is done.
	 * @return The jsonpadded, stringified version of this object if the <code>callback</code>
	 * is not null or empty, or just the stringified version of the object. 
	 */
	public String pad(String callback)
	{
		return (callback != null && callback.length() > 0) 
			? callback + "(" + toString() + ");"
			: toString();		
	}
	
	//-------------------------------------------------------------------------
	// END OF PUBLIC INTERFACE
	//-------------------------------------------------------------------------

    /**
     * Return an object representing the complete configuration
     * of a merge. The properties of the object represent paths
     * of the JSON structure being merged and the values represent
     * the set of options that apply to each path.
     */
    protected Json collectWithOptions(Json...options)
    {
        Json result = object();
        for (Json opt : options)
        {
            if (opt.isString())
                result.at("", object()).set(opt.asString(), true);
            else
            {
                Json forPaths = opt.at("for", array(""));
                if (!forPaths.isArray())
                    forPaths = array(forPaths);
                for (Json path : forPaths.asJsonList())
                {
                    Json at_path = result.at(path.asString(), object());
                    at_path.set("merge", opt.is("merge", true));
                    at_path.set("dup", opt.is("dup", true));
                    at_path.set("sort", opt.is("sort", true));
                    at_path.set("compareBy", opt.at("compareBy", nil()));
                }
            }
        }
        return result;
    }

	static class NullJson extends Json
	{
		NullJson() {}
		NullJson(Json e) {super(e);}
		
		public Object getValue() { return null; }
		public Json dup() { return new NullJson(); }
		public boolean isNull() { return true; }
		public String toString() { return "null"; }
		public List<Object> asList() { return (List<Object>)Collections.singletonList(null); }
		
		public int hashCode() { return 0; }
		public boolean equals(Object x)
		{
			return x instanceof NullJson;
		}
	}
	
	static NullJson topnull = new NullJson();

	static class BooleanJson extends Json
	{
		boolean val;
		BooleanJson() {}
		BooleanJson(Json e) {super(e);}
		BooleanJson(Boolean val, Json e) { super(e); this.val = val; }
		
		public Object getValue() { return val; }
        public Json dup() { return new BooleanJson(val, null); }		
		public boolean asBoolean() { return val; }		
		public boolean isBoolean() { return true;	}		
		public String toString() { return val ? "true" : "false"; }
		
		@SuppressWarnings("unchecked")
		public List<Object> asList() { return (List<Object>)(List<?>)Collections.singletonList(val); }
		public int hashCode() { return val ? 1 : 0; }
		public boolean equals(Object x)
		{
			return x instanceof BooleanJson && ((BooleanJson)x).val == val;
		}		
	}

	static class StringJson extends Json
	{
		String val;

		StringJson() {}
		StringJson(Json e) {super(e);}		
		StringJson(String val, Json e) { super(e); this.val = val; }
		
		public Json dup() { return new StringJson(val, null); }		
		public boolean isString() { return true; }
		public Object getValue() { return val; }
		public String asString() { return val; }
		public int asInteger() { return Integer.parseInt(val); }
		public float asFloat() { return Float.parseFloat(val); }
		public double asDouble() { return Double.parseDouble(val); }
		public long asLong() { return Long.parseLong(val); }
		public short asShort() { return Short.parseShort(val); }
		public byte asByte() { return Byte.parseByte(val); }
		public char asChar() { return val.charAt(0); }
		@SuppressWarnings("unchecked")
		public List<Object> asList() { return (List<Object>)(List<?>)Collections.singletonList(val); }
		
		public String toString()
		{
			return '"' + escaper.escapeJsonString(val) + '"'; 
		}
		public String toString(int maxCharacters) 
		{
			if (val.length() <= maxCharacters)
				return toString();
			else
				return '"' + escaper.escapeJsonString(val.subSequence(0,  maxCharacters)) + "...\"";
		}

        public int hashCode() { return val.hashCode(); }
		public boolean equals(Object x)
		{			
			return x instanceof StringJson && ((StringJson)x).val.equals(val); 
		}		
	}

	static class NumberJson extends Json
	{
		Number val;

		NumberJson() {}
		NumberJson(Json e) {super(e);}		
		NumberJson(Number val, Json e) { super(e); this.val = val; }
		
		public Json dup() { return new NumberJson(val, null); }		
		public boolean isNumber() { return true; }		
		public Object getValue() { return val; }
		public String asString() { return val.toString(); }
		public int asInteger() { return val.intValue(); }
		public float asFloat() { return val.floatValue(); }
		public double asDouble() { return val.doubleValue(); }
		public long asLong() { return val.longValue(); }
		public short asShort() { return val.shortValue(); }
		public byte asByte() { return val.byteValue(); }

		@SuppressWarnings("unchecked")
		public List<Object> asList() { return (List<Object>)(List<?>)Collections.singletonList(val); }
		
		public String toString() { return val.toString(); }
		public int hashCode() { return val.hashCode(); }
		public boolean equals(Object x)
		{			
			return x instanceof NumberJson && val.doubleValue() == ((NumberJson)x).val.doubleValue(); 
		}				
	}
	
	static class ArrayJson extends Json
	{
		List<Json> L = new ArrayList<Json>();
		
		ArrayJson() { }
		ArrayJson(Json e) { super(e); }
		

        public Json dup() 
        { 
            ArrayJson j = new ArrayJson();
            for (Json e : L)
            {
                Json v = e.dup();
                v.enclosing = j;
                j.L.add(v);
            }
            return j;
        }
        
        public Json set(int index, Object value) 
        { 
        	L.set(index, make(value));
        	return this;
        }
        
		public List<Json> asJsonList() { return L; }
		public List<Object> asList() 
		{
			ArrayList<Object> A = new ArrayList<Object>();
			for (Json x: L)
				A.add(x.getValue());
			return A; 
		}
        public boolean is(int index, Object value) 
        { 
            if (index < 0 || index >= L.size())
                return false;
            else
                return L.get(index).equals(make(value));
        }       		
		public Object getValue() { return asList(); }
		public boolean isArray() { return true; }
		public Json at(int index) { return L.get(index); }
		public Json add(Json el) { L.add(el); el.enclosing = this; return this; }
		public Json remove(Json el) { L.remove(el); el.enclosing = null; return this; }

        boolean isEqualJson(Json left, Json right)
        {
            if (left == null)
                return right == null;
            else
                return left.equals(right);
        }

        boolean isEqualJson(Json left, Json right, Json fields)
        {
            if (fields.isNull())
                return left.equals(right);
            else if (fields.isString())
                return isEqualJson(resolvePointer(fields.asString(), left),
                                   resolvePointer(fields.asString(), right));
            else if (fields.isArray())
            {
                for (Json field : fields.asJsonList())
                    if (!isEqualJson(resolvePointer(field.asString(), left),
                            resolvePointer(field.asString(), right)))
                        return false;
                return true;
            }
            else
                throw new MJsonException("Compare by options should be either a property name or an array of property names: " + fields);
        }

        int compareJson(Json left, Json right, Json fields)
        {
            if (fields.isNull())
                return ((Comparable)left.getValue()).compareTo(right.getValue());
            else if (fields.isString())
            {
                Json leftProperty = resolvePointer(fields.asString(), left);
                Json rightProperty = resolvePointer(fields.asString(), right);
                return ((Comparable)leftProperty).compareTo(rightProperty);
            }
            else if (fields.isArray())
            {
                for (Json field : fields.asJsonList())
                {
                    Json leftProperty = resolvePointer(field.asString(), left);
                    Json rightProperty = resolvePointer(field.asString(), right);
                    int result = ((Comparable) leftProperty).compareTo(rightProperty);
                    if (result != 0)
                        return result;
                }
                return 0;
            }
            else
                throw new MJsonException("Compare by options should be either a property name or an array of property names: " + fields);
        }

        Json withOptions(Json array, Json allOptions, String path)
        {
            Json opts = allOptions.at(path, object());
            boolean dup = opts.is("dup", true);
            Json compareBy = opts.at("compareBy", nil());
            if (opts.is("sort", true))
            {
                int thisIndex = 0, thatIndex = 0;
                while (thatIndex < array.asJsonList().size())
                {
                    Json thatElement = array.at(thatIndex);
                    if (thisIndex == L.size())
                    {
                        L.add(dup ? thatElement.dup() : thatElement);
                        thisIndex++;
                        thatIndex++;
                        continue;
                    }
                    int compared = compareJson(at(thisIndex), thatElement, compareBy);
                    if (compared < 0) // this < that
                        thisIndex++;
                    else if (compared > 0) // this > that
                    {
                        L.add(thisIndex, dup ? thatElement.dup() : thatElement);
                        thatIndex++;
                    } else { // equal, ignore 
                        thatIndex++;
                    }
                }
            }
            else
            {
                for (Json thatElement : array.asJsonList())
                {
                    boolean present = false;
                    for (Json thisElement : L)
                        if (isEqualJson(thisElement, thatElement, compareBy))
                        {
                            present = true;
                            break;
                        }
                    if (!present)
                        L.add(dup ? thatElement.dup() : thatElement);
                }
            }
            return this;
        }

		public Json with(Json object, Json...options)
		{
			if (object == null) return this;
			if (!object.isArray())
				add(object);
            else if (options.length > 0)
            {
                Json O = collectWithOptions(options);
                return withOptions(object, O, "");
            }
			else
				// what about "enclosing" here? we don't have a provision where a Json 
				// element belongs to more than one enclosing elements...
				L.addAll(((ArrayJson)object).L);
			return this;
		}
		
		public Json atDel(int index) 
		{ 
			Json el = L.remove(index); 
			if (el != null) 
				el.enclosing = null; 
			return el; 
		}
		
		public Json delAt(int index) 
		{ 
			Json el = L.remove(index); 
			if (el != null) 
				el.enclosing = null; 
			return this; 
		}
		
		public String toString()
		{
			return toString(Integer.MAX_VALUE);
		}
		
		public String toString(int maxCharacters) 
		{
			StringBuilder sb = new StringBuilder("[");
			for (Iterator<Json> i = L.iterator(); i.hasNext(); )
			{
				String s = i.next().toString(maxCharacters);
				if (sb.length() + s.length() > maxCharacters)
					s = s.substring(0, Math.max(0, maxCharacters - sb.length()));
				else
					sb.append(s);
				if (i.hasNext())
					sb.append(",");
				if (sb.length() >= maxCharacters)
				{
					sb.append("...");
					break;
				}
			}			
			sb.append("]");
			return sb.toString();			
		}
		
		public int hashCode() { return L.hashCode(); }
		public boolean equals(Object x)
		{			
			return x instanceof ArrayJson && ((ArrayJson)x).L.equals(L); 
		}		
	}
	
	static class ObjectJson extends Json
	{
		Map<String, Json> object = new HashMap<String, Json>();
		
		ObjectJson() { }
		ObjectJson(Json e) { super(e); }

		public Json dup() 
		{ 
		    ObjectJson j = new ObjectJson();
		    for (Map.Entry<String, Json> e : object.entrySet())
		    {
		        Json v = e.getValue().dup();
		        v.enclosing = j;
		        j.object.put(e.getKey(), v);
		    }
		    return j;
		}
		
		public boolean has(String property)
		{
			return object.containsKey(property);
		}
		
		public boolean is(String property, Object value) 
		{ 
		    Json p = object.get(property);
		    if (p == null)
		        return false;
		    else
		        return p.equals(make(value));
		}		
		
		public Json at(String property)
		{
			return object.get(property);
		}

        protected Json withOptions(Json other, Json allOptions, String path)
        {
            Json options = allOptions.at(path, object());
            boolean duplicate = options.is("dup", true);
            if (options.is("merge", true))
            {
                for (Map.Entry<String, Json> e : other.asJsonMap().entrySet())
                {
                    Json local = object.get(e.getKey());
                    if (local instanceof ObjectJson)
                        ((ObjectJson)local).withOptions(e.getValue(), allOptions, path + "/" + e.getKey());
                    else if (local instanceof ArrayJson)
                        ((ArrayJson)local).withOptions(e.getValue(), allOptions, path + "/" + e.getKey());
                    else
                        set(e.getKey(), duplicate ? e.getValue().dup() : e.getValue());
                }
            }
            else if (duplicate)
                for (Map.Entry<String, Json> e : other.asJsonMap().entrySet())
                    set(e.getKey(), e.getValue().dup());
            else
                for (Map.Entry<String, Json> e : other.asJsonMap().entrySet())
                    set(e.getKey(), e.getValue());
            return this;
        }

		public Json with(Json x, Json...options)
		{
			if (x == null) return this;			
			if (!x.isObject())
				throw unsupportedOperation();
            if (options.length > 0)
            {
                Json O = collectWithOptions(options);
                return withOptions(x, O, "");
            }
            else for (Map.Entry<String, Json> e : x.asJsonMap().entrySet())
                set(e.getKey(), e.getValue());
			return this;
		}
		
		public Json set(String property, Json el)
		{
			if (property == null)
				throw new MJsonException("Property names cannot be null, value was " + el);
			el.enclosing = this;
			object.put(property, el);
			return this;
		}

		public Json atDel(String property) 
		{
			Json el = object.remove(property);
			if (el != null)
				el.enclosing = null;
			return el;
		}
		
		public Json delAt(String property) 
		{
			Json el = object.remove(property);
			if (el != null)
				el.enclosing = null;
			return this;
		}
		
		public Object getValue() { return asMap(); }
		public boolean isObject() { return true; }
		public Map<String, Object> asMap() 
		{
			HashMap<String, Object> m = new HashMap<String, Object>();
			for (Map.Entry<String, Json> e : object.entrySet())
				m.put(e.getKey(), e.getValue().getValue());
			return m; 
		}
		@Override
		public Map<String, Json> asJsonMap() { return object; }
		
		public String toString()
		{
			return toString(Integer.MAX_VALUE);
		}
		
		public String toString(int maxCharacters)
		{
			StringBuilder sb = new StringBuilder("{");
			for (Iterator<Map.Entry<String, Json>> i = object.entrySet().iterator(); i.hasNext(); )
			{
				Map.Entry<String, Json> x  = i.next();
				sb.append('"');				
				sb.append(escaper.escapeJsonString(x.getKey()));
				sb.append('"');
				sb.append(":");
				String s = x.getValue().toString(maxCharacters);
				if (sb.length() + s.length() > maxCharacters)
					s = s.substring(0, Math.max(0, maxCharacters - sb.length()));
				sb.append(s);
				if (i.hasNext())
					sb.append(",");
				if (sb.length() >= maxCharacters)
				{
					sb.append("...");
					break;
				}
			}
			sb.append("}");
			return sb.toString();
		}
		public int hashCode() { return object.hashCode(); }
		public boolean equals(Object x)
		{			
			return x instanceof ObjectJson && ((ObjectJson)x).object.equals(object); 
		}				
	}
	
	// ------------------------------------------------------------------------
	// Extra utilities, taken from around the internet:
	// ------------------------------------------------------------------------
	
	/*
	 * Copyright (C) 2008 Google Inc.
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License");
	 * you may not use this file except in compliance with the License.
	 * You may obtain a copy of the License at
	 *
	 * http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 */

	/**
	 * A utility class that is used to perform JSON escaping so that ", <, >, etc. characters are
	 * properly encoded in the JSON string representation before returning to the client code.
	 *
	 * <p>This class contains a single method to escape a passed in string value:
	 * <pre>
	 *   String jsonStringValue = "beforeQuote\"afterQuote";
	 *   String escapedValue = Escaper.escapeJsonString(jsonStringValue);
	 * </pre></p>
	 *
	 * @author Inderjeet Singh
	 * @author Joel Leitch
	 */
	static Escaper escaper = new Escaper(false);
	
	final static class Escaper {

	  private static final char[] HEX_CHARS = {
	    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	  };

	  private static final Set<Character> JS_ESCAPE_CHARS;
	  private static final Set<Character> HTML_ESCAPE_CHARS;

	  static {
	    Set<Character> mandatoryEscapeSet = new HashSet<Character>();
	    mandatoryEscapeSet.add('"');
	    mandatoryEscapeSet.add('\\');
	    JS_ESCAPE_CHARS = Collections.unmodifiableSet(mandatoryEscapeSet);

	    Set<Character> htmlEscapeSet = new HashSet<Character>();
	    htmlEscapeSet.add('<');
	    htmlEscapeSet.add('>');
	    htmlEscapeSet.add('&');
	    htmlEscapeSet.add('=');
	    htmlEscapeSet.add('\'');
//	    htmlEscapeSet.add('/');  -- Removing slash for now since it causes some incompatibilities
	    HTML_ESCAPE_CHARS = Collections.unmodifiableSet(htmlEscapeSet);
	  }

	  private final boolean escapeHtmlCharacters;

	  Escaper(boolean escapeHtmlCharacters) {
	    this.escapeHtmlCharacters = escapeHtmlCharacters;
	  }

	  public String escapeJsonString(CharSequence plainText) {
	    StringBuilder escapedString = new StringBuilder(plainText.length() + 20);
	    try {
	      escapeJsonString(plainText, escapedString);
	    } catch (IOException e) {
	      throw new MJsonException(e);
	    }
	    return escapedString.toString();
	  }

	  private void escapeJsonString(CharSequence plainText, StringBuilder out) throws IOException {
	    int pos = 0;  // Index just past the last char in plainText written to out.
	    int len = plainText.length();

	    for (int charCount, i = 0; i < len; i += charCount) {
	      int codePoint = Character.codePointAt(plainText, i);
	      charCount = Character.charCount(codePoint);

	       if (!isControlCharacter(codePoint) && !mustEscapeCharInJsString(codePoint)) {
	          continue;
	       }

	       out.append(plainText, pos, i);
	       pos = i + charCount;
	       switch (codePoint) {
	         case '\b':
	           out.append("\\b");
	           break;
	         case '\t':
	           out.append("\\t");
	           break;
	         case '\n':
	           out.append("\\n");
	           break;
	         case '\f':
	           out.append("\\f");
	           break;
	         case '\r':
	           out.append("\\r");
	           break;
	         case '\\':
	           out.append("\\\\");
	           break;
	         case '/':
	           out.append("\\/");
	           break;
	         case '"':
	           out.append("\\\"");
	           break;
	         default:
	           appendHexJavaScriptRepresentation(codePoint, out);
	           break;
	       }
	     }
	     out.append(plainText, pos, len);
	  }

	  private boolean mustEscapeCharInJsString(int codepoint) {
	    if (!Character.isSupplementaryCodePoint(codepoint)) {
	      char c = (char) codepoint;
	      return JS_ESCAPE_CHARS.contains(c)
	          || (escapeHtmlCharacters && HTML_ESCAPE_CHARS.contains(c));
	    }
	    return false;
	  }

	  private static boolean isControlCharacter(int codePoint) {
	    // JSON spec defines these code points as control characters, so they must be escaped
	    return codePoint < 0x20
	        || codePoint == 0x2028  // Line separator
	        || codePoint == 0x2029  // Paragraph separator
	        || (codePoint >= 0x7f && codePoint <= 0x9f);
	  }

	  private static void appendHexJavaScriptRepresentation(int codePoint, Appendable out)
	      throws IOException {
	    if (Character.isSupplementaryCodePoint(codePoint)) {
	      // Handle supplementary unicode values which are not representable in
	      // javascript.  We deal with these by escaping them as two 4B sequences
	      // so that they will round-trip properly when sent from java to javascript
	      // and back.
	      char[] surrogates = Character.toChars(codePoint);
	      appendHexJavaScriptRepresentation(surrogates[0], out);
	      appendHexJavaScriptRepresentation(surrogates[1], out);
	      return;
	    }
	    out.append("\\u")
	        .append(HEX_CHARS[(codePoint >>> 12) & 0xf])
	        .append(HEX_CHARS[(codePoint >>> 8) & 0xf])
	        .append(HEX_CHARS[(codePoint >>> 4) & 0xf])
	        .append(HEX_CHARS[codePoint & 0xf]);
	  }
	}	
	
	private static class Reader
	{
	    private static final Object OBJECT_END = new Object();
	    private static final Object ARRAY_END = new Object();
	    private static final Object COLON = new Object();
	    private static final Object COMMA = new Object();
	    public static final int FIRST = 0;
	    public static final int CURRENT = 1;
	    public static final int NEXT = 2;

	    private static Map<Character, Character> escapes = new HashMap<Character, Character>();
	    static 
	    {
	        escapes.put(new Character('"'), new Character('"'));
	        escapes.put(new Character('\\'), new Character('\\'));
	        escapes.put(new Character('/'), new Character('/'));
	        escapes.put(new Character('b'), new Character('\b'));
	        escapes.put(new Character('f'), new Character('\f'));
	        escapes.put(new Character('n'), new Character('\n'));
	        escapes.put(new Character('r'), new Character('\r'));
	        escapes.put(new Character('t'), new Character('\t'));
	    }

	    private CharacterIterator it;
	    private char c;
	    private Object token;
	    private StringBuffer buf = new StringBuffer();

	    private char next() 
	    {
	        if (it.getIndex() == it.getEndIndex())
	            throw new MJsonException("Reached end of input at the " + 
	                                       it.getIndex() + "th character.");
	        c = it.next();
	        return c;
	    }

	    private char previous()
	    {
	    	c = it.previous();
	    	return c;
	    }
	    
	    private void skipWhiteSpace() 
	    {
	        do
	        {
	        	if (Character.isWhitespace(c))
	        		;
	        	else if (c == '/')
	        	{
	        		next();
	        		if (c == '*')
	        		{
	        			// skip multiline comments
	        			while (c != CharacterIterator.DONE)
	        				if (next() == '*' && next() == '/')
	        						break;
	        			if (c == CharacterIterator.DONE)
	        				throw new MJsonException("Unterminated comment while parsing JSON string.");
	        		}
	        		else if (c == '/')
	        			while (c != '\n' && c != CharacterIterator.DONE)
	        				next();
	        		else
	        		{
	        			previous();
	        			break;
	        		}
	        	}
	        	else
	        		break;
	        } while (next() != CharacterIterator.DONE);
	    }

	    public Object read(CharacterIterator ci, int start) 
	    {
	        it = ci;
	        switch (start) 
	        {
	        	case FIRST:
	        		c = it.first();
	        		break;
	        	case CURRENT:
	        		c = it.current();
	        		break;
	        	case NEXT:
	        		c = it.next();
	        		break;
	        }
	        return read();
	    }

	    public Object read(CharacterIterator it) 
	    {
	        return read(it, NEXT);
	    }

	    public Object read(String string) 
	    {
	        return read(new StringCharacterIterator(string), FIRST);
	    }

	    @SuppressWarnings("unchecked")
		private <T> T read() 
	    {
	        skipWhiteSpace();
	        char ch = c;
	        next();
	        switch (ch) 
	        {
	            case '"': token = readString(); break;
	            case '[': token = readArray(); break;
	            case ']': token = ARRAY_END; break;
	            case ',': token = COMMA; break;
	            case '{': token = readObject(); break;
	            case '}': token = OBJECT_END; break;
	            case ':': token = COLON; break;
	            case 't':
	                if (c != 'r' || next() != 'u' || next() != 'e')
	                	throw new MJsonException("Invalid JSON token: expected 'true' keyword.");
	                next();
	                token = factory().bool(Boolean.TRUE);
	                break;
	            case'f':
	                if (c != 'a' || next() != 'l' || next() != 's' || next() != 'e')
	                	throw new MJsonException("Invalid JSON token: expected 'false' keyword.");
	                next();
	                token = factory().bool(Boolean.FALSE);
	                break;
	            case 'n':
	                if (c != 'u' || next() != 'l' || next() != 'l')
	                	throw new MJsonException("Invalid JSON token: expected 'null' keyword.");
	                next();
	                token = nil();
	                break;
	            default:
	                c = it.previous();
	                if (Character.isDigit(c) || c == '-') {
	                    token = readNumber();
	                }
	                else throw new MJsonException("Invalid JSON near position: " + it.getIndex());
	        }
	        return (T)token;
	    }
	    
	    private String readObjectKey()
	    {
	    	Object key = read();
	    	if (key == null)
                throw new MJsonException(
                        "Missing object key (don't forget to put quotes!).");
	    	else if (key != OBJECT_END)
	    		return ((Json)key).asString();
	    	else
	    		return key.toString();
	    }
	    
	    private Json readObject() 
	    {
	        Json ret = object();
	        String key = readObjectKey();
	        while (token != OBJECT_END) 
	        {
	            read(); // should be a colon
	            if (token != OBJECT_END) 
	            {
	            	Json value = read();
	                ret.set(key, value);
	                if (read() == COMMA) {
	                    key = readObjectKey();
	                }
	            }
	        }
	        return ret;
	    }

	    private Json readArray() 
	    {
	        Json ret = array();
	        Object value = read();
	        while (token != ARRAY_END) 
	        {
	            ret.add((Json)value);
	            if (read() == COMMA) 
	                value = read();
	            else if (token != ARRAY_END)
	                throw new MJsonException("Unexpected token in array " + token);
	        }
	        return ret;
	    }

	    private Json readNumber() 
	    {
	        int length = 0;
	        boolean isFloatingPoint = false;
	        buf.setLength(0);
	        
	        if (c == '-') 
	        {
	            add();
	        }
	        length += addDigits();
	        if (c == '.') 
	        {
	            add();
	            length += addDigits();
	            isFloatingPoint = true;
	        }
	        if (c == 'e' || c == 'E') 
	        {
	            add();
	            if (c == '+' || c == '-') 
	            {
	                add();
	            }
	            addDigits();
	            isFloatingPoint = true;
	        }
	 
	        String s = buf.toString();
	        Number n = isFloatingPoint 
	            ? (length < 17) ? Double.valueOf(s) : new BigDecimal(s)
	            : (length < 20) ? Long.valueOf(s) : new BigInteger(s);
	        return factory().number(n);
	    }
	 
	    private int addDigits() 
	    {
	        int ret;
	        for (ret = 0; Character.isDigit(c); ++ret) 
	        {
	            add();
	        }
	        return ret;
	    }

	    private Json readString() 
	    {
	        buf.setLength(0);
	        while (c != '"') 
	        {
	            if (c == '\\') 
	            {
	                next();
	                if (c == 'u') 
	                {
	                    add(unicode());
	                } 
	                else 
	                {
	                    Object value = escapes.get(new Character(c));
	                    if (value != null) 
	                    {
	                        add(((Character) value).charValue());
	                    }
	                }
	            } 
	            else 
	            {
	                add();
	            }
	        }
	        next();
	        return factory().string(buf.toString());
	    }

	    private void add(char cc) 
	    {
	        buf.append(cc);
	        next();
	    }

	    private void add() 
	    {
	        add(c);
	    }

	    private char unicode() 
	    {
	        int value = 0;
	        for (int i = 0; i < 4; ++i) 
	        {
	            switch (next()) 
	            {
	            	case '0': case '1': case '2': case '3': case '4': 
	            	case '5': case '6': case '7': case '8': case '9':
	            		value = (value << 4) + c - '0';
	            		break;
	            	case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
	            		value = (value << 4) + (c - 'a') + 10;
	            		break;
	            	case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
	            		value = (value << 4) + (c - 'A') + 10;
	            		break;
	            }
	        }
	        return (char) value;
	    }
	}
	// END Reader
	
    /**
     * Runtime exception thrown by mJson when something goes awry (parsing, Json typecasting, etc...)
     * @author Taimo Peelo
     * */
    public static class MJsonException extends RuntimeException {
        public MJsonException() {
            super();
        }

        public MJsonException(String message) {
            super(message);
        }

        public MJsonException(String message, Throwable cause) {
            super(message, cause);
        }

        public MJsonException(Throwable cause) {
            super(cause);
        }
    }

    protected static final String JSON_CLASS_SIMPLE_NAME = Json.class.getSimpleName();
    /** Invoked from Json subclasses to indicate unsupported operation call at runtime. */
    protected MJsonException unsupportedOperation() {
    	String jsonElementType = this.getClass().getSimpleName();
    	if (this instanceof Json && jsonElementType.endsWith(JSON_CLASS_SIMPLE_NAME)) {
    		jsonElementType = jsonElementType.substring(0, jsonElementType.length() - JSON_CLASS_SIMPLE_NAME.length());
    	}
    	throw new MJsonException("Unsupported operation on " + jsonElementType + ".");
    }

    public static void main(String []argv)
    {
        System.out.println("JSON main");
    }
}
