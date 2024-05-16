package com.projectgalen.lib.cmdproc;
// ================================================================================================================================
//     PROJECT: PGJCmdProc
//    FILENAME: CmdProc.java
//         IDE: IntelliJ IDEA
//      AUTHOR: Galen Rhodes
//        DATE: April 25, 2024
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

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({ "UnusedReturnValue", "unused" })
public final class CmdProc {

    private static final ResourceBundle msgs         = ResourceBundle.getBundle("com.projectgalen.lib.cmdproc.messages");
    private static final Pattern        RX_LONG      = Pattern.compile("^\\s*--([^\\s=-][^\\s=]*)(?:\\s*(=)(.+))?\\s*$");
    private static final Pattern        RX_SHORT     = Pattern.compile("^\\s*-([^\\s-]\\S*)\\s*$");
    private static final String         HELP_LONG    = "--%s".formatted(msgs.getString("help.long"));
    private static final String         HELP_SHORT   = "-%s".formatted(msgs.getString("help.short"));
    private static final FPData[]       EMPTY_FPDATA = new FPData[0];

    private final List<String> others        = new ArrayList<>();
    private final String[]     args;
    private final CmdLnInfo    info;
    private final boolean      allowStdInRedirection;
    private       boolean      redirectStdIn = false;
    private       boolean      endOfSwitches = false;
    private       int          idx           = 0;

    public CmdProc(String @NotNull [] args, boolean allowStdInRedirection, Class<?> @NotNull ... classes) {
        this.args                  = args;
        this.allowStdInRedirection = allowStdInRedirection;
        this.info                  = CmdLnInfo.findAnnotatedMembers(classes);
    }

    /**
     * Prints the help page to STDERR.
     */
    public void displayHelp() {
        // TODO:
    }

    public boolean processCommandLine() {
        while(idx < args.length) {
            String rawArg = args[idx++];
            if(endOfSwitches) others.add(rawArg);
            else handler(rawArg.strip(), rawArg);
        }
        info.cmdLnOther().forEach(od -> od.setValues(others));
        return redirectStdIn;
    }

    private void handleFlagsAndParams(@NotNull String rawArg) {
        handleLong(RX_LONG.matcher(rawArg), rawArg);
    }

    /**
     * Prints the help page to STDERR and then terminates the process.
     * <B>THERE IS NO RETURNING FROM THIS METHOD. THE PROCESS WILL TERMINATE AFTER CALLING THIS METHOD.</B>
     */
    private void handleHelpSwitch() {
        displayHelp();
        System.exit(1);
    }

    private void handleLong(@NotNull Matcher matcher, @NotNull String rawArg) {
        if(matcher.matches()) {
            if("=".equals(matcher.group(2))) handleLongFP(matcher.group(1), matcher.group(3));
            else handleLongFP(matcher.group(1));
        }
        else {
            handleShort(RX_SHORT.matcher(rawArg), rawArg);
        }
    }

    private @NotNull String handleLongArg(@NotNull FPData fp) {
        if(fp.isFlag()) return fp.setValue(Boolean.TRUE.toString());
        if(idx < args.length) return fp.setValue(args[idx++]);
        return handleUserError(msgs.getString("msg.err.missing_arg").formatted(fp.getLongName()));
    }

    private void handleLongFP(@NotNull String name) {
        if(info.findLong(name).map(this::handleLongArg).findAny().isEmpty()) handleUserError(msgs.getString("msg.err.unknown_long").formatted(name));
    }

    private void handleLongFP(@NotNull String name, @NotNull String value) {
        if(info.findLong(name, false).map(fp -> fp.setValue(value)).findAny().isEmpty()) handleUserError(msgs.getString("msg.err.unknown_long").formatted(name));
    }

    private void handleRedirectionSwitch() {
        if(allowStdInRedirection) redirectStdIn = true;
        else handleUserError(msgs.getString("msg.err.cannon_redirect_stdin"));
    }

    private void handleShort(@NotNull Matcher matcher, @NotNull String rawArg) {
        if(matcher.matches()) {
            int[] cp = matcher.group(1).codePoints().toArray();
            int   i  = 0;

            while(i < cp.length) {
                int     ch  = cp[i++];
                boolean neg = ((i < cp.length) && (cp[i] == '-'));

                if(neg) ++i;
                handleShortFP(ch, neg);
            }
        }
        else {
            others.add(rawArg);
        }
    }

    private @NotNull String handleShortArg(@NotNull FPData fp, boolean neg) {
        if(fp.isFlag()) return fp.setValue((neg ? Boolean.FALSE : Boolean.TRUE).toString());
        if(idx < args.length) {
            String value = getArgument(fp.getShortNameStr());
            return fp.setValue(value);
        }
        return handleUserError(msgs.getString("msg.err.missing_arg").formatted(fp.getShortNameStr()));
    }

    private @NotNull String getArgument(String name) {
        String value = args[idx];
        if(RX_LONG.matcher(value).matches() || RX_SHORT.matcher(value).matches()) handleUserError(msgs.getString("msg.err.missing_arg").formatted(name));
        if(value.startsWith("\\-")) value = value.substring(1);
        ++idx;
        return value;
    }

    private void handleShortFP(int ch, boolean neg) {
        if(info.findShort(ch).map(fp -> handleShortArg(fp, neg)).findAny().isEmpty()) handleUserError(msgs.getString("msg.err.unknown_short").formatted(Character.toString(ch)));
    }

    /**
     * Print a message to stderr and terminate the process.
     * <B>THERE IS NO RETURNING FROM THIS METHOD. THE PROCESS WILL TERMINATE AFTER CALLING THIS METHOD.</B>
     *
     * @param msg The message to display.
     * @return The message - but in reality there is no returning from this method.
     */
    private String handleUserError(String msg) {
        System.err.println(msg);
        handleHelpSwitch();
        return msg;
    }

    private void handler(String arg, String rawArg) {
        if("--".equals(arg)) {
            endOfSwitches = true;
        }
        else if("-".equals(arg)) {
            handleRedirectionSwitch();
        }
        else if(HELP_LONG.equals(arg) || HELP_SHORT.equals(arg)) {
            handleHelpSwitch();
        }
        else {
            handleFlagsAndParams(rawArg.startsWith("\\-") ? rawArg.substring(1) : rawArg);
        }
    }

    public static boolean processCommandLine(String @NotNull [] args, boolean allowStdInRedirection, Class<?> @NotNull ... classes) {
        return new CmdProc(args, allowStdInRedirection, classes).processCommandLine();
    }
}
