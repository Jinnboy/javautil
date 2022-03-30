package tw.com.jinnboy.javautil.util.autobind;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tw.com.jinnboy.javautil.util.Dates;
import tw.com.jinnboy.javautil.util.Log;
import tw.com.jinnboy.javautil.util.Strings;

public class Reflect {

	private Reflect() {
	}

	public static void autoBind(Map<String, String> map, Object obj) {
		ParamGetter paramGetter = new ParamGetter() {
			@Override
			public String getParam(String... paramNames) {
				for (String paramName : paramNames) {
					String value = map.get(paramName);
					if (Strings.notEmpty(value)) {
						return value;
					}
				}
				return null;
			}
		};
		autoBind(paramGetter, obj);
	}

	public static void autoBind(ParamGetter paramGetter, Object obj) {
		Class<?> clazz = obj.getClass();
		List<Field> fields = Reflect.findFields(clazz);
		List<Method> methods = Reflect.findSetterMethods(clazz);
		for (Field field : fields) {
			if (field.isAnnotationPresent(AutoBind.class)) {
				AutoBind argField = field.getAnnotation(AutoBind.class);
				String fieldName = field.getName();
				String[] alias = argField.alias();
				String[] paramNames;
				if (alias.length == 0) {
					paramNames = new String[] { fieldName };
				} else {
					paramNames = new String[alias.length + 1];
					paramNames[0] = fieldName;
					System.arraycopy(alias, 0, paramNames, 1, alias.length);
				}
				String argValue = paramGetter.getParam(paramNames);
				if (Strings.notEmpty(argValue)) {
					Method setterMethod = findMethodByName(methods, "set" + fieldName, "set" + capitalize(fieldName));
					if (setterMethod != null) {
						Reflect.methodSet(setterMethod, obj, argValue);
					} else {
						Reflect.fieldSet(field, obj, argValue);
					}
				}
			}
		}
	}

	public static List<Field> findFields(final Class<?> clazz) {
		ArrayList<Class<?>> classList = new ArrayList<>();
		ArrayList<Field> feildList = new ArrayList<>();
		classList.add(clazz);
		for (int i = 0; i < classList.size(); i++) {
			Class<?> checkClass = classList.get(i);
			for (Field feild : checkClass.getDeclaredFields()) {
				feildList.add(feild);
			}
			Class<?> superclass = checkClass.getSuperclass();
			if (superclass != null && !superclass.equals(Object.class) && !classList.contains(superclass)) {
				classList.add(superclass);
			}
		}
		return feildList;
	}

	public static Field findFieldByName(List<Field> fields, String... names) {
		Field[] findFields = new Field[names.length];
		for (Field field : fields) {
			String fieldName = field.getName();
			for (int i = 0; i < names.length; i++) {
				if (fieldName.equals(names[i]) && findFields[i] == null) {
					findFields[i] = field;
				}
			}
		}
		for (Field findField : findFields) {
			if (findField != null) {
				return findField;
			}
		}
		return null;
	}

	public static List<Method> findMethods(final Class<?> clazz) {
		return _findMethods(clazz, false);
	}

	public static List<Method> findSetterMethods(final Class<?> clazz) {
		return _findMethods(clazz, true);
	}

	private static List<Method> _findMethods(final Class<?> clazz, boolean findSetter) {
		ArrayList<Class<?>> classList = new ArrayList<>();
		ArrayList<Method> methodList = new ArrayList<>();
		HashMap<String, Boolean> map = new HashMap<>(); // 紀錄已放到methodList的method，避免重複加入同名method
		classList.add(clazz);
		for (int i = 0; i < classList.size(); i++) {
			Class<?> checkClass = classList.get(i);
			for (Method method : checkClass.getDeclaredMethods()) {
				String methodName = method.getName();
				if (!findSetter || method.getParameterCount() == 1 && methodName.startsWith("set")) {
					String methodKey = method.toGenericString();
					methodKey = methodKey.substring(methodKey.lastIndexOf('.', methodKey.indexOf('(')) + 1);
					if (map.get(methodKey) == null) {
						methodList.add(method);
						map.put(methodKey, true);
					}
				}
			}
			Class<?>[] interfaces = checkClass.getInterfaces();
			for (Class<?> interfaceClass : interfaces) {
				if (!classList.contains(interfaceClass)) {
					classList.add(interfaceClass);
				}
			}
			Class<?> superclass = checkClass.getSuperclass();
			if (superclass != null && !superclass.equals(Object.class) && !classList.contains(superclass)) {
				classList.add(superclass);
			}
		}
		return methodList;
	}

	public static Method findMethodByName(List<Method> methods, String... names) {
		Method[] findMethods = new Method[names.length];
		for (Method method : methods) {
			String methodName = method.getName();
			for (int i = 0; i < names.length; i++) {
				if (methodName.equals(names[i]) && findMethods[i] == null) {
					findMethods[i] = method;
				}
			}
		}
		for (Method findMethod : findMethods) {
			if (findMethod != null) {
				return findMethod;
			}
		}
		return null;
	}

	public static void methodSet(Method method, Object obj, Object value) {
		try {
			if (!method.isAccessible()) {
				method.setAccessible(true);
			}
			method.invoke(obj, convert(value, method.getParameterTypes()[0]));
		} catch (IllegalAccessException e) {
			Log.error(e, "methodSet failed. method=%s, object=%s, value=%s", method, obj.getClass().getSimpleName(),
					value);
		} catch (IllegalArgumentException e) {
			Log.error(e, "methodSet failed. method=%s, object=%s, value=%s", method, obj.getClass().getSimpleName(),
					value);
		} catch (InvocationTargetException e) {
			Log.error(e, "methodSet failed. method='%s', object=%s, value=%s", method, obj.getClass().getSimpleName(),
					value);
		}
	}

	public static void fieldSet(Field field, Object obj, Object value) {
		try {
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			field.set(obj, convert(value, field.getType()));
		} catch (IllegalArgumentException e) {
			Log.error(e, "fieldSet failed. field='%s', object=%s, value=%s", field, obj.getClass().getSimpleName(),
					value);
		} catch (IllegalAccessException e) {
			Log.error(e, "fieldSet failed. field='%s', object=%s, value=%s", field, obj.getClass().getSimpleName(),
					value);
		}
	}

	// 嘗試針對常見的類型做轉換，否則返回value本身。
	public static Object convert(Object value, Class<?> type) {
		try {
			if (value == null) {
				return type == boolean.class ? false : type.isPrimitive() ? 0 : null;
			}
			Class<?> valueType = value.getClass();
			// String要判斷內容(null字串或其它內容)，先不要return String。
			if (valueType != String.class && (valueType == type || type.isAssignableFrom(valueType))) {
				return value;
			}
			if (value instanceof Number) {
				if (type == int.class || type == Integer.class) {
					return ((Number) value).intValue();
				} else if (type == long.class || type == Long.class) {
					return ((Number) value).longValue();
				} else if (type == short.class || type == Short.class) {
					return ((Number) value).shortValue();
				} else if (type == double.class || type == Double.class) {
					return ((Number) value).doubleValue();
				} else if (type == float.class || type == Float.class) {
					return ((Number) value).floatValue();
				} else if (type == byte.class || type == Byte.class) {
					return ((Number) value).byteValue();
				} else if (type == boolean.class || type == Boolean.class) {
					return ((Number) value).intValue() != 0;
				} else if (type == char.class || type == Character.class) {
					return (char) (((Number) value).intValue());
				}
			}
			if (value instanceof String) {
				String str = value.toString();
				if ("null".equalsIgnoreCase(str)) {
					return null;
				}
				if (type == String.class) {
					return value;
				} else if (type == int.class || type == Integer.class) {
					return Integer.parseInt(str);
				} else if (type == long.class || type == Long.class) {
					return Long.parseLong(str);
				} else if (type == short.class || type == Short.class) {
					return Short.parseShort(str);
				} else if (type == double.class || type == Double.class) {
					return Double.parseDouble(str);
				} else if (type == float.class || type == Float.class) {
					return Float.parseFloat(str);
				} else if (type == byte.class || type == Byte.class) {
					return Byte.parseByte(str);
				} else if (type == boolean.class || type == Boolean.class) {
					return "1".equals(str) || "true".equalsIgnoreCase(str);
				} else if (type == char.class || type == Character.class) {
					return str.length() > 0 ? str.charAt(0) : value;
				} else if (Date.class.isAssignableFrom(type)) {
					Date date = Dates.parse(str);
					if (date != null) {
						return date;
					}
				}
			}
		} catch (Exception e) {
		}
		return value;
	}

	/**
	 * modify from java.beans.Introspector
	 * 
	 * 把第1個字變小寫
	 * 
	 * Utility method to take a string and convert it to normal Java variable
	 * name capitalization.  This normally means converting the first
	 * character from upper case to lower case, but in the (unusual) special
	 * case when there is more than one character and both the first and
	 * second characters are upper case, we leave it alone.
	 * <p>
	 * Thus "FooBah" becomes "fooBah" and "X" becomes "x", but "URL" stays
	 * as "URL".
	 *
	 * @param  name The string to be decapitalized.
	 * @return  The decapitalized version of the string.
	 */
	public static String decapitalize(String name) {
		if (name == null || name.length() == 0 || Character.isLowerCase(name.charAt(0))) {
			return name;
		}
		if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
			return name;
		}
		char chars[] = name.toCharArray();
		chars[0] = Character.toLowerCase(chars[0]);
		return new String(chars);
	}

	/**
	 * 把第1個字變大寫
	 * 
	 * @param  name The string to be capitalized.
	 * @return  The capitalized version of the string.
	 */
	public static String capitalize(String name) {
		if (name == null || name.length() == 0 || Character.isUpperCase(name.charAt(0))) {
			return name;
		}
		char chars[] = name.toCharArray();
		chars[0] = Character.toUpperCase(chars[0]);
		return new String(chars);
	}
}
