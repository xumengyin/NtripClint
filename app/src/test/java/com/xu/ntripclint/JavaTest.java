package com.xu.ntripclint;


import com.xu.jniserialport.UtilityTools;

import org.junit.Test;

public class JavaTest {

    @Test
    public void test()
    {
        System.out.println("---"+ UtilityTools.byteToHex(10));
        System.out.println("---"+UtilityTools.byteToHex(100));
        System.out.println("---"+UtilityTools.byteToHex(2));

    }
}
