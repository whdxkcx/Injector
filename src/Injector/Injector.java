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
     //�����ɵĵ���ʵ���������������ע�����ֱ�ӻ�ȡ��Ȼ��ע���洢��ʽ�ǣ�keyֵΪclass����valueֵΪ��ʵ������Ķ��������
	private Map<Class<?>,Object>  singletons=Collections.synchronizedMap(new HashMap<>());
     
	//�Ǿ�̬���飬ֻ������ʵ����ʱ�ڹ��캯��֮ǰִ��,����ʱ�ѵ�ǰ�������singltons��
	{
		singletons.put(Injector.class, this);
	}
	
	//�����ɵ��޶���ʵ���������������ע���ֱ���������ã�
	//�޶��������ڵ��������ϼ���һ���޶����ѵ���������˷��࣬����ע��������,����ʹ�õľ���Named(),�����޶���
	private  Map<Class<?>,Map<Annotation,Object>> qualifieds=Collections.synchronizedMap(new HashMap<>());
	
	//��δ��ʼ���ĵ�����Ҳ�ŵ�һ��Map������
	private Map<Class<?>, Class<?>>  singletonClasses =Collections.synchronizedMap(new HashMap<>());
	
	//��δ��ʼ�����޶���������Ҳ�ŵ�һ��Map������
	private Map<Class<?>,Map<Annotation,Class<?>>> qualifiedClasses=Collections.synchronizedMap(new HashMap<>());
	
	
	
	//��һ������ں�������ȡҪע����޶�����ĵ�����ʵ����ͨ��getAnnotations��ȡ���ע�����飬
	//����������ע�����飬���ĳ��ע�����޶������ע�ͣ��͵���registerQualified ������ע��һ�������
	public <T> Injector registerQualifiedClass(Class<?> prantType,Class<T> clazz) throws Exception  {
		for(Annotation anno:clazz.getAnnotations()) {
			if(anno.annotationType().isAnnotationPresent(Qualifier.class)) {
				return this.registerQualifiedClass(prantType, anno,clazz);
			}
		}
		//���û���޶������ע�ͣ����׳��쳣
     throw new Exception();
	}
	
	
	//��ȡע��֮�����жϵ������������ ע���ǲ����޶���ע�ͣ�����������׳��쳣������һ����ϰ�ߣ��������Ա�����˷�������֪��
	//Ȼ���ԴӴ����δ��ʼ�����޶�����ʵ����map��ȡ��������Ӧ��ֵ
	//���ȡ����ֵΪ�գ�˵�����map�ﻹû������ࡣ�ʹ���һ��������map1������put��map��
	//������map�Ѿ��������ע�Ͷ�Ӧ���࣬���׳��쳣����ʾ�����ظ�����ĳ�����ͬһ���޶��������ࡣ
	public <T> Injector registerQualifiedClass(Class<?> parentType,Annotation anno,Class<T> clazz) throws Exception {
		if(!anno.annotationType().isAnnotationPresent(Qualifier.class)) {
			//�׳�һ���쳣
			throw new Exception();
		}
		
		var annos=qualifiedClasses.get(parentType);
		if(annos==null) {
			annos=Collections.synchronizedMap(new HashMap<>());
			qualifiedClasses.put(parentType, annos);
		}
		//���annos�Ѿ����������ע����������ô�ͻ᷵�����ע���Ӧ��valueֵ��
		//��ô˵������޶���ע���Ѿ�������һ�������ˣ���ʱ�������ڵڶ���ע���ϣ���ô��ȻҪ����
		//������annos��û�а������ע�⣬��ôput�������صľ���null
		if(annos.put(anno, clazz)!=null) {
			//�Ѿ�����������޶���ע�͵����࣬�Ͳ����ظ����壬�����׳��쳣
			throw new Exception();
		}
		return this;
	}
	
	
	//��������ͨ��Injector��getInstance����������һ����ʵ��������ȡ���󣬲��һ��Զ�ע�롣
	//������������createNew��������������һ��ʵ����
	public <T> T  getInstance(Class<T> clazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return createNew(clazz);
	}
	
	//���Ĳ�������createNew(Class<T> clazz,Consumer<T>  consumer)����������һ��ʵ����
	
	public <T> T createNew(Class<T> clazz) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return this.createNew(clazz,null);
	}
	
	
	//���岽�����ô���ʵ���ķ���������createNew��������������������һ����Ҫ�����Ķ����class����һ�������ں���ʽ��̵�һ������Cosuer����
	//���ȴӴ洢�Ѿ����ɵĵ�ʵ�������������ȡ��class�����Ӧ��ʵ�������ʵ����Ϊ��ֱ�ӷ���ʵ�������ʵ��Ϊ�գ���ô��Ҫ���ò����е�class�����������һ��ʵ����
	//���÷��䣬������������Ҫ��ȡ���������й��췽��������һ��list�С�����getDeclaredConstructors������á�
	//�����캯������@injectע�⣬���ҹ��캯�����вι��캯����ֱ��������
	//���ǲ��ɼ���Ҳ������
	//����ͼ��뵽list�����С�  
	//���list�����Ԫ�ظ�������0���ߴ���1���׳��쳣��
	//���list����Ĵ�СΪ1��������Ҫ��Ĺ��캯��ֻ��һ������������������Ϊ����������createFromConstructor�������½�һ��ʵ����
	//��������û�ñ�@singletonע�ͣ���ô���ж���δ��ʼ���ĵ������Ƿ��������
	//�������౻@singletonע���ˣ�������δ��ʼ���ĵ�����洢�����࣬��ô�Ͱ�������calss�����ʵ������洢ʵ�����ĵ�����map�singletons����
	//���consumer������null���͵���cosumer��accept��������������ࡣ
	//Ȼ�����injectMembers(target)�������������ĳ�Ա����������ע�롣
	//��󷵻��������
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
		//�׳��쳣�������������ע������ж������������Ҫ��
		
			return null;
		}
		if(cons.size()==0) {
			//�׳��쳣������û�й���������Ҫ��
	         return  null;
		}
		
		target=createFromConstructor(cons.get(0));//������ע��
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
	

	
	//��������ͨ�����캯�����ɶ���ʵ��
	//����һ��object�������洢���캯�����еĲ�����
	//ͨ��Constructor��getParameters��������ȡ���еĲ�����������Parameter���͡�
	//ͨ��createFromParameter����������������ʵ��������ע�룬������Щ����ʵ���洢��ǰ�涨���Object�����С�
	//����Constructor��newInstance����������ʵ������ǰ�涨���Object������Ϊ�������顣
	public <T> T createFromConstructor(Constructor<T> con) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var params=new Object[con.getParameterCount()];
		var i=0;
		for(Parameter param:con.getParameters()){
			var paramO=createFromParameter(param);
			if(paramO==null) {
				//�׳��쳣
				return null;
			}
			params[i++]=paramO;
		}
			return con.newInstance(params);
	}	        
	
	
	//�ڰ˲���ͨ�������������������������ʵ��
	//����ͨ��Parameter��getType��������ȡ���������������Ϣ����RTTI,����ʱ������Ϣ��
	//����createFromQualified��������ȡ������
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
	//�ھŲ�:�������д���������ʵ����
	@SuppressWarnings("unchecked")
	public <T> T createFromQualified(Class<?> declaringClass,Class<?> clazz,Annotation[] annos) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var qs=qualifieds.get(clazz);//���ȿ���ǰ�Ƿ��Ѿ�ʵ���������������࣬�����ֱ�ӷ��ػ����ע�ͺ������������
		if(qs!=null) {
			Set<Object> os=new HashSet<>();//���������ָͬ�����޶���ע�Ͷ�Ӧ�Ķ���
			for(var anno:annos) {
				var o=qs.get(anno);
				if(o!=null) {
					os.add(o);
				}
			}
			//һ����ֻ��ָ������һ���޶���
			if(os.size()>1) {
				//�׳��쳣�������ж���޶���
				return null;
			}
			if(!os.isEmpty()) {//����У���ֱ�ӷ����������
				return (T)(os.iterator().next());
			}
		}
		//���û���Ѿ�ʵ�������޶���������ô�ʹ�����������û��Ϊʵ������calss����
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
			//ͬ����һ����ֻ����һ���޶���
			if(oz.size()>1) {
				//�׳��쳣
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
	

	//��ʮ����  ע�ᣬ������һ���࣬ע�ͺͶ���
	public <T> Injector registerQualified(Class<T> clazz,Annotation anno,T o) {
		if(!anno.annotationType().isAnnotationPresent(Qualifier.class)) {
			//������ע�Ͳ����޶���ע�����׳��쳣
			return this;
		}
		var os=qualifieds.get(clazz);
		if(os==null) {
			os=Collections.synchronizedMap(new HashMap<>());
			qualifieds.put(clazz, os);
		}
		if(os.put(anno, o)!=null) {
			//���������null,����ͬһ���޶���������������ͬ���޶����������ˣ����׳��쳣
			return this;
		}
		return this;
	}
	
	//���߲���ע���Ա
	public <T> void  injectMembers(T t) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Field> fields=new ArrayList<>();
		for(Field field:t.getClass().getDeclaredFields()) {
			if(field.isAnnotationPresent(Inject.class)&&field.trySetAccessible()) {
				fields.add(field);//ɸѡ������ע����߲��ɼ���Ԫ��
			}
		}
		for(Field field:fields) {
			Object f=createFromField(field);
			field.set(t, f);
		}
	}
	
	
	//��ʮһ����������Ա������Ӧ��һ���ࡣ
	@SuppressWarnings("unchecked")
	private <T> T createFromField(Field field) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var clazz=field.getType();
		T t=createFromQualified(field.getDeclaringClass(),field.getType(),field.getAnnotations());
		if(t!=null) {
		return t;
		}
		return (T) createNew(clazz);
	}
	
	//��ʮ������ע��һ�������
	public <T> Injector registerSingletonClass(Class<T> clazz) {
		 return this.registerSingletonClass(clazz,clazz);
	}
	
	//��ʮ������ע��һ�������
	public <T> Injector registerSingletonClass(Class<?> parentType,Class<T> clazz) {
		if(singletonClasses.put(parentType,clazz)!=null) {
//			�׳��쳣
			return this;
		}
		return this;
	}
}
