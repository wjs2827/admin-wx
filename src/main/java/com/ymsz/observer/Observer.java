package com.ymsz.observer;

/**
 * @author ymsz
 * @description
 * @date 2021/11/9
 * @email jinshan.wang.it@foxmail.com
 */
public interface Observer {
    /**
     * 观察者的行动
     * @param target 发起通知的源类
     * @param args 参数
     */
    void action(Object target, Object... args);
}
