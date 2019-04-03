package com.skloda.netty.serializer;

import java.io.Serializable;

/**
 * @Author: jiangkun
 * @Description:
 * @Date: Created in 2019-04-03 10:23
 */
public class User implements Serializable {

    private int age;
    private String name;

    public User(int age, String name) {
        this.age = age;
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "User{" +
                "age=" + age +
                ", name='" + name + '\'' +
                '}';
    }
}
