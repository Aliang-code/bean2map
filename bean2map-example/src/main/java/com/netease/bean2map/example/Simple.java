package com.netease.bean2map.example;

import com.netease.bean2map.codec.MapCodec;

import java.util.List;
import java.util.Objects;

@MapCodec
public class Simple extends Parent {
    private String name;
    private List<String> rules;
    private boolean valid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRules() {
        return rules;
    }

    public void setRules(List<String> rules) {
        this.rules = rules;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Simple)) return false;
        if (!super.equals(o)) return false;
        Simple simple = (Simple) o;
        return valid == simple.valid &&
                Objects.equals(name, simple.name) &&
                Objects.equals(rules, simple.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, rules, valid);
    }

    @Override
    public String toString() {
        return "Simple{" +
                "name='" + name + '\'' +
                ", rules=" + rules +
                ", valid=" + valid +
                '}';
    }
}
