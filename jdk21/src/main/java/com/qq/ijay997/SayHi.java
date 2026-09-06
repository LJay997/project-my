package com.qq.ijay997;

import java.util.ArrayList;
import java.util.stream.Stream;

public interface SayHi {
    private String buildMessage() {
        return "Hello";
    }
    void sayHi(final String message);
    default void sayHi() {
        var strings = new ArrayList<>();
        Stream<Object> stream = strings.stream();
        sayHi(buildMessage());
    }
}