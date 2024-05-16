package com.projectgalen.lib.cmdproc;
// ================================================================================================================================
//     PROJECT: PGJCmdProc
//    FILENAME: FPData.java
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
import com.projectgalen.lib.cmdproc.annotations.CmdParam;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public final class FPData {
    private static final ResourceBundle msgs = ResourceBundle.getBundle("com.projectgalen.lib.cmdproc.messages");

    public final boolean          isFlag;
    public final boolean          required;
    public final String           longName;
    public final int              shortName;
    public final String           documentation;
    public final String[]         allowedValues;
    public final AccessibleObject accessibleObject;
    public final boolean          hasLongName;
    public final boolean          hasShortName;

    private boolean notSet = true;

    public FPData(@NotNull CmdFlag flag, @NotNull AccessibleObject ao) {
        isFlag           = true;
        required         = flag.required();
        longName         = flag.longName().trim();
        shortName        = flag.shortName();
        documentation    = flag.documentation().trim();
        allowedValues    = new String[] { Boolean.TRUE.toString(), Boolean.FALSE.toString() };
        accessibleObject = ao;
        hasLongName      = !longName.isEmpty();
        hasShortName     = (shortName != 0);
    }

    public FPData(@NotNull CmdParam param, @NotNull AccessibleObject ao) {
        isFlag           = false;
        required         = param.required();
        longName         = param.longName().trim();
        shortName        = param.shortName();
        documentation    = param.documentation().trim();
        allowedValues    = param.allowedValues();
        accessibleObject = ao;
        hasLongName      = !longName.isEmpty();
        hasShortName     = (shortName != 0);
    }

    public @Override boolean equals(Object object) {/*@f0*/
        return ((this == object)
                || ((object instanceof FPData fpData)
                    && ((isFlag == fpData.isFlag)
                        && (required == fpData.required)
                        && (shortName == fpData.shortName)
                        && Objects.equals(longName, fpData.longName)
                        && Objects.equals(documentation, fpData.documentation)
                        && Objects.deepEquals(allowedValues, fpData.allowedValues)
                        && Objects.equals(accessibleObject, fpData.accessibleObject))));
    }/*@f1*/

    public AccessibleObject getAccessibleObject()   { return accessibleObject; }

    public String[] getAllowedValues()              { return allowedValues; }

    public String getDocumentation()                { return documentation; }

    public String getLongName()                     { return longName; }

    public int getShortName()                       { return shortName; }

    public @NotNull String getShortNameStr()        { return Character.toString(shortName); }

    public boolean hasLongName()                    { return hasLongName; }

    public boolean hasShortName()                   { return hasShortName; }

    public @Override int hashCode()                 { return Objects.hash(isFlag, required, longName, shortName, documentation, Arrays.hashCode(allowedValues), accessibleObject); }

    public boolean isFlag()                         { return isFlag; }

    public boolean isLongName(@NotNull String name) { return name.equals(longName); }

    public boolean isParam()                        { return !isFlag; }

    public boolean isRequired()                     { return required; }

    public boolean isShortName(int name)            { return (name == shortName); }

    public boolean notSet()                         { return notSet; }

    public @NotNull String setValue(@NotNull String value) {
        // TODO: Implement.
        notSet = false;
        return value;
    }

    private void process(@NotNull List<FPData> data, @NotNull String kind, @NotNull String aoType) {
        validate(accessibleObject, kind, aoType);
        if(!(hasLongName || hasShortName)) throw new IllegalArgumentException(msgs.getString("msg.err.needs_name").formatted(kind, aoType, accessibleObject));
        data.add(this);
    }

    public static void process(@NotNull List<FPData> cmdLnData, @NotNull AccessibleObject ao, @NotNull String aoType) {
        CmdFlag  cf = ao.getAnnotation(CmdFlag.class);
        CmdParam cp = ao.getAnnotation(CmdParam.class);

        if((cf != null) && (cp != null)) throw new IllegalArgumentException(msgs.getString("msg.err.cannot_be_both").formatted(aoType, ao));
        else if(cf != null) new FPData(cf, ao).process(cmdLnData, msgs.getString("txt.cap.flag"), aoType);
        else if(cp != null) new FPData(cp, ao).process(cmdLnData, msgs.getString("txt.cap.param"), aoType);
    }

    public static void validate(@NotNull AccessibleObject accessibleObject, @NotNull String kind, @NotNull String aoType) {
        if(accessibleObject instanceof Field f) {
            if((f.getModifiers() & Modifier.STATIC) != Modifier.STATIC) throw new IllegalArgumentException(msgs.getString("msg.err.not_static").formatted(kind, aoType, accessibleObject));
            if((f.getModifiers() & Modifier.FINAL) == Modifier.FINAL) throw new IllegalArgumentException(msgs.getString("msg.err.is_final").formatted(kind, aoType, accessibleObject));
        }
        else if((accessibleObject instanceof Method m) && ((m.getModifiers() & Modifier.STATIC) != Modifier.STATIC)) {
            throw new IllegalArgumentException(msgs.getString("msg.err.not_static").formatted(kind, aoType, accessibleObject));
        }
    }
}
