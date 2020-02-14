package com.intellij.uiDesigner.core;

class Pair<T, E> {
    private T t;
    private E e;

    Pair(T t, E e) {
        this.t = t;
        this.e = e;
    }

    T getT() {
        return this.t;
    }

    E getE() {
        return this.e;
    }

    void setT(T t) {
        this.t  = t;
    }

    void setE(E e) {
        this.e = e;
    }
}
