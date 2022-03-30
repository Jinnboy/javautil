package tw.com.jinnboy.javautil.util;

import java.util.ArrayList;
import java.util.Hashtable;

import tw.com.jinnboy.javautil.util.autobind.ParamGetter;
import tw.com.jinnboy.javautil.util.autobind.Reflect;

/**
 * 使用方式： 在程式main函式一開始執行Args.load(args)。
 * 
 * 執行Args.get(argName,defaultValue)，來取得參數。
 * Args.get取值順序：優先取runtimeArg，其次找使用者輸入的命令列參數，再檢查Config是否有設定，若都沒設定
 * 最後才用defaultValue。
 * 
 * argName不區分大小寫。
 * 
 * @author 阿昌
 *
 */
public class Args {
	private static Hashtable<String, Object> table = new Hashtable<>(); // 使用者輸入的命令列參數
	private static Hashtable<String, Object> runtimeTable = new Hashtable<>(); // runtimeArg

	private Args() {
	}

	public static void setCategory(String category) {
		Config.setCategory(category);
	}

	public static String getCategory() {
		return Config.getCategory();
	}

	private static boolean isArgValue(String[] args, int position) {
		if (position >= 0 && position < args.length) {
			String arg = args[position];
			return arg != null && arg.length() > 0 && arg.charAt(0) != '-';
		}
		return false;
	}

	public static void load(String[] args) {
		if (args.length == 0) {
			return;
		}
		int i = 0;
		if (Config.containCategory(args[0])) {
			Config.setCategory(args[0]);
			i++;
		}
		String argName, argValue;
		for (; i < args.length; i++) {
			argName = args[i];
			if (argName != null && argName.length() > 0) {
				if (argName.charAt(0) == '-') {
					argName = argName.substring(1);
					argValue = isArgValue(args, i + 1) ? args[++i] : "1";
				} else {
					argValue = "1";
				}
				addArg(argName, argValue);
			}
		}
	}

	public static void addArg(String argName, String argValue) {
		Object preValue = table.get(argValue);
		if (preValue == null) {
			table.put(argName, argValue);
		} else if (preValue instanceof ArrayList) {
			@SuppressWarnings("unchecked")
			ArrayList<String> list = (ArrayList<String>) preValue;
			list.add(argValue);
		} else {
			ArrayList<String> list = new ArrayList<>(4);
			list.add(preValue.toString());
			list.add(argValue);
			table.put(argName, list);
		}
	}

	// 主要由Batch呼叫使用，處理多個Task使用共同變數的問題。
	// 舉例來說，Task的output都是用Args.get來取得的話，當有多個Task時會有問題，這時可以由Batch依不同時間點設定各Task執行時取得變數的值。
	public static void setRuntimeArg(String argName, String argValue) {
		runtimeTable.put(argName, argValue);
	}

	public static void removeRuntimeArg(String argName) {
		runtimeTable.remove(argName);
	}

	public static void clearRuntimeArgs() {
		runtimeTable.clear();
	}

	// 取得第一個arg，不去檢查Config
	public static String getArg(String argName) {
		Object obj = runtimeTable.get(argName);
		if (obj == null) {
			obj = table.get(argName);
		}
		String value = null;
		if (obj == null) {
		} else if (obj instanceof ArrayList) {
			@SuppressWarnings("unchecked")
			ArrayList<String> list = (ArrayList<String>) obj;
			for (String s : list) {
				if (Strings.notEmpty(s)) {
					value = s;
					break;
				}
			}
		} else {
			value = obj.toString();
		}
		return value;
	}

	// 取得第一個arg，其次取Config，都找不到的話會返回空字串。
	public static String get(String argName) {
		return getByNames(true, argName);
	}

	// 取得第一個arg，其次取Config，都找不到再取defaultValue。
	public static String get(String argName, String defaultValue) {
		String argValue = getByNames(true, argName);
		return Strings.isEmpty(argValue) ? "" : defaultValue;
	}

	// 當有一個變數有縮寫，有不同argNames時，例如「-o、-output」和「-d、-DATA_DATE」，可以用此函式同時檢查和取得結果。
	public static String getByNames(String... argNames) {
		return getByNames(true, argNames);
	}

	// findConfig=true時，當沒輸入arg，會去查找Config有沒有設定，都找不到的話會返回空字串。
	public static String getByNames(boolean findConfig, String... argNames) {
		Object obj;
		String argValue = null;
		for (String argName : argNames) {
			obj = runtimeTable.get(argName);
			if (obj != null) {
				argValue = obj.toString();
				break;
			}
		}
		if (Strings.isEmpty(argValue)) {
			for (String argName : argNames) {
				obj = table.get(argName);
				if (obj == null) {
					continue;
				} else if (obj instanceof ArrayList) {
					@SuppressWarnings("unchecked")
					ArrayList<String> list = (ArrayList<String>) obj;
					for (String s : list) {
						if (Strings.notEmpty(s)) {
							argValue = s;
							break;
						}
					}
					if (Strings.notEmpty(argValue)) {
						break;
					}
				} else {
					argValue = obj.toString();
					break;
				}
			}
		}
		if (findConfig && Strings.isEmpty(argValue)) {
			for (String argName : argNames) {
				argValue = Config.get(argName);
				if (Strings.notEmpty(argValue)) {
					break;
				}
			}
		}
		return Strings.isEmpty(argValue) ? "" : argValue;
	}

	// 如果有多個相同的args，如同時輸入多個-param，可以用函式取得。
	// 此函式只會檢查args，不會檢查Config。
	public static String[] getArgs(String argName) {
		Object value = runtimeTable.get(argName);
		if (value == null) {
			value = table.get(argName);
		}
		if (value == null) {
			String configValue = Config.get(argName);
			return Strings.isEmpty(configValue) ? new String[0] : new String[] { configValue };
		} else if (value instanceof ArrayList) {
			@SuppressWarnings("unchecked")
			ArrayList<String> list = (ArrayList<String>) value;
			return list.toArray(new String[list.size()]);
		} else {
			String str = value.toString();
			return str.length() == 0 ? new String[0] : new String[] { value.toString() };
		}
	}

	public static void autoBind(Object obj) {
		ParamGetter paramGetter = new ParamGetter() {
			@Override
			public String getParam(String... argNames) {
				return Args.getByNames(argNames);
			}
		};
		Reflect.autoBind(paramGetter, obj);
	}

}
