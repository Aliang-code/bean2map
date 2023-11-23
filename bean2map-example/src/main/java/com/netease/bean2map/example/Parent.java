package com.netease.bean2map.example;

import com.netease.bean2map.codec.DateFormat;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class Parent {
    private String id;
    private Long name;
    private Map<String,Object> goods;
    @DateFormat(pattern = "yyyy-MM-dd", timestamp = true)
    private Date createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getName() {
        return name;
    }

    public void setName(Long name) {
        this.name = name;
    }

    public Map<String, Object> getGoods() {
        return goods;
    }

    public void setGoods(Map<String, Object> goods) {
        this.goods = goods;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Parent)) return false;

        Parent parent = (Parent) o;

        if (!Objects.equals(id, parent.id)) return false;
        return Objects.equals(goods, parent.goods);
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (goods != null ? goods.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Parent{" +
                "id='" + id + '\'' +
                ", goods=" + goods +
                '}';
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
