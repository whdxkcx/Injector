package Injector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;







public class Injector {
     //已生成的单例实例放在这里，后续的注入可以直接获取，然后注，存储形式是，key值为class对象，value值为类实例化后的对象的引用
	private Map<Class<?>,Object>  singletons=Collections.synchronizedMap(new HashMap<>());
     
	//非静态语句块，只会在类实例化时在构造函数之前执行,作用时把当前对象放入singltons中
	{
		singletons.put(Injector.class, this);
	}
	
	//已生成的限定器实例放在这里，可连续注入就直接在这里拿，
	//限定器就是在单例基础上加了一个限定，把单例类进行了分类，根据注释来分类,这里使用的就是Named(),名称限定器
	private  Map<Class<?>,Map<Annotation,Object>> qualifieds=Collections.synchronizedMap(new HashMap<>());
	
	//尚未初始化的单例类也放到一个Map容器上
	private Map<Class<?>, Class<?>>  singletonClasses =Collections.synchronizedMap(new HashMap<>());
	
	//尚未初始化的限定器类别的类也放到一个Map容器上
	private Map<Class<?>,Map<Annotation,Class<?>>> qualifiedClasses=Collections.synchronizedMap(new HashMap<>());
	
	
	
	//第一步，入口函数，获取要注入的限定器类的单例类实例，通过getAnnotations获取类的注释数组，
	//遍历这个类的注释数组，如果某个注释是限定器类的注释，就调用registerQualified 方法来注释一下这个。
	public <T> Injector registerQualifiedClass(Class<?> prantType,Class<T> clazz) throws Exception  {
		for(Annotation anno:clazz.getAnnotations()) {
			if(anno.annotationType().isAnnotationPresent(Qualifier.class)) {
				return this.registerQualifiedClass(prantType, anno,clazz);
			}
		}
		//如果没有限定器类的注释，就抛出异常
     throw new Exception();
	}
	
	
	//获取注释之后，先判断调用这个方法的 注释是不是限定器注释，如果不是则抛出异常，这是一个好习惯，避免程序员错用了方法而不知道
	//然后尝试从存放尚未初始化的限定器类实例的map中取出这个类对应的值
	//如果取出的值为空，说明这个map里还没有这个类。就创建一个这个类的map1并把它put到map里
	//如果这个map已经有了这个注释对应的类，就抛出异常，表示不能重复创建某个类的同一个限定器的子类。
	public <T> Injector registerQualifiedClass(Class<?> parentType,Annotation anno,Class<T> clazz) throws Exception {
		if(!anno.annotationType().isAnnotationPresent(Qualifier.class)) {
			//抛出一个异常
			throw new Exception();
		}
		
		var annos=qualifiedClasses.get(parentType);
		if(annos==null) {
			annos=Collections.synchronizedMap(new HashMap<>());
			qualifiedClasses.put(parentType, annos);
		}
		//如果annos已经包含了这个注解的类对象，那么就会返回这个注解对应的value值，
		//那么说明这个限定器注解已经作用在一个类上了，此时是作用在第二个注解上，那么自然要报错
		//如果这个annos并没有包含这个注解，那么put函数返回的就是null
		if(annos.put(anno, clazz)!=null) {
			//已经包含了这个限定器注释的子类，就不能重复定义，所以抛出异常
			throw new Exception();
		}
		return this;
	}
	
	
	//第三步：通过Injector的getInstance（）来创建一个类实例，即获取对象，并且会自动注入。
	//这个方法会调用createNew（）方法来创建一个实例。
	public <T> T  getInstance(Class<T> clazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return createNew(clazz);
	}
	
	//第四步：调用createNew(Class<T> clazz,Consumer<T>  consumer)方法来创建一个实例。
	
	public <T> T createNew(Class<T> clazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return this.createNew(clazz,null);
	}
	
	
	//第五步：调用创建实例的方法，重载createNew方法，传入两个参数，一个是要创建的对象的class对象，一个是用于函数式编程的一个容器Cosuer对象
	//首先从存储已经生成的单实例对象的容器里取出class对象对应的实例，如果实例不为空直接返回实例，如果实例为空，那么就要利用参数中的class对象来反射出一个实例。
	//利用反射，构建对象，首先要获取这个类的所有构造方法，放在一个list中。调用getDeclaredConstructors方法获得。
	//若构造函数包含@inject注解，并且构造函数是有参构造函数就直接跳过。
	//若是不可见的也跳过。
	//否则就加入到list数组中。  
	//如果list数组的元素个数等于0或者大于1则抛出异常。
	//如果list数组的大小为1，即满足要求的构造函数只有一个，则把这个构造器作为参数，调用createFromConstructor方法来新建一个实例。
	//如果这个类没用被@singleton注释，那么就判断尚未初始化的单例里是否有这个类
	//如果这个类被@singleton注释了，或者尚未初始化的单例里存储了这类，那么就把这个类的calss对象和实例放入存储实例化的单例的map里（singletons）。
	//如果consumer不等于null，就调用cosumer的accept函数来接收这个类。
	//然后调用injectMembers(target)方法来给这个类的成员变量做依赖注入。
	//最后返回这个对象
	@SuppressWarnings("unchecked")
	public <T> T createNew(Class<T> clazz,Consumer<T>  consumer) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    		var o=singletons.get(clazz);
		if(o!=null) {
			return (T)o;
		}
		
		var cons=new ArrayList<Constructor<T>>();
		T target=null;
		for(var con:clazz.getDeclaredConstructors()) {
			if(!con.isAnnotationPresent(Inject.class)&&con.getParameterCount()>0) {
				continue;
			}
			if(!con.trySetAccessible()) {
				continue;
			}
			cons.add((Constructor<T>) con);
		}
		if(cons.size()>1) {
		//抛出异常，表明对于这个注入的类有多个构造器符合要求。
		
			return null;
		}
		if(cons.size()==0) {
			//抛出异常，表明没有构造器符合要求。
	         return  null;
		}
		
		target=createFromConstructor(cons.get(0));//构造器注入
		var isSingleton=clazz.isAnnotationPresent(Singleton.class);
		if(!isSingleton) {
			isSingleton=this.singletonClasses.containsKey(clazz);
		}
		if(isSingleton) {
			singletons.put(clazz, target);
		}
		
		if(consumer!=null) {
			consumer.accept(target);
		}
		
		injectMembers(target);
		
		return target;
	}
	

	
	//第六步：通过构造函数生成对象实例
	//生成一个object数组来存储构造函数所有的参数。
	//通过Constructor的getParameters方法来获取所有的参数。参数是Parameter类型。
	//通过createFromParameter方法来创建参数的实例对象来注入，并把这些对象实例存储到前面定义的Object数组中。
	//调用Constructor的newInstance方法来创建实例，以前面定义的Object数组作为参数数组。
	public <T> T createFromConstructor(Constructor<T> con) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var params=new Object[con.getParameterCount()];
		var i=0;
		for(Parameter param:con.getParameters()){
			var paramO=createFromParameter(param);
			if(paramO==null) {
				//抛出异常
				return null;
			}
			params[i++]=paramO;
		}
			return con.newInstance(params);
	}	        
	
	
	//第八步：通过参数对象来创建这个参数的实例
	//首先通过Parameter的getType方法来获取这个参数的类型信息，即RTTI,运行时类型信息。
	//调用createFromQualified方法来获取这个类的
	@SuppressWarnings("unchecked")
	public <T> T createFromParameter(Parameter param) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var clazz=param.getType();
		T t=createFromQualified(param.getDeclaringExecutable().getDeclaringClass(),
				clazz,param.getAnnotations());
		if(t!=null) {
			return t;
		}
		return (T)createNew(clazz);
	}
	//第九步:从容器中创建这个类的实例。
	@SuppressWarnings("unchecked")
	public <T> T createFromQualified(Class<?> declaringClass,Class<?> clazz,Annotation[] annos) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var qs=qualifieds.get(clazz);//首先看先前是否已经实例化过这个类的子类，如果有直接返回缓存的注释和类对象容器。
		if(qs!=null) {
			Set<Object> os=new HashSet<>();//用来存放相同指定的限定器注释对应的对象
			for(var anno:annos) {
				var o=qs.get(anno);
				if(o!=null) {
					os.add(o);
				}
			}
			//一个类只能指定至多一个限定器
			if(os.size()>1) {
				//抛出异常，该类有多个限定器
				return null;
			}
			if(!os.isEmpty()) {//如果有，则直接返回这个对象。
				return (T)(os.iterator().next());
			}
		}
		//如果没有已经实例化的限定器对象，那么就从容器中找有没有为实例化的calss对象
		var qz=qualifiedClasses.get(clazz);
		if(qz!=null) {
			Set<Class<?>> oz=new HashSet<>();
			Annotation annoz=null;
			for(var anno:annos) {
				var z=qz.get(anno);
				if(z!=null) {
					oz.add(z);
					annoz=anno;
				}
			}
			//同样，一个类只能有一个限定器
			if(oz.size()>1) {
				//抛出异常
				return null;
			}
			if(!oz.isEmpty()) {
				final var annoR=annoz;
				var t=(T) createNew(oz.iterator().next(),(o)->{
					this.registerQualified((Class<T>) clazz,annoR,(T) o);
				});
				return t;
			}
		}
		return null;
	}
	

	//第十步：  注册，即缓存一个类，注释和对象。
	public <T> Injector registerQualified(Class<T> clazz,Annotation anno,T o) {
		if(!anno.annotationType().isAnnotationPresent(Qualifier.class)) {
			//如果这个注释不是限定器注释则抛出异常
			return this;
		}
		var os=qualifieds.get(clazz);
		if(os==null) {
			os=Collections.synchronizedMap(new HashMap<>());
			qualifieds.put(clazz, os);
		}
		if(os.put(anno, o)!=null) {
			//如果不等于null,代表同一个限定器，用于两个不同的限定器对象上了，则抛出异常
			return this;
		}
		return this;
	}
	
	//第七步：注入成员
	public <T> void  injectMembers(T t) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Field> fields=new ArrayList<>();
		for(Field field:t.getClass().getDeclaredFields()) {
			if(field.isAnnotationPresent(Inject.class)&&field.trySetAccessible()) {
				fields.add(field);//筛选掉不能注入或者不可见的元素
			}
		}
		for(Field field:fields) {
			Object f=createFromField(field);
			field.set(t, f);
		}
	}
	
	
	//第十一步，创建成员变量对应的一个类。
	@SuppressWarnings("unchecked")
	private <T> T createFromField(Field field) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var clazz=field.getType();
		T t=createFromQualified(field.getDeclaringClass(),field.getType(),field.getAnnotations());
		if(t!=null) {
		return t;
		}
		return (T) createNew(clazz);
	}
	
	//第十二步：注册一个类对象
	public <T> Injector registerSingletonClass(Class<T> clazz) {
		 return this.registerSingletonClass(clazz,clazz);
	}
	
	//第十三步：注册一个类对象
	public <T> Injector registerSingletonClass(Class<?> parentType,Class<T> clazz) {
		if(singletonClasses.put(parentType,clazz)!=null) {
//			抛出异常
			return this;
		}
		return this;
	}
}
