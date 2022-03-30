package tw.com.jinnboy.javautil.test_entity;

import java.util.Date;

import tw.com.jinnboy.javautil.util.Dates;
import tw.com.jinnboy.javautil.util.autobind.AutoBind;
/**
 * 此類別為主要是demo AutoBind的用法
 *
 */
public class Person {
	@AutoBind(alias = { "n" })
	String name; // 可以輸入-name或-n來設定名字
	@AutoBind
	int age;
	@AutoBind
	Date birthday;
	@AutoBind
	String sex; // 如果變數有設定setXXX，會優先呼叫setXXX進行賦值

	public Person() {
	}

	public void setSex(String s) {
		s = s.toUpperCase();
		switch (s) {
		case "M":
		case "MALEN":
		case "MAN":
		case "男":
			this.sex = "M";
			break;
		case "F":
		case "FEMALE":
		case "WOMAN":
		case "女":
			this.sex = "F";
			break;
		}
	}

	@Override
	public String toString() {
		return String.format("name:%s, age:%s, birthday:%s, sex:%s", name, age, Dates.DateString(birthday), sex);
	}

}
