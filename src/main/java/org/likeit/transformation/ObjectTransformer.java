package org.likeit.transformation;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.likeit.transformation.deserialization.DeserializerProvider;
import org.likeit.transformation.serialization.BeanView;
import org.likeit.transformation.serialization.ChainedSerializer;
import org.likeit.transformation.serialization.DefaultSerializers;
import org.likeit.transformation.serialization.Serializer;
import org.likeit.transformation.serialization.SerializerFactory;
import org.likeit.transformation.serialization.SerializerProvider;
import org.likeit.transformation.stream.JsonWriter;
import org.likeit.transformation.stream.ObjectWriter;

/**
 * TODO utiliser p-e des weakreference pour les class et method, 
 * sinon il pourrait y avoir des fuites de memoires dans les environnements a la osgi (meme si c pas le cas pr nous)
 *
 * TODO utiliser un outputstream au lieu du writer dans le ObjectWriter, ca devrait etre plus rapide
 */
public class ObjectTransformer {
	private final SerializerProvider serializerProvider;
	private final DeserializerProvider deserializerProvider;
	
	private boolean skipNull;
	private boolean htmlSafe;
	
	public ObjectTransformer() {
		serializerProvider = new SerializerProvider(
				DefaultSerializers.createDefaultSerializers(), 
				DefaultSerializers.createDefaultSerializerFactories(), 
				DefaultSerializers.createDefaultDynamicSerializer());
		
		deserializerProvider = new DeserializerProvider();
	}
	
	public ObjectTransformer(SerializerProvider serializerProvider, DeserializerProvider deserializerProvider, boolean skipNull, boolean htmlSafe) {
		this.serializerProvider = serializerProvider;
		this.deserializerProvider = deserializerProvider;
		this.skipNull = skipNull;
		this.htmlSafe = htmlSafe;
	}
	
	public <T> String serialize(T o) throws TransformationException, IOException {
		StringWriter sw = new StringWriter();
		
		serialize(o, o.getClass(), new JsonWriter(sw, skipNull, htmlSafe), new Context(serializerProvider, deserializerProvider, null));
		
		return sw.toString();
	}
	
	public <T> String serialize(T o, Class<? extends BeanView<?>>...withViews) throws TransformationException {
		StringWriter sw = new StringWriter();
		
		serialize(o, o.getClass(), new JsonWriter(sw, skipNull, htmlSafe), new Context(serializerProvider, deserializerProvider, Arrays.asList(withViews)));
		
		return sw.toString();
	}
	
	public <T> void serialize(T o, ObjectWriter writer, Class<? extends BeanView<?>>...withViews) throws TransformationException {
		serialize(o, o.getClass(), writer, new Context(serializerProvider, deserializerProvider, Arrays.asList(withViews)));
	}
	
	public <T> void serialize(T obj, Type type, ObjectWriter writer, Context ctx) throws TransformationException {
		ctx.serialize(obj, type, writer);
	}
	
	public <T> T deserialize(Class<T> clazz, String source) throws TransformationException {
		
		return null;
	}
	
	public boolean isSkipNull() {
		return skipNull;
	}

	public boolean isHtmlSafe() {
		return htmlSafe;
	}

	public static class Builder {
		private final List<Serializer<?>> serializers;
		private boolean withDefaultSerializers;
		private boolean withDefaultSerializerFactories;
		private boolean withDefaultDynamicSerializer;
		private boolean skipNull;
		private boolean htmlSafe;
		
		private final List<SerializerFactory<? extends Serializer<?>>> serializerFactories;
		
		private ChainedSerializer dynaSerializer;
		
		public Builder() {
			serializers = new ArrayList<Serializer<?>>(); 
			serializerFactories = new ArrayList<SerializerFactory<? extends Serializer<?>>>();
			
			withDefaultSerializers = true;
			withDefaultSerializerFactories = true;
			withDefaultDynamicSerializer = true;
		}
		
		public Builder withSerializers(Serializer<?>...serializer) {
			serializers.addAll(Arrays.asList(serializer));
			return this;
		}
		
		public Builder withSerializerFactory(SerializerFactory<? extends Serializer<?>>...factory) {
			serializerFactories.addAll(Arrays.asList(factory));
			return this;
		}
		
		public Builder withDynaSerializer(ChainedSerializer serializer) {
			dynaSerializer = serializer;
			return this;
		}
		
		public Builder withoutDefaultSerializers() {
			withDefaultSerializers = false; return this;
		}
		
		public Builder withoutDefaultSerializerFactories() {
			withDefaultSerializerFactories = false; return this;
		}
		
		public Builder withoutDefaultDynamicSerializer() {
			withDefaultDynamicSerializer = false; return this;
		}

		public Builder setSkipNull(boolean skipNull) {
			this.skipNull = skipNull; return this;
		}

		public Builder setHtmlSafe(boolean htmlSafe) {
			this.htmlSafe = htmlSafe; return this;
		}
		
		public ObjectTransformer create() {
			if ( withDefaultSerializers ) {
				serializers.addAll(DefaultSerializers.createDefaultSerializers());
			}
			
			if ( withDefaultSerializerFactories ) {
				serializerFactories.addAll(DefaultSerializers.createDefaultSerializerFactories());
			}
			
			if ( withDefaultDynamicSerializer ) {
				if ( dynaSerializer != null )
					dynaSerializer.withNext(DefaultSerializers.createDefaultDynamicSerializer());
				else dynaSerializer = DefaultSerializers.createDefaultDynamicSerializer();
			}
			
			return new ObjectTransformer(new SerializerProvider(serializers, serializerFactories, dynaSerializer), new DeserializerProvider(), skipNull, htmlSafe);
		}
	}
}
