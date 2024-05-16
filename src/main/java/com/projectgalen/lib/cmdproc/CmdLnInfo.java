package com.projectgalen.lib.cmdproc;
// ================================================================================================================================
//     PROJECT: PGJCmdProc
//    FILENAME: CmdLnInfo.java
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

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.projectgalen.lib.utils.stream.Streams.intersection;
import static java.util.stream.Stream.concat;

public record CmdLnInfo(List<FPData> cmdLnData, List<OData> cmdLnOther) {

    private static final ResourceBundle msgs       = ResourceBundle.getBundle("com.projectgalen.lib.cmdproc.messages");
    private static final String         TXT_METHOD = msgs.getString("txt.method");
    private static final String         TXT_FIELD  = msgs.getString("txt.field");

    public @NotNull Stream<FPData> streamData() {
        return cmdLnData.stream();
    }

    public @NotNull Stream<FPData> streamData(String name, boolean isLong, boolean isFlag) {
        return (isLong ? streamLongNames(isFlag) : streamShortNames(isFlag)).filter(d -> name.equals(isLong ? d.getLongName() : d.getShortNameStr()));
    }

    public @NotNull Stream<FPData> findLong(@NotNull String name, boolean isFlag) {
        return streamData().filter(fp -> ((fp.isFlag() == isFlag) && fp.hasLongName() && name.equals(fp.getLongName())));
    }

    public @NotNull Stream<FPData> findLong(@NotNull String name) {
        return streamData().filter(fp -> (fp.hasLongName() && name.equals(fp.getLongName())));
    }

    public @NotNull Stream<FPData> findShort(int ch) {
        return streamData().filter(fp -> (fp.hasShortName() && (fp.getShortName() == ch)));
    }

    public @NotNull Stream<FPData> findShort(int ch, boolean isFlag) {
        return streamData().filter(fp -> ((fp.isFlag() == isFlag) && fp.hasShortName() && (fp.getShortName() == ch)));
    }

    public @NotNull Stream<FPData> streamData(boolean isFlag) {
        return streamData().filter(d -> (d.isFlag() == isFlag));
    }

    public @NotNull Stream<FPData> streamLongNames() {
        return streamData().filter(FPData::hasLongName);
    }

    public @NotNull Stream<FPData> streamLongNames(boolean isFlag) {
        return streamData(isFlag).filter(FPData::hasLongName);
    }

    public @NotNull Stream<FPData> streamShortNames() {
        return streamData().filter(FPData::hasShortName);
    }

    public @NotNull Stream<FPData> streamShortNames(boolean isFlag) {
        return streamData(isFlag).filter(FPData::hasShortName);
    }

    private @NotNull Stream<String> getLongNameConflicts() {
        return intersection(streamLongNames(true).map(FPData::getLongName), streamLongNames(false).map(FPData::getLongName));
    }

    private @NotNull Stream<String> getShortNameConflicts() {
        return intersection(streamShortNames(true).map(FPData::getShortNameStr), streamShortNames(false).map(FPData::getShortNameStr));
    }

    private @NotNull CmdLnInfo validate() {
        String tmp = msgs.getString("msg.err.switch_and_value");
        String msg = concat(getLongNameConflicts().map("--%s"::formatted), getShortNameConflicts().map("-%s"::formatted)).map(tmp::formatted).collect(Collectors.joining("\n"));
        if(!msg.isEmpty()) throw new IllegalArgumentException(msg);
        return this;
    }

    public static @NotNull CmdLnInfo findAnnotatedMembers(Class<?> @NotNull [] classes) {
        List<FPData> data   = new ArrayList<>();
        List<OData>  others = new ArrayList<>();
        for(Class<?> cls : classes) findAnnotatedMembers(data, others, cls);
        return new CmdLnInfo(data, others).validate();
    }

    private static void findAnnotatedMembers(@NotNull List<FPData> cmdLnData, @NotNull List<OData> cmdLnOther, @NotNull Class<?> cls) {
        while(cls != null) {
            // First look for flags and parameters...
            for(AccessibleObject f : cls.getDeclaredFields()) FPData.process(cmdLnData, f, TXT_FIELD);
            for(AccessibleObject m : cls.getDeclaredMethods()) FPData.process(cmdLnData, m, TXT_METHOD);

            // Now look for "others"...
            for(Field f : cls.getDeclaredFields()) OData.processOther(cmdLnOther, f, f.getType(), f.getGenericType(), TXT_FIELD);
            for(Method m : cls.getDeclaredMethods()) OData.processOther(cmdLnOther, m);

            cls = cls.getSuperclass();
        }
    }
}
