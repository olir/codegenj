/*
 * Copyright 2016 Oliver Rode
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.serviceflow.codegenj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

/**
 * D-Bus Introspection Data Format.
 * 
 * 
 * Objects may be introspected at runtime, returning an XML string that
 * describes the object. The same XML format may be used in other contexts as 
 * well, for example as an "IDL" for generating static language bindings.
 * 
 * Only the root <node> element can omit the node name, as it's known to be the
 * object that was introspected. If the root <node> does have a name attribute,
 * it must be an absolute object path. If child <node> have object paths, they
 * must be relative.
 * 
 * If a child <node> has any sub-elements, then they must represent a complete
 * introspection of the child. If a child <node> is empty, then it may or may
 * not have sub-elements; the child must be introspected in order to find out.
 * The intent is that if an object knows that its children are "fast" to
 * introspect it can go ahead and return their information, but otherwise it can
 * omit it.
 * 
 * The direction element on <arg> may be omitted, in which case it defaults to
 * "in" for method calls and "out" for signals. Signals only allow "out" so
 * while direction may be specified, it's pointless.
 * 
 * The possible directions are "in" and "out", unlike CORBA there is no "inout"
 * 
 * The possible property access flags are "readwrite", "read", and "write"
 * 
 * Multiple interfaces can of course be listed for one <node>.
 * 
 * The "name" attribute on arguments is optional.
 * 
 * Method, interface, property, and signal elements may have "annotations",
 * which are generic key/value pairs of metadata. They are similar conceptually
 * to Java's annotations and C# attributes.
 * 
 * @see https ://dbus.freedesktop.org/doc/dbus-specification.html
 */

/**
 * Root element of Unmashall structure, the object that was introspected. This
 * stucture is filled by JAX-B parser.
 */
@XmlRootElement
public class Node extends ProcessingObjective {


	@XmlAttribute(name = "name")
	private String name;

	@XmlElement(name = "interface")
	private List<Node.Interface> interfaces;

	@XmlElement(name = "node")
	private List<Node> childs;

	public String getName() {
		return name;
	}

	public List<Node.Interface> getInterfaces() {
		return interfaces != null ? interfaces : new ArrayList<Node.Interface>();
	}

	public List<Node> getChilds() {
		return childs != null ? childs : new ArrayList<Node>();
	}

	private Map<Interface,String> interfaceCollectorMap = new HashMap<Interface,String>();

	public Map<Interface,String> getInterfaceCollectorMap() {
		return interfaceCollectorMap;
	}

	
	private Iterator<Interface> i_node_interface = null;
	
	@Override
	public ProcessingObjective getProcessingObjective(String name2) {
		if ("interface".equals(name2))
			return i_node_interface.next();
		return null;
	}

	@Override
	public void startScan() {
		i_node_interface = getInterfaces().iterator();	
	}

	@Override
	public void endScan() {
		i_node_interface = null;
	}
	
	/**
	 * An interface to invoke a method call on, or that a signal is emitted
	 * from.
	 */
	public static class Interface extends ProcessingObjective {

		@XmlElement(name = "method")
		private List<Interface.Method> methods;

		@XmlElement(name = "signal")
		private List<Interface.Signal> signals;

		@XmlElement(name = "property")
		private List<Interface.Property> properties;

		@XmlElement(name = "annotation")
		private List<Member.Annotation> annotations;
		
		@XmlAttribute(name = "name")
		private String name;

		public String getName() {
			return name;
		}

		public List<Interface.Method> getMethods() {
			return methods != null ? methods : new ArrayList<Interface.Method>();
		}

		public List<Interface.Signal> getSignals() {
			return signals != null ? signals : new ArrayList<Interface.Signal>();
		}

		public List<Interface.Property> getProperties() {
			return properties != null ? properties : new ArrayList<Interface.Property>();
		}
		
		public List<Member.Annotation> getAnnotations() {
			return annotations != null ? annotations : new ArrayList<Member.Annotation>();
		}

		
		private Map<Interface,String> interfaceCollectorMap = new HashMap<Interface,String>();

		public Map<Interface,String> getInterfaceCollectorMap() {
			return interfaceCollectorMap;
		}

		
		private Iterator<Method> i_interface_method = null;
		private Iterator<Property> i_interface_property = null;
		
		@Override
		public ProcessingObjective getProcessingObjective(String name2) {
			if ("method".equals(name2))
				return i_interface_method.next();
			else if ("property".equals(name2))
				return i_interface_property.next();
			return null;
		}

		@Override
		public void startScan() {
			i_interface_method = getMethods().iterator();
			i_interface_property = getProperties().iterator();
		}

		@Override
		public void endScan() {
			i_interface_method = null;
			i_interface_property = null;
		}

		
		
		
		

		/**
		 * child element
		 */
		public static class Method extends Member {

			@Override
			public ProcessingObjective getProcessingObjective(String name2) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void startScan() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void endScan() {
				// TODO Auto-generated method stub
				
			}
		}

		/**
		 * child element
		 */
		public static class Signal extends Member {

			@Override
			public ProcessingObjective getProcessingObjective(String name2) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void startScan() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void endScan() {
				// TODO Auto-generated method stub
				
			}
		}

		/**
		 * child element
		 */
		public static abstract class Member extends ProcessingObjective {
			@XmlAttribute(name = "name")
			private String name;

			public String getName() {
				return name;
			}

			@XmlElement(name = "arg")
			private List<Member.Arg> args;

			public List<Member.Arg> getArgs() {
				return args != null ? args : new ArrayList<Member.Arg>();
			}

			@XmlElement(name = "annotation")
			private List<Member.Annotation> annotations;

			public List<Member.Annotation> getAnnotations() {
				return annotations != null ? annotations : new ArrayList<Member.Annotation>();
			}

			/**
			 * child element
			 */
			public static class Arg extends ProcessingObjective {
				@XmlAttribute(name = "name")
				private String name;

				public String getName() {
					return name;
				}

				@XmlAttribute(name = "type")
				private String type;

				public String getType() {
					return type;
				}

				@XmlAttribute(name = "direction")
				private String direction;

				public String getDirection() {
					return direction;
				}

				@Override
				public ProcessingObjective getProcessingObjective(String name2) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void startScan() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void endScan() {
					// TODO Auto-generated method stub
					
				}
			}

			/**
			 * child element
			 */
			public static class Annotation extends ProcessingObjective {
				@XmlAttribute(name = "name")
				private String name;

				public String getName() {
					return name;
				}

				@XmlAttribute(name = "value")
				private String value;

				public String getValue() {
					return value;
				}

				@Override
				public ProcessingObjective getProcessingObjective(String name2) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void startScan() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void endScan() {
					// TODO Auto-generated method stub
					
				}
			}

		}

		/**
		 * child element
		 */
		public static class Property extends ProcessingObjective {
			@XmlAttribute(name = "name")
			private String name;

			public String getName() {
				return name;
			}

			@XmlAttribute(name = "type")
			private String type;

			public String getType() {
				return type;
			}

			@XmlAttribute(name = "access")
			private String access;

			public String getAccess() {
				return access;
			}

			@XmlElement(name = "annotation")
			private List<Member.Annotation> annotations;

			public List<Member.Annotation> getAnnotations() {
				return annotations != null ? annotations : new ArrayList<Member.Annotation>();
			}


			@Override
			public void startScan() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void endScan() {
				// TODO Auto-generated method stub
				
			}


			@Override
			public ProcessingObjective getProcessingObjective(String name2) {
				// TODO Auto-generated method stub
				return null;
			}
		}


	}

}