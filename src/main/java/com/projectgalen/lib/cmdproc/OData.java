package com.projectgalen.lib.cmdproc;
// ================================================================================================================================
//     PROJECT: PGJCmdProc
//    FILENAME: OData.java
//         IDE: IntelliJ IDEA
//      AUTHOR: Galen Rhodes
//        DATE: May 13, 2024
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

import com.projectgalen.lib.cmdproc.annotations.CmdOther;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static java.util.Optional.ofNullable;

public final class OData {
    private static final ResourceBundle msgs = ResourceBundle.getBundle("com.projectgalen.lib.cmdproc.messages");

    public final AccessibleObject accessibleObject;
    public final boolean          isArray;

    public OData(AccessibleObject accessibleObject, boolean isArray) {
        this.accessibleObject = accessibleObject;
        this.isArray          = isArray;
    }

    public @Override boolean equals(Object object) {
        return ((this == object) || ((object instanceof OData d) && (isArray == d.isArray) && Objects.equals(accessibleObject, d.accessibleObject)));
    }

    public @Override int hashCode() {
        return Objects.hash(accessibleObject, isArray);
    }

    public void setValues(@NotNull List<String> others) {
        // TODO: implement
    }

    public static void processOther(@NotNull List<OData> cmdLnOther, @NotNull Method m) {
        if(m.isAnnotationPresent(CmdOther.class)) {
            if(m.getParameterCount() == 1) {
                Parameter param = m.getParameters()[0];
                process(cmdLnOther, m, param.getType(), param.getParameterizedType(), msgs.getString("txt.method"));
            }
            else {
                String txt = (m.getParameterCount() == 0) ? msgs.getString("txt.no") : msgs.getString("txt.too_many");
                throw new IllegalArgumentException(msgs.getString("msg.err.wrong_param_count").formatted(txt, m));
            }
        }
    }

    public static void processOther(@NotNull List<OData> cmdLnOther, @NotNull AccessibleObject f, @NotNull Class<?> type, @NotNull Type genericType, @NotNull String aoType) {
        if(f.isAnnotationPresent(CmdOther.class)) process(cmdLnOther, f, type, genericType, aoType);
    }

    private static boolean canTakeArrayList(@NotNull Class<?> cls) {
        return cls.isAssignableFrom(ArrayList.class);
    }

    private static boolean isArrayListOfString(Class<?> cls, Type genericType) {
        return (canTakeArrayList(cls) && isStringParameterizedType(genericType));
    }

    private static boolean isArrayOfString(@NotNull Class<?> cls) {
        return (cls.isArray() && (cls.getComponentType() == String.class));
    }

    private static @NotNull Boolean isBoundsString(Type @NotNull [] bounds) {
        return ofNullable((bounds.length == 1) ? bounds[0] : null).map(c -> (c == String.class)).orElse(false);
    }

    private static boolean isGenTypeString(Type @NotNull [] a) {
        return ((a.length == 1) && ((a[0] instanceof WildcardType w) ? isWildCardString(w.getLowerBounds(), w.getUpperBounds()) : (a[0] == String.class)));
    }

    private static boolean isStringParameterizedType(@NotNull Type genericType) {
        return ((genericType instanceof ParameterizedType parameterizedType) && isGenTypeString(parameterizedType.getActualTypeArguments()));
    }

    private static @NotNull Boolean isWildCardString(Type @NotNull [] lowerBounds, Type @NotNull [] upperBounds) {
        return ofNullable((lowerBounds.length == 1) ? lowerBounds[0] : null).map(c -> (c == String.class)).orElseGet(() -> isBoundsString(upperBounds));
    }

    private static void process(@NotNull List<OData> cmdLnOther, @NotNull AccessibleObject f, @NotNull Class<?> type, @NotNull Type genericType, @NotNull String aoType) {
        FPData.validate(f, msgs.getString("txt.other"), aoType);
        if(isArrayOfString(type)) {
            cmdLnOther.add(new OData(f, true));
        }
        else if(isArrayListOfString(type, genericType)) {
            cmdLnOther.add(new OData(f, false));
        }
        else {
            throw new IllegalArgumentException(msgs.getString("msg.err.other_bad_type").formatted(aoType, f));
        }
    }
}
