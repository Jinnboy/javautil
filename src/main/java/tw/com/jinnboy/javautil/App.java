package tw.com.jinnboy.javautil;

import tw.com.jinnboy.javautil.test_entity.Person;
import tw.com.jinnboy.javautil.util.Args;

/**
 * JavaUtil
 * 
 * @author 阿昌
 */
public class App {
	
	//測試
	public static void main(String[] args) {
		Args.load(args);
		Person person = new Person();
		Args.autoBind(person);
		System.out.println(person);
	}
}
