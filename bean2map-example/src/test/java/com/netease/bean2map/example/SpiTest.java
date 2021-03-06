package com.netease.bean2map.example;

import com.netease.bean2map.codec.IMapCodec;
import com.netease.bean2map.codec.MapCodecRegister;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

/**
 * @author aliang
 */
public class SpiTest {
    /**
     * Test if everything is working when sources are present
     */
    @Test
    public void test() {
        IMapCodec<Simple> codec = MapCodecRegister.getCodec(Simple.class);
        Simple simple1 = new Simple();
        simple1.setId("111");
        simple1.setGoods(Collections.singletonMap("bbb", 111));
        simple1.setName("aaa");
        simple1.setRules(Collections.singletonList("test"));
        simple1.setValid(false);
        Map<String, Object> map = codec.code(simple1);
        System.out.println(map);
        Simple simple2 = codec.decode(map);
        System.out.println(codec.decode(map));
        Assert.assertEquals(simple1, simple2);
    }
}
