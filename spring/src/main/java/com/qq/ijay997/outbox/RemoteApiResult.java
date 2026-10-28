package com.qq.ijay997.outbox;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 远端接口调用结果。
 *
 * @author ijay997
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RemoteApiResult {

    private boolean success;

    private String error;

    public static RemoteApiResult success() {
        return new RemoteApiResult(true, null);
    }

    public static RemoteApiResult failure(String error) {
        return new RemoteApiResult(false, error);
    }
}