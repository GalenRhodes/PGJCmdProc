package com.projectgalen.lib.cmdproc.test;
// ================================================================================================================================
//     PROJECT: PGJCmdProc
//    FILENAME: Test.java
//         IDE: IntelliJ IDEA
//      AUTHOR: Galen Rhodes
//        DATE: May 09, 2024
//
// Copyright © 2024 Project Galen. All rights reserved.
//
// Permission to use, copy, modify, and distribute this software for any purpose with or without fee is hereby granted, provided
// that the above copyright notice and this permission notice appear in all copies.
//
// THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR
// CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
// NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
// ================================================================================================================================

import com.projectgalen.lib.cmdproc.annotations.CmdFlag;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

public class Test {

    private static @CmdFlag(longName = "sample1", shortName = 'a') boolean      sample1;
    private static @CmdFlag(shortName = 'b')                       boolean      sample2;
    private static                                                 boolean      dummy;
    private static                                                 List<String> others = new ArrayList<>();

    public Test() { }

    public int run(String... args) throws Exception {
        // CmdProc.processCommandLine(args, Test.class);

        System.out.println();

        Field f = Test.class.getDeclaredField("others");
        foo3(f.getType(), f.getGenericType());

        System.out.println("--------------------------------------------------------------------------------------");

        for(Method m : Test.class.getDeclaredMethods()) {
            if(m.getName().equals("setOthers")) {
                Parameter[] params = m.getParameters();
                Parameter   prm    = params[0];

                System.out.printf("Param Count: %d - %s%n", params.length, prm);
                foo3(prm.getType(), prm.getParameterizedType());
            }
        }

        System.out.println();

        return 0;
    }

    public static void main(String... args) {
        try {
            System.exit(new Test().run(args));
        }
        catch(Throwable t) {
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void foo(@NotNull ParameterizedType pType) {
        System.out.printf("Raw type: %s - ", pType.getRawType());
        Type ata = pType.getActualTypeArguments()[0];
        if(ata instanceof WildcardType wt) {
            System.out.printf("Type args: %s - %s%n", "?", wt.getUpperBounds()[0]);
        }
        else if(ata instanceof Class<?> clz) {
            System.out.printf("Type args: %s%n", clz);
        }
    }

    private static void foo2(@NotNull Class<?> ft) {
        System.out.printf("Class type: %s - %s%n", ft.getName(), ft.isAssignableFrom(ArrayList.class));
    }

    private static void foo3(@NotNull Class<?> ft, @NotNull Type gt) {
        foo2(ft);
        if(gt instanceof ParameterizedType pType) foo(pType);
    }

    private static void setOthers(List<? extends String> args) {

    }
}
